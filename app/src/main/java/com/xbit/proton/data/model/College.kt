package com.xbit.proton.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class College(
    val slno: Int = 0,
    val colgcode: String = "",
    val colgname: String = "",
    val place: String = "",
    val dist: String = "",
    val branch: String = "",
    val branchcode: String = "",
    val category: String = "",
    val cutoff: Int = 0
)

@Parcelize
data class CollegeResult(
    val college: College,
    val distance: Double,          // lower = closer match
    val matchedBranch: String
) : Parcelable

data class PriorityOption(
    val slno: Int,
    val collegeCode: String,
    val collegeName: String,
    val branchCode: String,
    val branchName: String,
    val category: String
)

enum class EligibilityStatus { ELIGIBLE, BORDERLINE, UNLIKELY }
