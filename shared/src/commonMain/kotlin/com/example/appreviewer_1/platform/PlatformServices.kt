package com.example.appreviewer_1.platform

import androidx.compose.runtime.Composable
import com.example.appreviewer_1.domain.ai.AiEngine
import com.example.appreviewer_1.domain.model.AppMetadata

/** A user-picked APK file. [ref] is a platform-specific handle (content URI on Android). */
data class PickedApk(val displayName: String, val ref: String)

/** Whether this platform can inspect local APK files (Android only). */
expect val supportsApkPicking: Boolean

/** Extract [AppMetadata] from a picked APK. Throws if unsupported on this platform. */
expect suspend fun inspectApk(apk: PickedApk): AppMetadata

/**
 * The platform's best available on-device AI engine.
 * Android: swap in a Gemini Nano (AI Edge SDK) implementation here on
 * supported devices; everything else falls back to the rule-based engine.
 */
expect fun createAiEngine(): AiEngine

/**
 * Returns a launcher lambda that opens the platform file picker for APKs.
 * Calls [onPicked] with null if the user cancels. No-op on platforms where
 * [supportsApkPicking] is false.
 */
@Composable
expect fun rememberApkPicker(onPicked: (PickedApk?) -> Unit): () -> Unit

/** Whether the app can audit its own installed package (Android only). */
expect val supportsSelfAudit: Boolean

/** Inspect this app's own installed package — the "review the reviewer" demo. */
expect suspend fun selfAuditMetadata(): AppMetadata

/** Open the platform share sheet with plain text (the Markdown report). */
expect fun shareText(text: String)

/** Directory for app-private files (scan history). */
expect fun appStorageDir(): String

expect fun currentTimeMillis(): Long

expect fun readTextFile(path: String): String?

expect fun writeTextFile(path: String, content: String)
