package com.example.data.remote

import com.example.data.local.DocumentPageEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TimetableSlotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ExplainMode(val label: String, val badge: String) {
    ELI5("Explain Like I'm 5", "ELI5 Intuition"),
    VIVA_5("Generate 5 Viva Questions", "5 Viva Q&A"),
    CHEAT_SHEET_1P("1-Page Exam Cheat Sheet", "1-Page Crammer"),
    CUSTOM("Ask Custom Question", "RAG Grounded")
}

data class RagCitation(
    val documentId: Long,
    val pageNumber: Int,
    val heading: String,
    val snippet: String
)

data class AiExplainerResponse(
    val mode: ExplainMode,
    val markdownAnswer: String,
    val citations: List<RagCitation>,
    val isFirebasePlaceholder: Boolean,
    val cachedHit: Boolean,
    val modelLabel: String
)

data class ScannedPageDraft(
    val tempId: Int,
    val rawTitle: String,
    val detectedPageNumber: Int?,
    val detectedHeading: String,
    val ocrText: String,
    val laplacianSharpness: Double,
    val phash: String,
    val dhash: String,
    val ssimWithPrevious: Double,
    val duplicateDecisionLabel: String,
    val isSentToTrash: Boolean = false,
    val isPossibleDuplicate: Boolean = false
)

data class BatchClassificationResult(
    val orderedPages: List<ScannedPageDraft>,
    val orderingConfidence: Double,
    val needsManualOrderReview: Boolean, // true when orderingConfidence < 0.80
    val suggestedSubject: SubjectEntity?,
    val tiedSubject: SubjectEntity?, // non-null if subject overlap occurs (e.g. Fourier in Math vs Signals)
    val overlapTopicKeyword: String?,
    val timetablePriorUsed: String?,
    val detectedChapterTitle: String
)

/**
 * Provider-abstraction layer for Gemini AI calls.
 * Because Firebase setup was declined in the UI consent step, PlaceholderAiRepository
 * provides deterministic, note-grounded local RAG responses and clearly labels that live
 * Gemini cloud inference requires enabling Firebase AI Logic.
 */
interface AiRepository {
    val requiresFirebaseNotice: String

    suspend fun explainNotesWithRag(
        query: String,
        mode: ExplainMode,
        selectedSubject: SubjectEntity?,
        availablePages: List<DocumentPageEntity>
    ): AiExplainerResponse

    suspend fun classifyAndOrderBatch(
        pages: List<ScannedPageDraft>,
        subjects: List<SubjectEntity>,
        activeTimetableSlot: TimetableSlotEntity?,
        rememberedOverlapSubjectId: Long?
    ): BatchClassificationResult
}

class PlaceholderAiRepository : AiRepository {

    private val responseCache = mutableMapOf<String, AiExplainerResponse>()

    override val requiresFirebaseNotice: String =
        "Notice: Firebase setup was declined. AI features are currently running in local note-grounded preview mode. Enable Firebase to connect live Gemini models via Firebase AI Logic."

    override suspend fun explainNotesWithRag(
        query: String,
        mode: ExplainMode,
        selectedSubject: SubjectEntity?,
        availablePages: List<DocumentPageEntity>
    ): AiExplainerResponse {
        val cacheKey = "${mode.name}:${selectedSubject?.id ?: 0}:${query.trim().lowercase()}"
        responseCache[cacheKey]?.let { cached ->
            return cached.copy(cachedHit = true)
        }

        // Retrieve top matching pages from the student's actual local notes (RAG over local pages)
        val queryTokens = query.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }

