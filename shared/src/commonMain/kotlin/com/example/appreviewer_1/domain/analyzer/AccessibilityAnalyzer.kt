package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Category
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity

class AccessibilityAnalyzer : Analyzer {
    override val name = "Accessibility & UX"

    override fun analyze(metadata: AppMetadata): List<Finding> = buildList {
        if ("android.permission.SYSTEM_ALERT_WINDOW" in metadata.permissions) {
            add(
                Finding(
                    id = "a11y_overlay",
                    title = "Draw-over-other-apps permission",
                    detail = "Overlays interfere with TalkBack focus order and are heavily " +
                        "scrutinised in Play review (tapjacking risk).",
                    recommendation = "Replace overlays with notifications, bubbles, or " +
                        "picture-in-picture where possible.",
                    severity = Severity.MEDIUM,
                    category = Category.ACCESSIBILITY,
                )
            )
        }

        if (metadata.permissions.any { it.contains("BIND_ACCESSIBILITY_SERVICE") }) {
            add(
                Finding(
                    id = "a11y_service",
                    title = "Accessibility service declared",
                    detail = "Play only permits AccessibilityService for apps that genuinely assist " +
                        "users with disabilities; anything else needs a prominent disclosure and is " +
                        "frequently rejected.",
                    recommendation = "If the service is not assisting disabled users, migrate to a " +
                        "supported API before submission.",
                    severity = Severity.HIGH,
                    category = Category.POLICY,
                )
            )
        }

        if (metadata.activityCount > 25) {
            add(
                Finding(
                    id = "ux_activity_sprawl",
                    title = "${metadata.activityCount} activities in one app",
                    detail = "Very large activity counts usually mean deep, inconsistent navigation " +
                        "stacks and slower cold starts.",
                    recommendation = "Consolidate flows into fewer single-Activity graphs " +
                        "(Navigation Compose) and lazy-load rarely used screens.",
                    severity = Severity.LOW,
                    category = Category.EXPERIENCE,
                )
            )
        }

        // Static metadata can't prove content descriptions or contrast — be honest about that
        // and tell the developer what to run next.
        add(
            Finding(
                id = "a11y_runtime_checks",
                title = "Run runtime accessibility checks before submitting",
                detail = "Content descriptions, touch-target sizes and contrast can only be " +
                    "verified on rendered UI, not from package metadata.",
                recommendation = "Run the Accessibility Scanner app or enable " +
                    "AccessibilityChecks.enable() in your Espresso/Compose UI tests.",
                severity = Severity.INFO,
                category = Category.ACCESSIBILITY,
            )
        )
    }
}
