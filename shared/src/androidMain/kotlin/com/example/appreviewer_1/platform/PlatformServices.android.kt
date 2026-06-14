package com.example.appreviewer_1.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.appreviewer_1.data.TrackerSignatures
import com.example.appreviewer_1.domain.ai.AiEngine
import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipFile

/** Application context for non-composable platform calls. Set once at startup. */
object AppReviewerRuntime {
    lateinit var appContext: Context
}

fun initAppReviewer(context: Context) {
    AppReviewerRuntime.appContext = context.applicationContext
}

actual val supportsApkPicking: Boolean = true
actual val supportsSelfAudit: Boolean = true

actual fun createAiEngine(): AiEngine = GeminiNanoAiEngine()

actual suspend fun inspectApk(apk: PickedApk): AppMetadata = withContext(Dispatchers.IO) {
    val context = AppReviewerRuntime.appContext

    // PackageManager can only parse APKs from a real file path, so stage the
    // content URI into cache first. A user can hand us any file, so the copy is
    // size-capped: an "APK" larger than this isn't a real app, and staging it
    // would just fill the cache.
    val apkFile = File(context.cacheDir, "inspect.apk")
    try {
        context.contentResolver.openInputStream(Uri.parse(apk.ref))?.use { input ->
            apkFile.outputStream().use { output -> input.copyUpTo(output, MAX_APK_BYTES) }
        } ?: throw IllegalStateException("Couldn't read the selected file")
    } catch (e: Exception) {
        apkFile.delete()
        throw e
    }

    try {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            INSPECT_FLAGS,
        ) ?: throw IllegalStateException("${apk.displayName} is not a valid APK")

        info.applicationInfo?.apply {
            sourceDir = apkFile.absolutePath
            publicSourceDir = apkFile.absolutePath
        }
        buildMetadata(info, apk.displayName, apkFile)
    } finally {
        apkFile.delete()
    }
}

actual suspend fun selfAuditMetadata(): AppMetadata = withContext(Dispatchers.IO) {
    val context = AppReviewerRuntime.appContext
    @Suppress("DEPRECATION")
    val info = context.packageManager.getPackageInfo(context.packageName, INSPECT_FLAGS)
    buildMetadata(info, "Preflight · self-audit", File(context.applicationInfo.sourceDir))
}

private const val INSPECT_FLAGS =
    PackageManager.GET_PERMISSIONS or
        PackageManager.GET_ACTIVITIES or
        PackageManager.GET_SERVICES or
        PackageManager.GET_RECEIVERS

/** Refuse to stage anything larger than this — well above any real APK. */
private const val MAX_APK_BYTES = 500L * 1024 * 1024

/** Cap on bytes read from a single dex during the tracker scan, so a crafted
 *  archive that decompresses enormously ("zip bomb") can't spin it forever.
 *  Real dex files are far smaller. */
private const val MAX_DEX_SCAN_BYTES = 200L * 1024 * 1024

/** Streaming copy that aborts once [limit] bytes have been read. */
private fun InputStream.copyUpTo(out: OutputStream, limit: Long) {
    val buffer = ByteArray(1 shl 16)
    var total = 0L
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > limit) throw IllegalStateException("That file is too large to analyze")
        out.write(buffer, 0, read)
    }
}

private fun buildMetadata(info: PackageInfo, label: String, apkFile: File): AppMetadata {
    val pm = AppReviewerRuntime.appContext.packageManager
    val appInfo = info.applicationInfo
    val flags = appInfo?.flags ?: 0
    // A corrupt or hostile archive shouldn't sink the whole scan — degrade to an
    // empty footprint and keep the manifest-level findings.
    val footprint = runCatching { footprintOf(apkFile) }
        .getOrDefault(ApkFootprint(apkFile.length().coerceAtLeast(0), 0, emptyList(), emptyList()))

    return AppMetadata(
        sourceType = SourceType.APK,
        sourceLabel = label,
        appName = appInfo?.loadLabel(pm)?.toString(),
        packageName = info.packageName,
        versionName = info.versionName,
        minSdk = appInfo?.minSdkVersion,
        targetSdk = appInfo?.targetSdkVersion,
        permissions = info.requestedPermissions?.toList().orEmpty(),
        activityCount = info.activities?.size ?: 0,
        serviceCount = info.services?.size ?: 0,
        receiverCount = info.receivers?.size ?: 0,
        exportedActivities = info.activities?.count { it.exported } ?: 0,
        exportedServices = info.services?.count { it.exported } ?: 0,
        exportedReceivers = info.receivers?.count { it.exported } ?: 0,
        debuggable = flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        allowBackup = flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0,
        usesCleartextTraffic = flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC != 0,
        apkSizeBytes = footprint.apkSize,
        dexSizeBytes = footprint.dexSize,
        nativeAbis = footprint.abis,
        trackers = footprint.trackers,
    )
}