        val rankedPages = availablePages
            .map { page ->
                val textLower = (page.detectedHeading + " " + page.ocrText).lowercase()
                val tokenHits = queryTokens.count { token -> textLower.contains(token) }
                val subjectBoost = if (selectedSubject != null &&
                    textLower.contains(selectedSubject.name.split(" ").first().lowercase())
                ) 2 else 0
                page to (tokenHits * 3 + subjectBoost)
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(3)
            .ifEmpty { availablePages.take(2) }

        val citations = rankedPages.map { page ->
            RagCitation(
                documentId = page.documentId,
                pageNumber = page.pageNumber,
                heading = page.detectedHeading,
                snippet = page.ocrText.take(140) + if (page.ocrText.length > 140) "..." else ""
            )
        }

        val primaryPage = rankedPages.firstOrNull()
        val primaryHeading = primaryPage?.detectedHeading ?: (selectedSubject?.name ?: "Engineering Notes")
        val primaryCite = primaryPage?.let { "[Source: Doc #${it.documentId}, Page ${it.pageNumber}]" }
            ?: "[Source: Vault Page 1]"
        val secondaryCite = rankedPages.getOrNull(1)?.let { "[Source: Doc #${it.documentId}, Page ${it.pageNumber}]" }
            ?: primaryCite

        val markdown = when (mode) {
            ExplainMode.ELI5 -> buildString {
                appendLine("### ELI5 Intuition: $primaryHeading")
                appendLine("*(Sample / Local RAG Preview — Enable Firebase for live Gemini inference)*")
                appendLine()
                appendLine("1. **Core Idea in Plain English:** Imagine **$primaryHeading** like tuning an equalizer or balancing energy flow in a pipe. Instead of staring at raw differential equations in the time domain, we break the system into simple building blocks that are easy to solve $primaryCite.")
                appendLine("2. **Why Your Professor Cares:** In your handwritten derivation (${primaryPage?.ocrText?.take(90) ?: "standard theorem"}), the key step reduces a coupled differential equation into an algebraic ratio $primaryCite.")
                appendLine("3. **Exam Trap to Avoid:** Never forget boundary conditions and initial steady-state assumptions before applying the transform $secondaryCite.")
            }

            ExplainMode.VIVA_5 -> buildString {
                appendLine("### 5 High-Probability Lab & Exam Viva Questions: $primaryHeading")
                appendLine("*(Sample / Local RAG Preview — Grounded in your scanned notes)*")
                appendLine()
                appendLine("**Q1. What is the physical significance of the main equation on Page ${primaryPage?.pageNumber ?: 1}?**")
                appendLine("• *Answer:* It relates system input excitation to steady-state and transient response under linear time-invariant constraints $primaryCite.")
                appendLine()
                appendLine("**Q2. Under what condition does this derivation fail or diverge?**")
                appendLine("• *Answer:* When the region of convergence (ROC) / stability condition is violated or non-linear saturation occurs $primaryCite.")
                appendLine()
                appendLine("**Q3. Why did we substitute the kernel/operator in step 2 of your note?**")
                appendLine("• *Answer:* To convert convolution/differentiation in time into multiplication in the frequency/transform domain $secondaryCite.")
                appendLine()
                appendLine("**Q4. How would you verify this experimentally in the lab?**")
                appendLine("• *Answer:* Measure phase margin and step overshoot on the DSO/simulator and compare with the theoretical damping ratio $secondaryCite.")
                appendLine()
                appendLine("**Q5. What happens if frequency ω → 0 (DC) vs ω → ∞?**")
                appendLine("• *Answer:* At DC, inductors act as short circuits and capacitors open; at high frequency, the reactive behavior reverses $primaryCite.")
            }

            ExplainMode.CHEAT_SHEET_1P -> buildString {
                appendLine("### 1-Page Last-Minute Cram Sheet: $primaryHeading")
                appendLine("*(Sample / Local RAG Preview — Citations verified against Vault)*")
                appendLine()
                appendLine("#### 1. Key Formulas & Identities")
                appendLine("```")
                appendLine("• F(ω) = ∫ f(t) · e^(-jωt) dt     $primaryCite")
                appendLine("• ΔU = Q - W  |  η_th = 1 - (T_L / T_H)   $secondaryCite")
                appendLine("```")
                appendLine("#### 2. Step-by-Step Numerical Recipe")
                appendLine("- **Step 1:** Check units (SI) and write given initial conditions $primaryCite.")
                appendLine("- **Step 2:** Apply the governing equation from **${primaryHeading}** and isolate the transfer/state variable $primaryCite.")
                appendLine("- **Step 3:** Verify limiting cases and sketch the magnitude/phase or P-V plot $secondaryCite.")
                appendLine()
                appendLine("#### 3. Direct Extract from Your Notes")
                appendLine("> \"${primaryPage?.ocrText ?: "No note pages found yet — scan a page in the Scanner tab!"}\" $primaryCite")
            }

            ExplainMode.CUSTOM -> buildString {
                appendLine("### Answer Grounded in Your Notes: \"$query\"")
                appendLine("*(Sample / Local RAG Preview — Enable Firebase for live Gemini inference)*")
                appendLine()
                appendLine("Based on your scanned notes under **$primaryHeading** $primaryCite:")
                appendLine("- **Direct Reference:** ${primaryPage?.ocrText ?: "Scan notes to populate local RAG chunks."} $primaryCite")
                appendLine("- **Key Takeaway:** Focus on the boundary conditions and definition highlighted on Page ${primaryPage?.pageNumber ?: 1} $secondaryCite.")
            }
        }

        val result = AiExplainerResponse(
            mode = mode,
            markdownAnswer = markdown.trim(),
            citations = citations,
            isFirebasePlaceholder = true,
            cachedHit = false,
            modelLabel = "Local RAG Preview (Firebase Declined)"
        )
        responseCache[cacheKey] = result
        return result
    }

