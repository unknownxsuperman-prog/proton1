package com.xbit.proton.data.model

import java.util.UUID

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val messages: MutableList<Message> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)
