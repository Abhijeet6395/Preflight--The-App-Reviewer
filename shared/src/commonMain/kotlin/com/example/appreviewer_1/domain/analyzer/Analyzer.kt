package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.Finding

interface Analyzer {
    val name: String
    fun analyze(metadata: AppMetadata): List<Finding>
}

fun defaultAnalyzers(): List<Analyzer> = listOf(
    SecurityAnalyzer(),
    PermissionAnalyzer(),
    PolicyAnalyzer(),
    AccessibilityAnalyzer(),
    TrackerAnalyzer(),
    FootprintAnalyzer(),
)
