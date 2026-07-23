package com.xbit.proton.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xbit.proton.R
import com.xbit.proton.ui.viewmodel.ChatViewModel
import com.xbit.proton.util.StorageManager
import com.xbit.proton.util.ThemeManager

class SideMenuFragment : Fragment() {

    private lateinit var viewModel: ChatViewModel
    private lateinit var storage: StorageManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_side_menu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[ChatViewModel::class.java]
        storage = StorageManager(requireContext())

        // Username header
        view.findViewById<TextView>(R.id.tvUsername).text = storage.username

        // Dark mode toggle
        val darkSwitch = view.findViewById<SwitchCompat>(R.id.switchDarkMode)
        darkSwitch.isChecked = storage.isDarkMode
        darkSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            storage.isDarkMode = isChecked
            ThemeManager.apply(isChecked)
        }

        // New chat button
        view.findViewById<View>(R.id.btnNewChatMenu).setOnClickListener {
            viewModel.startNewChat()
        }

        // Recent chats list
        val chatList = view.findViewById<LinearLayout>(R.id.recentChatsList)
        refreshChatList(chatList)

        viewModel.messages.observe(viewLifecycleOwner) {
            refreshChatList(chatList)
        }
    }

    private fun refreshChatList(container: LinearLayout) {
        container.removeAllViews()
        val chats = viewModel.getAllChats()
        if (chats.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = getString(R.string.no_chats)
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                setPadding(16, 16, 16, 16)
            }
            container.addView(empty)
            return
        }
        chats.forEach { chat ->
            val row = layoutInflater.inflate(R.layout.item_recent_chat, container, false)
            row.findViewById<TextView>(R.id.tvChatTitle).text = chat.title
            row.setOnClickListener { /* TODO: load chat */ }
            row.setOnLongClickListener {
                viewModel.deleteChat(chat.id)
                refreshChatList(container)
                true
            }
            container.addView(row)
        }
    }
}
