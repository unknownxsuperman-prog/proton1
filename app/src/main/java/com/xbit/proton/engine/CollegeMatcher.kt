package com.xbit.proton.engine

import com.xbit.proton.data.model.College
import com.xbit.proton.data.model.CollegeResult
import com.xbit.proton.data.model.PriorityOption

/**
 * Matches colleges from a dataset against a user query (rank, branch, category).
 * Uses fuzzy branch matching: exact → substring → Jaccard ≥ 0.4.
 */
class CollegeMatcher(private val branchAliases: Map<String, List<String>>) {

    // ─── Branch fuzzy matching ────────────────────────────────────────────────

    /** Returns the canonical branch name for a user-supplied branch string, or null. */
    fun canonicalize(input: String): String? {
        val q = input.trim().lowercase()
        // Exact canonical match
        branchAliases.keys.firstOrNull { it.lowercase() == q }?.let { return it }
        // Exact alias match
        branchAliases.entries.firstOrNull { (_, aliases) ->
            aliases.any { it.lowercase() == q }
        }?.let { return it.key }
        // Substring
        branchAliases.entries.firstOrNull { (canonical, aliases) ->
            canonical.lowercase().contains(q) ||
                aliases.any { it.lowercase().contains(q) }
        }?.let { return it.key }
        // Jaccard token overlap
        return branchAliases.entries
            .mapNotNull { (canonical, aliases) ->
                val best = (listOf(canonical) + aliases).maxOfOrNull { jaccard(q, it.lowercase()) } ?: 0.0
                if (best >= 0.4) canonical to best else null
            }
            .maxByOrNull { it.second }?.first
    }

    private fun jaccard(a: String, b: String): Double {
        val ta = a.split(" ", "-", "_").filter { it.isNotBlank() }.toSet()
        val tb = b.split(" ", "-", "_").filter { it.isNotBlank() }.toSet()
        if (ta.isEmpty() && tb.isEmpty()) return 1.0
        val intersect = ta.intersect(tb).size.toDouble()
        val union = ta.union(tb).size.toDouble()
        return if (union == 0.0) 0.0 else intersect / union
    }

    // ─── College search ───────────────────────────────────────────────────────

    /**
     * Find colleges where cutoff >= rank (i.e., student rank is within cutoff)
     * filtered by category, optionally by branch.
     *
     * @param colleges  full dataset
     * @param rank      user's rank
     * @param category  e.g. "GM", "SC", "ST", "OBC", "HK_GM"
     * @param branch    optional branch filter (fuzzy)
     * @param maxResults maximum results to return
     */
    fun findMatches(
        colleges: List<College>,
        rank: Int,
        category: String,
        branch: String? = null,
        maxResults: Int = 100
    ): List<CollegeResult> {
        val canonBranch = branch?.let { canonicalize(it) }

        return colleges
            .filter { c ->
                // Category match (case-insensitive)
                c.category.equals(category, ignoreCase = true) &&
                // Cutoff >= rank means the student's rank qualifies
                c.cutoff >= rank &&
                // Branch filter
                (canonBranch == null || branchScore(c.branch, canonBranch) > 0.0)
            }
            .map { c ->
                val score = if (canonBranch != null) branchScore(c.branch, canonBranch) else 1.0
                // distance: how far the cutoff is above the rank (closer = better fit)
                val cutoffDist = (c.cutoff - rank).toDouble()
                CollegeResult(college = c, distance = cutoffDist, matchedBranch = c.branch)
            }
            .sortedWith(compareBy({ it.distance }, { it.college.colgname }))
            .take(maxResults)
    }

    /**
     * Match a parsed priority list against the datasets, returning eligibility
     * for each priority option.
     */
    fun matchPriorityOptions(
        options: List<PriorityOption>,
        colleges: List<College>,
        rank: Int,
        category: String
    ): List<Pair<PriorityOption, CollegeResult?>> {
        return options.map { option ->
            val match = colleges.firstOrNull { c ->
                c.colgcode.equals(option.collegeCode, ignoreCase = true) &&
                    c.branchcode.equals(option.branchCode, ignoreCase = true) &&
                    c.category.equals(category, ignoreCase = true)
            }
            val result = match?.let {
                CollegeResult(it, (it.cutoff - rank).toDouble(), it.branch)
            }
            option to result
        }
    }

    private fun branchScore(collegeBranch: String, canonQuery: String): Double {
        val cb = collegeBranch.lowercase()
        val cq = canonQuery.lowercase()
        return when {
            cb == cq -> 1.0
            cb.contains(cq) || cq.contains(cb) -> 0.8
            jaccard(cb, cq) >= 0.4 -> jaccard(cb, cq)
            else -> 0.0
        }
    }
}
