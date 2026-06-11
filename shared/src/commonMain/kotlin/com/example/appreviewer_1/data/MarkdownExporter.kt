package com.example.appreviewer_1.data

import com.example.appreviewer_1.domain.model.AnalysisReport
import com.example.appreviewer_1.domain.model.Severity

/** Renders a report as Markdown for the share sheet — paste-ready for PRs and chats. */
object MarkdownExporter {

    fun export(report: AnalysisReport): String = buildString {
        val meta = report.metadata
        appendLine("# Preflight audit — ${meta.appName ?: meta.sourceLabel}")
        appendLine()
        appendLine("**Score: ${report.score}/100 (Grade ${report.grade})**")
        appendLine()
        meta.packageName?.let { appendLine("- Package: `$it`") }
        meta.versionName?.let { appendLine("- Version: $it") }
        meta.targetSdk?.let { appendLine("- targetSdk: $it") }
        appendLine("- Permissions: ${meta.permissions.size}")
        if (meta.trackers.isNotEmpty()) {
            appendLine("- Third-party SDKs: ${meta.trackers.joinToString()}")
        }
        appendLine()
        appendLine("## AI insight (${report.insight.engineName})")
        appendLine()
        appendLine(report.insight.summary)
        if (report.insight.priorities.isNotEmpty()) {
            appendLine()
            appendLine("**Fix first:**")
            report.insight.priorities.forEachIndexed { i, p ->
                appendLine("${i + 1}. $p")
            }
        }

        for ((category, findings) in report.findingsByCategory()) {
            appendLine()
            appendLine("## ${category.title}")
            appendLine()
            for (finding in findings) {
                val marker = when (finding.severity) {
                    Severity.CRITICAL -> "🔴"
                    Severity.HIGH -> "🟠"
                    Severity.MEDIUM -> "🟡"
                    Severity.LOW -> "🔵"
                    Severity.INFO -> "ℹ️"
                }
                appendLine("- $marker **${finding.title}** (${finding.severity.label})")
                appendLine("  ${finding.detail}")
                appendLine("  *Fix:* ${finding.recommendation}")
            }
        }
        appendLine()
        appendLine("---")
        appendLine("_Generated on-device by Preflight — no code left the machine._")
    }
}
