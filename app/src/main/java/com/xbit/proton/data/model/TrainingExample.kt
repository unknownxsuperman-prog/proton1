package com.xbit.proton.data.model

data class TrainingExample(
    val input: String = "",
    val intent: String = "",
    val entities: Map<String, String> = emptyMap()
)
