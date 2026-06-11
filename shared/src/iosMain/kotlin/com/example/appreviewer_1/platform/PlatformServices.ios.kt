package com.example.appreviewer_1.platform

import androidx.compose.runtime.Composable
import com.example.appreviewer_1.domain.ai.AiEngine
import com.example.appreviewer_1.domain.ai.RuleBasedAiEngine
import com.example.appreviewer_1.domain.model.AppMetadata
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

// iOS can't parse Android APKs; the UI hides the picker and offers
// GitHub-repo analysis and the bundled sample instead.
actual val supportsApkPicking: Boolean = false
actual val supportsSelfAudit: Boolean = false

actual suspend fun inspectApk(apk: PickedApk): AppMetadata =
    throw UnsupportedOperationException("APK inspection is only available on Android")

actual suspend fun selfAuditMetadata(): AppMetadata =
    throw UnsupportedOperationException("Self-audit is only available on Android")

actual fun createAiEngine(): AiEngine = RuleBasedAiEngine()

@Composable
actual fun rememberApkPicker(onPicked: (PickedApk?) -> Unit): () -> Unit = {}

actual fun shareText(text: String) {
    val controller = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null,
    )
    UIApplication.sharedApplication.keyWindow?.rootViewController
        ?.presentViewController(controller, animated = true, completion = null)
}

actual fun appStorageDir(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .first() as String

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

@OptIn(ExperimentalForeignApi::class)
actual fun readTextFile(path: String): String? {
    val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
    return NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
}

@OptIn(ExperimentalForeignApi::class)
actual fun writeTextFile(path: String, content: String) {
    @Suppress("CAST_NEVER_SUCCEEDS")
    val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
    data.writeToFile(path, atomically = true)
}
