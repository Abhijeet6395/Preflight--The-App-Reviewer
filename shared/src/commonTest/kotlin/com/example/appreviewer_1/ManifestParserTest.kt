package com.example.appreviewer_1

import com.example.appreviewer_1.data.GithubRepoFetcher
import com.example.appreviewer_1.data.ManifestParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManifestParserTest {

    private val manifest = """
        <?xml version="1.0" encoding="utf-8"?>
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
            package="dev.example.demo">
            <uses-permission android:name="android.permission.INTERNET" />
            <uses-permission android:name="android.permission.CAMERA" />
            <application
                android:debuggable="true"
                android:usesCleartextTraffic="true"
                android:label="Demo">
                <activity android:name=".MainActivity" android:exported="true" />
                <activity android:name=".DetailActivity" />
                <service android:name=".SyncService" android:exported="true" />
                <receiver android:name=".BootReceiver" />
            </application>
        </manifest>
    """.trimIndent()

    @Test
    fun parsesPermissionsComponentsAndFlags() {
        val meta = ManifestParser.parse(manifest, "owner/repo")

        assertEquals("dev.example.demo", meta.packageName)
        assertEquals(
            listOf("android.permission.INTERNET", "android.permission.CAMERA"),
            meta.permissions,
        )
        assertEquals(2, meta.activityCount)
        assertEquals(1, meta.exportedActivities)
        assertEquals(1, meta.serviceCount)
        assertEquals(1, meta.exportedServices)
        assertEquals(1, meta.receiverCount)
        assertEquals(0, meta.exportedReceivers)
        assertEquals(true, meta.debuggable)
        assertEquals(true, meta.usesCleartextTraffic)
    }

    @Test
    fun extractsSdkVersionsFromGradle() {
        val gradle = """
            android {
                defaultConfig {
                    applicationId = "dev.example.demo"
                    minSdk = 24
                    targetSdk = 35
                    versionName = "1.2.3"
                }
            }
        """.trimIndent()
        val meta = ManifestParser.parse("<manifest></manifest>", "owner/repo", gradle)

        assertEquals(24, meta.minSdk)
        assertEquals(35, meta.targetSdk)
        assertEquals("1.2.3", meta.versionName)
    }

    @Test
    fun parsesGithubUrls() {
        assertEquals(
            "android" to "nowinandroid",
            GithubRepoFetcher.parseRepoUrl("https://github.com/android/nowinandroid"),
        )
        assertEquals(
            "owner" to "repo",
            GithubRepoFetcher.parseRepoUrl("github.com/owner/repo.git"),
        )
        assertEquals(
            "owner" to "repo",
            GithubRepoFetcher.parseRepoUrl("https://github.com/owner/repo/tree/main/app"),
        )
        // soft keyboards auto-capitalize; hostnames are case-insensitive
        assertEquals(
            "owner" to "repo",
            GithubRepoFetcher.parseRepoUrl("GitHub.com/owner/repo"),
        )
        assertNull(GithubRepoFetcher.parseRepoUrl("https://gitlab.com/owner/repo"))
        assertNull(GithubRepoFetcher.parseRepoUrl("not a url"))
    }

    @Test
    fun choosesManifestInNonStandardLayouts() {
        // monorepo layout: Android client nested beside a server
        val monorepo = listOf(
            "README.md",
            "server/main.ts",
            "client/app/src/main/AndroidManifest.xml",
            "client/app/src/test/AndroidManifest.xml",
            "client/buildSrc/src/main/AndroidManifest.xml",
        )
        assertEquals(
            listOf("client/app/src/main/AndroidManifest.xml"),
            GithubRepoFetcher.chooseManifestPaths(monorepo),
        )

        // app module ranks above library modules
        val multiModule = listOf(
            "core/src/main/AndroidManifest.xml",
            "app/src/main/AndroidManifest.xml",
        )
        assertEquals(
            "app/src/main/AndroidManifest.xml",
            GithubRepoFetcher.chooseManifestPaths(multiModule).first(),
        )

        // a sample-only repo still resolves instead of failing
        val sampleOnly = listOf("sample/src/main/AndroidManifest.xml")
        assertEquals(sampleOnly, GithubRepoFetcher.chooseManifestPaths(sampleOnly))

        assertEquals(emptyList(), GithubRepoFetcher.chooseManifestPaths(listOf("README.md")))
    }

    @Test
    fun emptyManifestProducesEmptyMetadata() {
        val meta = ManifestParser.parse("<manifest></manifest>", "owner/repo")
        assertTrue(meta.permissions.isEmpty())
        assertEquals(0, meta.activityCount)
        assertEquals(false, meta.debuggable)
    }
}
