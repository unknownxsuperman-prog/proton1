package com.xbit.proton.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xbit.proton.R
import com.xbit.proton.data.model.CardType
import com.xbit.proton.data.model.Message
import com.xbit.proton.data.model.MessageRole
import com.xbit.proton.ui.viewmodel.ChatViewModel
import com.xbit.proton.ui.widget.SearchBar

class ChatFragment : Fragment() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchBar: SearchBar
    private lateinit var emptyHero: View
    private lateinit var typingIndicator: View
    private lateinit var pillContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ChatViewModel::class.java]

        recyclerView = view.findViewById(R.id.rvMessages)
        searchBar = view.findViewById(R.id.searchBar)
        emptyHero = view.findViewById(R.id.emptyHero)
        typingIndicator = view.findViewById(R.id.typingIndicator)
        pillContainer = view.findViewById(R.id.pillContainer)

        adapter = ChatAdapter(viewModel)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        searchBar.onSendListener = { text -> viewModel.sendMessage(text) }

        // Quick-action pills
        setupPills()

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.submitList(messages.toList())
            emptyHero.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (messages.isEmpty()) View.GONE else View.VISIBLE
            if (messages.isNotEmpty()) {
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }

        viewModel.isTyping.observe(viewLifecycleOwner) { typing ->
            typingIndicator.visibility = if (typing) View.VISIBLE else View.GONE
        }
    }

    private fun setupPills() {
        val pills = listOf(
            getString(R.string.pill_rank) to CardType.RANK_FORM,
            getString(R.string.pill_college) to CardType.COLLEGE_FORM,
            getString(R.string.pill_priority) to CardType.PRIORITY_FORM
        )
        pills.forEach { (label, cardType) ->
            val pill = layoutInflater.inflate(R.layout.item_pill, pillContainer, false)
            pill.findViewById<TextView>(R.id.tvPill).text = label
            pill.setOnClickListener {
                val text = label.replace(Regex("^[^a-zA-Z]+"), "")
                viewModel.sendMessage(text)
            }
            pillContainer.addView(pill)
        }
    }
}
