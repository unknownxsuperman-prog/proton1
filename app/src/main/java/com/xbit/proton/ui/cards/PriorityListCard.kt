package com.xbit.proton.ui.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.xbit.proton.R
import com.xbit.proton.ui.viewmodel.ChatViewModel

class PriorityListCard @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var viewModel: ChatViewModel? = null

    private val btnUpload: Button
    private val etRank: EditText
    private val spinnerCategory: Spinner
    private val spinnerDataset: Spinner
    private val btnMatch: Button
    private val tvStatus: TextView

    private val categories = listOf(
        "GM", "SC", "ST", "OBC", "2A", "2B", "3A", "3B",
        "HK_GM", "HK_SC", "HK_ST", "HK_2A", "HK_2B"
    )
    private val datasets = listOf("Engineering (General)", "Engineering (HK Region)")

    init {
        LayoutInflater.from(context).inflate(R.layout.card_priority_list, this, true)
        btnUpload       = findViewById(R.id.btnUploadPdf)
        etRank          = findViewById(R.id.etPriorityRank)
        spinnerCategory = findViewById(R.id.spinnerPriorityCategory)
        spinnerDataset  = findViewById(R.id.spinnerPriorityDataset)
        btnMatch        = findViewById(R.id.btnMatchPriority)
        tvStatus        = findViewById(R.id.tvUploadStatus)

        spinnerCategory.adapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerDataset.adapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_dropdown_item, datasets)

        btnUpload.setOnClickListener {
            tvStatus.text = "Picking PDF…"
            viewModel?.triggerPdfPick()
        }
        btnMatch.setOnClickListener { match() }
    }

    private fun match() {
        val rank = etRank.text.toString().toIntOrNull()
            ?: run { Toast.makeText(context, "Enter your rank", Toast.LENGTH_SHORT).show(); return }
        val category = categories[spinnerCategory.selectedItemPosition]
        val dataset = datasets[spinnerDataset.selectedItemPosition]
        viewModel?.matchPriorityList(rank, category, dataset)
    }
}
