package com.example.appreviewer_1.data

/**
 * Known SDK class-path prefixes, Exodus Privacy style. The Android inspector
 * byte-scans each classes*.dex for these descriptors; matches surface as
 * privacy findings.
 */
object TrackerSignatures {
    val signatures: Map<String, String> = mapOf(
        "Google AdMob" to "Lcom/google/android/gms/ads/",
        "Firebase Analytics" to "Lcom/google/firebase/analytics/",
        "Crashlytics" to "Lcom/google/firebase/crashlytics/",
        "Meta App Events" to "Lcom/facebook/appevents/",
        "Facebook Audience Network" to "Lcom/facebook/ads/",
        "AppsFlyer" to "Lcom/appsflyer/",
        "Adjust" to "Lcom/adjust/sdk/",
        "Branch" to "Lio/branch/referral/",
        "Amplitude" to "Lcom/amplitude/",
        "Mixpanel" to "Lcom/mixpanel/android/",
        "Segment" to "Lcom/segment/analytics/",
        "Sentry" to "Lio/sentry/",
        "OneSignal" to "Lcom/onesignal/",
        "Braze" to "Lcom/braze/",
        "AppLovin" to "Lcom/applovin/",
        "Unity Ads" to "Lcom/unity3d/ads/",
        "ironSource" to "Lcom/ironsource/",
        "Vungle" to "Lcom/vungle/",
        "InMobi" to "Lcom/inmobi/",
        "Chartboost" to "Lcom/chartboost/",
    )
}
