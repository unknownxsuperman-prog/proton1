package com.xbit.proton.ui.chat

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xbit.proton.R
import com.xbit.proton.data.model.CardType
import com.xbit.proton.data.model.Message
import com.xbit.proton.data.model.MessageRole
import com.xbit.proton.ui.cards.AllCollegesActivity
import com.xbit.proton.ui.cards.CollegePredictionCard
import com.xbit.proton.ui.cards.PriorityListCard
import com.xbit.proton.ui.cards.RankPredictionCard
import com.xbit.proton.ui.viewmodel.ChatViewModel

private const val VT_USER       = 0
private const val VT_ASSISTANT  = 1
private const val VT_RANK_FORM  = 2
private const val VT_COLLEGE_FORM = 3
private const val VT_PRIORITY_FORM = 4
private const val VT_RANK_RESULT = 5
private const val VT_COLLEGE_RESULTS = 6

class ChatAdapter(private val viewModel: ChatViewModel) :
    ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiff) {

    override fun getItemViewType(position: Int): Int = when {
        getItem(position).role == MessageRole.USER -> VT_USER
        getItem(position).cardType == CardType.RANK_FORM -> VT_RANK_FORM
        getItem(position).cardType == CardType.COLLEGE_FORM -> VT_COLLEGE_FORM
        getItem(position).cardType == CardType.PRIORITY_FORM -> VT_PRIORITY_FORM
        getItem(position).cardType == CardType.RANK_RESULT -> VT_RANK_RESULT
        getItem(position).cardType == CardType.COLLEGE_RESULTS -> VT_COLLEGE_RESULTS
        else -> VT_ASSISTANT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        fun inflate(id: Int) = LayoutInflater.from(parent.context).inflate(id, parent, false)
        return when (viewType) {
            VT_USER            -> UserVH(inflate(R.layout.item_message_user))
            VT_RANK_FORM       -> RankFormVH(inflate(R.layout.item_rank_form))
            VT_COLLEGE_FORM    -> CollegeFormVH(inflate(R.layout.item_college_form))
            VT_PRIORITY_FORM   -> PriorityFormVH(inflate(R.layout.item_priority_form))
            VT_RANK_RESULT     -> RankResultVH(inflate(R.layout.item_rank_result))
            VT_COLLEGE_RESULTS -> CollegeResultsVH(inflate(R.layout.item_college_results))
            else               -> AssistantVH(inflate(R.layout.item_message_assistant))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserVH            -> holder.bind(msg)
            is AssistantVH       -> holder.bind(msg)
            is RankFormVH        -> holder.bind(viewModel)
            is CollegeFormVH     -> holder.bind(viewModel)
            is PriorityFormVH    -> holder.bind(viewModel)
            is RankResultVH      -> holder.bind(msg, viewModel)
            is CollegeResultsVH  -> holder.bind(viewModel)
        }
    }

    // ─── ViewHolders ──────────────────────────────────────────────────────────

    class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText = view.findViewById<TextView>(R.id.tvUserMessage)
        fun bind(msg: Message) { tvText.text = msg.text }
    }

    class AssistantVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvText = view.findViewById<TextView>(R.id.tvAssistantMessage)
        fun bind(msg: Message) { tvText.text = msg.text }
    }

    class RankFormVH(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<RankPredictionCard>(R.id.rankPredictionCard)
        fun bind(vm: ChatViewModel) { card.viewModel = vm }
    }

    class CollegeFormVH(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<CollegePredictionCard>(R.id.collegePredictionCard)
        fun bind(vm: ChatViewModel) { card.viewModel = vm }
    }

    class PriorityFormVH(view: View) : RecyclerView.ViewHolder(view) {
        private val card = view.findViewById<PriorityListCard>(R.id.priorityListCard)
        fun bind(vm: ChatViewModel) { card.viewModel = vm }
    }

    class RankResultVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvRank = view.findViewById<TextView>(R.id.tvRankValue)
        private val btnSearch = view.findViewById<Button>(R.id.btnSearchColleges)
        private val btnPredict = view.findViewById<Button>(R.id.btnPredictCollege)
        fun bind(msg: Message, vm: ChatViewModel) {
            val rank = msg.text.filter { it.isDigit() }.toIntOrNull() ?: 0
            tvRank.text = if (rank > 0) rank.toString() else "—"
            btnSearch.setOnClickListener { vm.sendMessage("Search colleges for rank $rank") }
            btnPredict.setOnClickListener { vm.sendMessage("Predict colleges for rank $rank") }
        }
    }

    class CollegeResultsVH(view: View) : RecyclerView.ViewHolder(view) {
        private val container = view.findViewById<ViewGroup>(R.id.topCollegesContainer)
        private val btnSeeAll = view.findViewById<Button>(R.id.btnSeeAll)
        fun bind(vm: ChatViewModel) {
            vm.collegeResults.value?.let { results ->
                container.removeAllViews()
                results.take(5).forEach { result ->
                    val row = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_college_card, container, false)
                    row.findViewById<TextView>(R.id.tvCollegeName).text = result.college.colgname
                    row.findViewById<TextView>(R.id.tvBranch).text = result.matchedBranch
                    row.findViewById<TextView>(R.id.tvCutoff).text = "Cutoff: ${result.college.cutoff}"
                    row.findViewById<TextView>(R.id.tvLocation).text = result.college.place
                    row.findViewById<TextView>(R.id.tvCollegeCode).text = result.college.colgcode
                    container.addView(row)
                }
                btnSeeAll.text = itemView.context.getString(R.string.see_all, results.size)
                btnSeeAll.setOnClickListener {
                    val intent = Intent(itemView.context, AllCollegesActivity::class.java)
                    intent.putParcelableArrayListExtra("results", ArrayList(results))
                    itemView.context.startActivity(intent)
                }
            }
        }
    }

    companion object {
        val MessageDiff = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(a: Message, b: Message) = a.id == b.id
            override fun areContentsTheSame(a: Message, b: Message) = a == b
        }
    }
}