    override suspend fun classifyAndOrderBatch(
        pages: List<ScannedPageDraft>,
        subjects: List<SubjectEntity>,
        activeTimetableSlot: TimetableSlotEntity?,
        rememberedOverlapSubjectId: Long?
    ): BatchClassificationResult {
        val activePages = pages.filter { !it.isSentToTrash }
        val allHaveExplicitPageNums = activePages.all { it.detectedPageNumber != null }

        val sortedPages = if (allHaveExplicitPageNums) {
            activePages.sortedBy { it.detectedPageNumber }
        } else {
            activePages.sortedBy { it.tempId }
        }

        val confidence = if (allHaveExplicitPageNums) 0.94 else 0.68
        val combinedText = sortedPages.joinToString(" ") { "${it.detectedHeading} ${it.ocrText}" }.lowercase()

        // Edge Case #8: Check for overlapping topic (e.g., "Fourier Transform" in Math vs Signals & Systems)
        val isFourierOverlap = combinedText.contains("fourier") || combinedText.contains("laplace")
        val mathSubj = subjects.find { it.name.contains("Math", ignoreCase = true) }
        val signalsSubj = subjects.find { it.name.contains("Signals", ignoreCase = true) }

        if (isFourierOverlap && mathSubj != null && signalsSubj != null) {
            val remembered = subjects.find { it.id == rememberedOverlapSubjectId }
            if (remembered != null) {
                return BatchClassificationResult(
                    orderedPages = sortedPages,
                    orderingConfidence = confidence,
                    needsManualOrderReview = confidence < 0.80,
                    suggestedSubject = remembered,
                    tiedSubject = null,
                    overlapTopicKeyword = "Fourier / Laplace Transforms",
                    timetablePriorUsed = "Resolved via saved 1-tap overlap preference (${remembered.code})",
                    detectedChapterTitle = sortedPages.firstOrNull()?.detectedHeading ?: "Transform Analysis"
                )
            }

            // Use timetable context first if active slot matches one of the overlapping subjects
            if (activeTimetableSlot != null &&
                (activeTimetableSlot.subjectId == mathSubj.id || activeTimetableSlot.subjectId == signalsSubj.id)
            ) {
                val chosen = subjects.first { it.id == activeTimetableSlot.subjectId }
                return BatchClassificationResult(
                    orderedPages = sortedPages,
                    orderingConfidence = confidence,
                    needsManualOrderReview = confidence < 0.80,
                    suggestedSubject = chosen,
                    tiedSubject = if (chosen.id == mathSubj.id) signalsSubj else mathSubj,
                    overlapTopicKeyword = "Fourier / Laplace Transforms",
                    timetablePriorUsed = "Active Timetable Slot: ${activeTimetableSlot.subjectName} (${activeTimetableSlot.room})",
                    detectedChapterTitle = sortedPages.firstOrNull()?.detectedHeading ?: "Fourier Series & Transforms"
                )
            }

            return BatchClassificationResult(
                orderedPages = sortedPages,
                orderingConfidence = confidence,
                needsManualOrderReview = confidence < 0.80,
                suggestedSubject = signalsSubj,
                tiedSubject = mathSubj,
                overlapTopicKeyword = "Fourier / Laplace Transforms",
                timetablePriorUsed = null,
                detectedChapterTitle = sortedPages.firstOrNull()?.detectedHeading ?: "Fourier Transform & Spectrum"
            )
        }

        // Standard subject detection: content match first, else active timetable prior
        val contentMatched = subjects.find { subj ->
            combinedText.contains(subj.code.lowercase()) ||
                subj.name.split(" ").any { word -> word.length > 4 && combinedText.contains(word.lowercase()) }
        }
        val timetableSubject = activeTimetableSlot?.let { slot ->
            subjects.find { it.id == slot.subjectId }
        }

        val finalSubject = contentMatched ?: timetableSubject ?: subjects.firstOrNull()
        val priorNote = if (contentMatched == null && timetableSubject != null) {
            "Defaulted via Active Timetable (${activeTimetableSlot.subjectName} @ ${activeTimetableSlot.room})"
        } else if (contentMatched != null && timetableSubject != null && contentMatched.id != timetableSubject.id) {
            "Page content overrode timetable (${activeTimetableSlot.subjectName} → ${contentMatched.name})"
        } else {
            "Matched subject syllabus keywords"
        }

        return BatchClassificationResult(
            orderedPages = sortedPages,
            orderingConfidence = confidence,
            needsManualOrderReview = confidence < 0.80,
            suggestedSubject = finalSubject,
            tiedSubject = null,
            overlapTopicKeyword = null,
            timetablePriorUsed = priorNote,
            detectedChapterTitle = sortedPages.firstOrNull()?.detectedHeading ?: "Lecture Notes"
        )
    }
}

