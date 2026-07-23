package com.xbit.proton.ui.cards

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.xbit.proton.R
import com.xbit.proton.data.model.CollegeResult

class AllCollegesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_colleges)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        @Suppress("DEPRECATION")
        val results: List<CollegeResult> =
            intent.getParcelableArrayListExtra("results") ?: emptyList()

        supportActionBar?.title = "${results.size} Colleges"

        val container = findViewById<LinearLayout>(R.id.collegesContainer)
        results.forEach { result ->
            val card = layoutInflater.inflate(R.layout.item_college_card, container, false)
            card.findViewById<TextView>(R.id.tvCollegeName).text = result.college.colgname
            card.findViewById<TextView>(R.id.tvBranch).text = result.matchedBranch
            card.findViewById<TextView>(R.id.tvCutoff).text = "Cutoff: ${result.college.cutoff}"
            card.findViewById<TextView>(R.id.tvLocation).text = result.college.place
            card.findViewById<TextView>(R.id.tvCollegeCode).text = result.college.colgcode
            container.addView(card)
        }
    }
}
