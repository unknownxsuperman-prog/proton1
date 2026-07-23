package com.xbit.proton.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xbit.proton.data.model.Chat

private const val PREF_FILE = "xbit_prefs"
private const val KEY_USERNAME = "username"
private const val KEY_DARK_MODE = "dark_mode"
private const val KEY_CHATS = "chats"
private const val KEY_ONBOARDED = "onboarded"

class StorageManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ─── Onboarding ───────────────────────────────────────────────────────────

    var isOnboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(v) = prefs.edit().putBoolean(KEY_ONBOARDED, v).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "Student") ?: "Student"
        set(v) = prefs.edit().putString(KEY_USERNAME, v).apply()

    // ─── Theme ────────────────────────────────────────────────────────────────

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(v) = prefs.edit().putBoolean(KEY_DARK_MODE, v).apply()

    // ─── Chats ────────────────────────────────────────────────────────────────

    fun saveChats(chats: List<Chat>) {
        prefs.edit().putString(KEY_CHATS, gson.toJson(chats)).apply()
    }

    fun loadChats(): MutableList<Chat> {
        val json = prefs.getString(KEY_CHATS, null) ?: return mutableListOf()
        return try {
            gson.fromJson<MutableList<Chat>>(
                json, object : TypeToken<MutableList<Chat>>() {}.type
            ) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }
}