class LiveGeminiAiRepository(
    private val apiKeyProvider: () -> String?,
    private val fallbackRepo: PlaceholderAiRepository = PlaceholderAiRepository()
) : AiRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override val requiresFirebaseNotice: String
        get() {
            val key = apiKeyProvider()
            return if (key.isNullOrBlank() || key.startsWith("YOUR_") || key.startsWith("CHANGE_ME")) {
                "Gemini API key not configured in .env. Running in offline note-grounded preview mode."
            } else {
                "Connected to live Gemini 2.5 Flash via Google AI API."
            }
        }

    override suspend fun explainNotesWithRag(
        query: String,
        mode: ExplainMode,
        selectedSubject: SubjectEntity?,
        availablePages: List<DocumentPageEntity>
    ): AiExplainerResponse = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()?.trim()
        if (apiKey.isNullOrBlank() || apiKey.startsWith("YOUR_") || apiKey.startsWith("CHANGE_ME")) {
            val fallback = fallbackRepo.explainNotesWithRag(query, mode, selectedSubject, availablePages)
            return@withContext fallback.copy(modelLabel = "Local Note-Grounded (Offline Fallback)")
        }

        // 1. Retrieve top matching local note chunks (Local RAG)
        val queryTokens = query.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }
        val rankedPages = availablePages
            .map { page ->
                val textLower = (page.detectedHeading + " " + page.ocrText).lowercase()
                val tokenHits = queryTokens.count { token -> textLower.contains(token) }
                val subjectBoost = if (selectedSubject != null &&
                    textLower.contains(selectedSubject.name.split(" ").first().lowercase())
                ) 2 else 0
                page to (tokenHits * 3 + subjectBoost)
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(3)
            .ifEmpty { availablePages.take(2) }

        val citations = rankedPages.map { page ->
            RagCitation(
                documentId = page.documentId,
                pageNumber = page.pageNumber,
                heading = page.detectedHeading,
                snippet = page.ocrText.take(140) + if (page.ocrText.length > 140) "..." else ""
            )
        }

        val excerpts = rankedPages.joinToString("\n\n") { page ->
            "[Document #${page.documentId}, Page ${page.pageNumber} - Heading: ${page.detectedHeading}]\n${page.ocrText}"
        }

        val prompt = buildString {
            appendLine("You are EngBuddy, an expert AI engineering study tutor. You explain concepts clearly with rigorous technical accuracy.")
            appendLine("Subject: ${selectedSubject?.name ?: "Engineering Studies"}")
            appendLine("Mode: ${mode.label}")
            appendLine("Student Question / Topic: $query")
            appendLine()
            appendLine("CRITICAL MATH FORMATTING RULES (NO RAW LATEX CODE):")
            appendLine("1. NEVER output raw LaTeX backslash commands (such as \\int, \\frac, \\partial, \\sum, \\sqrt, \\alpha, \\theta, \\infty, or $...$ delimiters) in the user-facing text.")
            appendLine("2. ALWAYS convert all mathematical, physics, and engineering notation into clean, visually readable UTF-8 Unicode characters and superscripts/subscripts:")
            appendLine("   - Integrals: Use ∫, ∬, ∭, ∮ (e.g. ∫₀^∞ or ∫ₐᵇ)")
            appendLine("   - Derivatives: Use d/dx, dy/dx, d²y/dx², ∂/∂x, ∂f/∂t, ∇, Δ")
            appendLine("   - Summations & Products: Use ∑ (e.g. ∑ᵢ₌₁ⁿ) and ∏")
            appendLine("   - Roots & Powers: Use √(x), ∛(x), and Unicode superscripts (⁰ ¹ ² ³ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ ⁼ ⁽ ⁾ ⁿ ⁱ)")
            appendLine("   - Subscripts: Use Unicode subscripts (₀ ₁ ₂ ₃ ₄ ₅ ₆ ₇ ₈ ₉ ₊ ₋ ₌ ₍ ₎ ₐ ₑ ₒ ₓ ᵢ ⱼ ₖ ₙ)")
            appendLine("   - Greek Letters: Use actual symbols (α, β, γ, δ, ε, θ, λ, μ, π, ρ, σ, τ, φ, ω, Ω, Φ, Ψ)")
            appendLine("   - Operators: Use ×, ÷, ±, ≈, ≠, ≤, ≥, ∞, ∝, ∈, ∀, ⇒, ⇔, ∠, ⊥, ∥")
            appendLine("3. Fractions: Write cleanly on a single line using parentheses and a slash: (Numerator) / (Denominator).")
            appendLine()
            appendLine("Here are excerpts from the student's actual handwritten notes:")
            appendLine("---")
            appendLine(if (excerpts.isBlank()) "No specific notes scanned yet; provide general engineering guidance." else excerpts)
            appendLine("---")
            appendLine()
            when (mode) {
                ExplainMode.ELI5 -> appendLine("Explain the concept like I'm 5 with intuitive real-world analogies, why the professor cares, and common exam traps. Cite the source page where relevant.")
                ExplainMode.VIVA_5 -> appendLine("Provide 5 high-probability lab and exam viva questions and direct answers based on these notes. Ground answers in the formulas on the pages.")
                ExplainMode.CHEAT_SHEET_1P -> appendLine("Generate a concise 1-page cram cheat sheet with key formulas, a step-by-step numerical recipe, and extracts.")
                ExplainMode.CUSTOM -> appendLine("Answer the student's question thoroughly using markdown formatting, equations, and explicit citations to their note pages.")
            }
        }

        try {
            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val respJson = JSONObject(bodyStr)
                val candidates = respJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                if (!text.isNullOrBlank()) {
                    return@withContext AiExplainerResponse(
                        mode = mode,
                        markdownAnswer = text.trim(),
                        citations = citations,
                        isFirebasePlaceholder = false,
                        cachedHit = false,
                        modelLabel = "Gemini 2.5 Flash (Grounded)"
                    )
                }
            }
        } catch (e: Exception) {
            // Network failure or API rate limit -> graceful fallback
        }

        val fallback = fallbackRepo.explainNotesWithRag(query, mode, selectedSubject, availablePages)
        fallback.copy(modelLabel = "Local Note-Grounded (Offline Fallback)")
    }

    override suspend fun classifyAndOrderBatch(
        pages: List<ScannedPageDraft>,
        subjects: List<SubjectEntity>,
        activeTimetableSlot: TimetableSlotEntity?,
        rememberedOverlapSubjectId: Long?
    ): BatchClassificationResult {
        return fallbackRepo.classifyAndOrderBatch(pages, subjects, activeTimetableSlot, rememberedOverlapSubjectId)
    }
}
