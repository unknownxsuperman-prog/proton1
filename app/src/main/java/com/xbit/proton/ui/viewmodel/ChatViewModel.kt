package com.xbit.proton.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xbit.proton.data.model.*
import com.xbit.proton.data.repository.CollegeRepository
import com.xbit.proton.engine.CollegeMatcher
import com.xbit.proton.engine.KcetRankPredictor
import com.xbit.proton.engine.NlpEngine
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel : ViewModel() {

    // ─── State ────────────────────────────────────────────────────────────────

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isTyping = MutableLiveData(false)
    val isTyping: LiveData<Boolean> = _isTyping

    private val _pickPdfEvent = MutableLiveData(false)
    val pickPdfEvent: LiveData<Boolean> = _pickPdfEvent

    // Slot-fill memory across turns
    private val pendingEntities = mutableMapOf<String, Any>()

    // NLP engine (lazy-initialised after training data loads)
    private var nlpEngine: NlpEngine? = null

    // ─── Chat management ──────────────────────────────────────────────────────

    private val allChats = mutableListOf<Chat>()
    private var currentChat: Chat = newChat()

    private fun newChat(): Chat = Chat(id = UUID.randomUUID().toString(), title = "New Chat")

    fun startNewChat() {
        if (currentChat.messages.isNotEmpty()) allChats.add(0, currentChat)
        currentChat = newChat()
        pendingEntities.clear()
        _messages.value = emptyList()
    }

    fun getAllChats(): List<Chat> = allChats.toList()

    fun deleteChat(chatId: String) {
        allChats.removeAll { it.id == chatId }
    }

    // ─── Message sending ──────────────────────────────────────────────────────

    fun sendMessage(text: String) {
        val userMsg = Message(role = MessageRole.USER, text = text)
        addMessage(userMsg)
        processInput(text)
    }

    private fun addMessage(msg: Message) {
        val current = _messages.value?.toMutableList() ?: mutableListOf()
        current.add(msg)
        _messages.value = current
        currentChat.messages.add(msg)
        if (currentChat.title == "New Chat" && currentChat.messages.size == 1) {
            currentChat = currentChat.copy(title = msg.text.take(40))
        }
    }

    // ─── Rank card callback ───────────────────────────────────────────────────

    fun onRankCardResult(rank: Int) {
        pendingEntities["rank"] = rank
        addMessage(
            Message(
                role = MessageRole.ASSISTANT,
                text = "Your estimated KCET rank is **$rank**. Want me to predict colleges for this rank?",
                cardType = CardType.RANK_RESULT
            )
        )
    }

    // ─── College search ───────────────────────────────────────────────────────

    fun searchColleges(rank: Int, category: String, branch: String?, dataset: String) {
        _isTyping.value = true
        viewModelScope.launch {
            try {
                val aliases = CollegeRepository.getBranchAliases()
                val matcher = CollegeMatcher(aliases)
                val colleges = if (dataset.contains("HK", ignoreCase = true))
                    CollegeRepository.getEngineeringHkColleges()
                else
                    CollegeRepository.getEngineeringColleges()

                val results = matcher.findMatches(colleges, rank, category, branch, 100)
                _isTyping.value = false
                addMessage(
                    Message(
                        role = MessageRole.ASSISTANT,
                        text = "__COLLEGE_RESULTS__",
                        cardType = CardType.COLLEGE_RESULTS
                    ).copy(text = "__COLLEGE_RESULTS__:${resultsSummary(results)}")
                )
                _collegeResults.value = results
            } catch (e: Exception) {
                _isTyping.value = false
                addMessage(
                    Message(role = MessageRole.ASSISTANT,
                        text = "Sorry, I couldn't fetch college data right now. Please check your internet connection.")
                )
            }
        }
    }

    private val _collegeResults = MutableLiveData<List<CollegeResult>>()
    val collegeResults: LiveData<List<CollegeResult>> = _collegeResults

    private fun resultsSummary(results: List<CollegeResult>): String =
        if (results.isEmpty()) "No colleges found for this rank/category combination."
        else "Found ${results.size} eligible colleges."

    // ─── PDF parsed callback ──────────────────────────────────────────────────

    fun onPdfParsed(options: List<PriorityOption>) {
        if (options.isEmpty()) {
            addMessage(
                Message(
                    role = MessageRole.ASSISTANT,
                    text = "I couldn't extract any priority options from that PDF. Please make sure it's a valid KEA Option Entry PDF."
                )
            )
            return
        }
        pendingEntities["priorityOptions"] = options
        addMessage(
            Message(
                role = MessageRole.ASSISTANT,
                text = "Got it! I found **${options.size} priority options** in your PDF. Now enter your rank and category to check eligibility.",
                cardType = CardType.PRIORITY_FORM
            )
        )
    }

    fun onPdfPickConsumed() {
        _pickPdfEvent.value = false
    }

    fun triggerPdfPick() {
        _pickPdfEvent.value = true
    }

    // ─── Priority list matching ───────────────────────────────────────────────

    fun matchPriorityList(rank: Int, category: String, dataset: String) {
        @Suppress("UNCHECKED_CAST")
        val options = pendingEntities["priorityOptions"] as? List<PriorityOption>
        if (options == null) {
            addMessage(Message(role = MessageRole.ASSISTANT,
                text = "Please upload your priority list PDF first."))
            return
        }
        _isTyping.value = true
        viewModelScope.launch {
            try {
                val aliases = CollegeRepository.getBranchAliases()
                val matcher = CollegeMatcher(aliases)
                val colleges = if (dataset.contains("HK", ignoreCase = true))
                    CollegeRepository.getEngineeringHkColleges()
                else
                    CollegeRepository.getEngineeringColleges()
                val matched = matcher.matchPriorityOptions(options, colleges, rank, category)
                _isTyping.value = false
                val eligible = matched.count { it.second != null && (it.second?.distance ?: -1.0) >= 0 }
                addMessage(
                    Message(role = MessageRole.ASSISTANT,
                        text = "Out of ${options.size} priorities, you're eligible for **$eligible** colleges based on rank $rank ($category).")
                )
            } catch (e: Exception) {
                _isTyping.value = false
                addMessage(Message(role = MessageRole.ASSISTANT,
                    text = "Error fetching data. Please check your connection."))
            }
        }
    }

    // ─── NLP routing ─────────────────────────────────────────────────────────

    private fun processInput(text: String) {
        _isTyping.value = true
        viewModelScope.launch {
            try {
                // Ensure NLP engine is loaded
                if (nlpEngine == null) {
                    val training = CollegeRepository.getTrainingData()
                    nlpEngine = NlpEngine(training)
                }
                val engine = nlpEngine!!
                val intent = engine.classifyIntent(text)
                val entities = engine.extractEntities(text)

                // Merge extracted entities into pending slot memory
                entities.rank?.let { pendingEntities["rank"] = it }
                entities.kcetMarks?.let { pendingEntities["kcetMarks"] = it }
                entities.category?.let { pendingEntities["category"] = it }
                entities.branch?.let { pendingEntities["branch"] = it }

                _isTyping.value = false

                when (intent) {
                    NlpEngine.Intent.RANK_PREDICTION -> {
                        addMessage(Message(role = MessageRole.ASSISTANT,
                            text = "Sure! Let me show you the rank predictor.",
                            cardType = CardType.RANK_FORM))
                    }
                    NlpEngine.Intent.COLLEGE_PREDICTION -> {
                        addMessage(Message(role = MessageRole.ASSISTANT,
                            text = "Let's find colleges for you!",
                            cardType = CardType.COLLEGE_FORM))
                    }
                    NlpEngine.Intent.PRIORITY_LIST -> {
                        addMessage(Message(role = MessageRole.ASSISTANT,
                            text = "Upload your KEA Option Entry PDF and I'll check your eligibility.",
                            cardType = CardType.PRIORITY_FORM))
                    }
                    NlpEngine.Intent.GREETING -> {
                        addMessage(Message(role = MessageRole.ASSISTANT,
                            text = "Hello! 👋 I'm Proton, your KCET assistant. I can predict your rank, find eligible colleges, or check your option entry list. What would you like to do?"))
                    }
                    NlpEngine.Intent.HELP -> {
                        addMessage(Message(role = MessageRole.ASSISTANT,
                            text = "Here's what I can do:\n• **Rank Prediction** — estimate your KCET rank from your marks\n• **College Prediction** — find colleges based on rank, category & branch\n• **Priority List Check** — upload your KEA PDF and check eligibility\n\nJust ask or tap a pill below!"))
                    }
                    NlpEngine.Intent.UNKNOWN -> {
                        addMessage(Message(role = MessageRole.ASSISTANT,
                            text = "I'm not sure what you mean. I can help with KCET rank prediction, college search, or checking your option entry PDF. Try asking one of those!"))
                    }
                }
            } catch (e: Exception) {
                _isTyping.value = false
                addMessage(Message(role = MessageRole.ASSISTANT,
                    text = "Hmm, something went wrong. Please try again."))
            }
        }
    }
}
