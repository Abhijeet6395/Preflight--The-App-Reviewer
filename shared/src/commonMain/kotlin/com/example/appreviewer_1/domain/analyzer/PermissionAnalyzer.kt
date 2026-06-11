package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Category
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity

class PermissionAnalyzer : Analyzer {
    override val name = "Permissions"

    private val dangerous = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.READ_PHONE_STATE",
        "android.permission.BODY_SENSORS",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
    )

    /** Permissions Google Play restricts to specific app categories with a declaration form. */
    private val playRestricted = setOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
    )

    override fun analyze(metadata: AppMetadata): List<Finding> = buildList {
        val perms = metadata.permissions

        val restrictedUsed = perms.filter { it in playRestricted }
        if (restrictedUsed.isNotEmpty()) {
            add(
                Finding(
                    id = "perm_restricted",
                    title = "Play-restricted permissions requested",
                    detail = "These permissions require a Play Console declaration and are a common " +
                        "rejection reason: ${restrictedUsed.joinToString { it.substringAfterLast('.') }}.",
                    recommendation = "Remove them unless they are core to your app's purpose; " +
                        "otherwise prepare the permissions declaration form and an in-app disclosure.",
                    severity = Severity.HIGH,
                    category = Category.POLICY,
                )
            )
        }

        val dangerousUsed = perms.filter { it in dangerous }
        if (dangerousUsed.size >= 3) {
            add(
                Finding(
                    id = "perm_dangerous",
                    title = "${dangerousUsed.size} dangerous permissions requested",
                    detail = "Runtime permissions touching private user data: " +
                        "${dangerousUsed.joinToString { it.substringAfterLast('.') }}. Users see every " +
                        "one of these, and each lowers install conversion.",
                    recommendation = "Audit each against a real feature. Prefer scoped alternatives " +
                        "(Photo Picker over storage, approximate over fine location).",
                    severity = Severity.MEDIUM,
                    category = Category.PRIVACY,
                )
            )
        }

        if ("android.permission.WRITE_EXTERNAL_STORAGE" in perms && (metadata.targetSdk ?: 0) >= 30) {
            add(
                Finding(
                    id = "perm_legacy_storage",
                    title = "Legacy storage permission has no effect",
                    detail = "WRITE_EXTERNAL_STORAGE is ignored from Android 11 (API 30) onward but " +
                        "still appears in your manifest, signalling unmaintained code.",
                    recommendation = "Drop it and use MediaStore, the Photo Picker, or the Storage " +
                        "Access Framework.",
                    severity = Severity.LOW,
                    category = Category.POLICY,
                )
            )
        }

        if (perms.size > 12) {
            add(
                Finding(
                    id = "perm_count",
                    title = "Large permission footprint (${perms.size})",
                    detail = "Apps requesting many permissions get flagged more often in review and " +
                        "abandoned more often by users.",
                    recommendation = "Trim to what the current release actually uses; re-add when a " +
                        "feature ships.",
                    severity = Severity.LOW,
                    category = Category.PRIVACY,
                )
            )
        }
    }
}
