package com.example

import com.example.data.local.DocumentPageEntity
import com.example.data.remote.ExplainMode
import com.example.data.remote.PlaceholderAiRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun placeholderAiRepository_groundsAnswersWithPageCitationsAndCachesResults() = runTest {
        val repo = PlaceholderAiRepository()
        val pages = listOf(
            DocumentPageEntity(
                id = 1L,
                documentId = 10L,
                pageNumber = 2,
                detectedHeading = "Carnot Cycle Efficiency",
                ocrText = "Thermal efficiency η = 1 - T_L/T_H for reversible Carnot heat engine.",
                laplacianSharpness = 180.0,
                phash = "A4F19C3B82D011E4",
                dhash = "9C31A8F042E1B305",
                ssimScore = 0.20,
                duplicateFlag = "NONE",
                confidence = 0.96
            )
        )

        val first = repo.explainNotesWithRag(
            query = "Explain Carnot efficiency",
            mode = ExplainMode.ELI5,
            selectedSubject = null,
            availablePages = pages
        )
        assertTrue(first.isFirebasePlaceholder)
        assertEquals(false, first.cachedHit)
        assertEquals(1, first.citations.size)
        assertEquals(2, first.citations.first().pageNumber)
        assertTrue(first.markdownAnswer.contains("[Source: Doc #10, Page 2]"))

        val second = repo.explainNotesWithRag(
            query = "Explain Carnot efficiency",
            mode = ExplainMode.ELI5,
            selectedSubject = null,
            availablePages = pages
        )
        assertTrue(second.cachedHit)
    }

    @Test
    fun liveGeminiAiRepository_gracefullyFallsBackWhenNoKey() = runTest {
        val repo = com.example.data.remote.LiveGeminiAiRepository(apiKeyProvider = { null })
        val pages = listOf(
            DocumentPageEntity(
                id = 1L,
                documentId = 10L,
                pageNumber = 2,
                detectedHeading = "Carnot Cycle Efficiency",
                ocrText = "Thermal efficiency η = 1 - T_L/T_H for reversible Carnot heat engine.",
                laplacianSharpness = 180.0,
                phash = "A4F19C3B82D011E4",
                dhash = "9C31A8F042E1B305",
                ssimScore = 0.20,
                duplicateFlag = "NONE",
                confidence = 0.96
            )
        )
        val res = repo.explainNotesWithRag(
            query = "Explain Carnot efficiency",
            mode = ExplainMode.ELI5,
            selectedSubject = null,
            availablePages = pages
        )
        assertTrue(res.markdownAnswer.isNotBlank())
        assertEquals("Local Note-Grounded (Offline Fallback)", res.modelLabel)
    }
}
