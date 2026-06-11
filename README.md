# ✈️ Preflight

> **Clear your app for takeoff.**

Preflight is an **on-device pre-submission auditor for Android apps**. Point it at an APK
or a public GitHub repo, and it scans for the things that get apps rejected from Google
Play — security misconfigurations, policy violations, privacy red flags, tracker SDKs and
accessibility gaps — then has **Gemini Nano** write you a reviewer-style verdict.

No server. No API keys. No code ever leaves your device. **Total running cost: $0.**

## Why "Preflight"?

Pilots never take off without a **preflight check** — a disciplined walk around the
aircraft to catch problems while they're still cheap to fix. Developers know the word
too: preflight requests, preflight scripts, preflight checklists. That's exactly what
this app is: the systematic check you run **before** you submit to the Play Store, so
the reviewer finds nothing you didn't already know about. Your app, cleared for takeoff.

## What it catches

| Category | Examples |
|---|---|
| **Security** | `debuggable` builds, cleartext traffic, full-data backups, exported components |
| **Play Policy** | outdated `targetSdk`, Play-restricted permissions (SMS, call log, `QUERY_ALL_PACKAGES`…), accessibility-service misuse |
| **Privacy** | tracker/ad SDKs found by scanning dex bytecode (Exodus-Privacy style), dangerous-permission footprint |
| **Experience** | APK/dex bloat, legacy x86 native libs, activity sprawl |
| **Accessibility** | overlay permissions, pointers to runtime accessibility checks |

Every finding comes with severity, an explanation, and a concrete fix. The report ends
in a 0–100 score with a grade — and if you've scanned that app before, a
**"▲ +26 since last scan"** delta.

## How it works

```
APK / public GitHub repo / self-audit / bundled sample
        ↓
Static analysis            manifest, permissions, policy rules,
                           dex tracker scan, size & ABI footprint
        ↓
On-device AI               Gemini Nano (ML Kit GenAI Prompt API) writes the verdict;
                           a rule-based engine takes over on devices without Nano
        ↓
Animated report            score ring, grade, prioritized fixes, score delta,
                           shareable as Markdown — everything stays local
```

- **APK mode** (Android): inspects any APK via `PackageManager`, then walks the archive
  itself — dex sizes, native ABIs, and a byte-scan of `classes*.dex` for ~20 known
  tracker SDK signatures.
- **GitHub mode** (Android + iOS): fetches `AndroidManifest.xml` and the matching Gradle
  script from **public repos only** — every request is anonymous; the app never holds
  credentials. Manifest discovery uses the git-trees API, so monorepos and non-standard
  layouts (`client/app/…`) work.
- **Self-audit**: one tap runs the full pipeline on Preflight's own installed package.

## Tech

- **Kotlin Multiplatform + Compose Multiplatform** — one codebase, shared UI, Android & iOS
- **MVI architecture** — single immutable `StateFlow` per screen, intents in, effects out
- **Gemini Nano** via ML Kit GenAI Prompt API (`com.google.mlkit:genai-prompt`) — free,
  offline LLM inference on supported devices (Pixel 9+, recent Galaxy S)
- **Lottie via [Compottie](https://github.com/alexzhirkevich/compottie)** — the KMP Lottie
  renderer (radar scan, success check, hero orb)
- **Ktor** for the GitHub fetcher, **kotlinx.serialization** for report persistence
- Custom Compose animation throughout: animated score gauge, aurora background,
  staggered entrances, typewriter AI text

## Project structure

```
shared/
  commonMain/
    mvi/              MviViewModel base (Intent → reduce → State + Effects)
    domain/
      analyzer/       Security, Permission, Policy, Accessibility,
                      Tracker, Footprint analyzers + scoring
      ai/             AiEngine interface + rule-based engine
      AnalysisPipeline.kt   staged Flow the UI animates against
    data/             GitHub fetcher, manifest parser, history store,
                      Markdown exporter, tracker signatures
    ui/               theme, animated components, Home / Analyzing / Report
  androidMain/        APK inspector, dex scanner, Gemini Nano engine,
                      file picker, share sheet
  iosMain/            GitHub + sample modes, share sheet
androidApp/           Android entry point + adaptive launcher icon
iosApp/               iOS (SwiftUI) entry point
```

## Getting started

Requirements: JDK 17+, Android Studio (or just the SDK), a device/emulator on API 26+.

```bash
# Android app
./gradlew :androidApp:assembleDebug      # build
./gradlew :androidApp:installDebug       # install to a connected device

# tests
./gradlew :shared:testAndroidHostTest
```

iOS: open [`iosApp/`](./iosApp) in Xcode on macOS and run. (APK inspection and Gemini
Nano are Android-only; iOS gets GitHub mode and the sample audit.)

> **Gemini Nano note:** on supported devices the first scan may trigger a one-time
> on-device model download; until it completes — and on all other devices — the
> rule-based engine produces the insight, and the report says so honestly.

## Privacy stance

- Analysis is 100% local; APKs are never uploaded anywhere.
- GitHub mode talks only to public, unauthenticated GitHub endpoints.
- Scan history lives in app-private storage on your device.
- The app has exactly one permission: `INTERNET` (for GitHub mode).

## Roadmap

- [ ] Per-category score breakdown
- [ ] Desktop target (drag-and-drop APKs) — the KMP setup makes this nearly free
- [ ] CLI / GitHub Action wrapping the same shared analyzers for CI
- [ ] Rule packs as versioned data, open to community contributions

## License

[MIT](./LICENSE) — © 2026 Abhijeet
