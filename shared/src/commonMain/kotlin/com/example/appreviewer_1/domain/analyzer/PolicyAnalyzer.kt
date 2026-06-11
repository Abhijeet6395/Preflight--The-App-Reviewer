package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Category
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity
import com.example.appreviewer_1.domain.model.SourceType

class PolicyAnalyzer : Analyzer {
    override val name = "Play Policy"

    override fun analyze(metadata: AppMetadata): List<Finding> = buildList {
        val target = metadata.targetSdk
        if (target != null) {
            when {
                target < 34 -> add(
                    Finding(
                        id = "policy_target_old",
                        title = "targetSdk $target is below Play's minimum",
                        detail = "Google Play requires new apps and updates to target API 34+ " +
                            "(Android 14). Submissions below that are rejected outright.",
                        recommendation = "Bump targetSdk to the latest stable API and walk through " +
                            "the behaviour-change checklists for each skipped version.",
                        severity = Severity.CRITICAL,
                        category = Category.POLICY,
                    )
                )
                target < 35 -> add(
                    Finding(
                        id = "policy_target_aging",
                        title = "targetSdk $target is one cycle behind",
                        detail = "Play's annual target-API deadline will make this version " +
                            "non-compliant at the next enforcement window.",
                        recommendation = "Schedule the targetSdk bump now rather than against the " +
                            "deadline.",
                        severity = Severity.MEDIUM,
                        category = Category.POLICY,
                    )
                )
            }
        }

        val min = metadata.minSdk
        if (min != null && min < 23) {
            add(
                Finding(
                    id = "policy_min_low",
                    title = "minSdk $min predates runtime permissions",
                    detail = "Devices below API 23 grant all permissions at install time and miss " +
                        "modern security patches; supporting them complicates permission UX.",
                    recommendation = "Consider raising minSdk — Play console shows the user % you'd " +
                        "actually lose (usually <1% below API 23).",
                    severity = Severity.LOW,
                    category = Category.POLICY,
                )
            )
        }

        if (metadata.versionName.isNullOrBlank() && metadata.sourceType != SourceType.GITHUB) {
            add(
                Finding(
                    id = "policy_version",
                    title = "Missing versionName",
                    detail = "No human-readable version string was found in the package.",
                    recommendation = "Set a semantic versionName so crash reports and reviews can be " +
                        "tied to releases.",
                    severity = Severity.INFO,
                    category = Category.POLICY,
                )
            )
        }
    }
}
