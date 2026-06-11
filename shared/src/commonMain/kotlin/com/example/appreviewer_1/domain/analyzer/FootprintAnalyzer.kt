package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Category
import com.example.appreviewer_1.domain.model.Finding
import com.example.appreviewer_1.domain.model.Severity

class FootprintAnalyzer : Analyzer {
    override val name = "Footprint"

    override fun analyze(metadata: AppMetadata): List<Finding> = buildList {
        val apkMb = metadata.apkSizeBytes?.let { it / (1024.0 * 1024.0) }
        if (apkMb != null && apkMb > 60) {
            add(
                Finding(
                    id = "footprint_apk_size",
                    title = "APK is ${apkMb.toInt()} MB",
                    detail = "Install conversion drops measurably for every 10 MB of " +
                        "download size, especially in emerging markets.",
                    recommendation = "Publish as an App Bundle, enable resource shrinking " +
                        "and R8, and audit bundled assets.",
                    severity = if (apkMb > 150) Severity.MEDIUM else Severity.LOW,
                    category = Category.EXPERIENCE,
                )
            )
        }

        val dexMb = metadata.dexSizeBytes?.let { it / (1024.0 * 1024.0) }
        if (dexMb != null && dexMb > 24) {
            add(
                Finding(
                    id = "footprint_dex_size",
                    title = "${dexMb.toInt()} MB of dex code",
                    detail = "Large dex payloads slow cold start and usually mean unused " +
                        "library code is shipping.",
                    recommendation = "Enable R8 minification (isMinifyEnabled = true) for " +
                        "release builds and check for duplicate-functionality libraries.",
                    severity = Severity.LOW,
                    category = Category.EXPERIENCE,
                )
            )
        }

        val legacyAbis = metadata.nativeAbis.filter { it.startsWith("x86") || it == "armeabi" }
        if (legacyAbis.isNotEmpty()) {
            add(
                Finding(
                    id = "footprint_legacy_abi",
                    title = "Legacy native ABIs bundled: ${legacyAbis.joinToString()}",
                    detail = "Almost no consumer devices need these; in a universal APK " +
                        "they are pure dead weight.",
                    recommendation = "Ship an App Bundle so Play serves per-ABI splits, or " +
                        "restrict abiFilters to arm64-v8a (+ armeabi-v7a if needed).",
                    severity = Severity.LOW,
                    category = Category.EXPERIENCE,
                )
            )
        }
    }
}
