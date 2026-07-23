package com.xbit.proton.data.model

import java.util.UUID

enum class MessageRole { USER, ASSISTANT }

enum class CardType { NONE, RANK_FORM, COLLEGE_FORM, PRIORITY_FORM, RANK_RESULT, COLLEGE_RESULTS }

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String = "",
    val cardType: CardType = CardType.NONE,
    val timestamp: Long = System.currentTimeMillis()
)
