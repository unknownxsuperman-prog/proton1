package com.xbit.proton

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xbit.proton.engine.PdfPriorityParser
import com.xbit.proton.ui.chat.ChatFragment
import com.xbit.proton.ui.menu.SideMenuFragment
import com.xbit.proton.ui.viewmodel.ChatViewModel
import com.xbit.proton.util.StorageManager
import com.xbit.proton.util.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewModel: ChatViewModel
    private lateinit var storage: StorageManager

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        val options = PdfPriorityParser.parse(this, uri)
        viewModel.onPdfParsed(options)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storage = StorageManager(this)
        ThemeManager.apply(storage.isDarkMode)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        drawerLayout = findViewById(R.id.drawerLayout)

        // Hamburger button
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // New chat button in top bar
        findViewById<ImageButton>(R.id.btnNewChat).setOnClickListener {
            viewModel.startNewChat()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.chatContainer, ChatFragment())
                .replace(R.id.navDrawerContainer, SideMenuFragment())
                .commit()
        }

        // PDF pick trigger from ViewModel
        viewModel.pickPdfEvent.observe(this) { triggered ->
            if (triggered) {
                pdfPickerLauncher.launch("application/pdf")
                viewModel.onPdfPickConsumed()
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
