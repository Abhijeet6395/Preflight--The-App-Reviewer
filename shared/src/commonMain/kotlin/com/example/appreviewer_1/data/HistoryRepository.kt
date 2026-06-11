package com.example.appreviewer_1.data

import com.example.appreviewer_1.domain.model.AnalysisReport
import com.example.appreviewer_1.platform.appStorageDir
import com.example.appreviewer_1.platform.currentTimeMillis
import com.example.appreviewer_1.platform.readTextFile
import com.example.appreviewer_1.platform.writeTextFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HistoryEntry(
    val id: Long,
    val sourceKey: String,
    val timestamp: Long,
    val report: AnalysisReport,
)

data class RecordResult(
    val history: List<HistoryEntry>,
    /** score change vs the previous scan of the same app, null on first scan */
    val scoreDelta: Int?,
)

/**
 * JSON-file-backed scan history. A single small file keeps this dependency-free
 * and trivially KMP; swap for SQLDelight if entries ever number in the thousands.
 */
class HistoryRepository(
    private val dirProvider: () -> String = { appStorageDir() },
    private val maxEntries: Int = 30,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val path get() = "${dirProvider()}/scan_history.json"

    fun load(): List<HistoryEntry> {
        val raw = readTextFile(path) ?: return emptyList()
        return runCatching { json.decodeFromString<List<HistoryEntry>>(raw) }
            .getOrDefault(emptyList())
    }

    fun record(report: AnalysisReport): RecordResult {
        val entries = load()
        val key = sourceKeyOf(report)
        val previous = entries.filter { it.sourceKey == key }.maxByOrNull { it.timestamp }
        val now = currentTimeMillis()

        val updated = (entries + HistoryEntry(now, key, now, report))
            .sortedByDescending { it.timestamp }
            .take(maxEntries)
        writeTextFile(path, json.encodeToString(updated))

        return RecordResult(
            history = updated,
            scoreDelta = previous?.let { report.score - it.report.score },
        )
    }

    private fun sourceKeyOf(report: AnalysisReport): String =
        report.metadata.packageName ?: report.metadata.sourceLabel
}
