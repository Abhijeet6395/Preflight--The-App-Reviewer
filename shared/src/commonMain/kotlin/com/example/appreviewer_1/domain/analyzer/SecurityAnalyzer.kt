package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Category
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity

class SecurityAnalyzer : Analyzer {
    override val name = "Security"

    override fun analyze(metadata: AppMetadata): List<Finding> = buildList {
        if (metadata.debuggable == true) {
            add(
                Finding(
                    id = "sec_debuggable",
                    title = "App is debuggable",
                    detail = "android:debuggable is enabled, so anyone can attach a debugger, " +
                        "dump memory and read app data on a production build.",
                    recommendation = "Remove android:debuggable=\"true\" from the manifest (release " +
                        "builds set it to false automatically). Google Play blocks debuggable uploads.",
                    severity = Severity.CRITICAL,
                    category = Category.SECURITY,
                )
            )
        }
        if (metadata.usesCleartextTraffic == true) {
            add(
                Finding(
                    id = "sec_cleartext",
                    title = "Cleartext (HTTP) traffic allowed",
                    detail = "android:usesCleartextTraffic is enabled, allowing unencrypted HTTP " +
                        "connections that can be intercepted or modified on the network.",
                    recommendation = "Serve all endpoints over HTTPS and remove the flag, or scope " +
                        "exceptions with a networkSecurityConfig.",
                    severity = Severity.HIGH,
                    category = Category.SECURITY,
                )
            )
        }
        if (metadata.allowBackup == true) {
            add(
                Finding(
                    id = "sec_backup",
                    title = "Full app data backup enabled",
                    detail = "android:allowBackup is true, so app data (tokens, databases) can be " +
                        "extracted via adb backup or migrated to other devices.",
                    recommendation = "Set android:allowBackup=\"false\" or define backup rules that " +
                        "exclude secrets.",
                    severity = Severity.MEDIUM,
                    category = Category.SECURITY,
                )
            )
        }

        val exported = metadata.exportedActivities + metadata.exportedServices + metadata.exportedReceivers
        if (exported > 1) {
            add(
                Finding(
                    id = "sec_exported",
                    title = "$exported exported components",
                    detail = "${metadata.exportedActivities} activities, ${metadata.exportedServices} " +
                        "services and ${metadata.exportedReceivers} receivers are reachable by other " +
                        "apps. Each exported component is attack surface.",
                    recommendation = "Set android:exported=\"false\" on everything that does not need " +
                        "to be launched externally, and guard the rest with permissions.",
                    severity = if (exported > 4) Severity.HIGH else Severity.MEDIUM,
                    category = Category.SECURITY,
                )
            )
        }
    }
}
