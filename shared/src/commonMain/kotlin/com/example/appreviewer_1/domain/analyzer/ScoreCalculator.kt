package com.example.appreviewer_1.domain.analyzer

import com.example.appreviewer_1.domain.model.Finding

object ScoreCalculator {

    fun score(findings: List<Finding>): Int {
        val deduction = findings.sumOf { it.severity.weight }
        return (100 - deduction).coerceIn(5, 100)
    }

    fun grade(score: Int): String = when {
        score >= 95 -> "A+"
        score >= 85 -> "A"
        score >= 70 -> "B"
        score >= 55 -> "C"
        score >= 40 -> "D"
        else -> "F"
    }
}
