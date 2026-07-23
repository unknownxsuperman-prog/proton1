package com.xbit.proton.engine

import com.xbit.proton.data.model.TrainingExample

/**
 * Lightweight bag-of-words NLP engine for intent classification and entity extraction.
 * Uses Jaccard similarity against training examples from the server-side dataset.
 */
class NlpEngine(private val trainingData: List<TrainingExample>) {

    // ─── Intent classification ────────────────────────────────────────────────

    enum class Intent {
        RANK_PREDICTION, COLLEGE_PREDICTION, PRIORITY_LIST,
        GREETING, HELP, UNKNOWN
    }

    fun classifyIntent(text: String): Intent {
        val tokens = tokenize(text)

        // Check hard-coded keyword shortcuts first (fast path)
        val lower = text.lowercase()
        if (lower.contains("rank") && (lower.contains("predict") || lower.contains("estimate") || lower.contains("my rank")))
            return Intent.RANK_PREDICTION
        if (lower.contains("college") && (lower.contains("predict") || lower.contains("search") || lower.contains("eligible")))
            return Intent.COLLEGE_PREDICTION
        if (lower.contains("priority") || lower.contains("pdf") || lower.contains("option entry"))
            return Intent.PRIORITY_LIST
        if (lower.matches(Regex("(hi|hello|hey|howdy|namaste|hola)(\\W.*)?", RegexOption.IGNORE_CASE)))
            return Intent.GREETING
        if (lower.contains("help") || lower.contains("what can") || lower.contains("how do"))
            return Intent.HELP

        // Fallback: nearest-neighbour in training data
        val best = trainingData
            .map { ex -> ex to jaccard(tokens, tokenize(ex.input)) }
            .maxByOrNull { it.second }

        if (best != null && best.second >= 0.3) {
            return when (best.first.intent.lowercase()) {
                "rank_prediction"    -> Intent.RANK_PREDICTION
                "college_prediction" -> Intent.COLLEGE_PREDICTION
                "priority_list"      -> Intent.PRIORITY_LIST
                "greeting"           -> Intent.GREETING
                "help"               -> Intent.HELP
                else                 -> Intent.UNKNOWN
            }
        }
        return Intent.UNKNOWN
    }

    // ─── Entity extraction ────────────────────────────────────────────────────

    data class Entities(
        val rank: Int? = null,
        val kcetMarks: Double? = null,
        val boardPercent: Double? = null,
        val board: String? = null,
        val category: String? = null,
        val branch: String? = null,
        val location: String? = null
    )

    fun extractEntities(text: String): Entities {
        val lower = text.lowercase()

        val rank = extractRank(lower)
        val kcetMarks = extractKcetMarks(lower)
        val (boardPercent, board) = extractBoardPercent(lower)
        val category = extractCategory(lower)
        val branch = extractBranch(lower)
        val location = extractLocation(lower)

        return Entities(rank, kcetMarks, boardPercent, board, category, branch, location)
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private val rankRegex = Regex("""(?:rank\s*(?:is|:)?\s*|my rank[:\s]+)(\d{1,6})""")
    private val marksRegex = Regex("""(\d{1,3}(?:\.\d+)?)\s*(?:/\s*180|marks?|out of 180)""")
    private val percentRegex = Regex("""(\d{2,3}(?:\.\d+)?)\s*%""")

    private fun extractRank(text: String): Int? =
        rankRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""(\d{4,6})""").find(text)?.groupValues?.get(1)?.toIntOrNull()

    private fun extractKcetMarks(text: String): Double? =
        marksRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()

    private fun extractBoardPercent(text: String): Pair<Double?, String?> {
        val pct = percentRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()
        val board = when {
            text.contains("cbse") -> "CBSE"
            text.contains("isc") || text.contains("icse") -> "ISC"
            text.contains("karnataka") || text.contains("puc") -> "Karnataka"
            else -> if (pct != null) "Karnataka" else null
        }
        return pct to board
    }

    private fun extractCategory(text: String): String? = when {
        text.contains("hk") && text.contains("gm") -> "HK_GM"
        text.contains("hk") && text.contains("sc")  -> "HK_SC"
        text.contains("hk") && text.contains("st")  -> "HK_ST"
        text.contains("hk") && (text.contains("2a") || text.contains("2b")) -> "HK_2A"
        text.contains("hk") -> "HK_GM"
        text.contains(" sc ") || text.endsWith(" sc") -> "SC"
        text.contains(" st ") || text.endsWith(" st") -> "ST"
        text.contains("2a") -> "2A"
        text.contains("2b") -> "2B"
        text.contains("3a") -> "3A"
        text.contains("3b") -> "3B"
        text.contains("gm") || text.contains("general") -> "GM"
        text.contains("obc") -> "OBC"
        else -> null
    }

    private val branchKeywords = listOf(
        "computer science", "cse", "cs", "information science", "ise", "is",
        "electronics", "ece", "eee", "electrical", "mechanical", "mech",
        "civil", "chemical", "biotechnology", "bio tech", "aerospace",
        "industrial", "instrumentation", "ai", "artificial intelligence",
        "machine learning", "ml", "data science", "automobile", "aeronautical",
        "mining", "textile", "polymer", "environmental", "marine", "naval"
    ).sortedByDescending { it.length }

    private fun extractBranch(text: String): String? =
        branchKeywords.firstOrNull { text.contains(it) }

    private val locationKeywords = listOf(
        "bangalore", "bengaluru", "mysore", "mysuru", "hubli", "dharwad",
        "belgaum", "belagavi", "mangalore", "mangaluru", "tumkur", "shimoga",
        "davanagere", "bellary", "gulbarga", "kalaburagi", "hassan", "mandya",
        "udupi", "karwar", "bidar", "raichur", "koppal"
    )

    private fun extractLocation(text: String): String? =
        locationKeywords.firstOrNull { text.contains(it) }

    private fun tokenize(text: String): Set<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 1 }.toSet()

    private fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val inter = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union == 0.0) 0.0 else inter / union
    }

    companion object {
        val ALL_CATEGORIES = listOf("GM", "SC", "ST", "OBC", "2A", "2B", "3A", "3B")
        val HK_CAT_SET = setOf("HK_GM", "HK_SC", "HK_ST", "HK_2A", "HK_2B", "HK_3A", "HK_3B")
    }
}