private class ApkFootprint(
    val apkSize: Long,
    val dexSize: Long,
    val abis: List<String>,
    val trackers: List<String>,
)

private fun footprintOf(apkFile: File): ApkFootprint {
    var dexSize = 0L
    val abis = sortedSetOf<String>()
    val trackers: List<String>

    ZipFile(apkFile).use { zip ->
        for (entry in zip.entries()) {
            val name = entry.name
            when {
                name.endsWith(".dex") && !name.contains('/') -> dexSize += entry.size
                name.startsWith("lib/") ->
                    name.split('/').getOrNull(1)?.let { abis.add(it) }
            }
        }
        trackers = scanForTrackers(zip)
    }

    return ApkFootprint(
        apkSize = apkFile.length(),
        dexSize = dexSize,
        abis = abis.toList(),
        trackers = trackers,
    )
}

/**
 * Streams every classes*.dex looking for known tracker class descriptors
 * (Exodus Privacy approach). Plain byte search — no dex parsing needed, since
 * class descriptors appear verbatim in the dex string pool.
 */
private fun scanForTrackers(zip: ZipFile): List<String> {
    val pending = TrackerSignatures.signatures
        .mapValues { (_, prefix) -> prefix.encodeToByteArray() }
        .toMutableMap()
    val maxSigLen = pending.values.maxOfOrNull { it.size } ?: return emptyList()
    val found = mutableListOf<String>()

    for (entry in zip.entries()) {
        if (pending.isEmpty()) break
        if (!entry.name.endsWith(".dex") || entry.name.contains('/')) continue

        zip.getInputStream(entry).use { input ->
            val buffer = ByteArray(1 shl 16)
            var carry = ByteArray(0)
            var scanned = 0L
            while (pending.isNotEmpty()) {
                val read = input.read(buffer)
                if (read <= 0) break
                scanned += read
                if (scanned > MAX_DEX_SCAN_BYTES) break
                val window = carry + buffer.copyOf(read)
                val iterator = pending.iterator()
                while (iterator.hasNext()) {
                    val (name, signature) = iterator.next()
                    if (window.containsSub(signature)) {
                        found.add(name)
                        iterator.remove()
                    }
                }
                // keep a tail so signatures spanning chunk boundaries still match
                carry = window.copyOfRange(
                    (window.size - (maxSigLen - 1)).coerceAtLeast(0),
                    window.size,
                )
            }
        }
    }
    return found.sorted()
}

private fun ByteArray.containsSub(needle: ByteArray): Boolean {
    if (needle.isEmpty() || needle.size > size) return false
    outer@ for (i in 0..size - needle.size) {
        for (j in needle.indices) {
            if (this[i + j] != needle[j]) continue@outer
        }
        return true
    }
    return false
}

actual fun shareText(text: String) {
    val context = AppReviewerRuntime.appContext
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(send, "Share audit report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

actual fun appStorageDir(): String = AppReviewerRuntime.appContext.filesDir.absolutePath

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun readTextFile(path: String): String? =
    File(path).takeIf { it.exists() }?.readText()

actual fun writeTextFile(path: String, content: String) {
    File(path).writeText(content)
}

@Composable
actual fun rememberApkPicker(onPicked: (PickedApk?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        onPicked(uri?.let { PickedApk(displayName(context, it), it.toString()) })
    }
    // APKs come back as octet-stream from many file managers, so accept both.
    return {
        launcher.launch(
            arrayOf("application/vnd.android.package-archive", "application/octet-stream")
        )
    }
}

private fun displayName(context: Context, uri: Uri): String {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
    }
    return uri.lastPathSegment ?: "selected.apk"
}
