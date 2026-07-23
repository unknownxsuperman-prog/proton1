package com.xbit.proton.data.model

data class BranchAlias(
    val canonical: String = "",
    val aliases: List<String> = emptyList()
)
