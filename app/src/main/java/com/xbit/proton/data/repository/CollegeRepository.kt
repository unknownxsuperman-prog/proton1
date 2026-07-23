package com.xbit.proton.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xbit.proton.data.model.BranchAlias
import com.xbit.proton.data.model.College
import com.xbit.proton.data.model.TrainingExample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Singleton repository that fetches and caches all remote datasets.
 * External files live at: https://unknownxsuperman-prog.github.io/x-bit-kea/<filename>
 */
object CollegeRepository {

    private const val TAG = "CollegeRepository"
    private const val BASE_URL = "https://unknownxsuperman-prog.github.io/x-bit-kea/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Caches
    private var engineeringColleges: List<College>? = null
    private var engineeringHkColleges: List<College>? = null
    private var branchAliases: Map<String, List<String>>? = null
    private var trainingData: List<TrainingExample>? = null
    private var trainingData1: List<TrainingExample>? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    suspend fun getEngineeringColleges(): List<College> =
        engineeringColleges ?: fetchEngineering().also { engineeringColleges = it }

    suspend fun getEngineeringHkColleges(): List<College> =
        engineeringHkColleges ?: fetchEngineeringHk().also { engineeringHkColleges = it }

    suspend fun getBranchAliases(): Map<String, List<String>> =
        branchAliases ?: fetchBranchAliases().also { branchAliases = it }

    suspend fun getTrainingData(): List<TrainingExample> {
        if (trainingData == null) trainingData = fetchTrainingSet("trainingset.json")
        if (trainingData1 == null) trainingData1 = fetchTrainingSet("trainingset1.json")
        return (trainingData ?: emptyList()) + (trainingData1 ?: emptyList())
    }

    // ─── Fetchers ─────────────────────────────────────────────────────────────

    private suspend fun fetchEngineering(): List<College> = withContext(Dispatchers.IO) {
        try {
            val raw = fetchText("firstroundeng.js")
            val json = stripJsWrapper(raw)
            gson.fromJson<List<College>>(json, object : TypeToken<List<College>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchEngineering failed", e)
            emptyList()
        }
    }

    private suspend fun fetchEngineeringHk(): List<College> = withContext(Dispatchers.IO) {
        try {
            val raw = fetchText("firstroundenghk.js")
            val json = stripJsWrapper(raw)
            gson.fromJson<List<College>>(json, object : TypeToken<List<College>>() {}.type)
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "fetchEngineeringHk failed", e)
            emptyList()
        }
    }

    private suspend fun fetchBranchAliases(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        try {
            val json = fetchText("branch-aliases.json")
            val list = gson.fromJson<List<BranchAlias>>(
                json, object : TypeToken<List<BranchAlias>>() {}.type
            ) ?: emptyList()
            list.associate { it.canonical to it.aliases }
        } catch (e: Exception) {
            Log.e(TAG, "fetchBranchAliases failed", e)
            emptyMap()
        }
    }

    private suspend fun fetchTrainingSet(filename: String): List<TrainingExample> =
        withContext(Dispatchers.IO) {
            try {
                val json = fetchText(filename)
                gson.fromJson<List<TrainingExample>>(
                    json, object : TypeToken<List<TrainingExample>>() {}.type
                ) ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "fetchTrainingSet($filename) failed", e)
                emptyList()
            }
        }

    // ─── Utilities ────────────────────────────────────────────────────────────

    /**
     * Strips the JS variable assignment wrapper from .js data files.
     * e.g.  window.XOS_CUTOFF = [...];  →  [...]
     */
    private fun stripJsWrapper(raw: String): String {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
    }

    private fun fetchText(filename: String): String {
        val request = Request.Builder().url(BASE_URL + filename).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code} for $filename")
            return response.body?.string() ?: throw Exception("Empty body for $filename")
        }
    }
}
