package com.example.appreviewer_1

import com.example.appreviewer_1.domain.ai.VerdictSanitizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VerdictSanitizerTest {

    @Test
    fun preservesUnderscoresInIdentifiers() {
        val out = VerdictSanitizer.clean("Remove READ_SMS and WRITE_EXTERNAL_STORAGE before submitting.")
        assertEquals("Remove READ_SMS and WRITE_EXTERNAL_STORAGE before submitting.", out)
    }

    @Test
    fun stripsMarkdownEmphasisAndCode() {
        val out = VerdictSanitizer.clean("This app is **not ready** to `submit` right now today.")
        assertEquals("This app is not ready to submit right now today.", out)
    }

    @Test
    fun flattensBulletListsToProse() {
        val out = VerdictSanitizer.clean("Fix these issues now:\n- debuggable build\n- cleartext traffic")
        assertEquals("Fix these issues now: debuggable build cleartext traffic", out)
    }

    @Test
    fun stripsHeadingsAndCodeFences() {
        val out = VerdictSanitizer.clean("# Verdict\n```\nNot ready for submission yet.\n```")
        assertNotNull(out)
        assertTrue(!out.contains("#") && !out.contains("`"))
        assertTrue(out.contains("Not ready for submission yet."))
    }

    @Test
    fun rejectsBlankOrTooShort() {
        assertNull(VerdictSanitizer.clean(null))
        assertNull(VerdictSanitizer.clean("   "))
        assertNull(VerdictSanitizer.clean("ok"))
    }

    @Test
    fun rejectsRunawayOutput() {
        assertNull(VerdictSanitizer.clean("x".repeat(2000)))
    }
}
