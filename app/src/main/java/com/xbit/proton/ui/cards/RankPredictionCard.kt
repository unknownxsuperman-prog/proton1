package com.xbit.proton.ui.cards

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.xbit.proton.R
import com.xbit.proton.engine.KcetRankPredictor
import com.xbit.proton.ui.viewmodel.ChatViewModel

/**
 * Inline card for rank prediction. Supports two modes:
 *  • Marks mode  — user enters PCM marks (out of 60 each) or total
 *  • Percent mode — user enters 12th board % + board name
 */
class RankPredictionCard @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var viewModel: ChatViewModel? = null

    private val etPhysics: EditText
    private val etChemistry: EditText
    private val etMaths: EditText
    private val etTotal: EditText
    private val etPercent: EditText
    private val etBoard: EditText
    private val tabMarks: TextView
    private val tabTotal: TextView
    private val tabPercent: TextView
    private val btnPredict: Button
    private val marksGroup: LinearLayout
    private val totalGroup: LinearLayout
    private val percentGroup: LinearLayout

    private var mode = MODE_MARKS

    init {
        LayoutInflater.from(context).inflate(R.layout.card_rank_prediction, this, true)
        etPhysics   = findViewById(R.id.etPhysics)
        etChemistry = findViewById(R.id.etChemistry)
        etMaths     = findViewById(R.id.etMaths)
        etTotal     = findViewById(R.id.etTotal)
        etPercent   = findViewById(R.id.etPercent)
        etBoard     = findViewById(R.id.etBoard)
        tabMarks    = findViewById(R.id.tabMarks)
        tabTotal    = findViewById(R.id.tabTotal)
        tabPercent  = findViewById(R.id.tabPercent)
        btnPredict  = findViewById(R.id.btnPredictRank)
        marksGroup  = findViewById(R.id.groupMarks)
        totalGroup  = findViewById(R.id.groupTotal)
        percentGroup= findViewById(R.id.groupPercent)

        tabMarks.setOnClickListener   { setMode(MODE_MARKS) }
        tabTotal.setOnClickListener   { setMode(MODE_TOTAL) }
        tabPercent.setOnClickListener { setMode(MODE_PERCENT) }
        setMode(MODE_MARKS)

        btnPredict.setOnClickListener { predict() }
    }

    private fun setMode(m: Int) {
        mode = m
        marksGroup.visibility  = if (m == MODE_MARKS)  VISIBLE else GONE
        totalGroup.visibility  = if (m == MODE_TOTAL)  VISIBLE else GONE
        percentGroup.visibility= if (m == MODE_PERCENT) VISIBLE else GONE
        tabMarks.isSelected    = m == MODE_MARKS
        tabTotal.isSelected    = m == MODE_TOTAL
        tabPercent.isSelected  = m == MODE_PERCENT
    }

    private fun predict() {
        val marks: Double = when (mode) {
            MODE_MARKS -> {
                val p = etPhysics.text.toString().toDoubleOrNull() ?: return showError("Enter Physics marks")
                val c = etChemistry.text.toString().toDoubleOrNull() ?: return showError("Enter Chemistry marks")
                val m = etMaths.text.toString().toDoubleOrNull() ?: return showError("Enter Maths marks")
                p + c + m
            }
            MODE_TOTAL -> etTotal.text.toString().toDoubleOrNull()
                ?: return showError("Enter total KCET marks")
            MODE_PERCENT -> {
                val pct = etPercent.text.toString().toDoubleOrNull()
                    ?: return showError("Enter board percentage")
                val board = etBoard.text.toString().ifBlank { "Karnataka" }
                KcetRankPredictor.boardPercentToKcetEquiv(pct, board)
            }
            else -> return
        }
        val rank = KcetRankPredictor.estimateRank(marks)
        viewModel?.onRankCardResult(rank)
    }

    private fun showError(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MODE_MARKS   = 0
        private const val MODE_TOTAL   = 1
        private const val MODE_PERCENT = 2
    }
}
