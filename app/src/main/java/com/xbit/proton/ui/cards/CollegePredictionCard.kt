package com.xbit.proton.ui.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.xbit.proton.R
import com.xbit.proton.ui.viewmodel.ChatViewModel

class CollegePredictionCard @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var viewModel: ChatViewModel? = null

    private val etRank: EditText
    private val spinnerCategory: Spinner
    private val etBranch: AutoCompleteTextView
    private val spinnerDataset: Spinner
    private val btnSearch: Button

    private val categories = listOf(
        "GM", "SC", "ST", "OBC", "2A", "2B", "3A", "3B",
        "HK_GM", "HK_SC", "HK_ST", "HK_2A", "HK_2B"
    )
    private val datasets = listOf("Engineering (General)", "Engineering (HK Region)")

    init {
        LayoutInflater.from(context).inflate(R.layout.card_college_prediction, this, true)
        etRank          = findViewById(R.id.etRank)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etBranch        = findViewById(R.id.etBranch)
        spinnerDataset  = findViewById(R.id.spinnerDataset)
        btnSearch       = findViewById(R.id.btnSearchColleges)

        spinnerCategory.adapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerDataset.adapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_dropdown_item, datasets)

        btnSearch.setOnClickListener { search() }
    }

    private fun search() {
        val rank = etRank.text.toString().toIntOrNull()
            ?: run { Toast.makeText(context, "Enter your rank", Toast.LENGTH_SHORT).show(); return }
        val category = categories[spinnerCategory.selectedItemPosition]
        val branch = etBranch.text.toString().trimEnd()
        val dataset = datasets[spinnerDataset.selectedItemPosition]
        viewModel?.searchColleges(rank, category, branch.ifBlank { null }, dataset)
    }
}
