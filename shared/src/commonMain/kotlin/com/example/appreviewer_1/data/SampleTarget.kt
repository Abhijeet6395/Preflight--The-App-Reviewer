package com.example.appreviewer_1.data

import com.example.appreviewer_1.domain.model.AppMetadata
import com.example.appreviewer_1.domain.model.SourceType

/**
 * A deliberately flawed demo app so the full report can be shown on stage
 * (or on iOS, where APK inspection isn't available) without picking a file.
 */
object SampleTarget {
    val metadata = AppMetadata(
        sourceType = SourceType.SAMPLE,
        sourceLabel = "Sample · PhotoShare",
        appName = "PhotoShare",
        packageName = "dev.example.photoshare",
        versionName = "2.3.1",
        minSdk = 21,
        targetSdk = 31,
        permissions = listOf(
            "android.permission.INTERNET",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.QUERY_ALL_PACKAGES",
            "android.permission.WAKE_LOCK",
            "android.permission.VIBRATE",
        ),
        activityCount = 31,
        serviceCount = 6,
        receiverCount = 4,
        exportedActivities = 3,
        exportedServices = 2,
        exportedReceivers = 2,
        debuggable = true,
        allowBackup = true,
        usesCleartextTraffic = true,
        apkSizeBytes = 87L * 1024 * 1024,
        dexSizeBytes = 29L * 1024 * 1024,
        nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"),
        trackers = listOf("Meta App Events", "AppsFlyer", "Mixpanel", "AppLovin", "ironSource"),
    )
}
