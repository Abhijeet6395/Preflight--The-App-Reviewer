# Preflight — AI Workflow & Nano Control Points

How Preflight generates its "reviewer verdict" today, where every piece lives, when
it runs, and the exact seams where you can take control of the on-device model.

---

## TL;DR

- **Model:** Google **Gemini Nano**, fully on-device.
- **API:** Google **ML Kit GenAI Prompt API** (`com.google.mlkit:genai-prompt:1.0.0-beta2`).
- **Framework:** none. No LangChain, no LangGraph, no agent/RAG layer — a single
  `generateContent(prompt)` call wrapped in Kotlin.
- **Fallback:** a deterministic, dependency-free `RuleBasedAiEngine` (pure Kotlin).
- **Platforms:** Kotlin Multiplatform. Android runs Nano; iOS uses the rule-based engine.
- **Cost / privacy:** $0, no API keys, nothing leaves the device.

---

## The cast (where each piece lives)

| Component | File | Role |
|---|---|---|
| Orchestrator | `domain/AnalysisPipeline.kt` | Staged `Flow<PipelineEvent>` the UI animates |
| Engine interface | `domain/ai/AiEngine.kt` | `AiEngine` + `RuleBasedAiEngine` + `topPriorities()` |
| Nano engine | `androidMain/platform/GeminiNanoAiEngine.kt` | The **only** place Nano is touched; holds `buildPrompt()` |
| Engine selection | `platform/PlatformServices.{android,ios}.kt` | `createAiEngine()` picks the engine per platform |
| Entry point | `ui/reviewer/ReviewerViewModel.kt` | Starts the pipeline on user intent |

---

## End-to-end sequence

```
User taps  Scan an APK / GitHub repo / self-audit / sample
   │  ReviewerViewModel.startAnalysis()              (viewModelScope coroutine)
   ▼
AnalysisPipeline.run(inspect)  → emits Flow<PipelineEvent> the UI animates
   │
   ├─ Stage INSPECT  "Unpacking"        metadata = inspect()     ← reads APK/repo (Dispatchers.IO)
   ├─ Stage STATIC   "Static analysis"  findings  = analyzers.flatMap { analyze(metadata) }
   ├─ Stage AI       "AI insights"      insight   = aiEngine.generateInsight(metadata, findings)  ← NANO
   └─ Stage REPORT   "Report"           score     = ScoreCalculator.score(findings)
   ▼
PipelineEvent.Completed(AnalysisReport) → ViewModel stores it → ReportScreen
```

The engine is chosen **once**, when `ReviewerViewModel` is constructed:
`createAiEngine()` → on Android returns `GeminiNanoAiEngine(fallback = RuleBasedAiEngine())`.

---

## Inside the AI stage (`GeminiNanoAiEngine.generateInsight`)

1. `Generation.getClient()` — get the ML Kit GenAI Prompt client.
2. `model.checkStatus()`:
   - **AVAILABLE** → proceed.
   - **DOWNLOADABLE** → start the (multi-GB) model download on a background scope so it's
     ready *next* time, and **return the rule-based insight now** — no waiting on this scan.
   - **anything else** → rule-based insight.
3. `model.generateContent(buildPrompt(metadata, findings))` — the single Nano call.
   **This is the slow step** (several seconds, variable).
4. Read `response.candidates.first().text`; if blank → rule-based.
5. Wrap into `AiInsight(engineName, summary = Nano text, priorities = topPriorities(findings))`.
6. Any exception → rule-based fallback.

### Key design fact
Even when Nano runs, it **only writes the prose summary**. The "Fix first" list
(`priorities`) is **always** computed deterministically by `topPriorities()` in Kotlin —
the rules rank the fixes, the LLM just narrates. So today Nano controls exactly one thing:
the 2–3 sentence verdict text.

### The current prompt
`buildPrompt()` is a fixed template: a "Google Play reviewer" persona, the app name,
the *count* of permissions, tracker names, and finding *titles* by severity. It does not
yet use the specific permission names, the worst risk category, the target-SDK gap, or the
concrete finding details — so the output reads generic.

---

## Where you can take control of Nano

### First, what "fine-tune" can mean here
- **Weight-level fine-tuning of Nano on-device is _not_ available** through the ML Kit
  GenAI Prompt API in use. (Google's lower-level **AICore** SDK has historically had
  *experimental, allowlisted* LoRA-adapter fine-tuning for Nano — a separate, harder path,
  not `genai-prompt`. Verify against current docs if you truly want to touch weights.)
- What you realistically control — and it's a lot — is the **prompt**, the **generation
  parameters**, and the **post-processing**. That is "fine-tuning the response" in the
  practical, prompt-engineering sense.

There are three seams, all around the single `generateContent` call:

### Seam 1 — Pre-Nano: shape the prompt *(the big lever)*
`buildPrompt()` is the single chokepoint before the model runs. Promote it to a shared
`PromptBuilder` in `commonMain` and make it app-aware:
- name the *specific* Play-sensitive permissions (SMS, call log, `QUERY_ALL_PACKAGES`, …)
- call out the worst risk **category** and the **target-SDK gap**
- include exported-component exposure and concrete finding details (not just titles)
- switch the **instruction** by risk profile (critical → "explain the rejection + the one
  fix that matters"; clean → "give a confident go")
- optionally add **few-shot examples** and a strict **output format**

This is the cleanest place to "take control before Nano takes over."

### Seam 2 — The Nano call config: generation parameters
The ML Kit GenAI Prompt API lets you pass a generation config (temperature, topK, max
output tokens) when building the client. Lower temperature → more consistent, reviewer-like
verdicts; cap tokens to enforce brevity. Today none is passed (pure defaults).
*(Confirm the exact config class for `genai-prompt:1.0.0-beta2` — the beta surface shifts.)*

### Seam 3 — Post-Nano: guard and reshape the output
After `generateContent` the code currently just `trim()`s. This is where you enforce
control over what Nano produced:
- validate length / format, strip markdown if it leaked
- reject and **retry with a stricter prompt** if the output is off
- merge with / override using your deterministic data

A reject-and-retry loop is the closest thing to "supervising" Nano without touching weights.

---

## Why not LangChain / LangGraph?

- They are Python / JS libraries — they cannot run inside an Android/KMP app. The JVM
  equivalents are LangChain4j / langgraph4j.
- Those target **cloud** LLMs (API keys, server calls), not on-device Nano — adopting them
  would break the $0 / offline / privacy promise.
- They are JVM-only, so they cannot live in `commonMain` or compile for iOS.
- The use case is a **single prompt → single completion**; orchestration frameworks add
  dependencies and indirection with no payoff.

**Recommendation:** keep the direct ML Kit call and put the intelligence in a small,
testable Kotlin `PromptBuilder` (Seam 1), optionally with generation config (Seam 2) and
output guardrails (Seam 3).
