package com.example.appreviewer_1.data

import com.example.appreviewer_1.domain.model.AppMetadata
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pulls AndroidManifest.xml (and the matching build.gradle) straight from
 * raw.githubusercontent.com — no API key needed.
 *
 * PUBLIC REPOSITORIES ONLY, by design: every request is anonymous (no token,
 * no auth header is ever attached), so private repos are unreachable and
 * always surface as "not accessible". Keep it that way — this app's promise
 * is that it never holds credentials.
 *
 * Manifest discovery uses the unauthenticated git-trees API (60 req/h), which
 * finds manifests in any module layout (app/, client/app/, monorepos…). If the
 * API is rate-limited, a list of conventional paths is probed instead.
 */
class GithubRepoFetcher(private val client: HttpClient = HttpClient()) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(repoUrl: String): AppMetadata {
        val (owner, repo) = parseRepoUrl(repoUrl)
            ?: throw IllegalArgumentException("Not a valid GitHub repository URL")

        val manifestPaths = discoverManifestPaths(owner, repo) ?: FALLBACK_PATHS

        for (path in manifestPaths) {
            val manifest = rawFile(owner, repo, path) ?: continue
            val moduleDir = path.removeSuffix("src/main/AndroidManifest.xml")
            val gradle = rawFile(owner, repo, "${moduleDir}build.gradle.kts")
                ?: rawFile(owner, repo, "${moduleDir}build.gradle")
            return ManifestParser.parse(
                manifestXml = manifest,
                sourceLabel = "$owner/$repo",
                gradleScript = gradle,
            )
        }
        throw IllegalStateException(
            "No AndroidManifest.xml found in $owner/$repo — is it an Android project?"
        )
    }

    /**
     * Lists the repo tree and returns manifest paths ranked by likelihood of
     * being the main app module. Returns null when the API can't be used
     * (rate limit, network hiccup) so the caller can fall back; throws when
     * the repo itself isn't accessible — that's a user-facing condition, not
     * a discovery failure.
     */
    private suspend fun discoverManifestPaths(owner: String, repo: String): List<String>? {
        val response = try {
            client.get("https://api.github.com/repos/$owner/$repo/git/trees/HEAD?recursive=1")
        } catch (e: Exception) {
            return null
        }

        if (response.status == HttpStatusCode.NotFound) {
            throw IllegalStateException(
                "$owner/$repo isn't accessible — only public GitHub repos are supported. " +
                    "It may be private, renamed, or misspelled."
            )
        }
        if (!response.status.isSuccess()) return null

        val paths = try {
            json.parseToJsonElement(response.bodyAsText())
                .jsonObject["tree"]?.jsonArray
                ?.mapNotNull { it.jsonObject["path"]?.jsonPrimitive?.content }
        } catch (e: Exception) {
            null
        } ?: return null

        val manifests = chooseManifestPaths(paths)
        if (manifests.isEmpty()) {
            throw IllegalStateException(
                "No AndroidManifest.xml found anywhere in $owner/$repo — is it an Android project?"
            )
        }
        return manifests
    }

    private suspend fun rawFile(owner: String, repo: String, path: String): String? {
        val response = try {
            client.get("https://raw.githubusercontent.com/$owner/$repo/HEAD/$path")
        } catch (e: Exception) {
            return null
        }
        return if (response.status.isSuccess()) response.bodyAsText() else null
    }

    companion object {
        private val FALLBACK_PATHS = listOf(
            "app/src/main/AndroidManifest.xml",
            "androidApp/src/main/AndroidManifest.xml",
            "composeApp/src/main/AndroidManifest.xml",
            "android/app/src/main/AndroidManifest.xml",
            "src/main/AndroidManifest.xml",
        )

        fun parseRepoUrl(url: String): Pair<String, String>? {
            // IGNORE_CASE: soft keyboards auto-capitalize ("GitHub.com/…"), and
            // hostnames are case-insensitive anyway.
            val match = Regex(
                """(?:https?://)?(?:www\.)?github\.com/([\w.-]+)/([\w.-]+?)(?:\.git)?(?:[/?#].*)?$""",
                RegexOption.IGNORE_CASE,
            ).find(url.trim()) ?: return null
            return match.groupValues[1] to match.groupValues[2]
        }

        /**
         * Filters a repo file listing down to production manifests and ranks
         * them: app-ish module names first, then shallower paths.
         */
        fun chooseManifestPaths(paths: List<String>): List<String> {
            val all = paths.filter { it.endsWith("src/main/AndroidManifest.xml") }
            val production = all.filterNot { path ->
                val lower = path.lowercase()
                listOf("buildsrc", "/test", "sample", "benchmark", "lint")
                    .any { lower.contains(it) }
            }
            return production.ifEmpty { all }
                .sortedWith(
                    compareByDescending<String> { path ->
                        val module = path.removeSuffix("/src/main/AndroidManifest.xml")
                            .substringAfterLast('/')
                            .lowercase()
                        module.contains("app") || module.contains("android")
                    }.thenBy { path -> path.count { it == '/' } }
                )
        }
    }
}
