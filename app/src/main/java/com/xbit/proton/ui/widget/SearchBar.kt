package com.xbit.proton.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.xbit.proton.R

class SearchBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var onSendListener: ((String) -> Unit)? = null

    private val etInput: EditText
    private val btnSend: ImageButton

    init {
        LayoutInflater.from(context).inflate(R.layout.view_search_bar, this, true)
        etInput = findViewById(R.id.etMessageInput)
        btnSend = findViewById(R.id.btnSend)

        btnSend.setOnClickListener { send() }
        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { send(); true } else false
        }
    }

    private fun send() {
        val text = etInput.text.toString().trim()
        if (text.isEmpty()) return
        onSendListener?.invoke(text)
        etInput.text?.clear()
    }
}
