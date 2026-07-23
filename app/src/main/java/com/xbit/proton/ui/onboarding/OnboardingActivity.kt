package com.xbit.proton.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.xbit.proton.MainActivity
import com.xbit.proton.R
import com.xbit.proton.util.StorageManager
import com.xbit.proton.util.ThemeManager

class OnboardingActivity : AppCompatActivity() {

    private lateinit var storage: StorageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = StorageManager(this)

        // Apply persisted theme
        ThemeManager.apply(storage.isDarkMode)

        // Skip onboarding if already done
        if (storage.isOnboarded) {
            launchMain()
            return
        }

        setContentView(R.layout.activity_onboarding)

        val etName = findViewById<EditText>(R.id.etName)
        val btnStart = findViewById<Button>(R.id.btnStart)

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            storage.username = name
            storage.isOnboarded = true
            launchMain()
        }
    }

    private fun launchMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
