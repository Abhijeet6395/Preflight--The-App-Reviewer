package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Category
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity

class TrackerAnalyzer : Analyzer {
    override val name = "Trackers"

    override fun analyze(metadata: AppMetadata): List<Finding> = buildList {
        val trackers = metadata.trackers
        if (trackers.isEmpty()) return@buildList

        add(
            Finding(
                id = "privacy_trackers",
                title = "${trackers.size} third-party SDKs detected in code",
                detail = "Class signatures found in the dex bytecode: " +
                    "${trackers.joinToString()}. Each must be declared in the Play " +
                    "Console Data Safety form — undeclared data collection is a " +
                    "common takedown reason.",
                recommendation = "Audit the Data Safety form against this list and drop " +
                    "SDKs that no shipped feature depends on.",
                severity = when {
                    trackers.size >= 5 -> Severity.HIGH
                    trackers.size >= 3 -> Severity.MEDIUM
                    else -> Severity.LOW
                },
                category = Category.PRIVACY,
            )
        )

        val adNetworks = trackers.filter {
            it.contains("Ad", ignoreCase = false) || it in setOf(
                "AppLovin", "Vungle", "ironSource", "InMobi", "Chartboost",
            )
        }
        if (adNetworks.size >= 2) {
            add(
                Finding(
                    id = "privacy_ad_stack",
                    title = "Multiple ad networks bundled (${adNetworks.size})",
                    detail = "${adNetworks.joinToString()} all ship in this build. Ad " +
                        "mediation stacks inflate APK size and each network needs its own " +
                        "ads declaration and families-policy review.",
                    recommendation = "Use one mediation layer and remove direct " +
                        "integrations it already covers.",
                    severity = Severity.MEDIUM,
                    category = Category.POLICY,
                )
            )
        }
    }
}
