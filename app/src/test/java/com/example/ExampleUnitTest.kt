package com.example

import com.example.data.local.SubjectEntity
import com.example.domain.AttendanceCalculator
import com.example.domain.AttendanceZone
import com.example.domain.DuplicateDecision
import com.example.domain.VisionQualityPipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun attendanceCalculator_calculatesSafeBunksAndLabWeightsCorrectly() {
        val thermo = SubjectEntity(
            id = 1L,
            code = "ME-204",
            name = "Thermodynamics",
            professorName = "Prof. Verma",
            targetPercentage = 75.0,
            labWeightClasses = 2,
            medicalLeaveExcused = true,
            roundingMode = "NEAREST",
            attendedClasses = 26,
            totalClasses = 32,
            medicalExcusedClasses = 1
        )
        val analysis = AttendanceCalculator.analyze(thermo)
        // Effective attended = 27, total = 32 -> 84.4% (SAFE_GREEN > 78%)
        // floor((27 - 0.75 * 32) / 0.75) = floor(3 / 0.75) = 4
        assertTrue(analysis.isAboveThreshold)
        assertEquals(AttendanceZone.SAFE_GREEN, analysis.zone)
        assertEquals(0, analysis.mustAttendClasses)
        assertEquals(4, analysis.safeToSkipClasses)
        assertEquals(2, analysis.safeToSkipLabs)
        assertEquals("You can skip the next 4 classes and still stay at 75%.", analysis.liquidPillCopy)
    }

    @Test
    fun attendanceCalculator_detectsWarningYellowBufferZoneAndTutorialBonus() {
        val math = SubjectEntity(
            id = 3L,
            code = "MA-302",
            name = "Engineering Math",
            professorName = "Prof. Kulkarni",
            targetPercentage = 75.0,
            labWeightClasses = 1,
            medicalLeaveExcused = false,
            tutorialBonusPoints = 1,
            roundingMode = "NEAREST",
            attendedClasses = 22,
            totalClasses = 30
        )
        // Effective attended = 22 + 1 bonus = 23 / 30 = 76.7% -> WARNING_YELLOW (between 75% and 78%)
        val analysis = AttendanceCalculator.analyze(math)
        assertTrue(analysis.isAboveThreshold)
        assertEquals(AttendanceZone.WARNING_YELLOW, analysis.zone)
    }

    @Test
    fun attendanceCalculator_calculatesMustAttendWhenBelowThreshold() {
        val signals = SubjectEntity(
            id = 2L,
            code = "EC-304",
            name = "Signals & Systems",
            professorName = "Dr. Nair",
            targetPercentage = 75.0,
            labWeightClasses = 2,
            medicalLeaveExcused = false,
            roundingMode = "FLOOR",
            attendedClasses = 18,
            totalClasses = 28,
            medicalExcusedClasses = 0
        )
        val analysis = AttendanceCalculator.analyze(signals)
        assertFalse(analysis.isAboveThreshold)
        assertEquals(AttendanceZone.DANGER_RED, analysis.zone)
        assertEquals(12, analysis.mustAttendClasses)
        assertEquals(0, analysis.safeToSkipClasses)
        assertEquals("Attend the next 12 classes straight to recover to 75%.", analysis.liquidPillCopy)
    }

    @Test
    fun visionPipeline_confirmsDuplicateWhenHashAndSsimBothMatch() {
        val hashA = "A4F19C3B82D011E4"
        val hashB = "A4F19C3B82D011E5"
        val eval = VisionQualityPipeline.evaluateDuplicatePair(
            phashA = hashA,
            dhashA = hashA,
            phashB = hashB,
            dhashB = hashB,
            ssimScore = 0.93
        )
        assertEquals(DuplicateDecision.CONFIRMED_DUPLICATE_SEND_LOWER_TO_TRASH, eval.decision)
    }

    @Test
    fun visionPipeline_flagsPossibleDuplicateWhenHashAndSsimDisagree() {
        val hashA = "A4F19C3B82D011E4"
        val hashB = "A4F19C3B82D011E0"
        val eval = VisionQualityPipeline.evaluateDuplicatePair(
            phashA = hashA,
            dhashA = hashA,
            phashB = hashB,
            dhashB = hashB,
            ssimScore = 0.65
        )
        assertEquals(DuplicateDecision.POSSIBLE_DUPLICATE_KEEP_BOTH_FLAGGED, eval.decision)
    }

    @Test
    fun attendanceCalculator_ceilRoundingNeverContradictsThresholdStatus() {
        val subject = SubjectEntity(
            id = 5L,
            code = "CS-401",
            name = "Advanced Algorithms",
            professorName = "Dr. Rao",
            targetPercentage = 75.0,
            roundingMode = "CEIL",
            attendedClasses = 7495,
            totalClasses = 10000
        )
        val analysis = AttendanceCalculator.analyze(subject)
        assertTrue(analysis.isAboveThreshold)
        assertEquals(0, analysis.mustAttendClasses)
        assertFalse(analysis.statusBadge.contains("ATTEND +"))
    }

    @Test
    fun attendanceCalculator_zeroTotalClassesHandledGracefully() {
        val subject = SubjectEntity(
            id = 6L,
            code = "NEW-101",
            name = "Intro to Quantum Computing",
            professorName = "Dr. Bose",
            targetPercentage = 75.0,
            attendedClasses = 0,
            totalClasses = 0
        )
        val analysis = AttendanceCalculator.analyze(subject)
        assertEquals(0, analysis.mustAttendClasses)
        assertEquals(0, analysis.safeToSkipClasses)
        assertEquals("NO CLASSES RECORDED", analysis.statusBadge)
        assertTrue(analysis.liquidPillCopy.contains("No classes recorded yet"))
    }

    @Test
    fun attendanceCalculator_highTargetPercentageHandledWithoutDivisionByZero() {
        val subject = SubjectEntity(
            id = 7L,
            code = "AI-501",
            name = "Deep Learning",
            professorName = "Prof. LeCun",
            targetPercentage = 100.0,
            attendedClasses = 10,
            totalClasses = 10
        )
        val analysis = AttendanceCalculator.analyze(subject)
        assertTrue(analysis.isAboveThreshold)
        assertEquals(0, analysis.mustAttendClasses)
    }

    @Test
    fun timetableAlarmScheduler_computesFutureTriggerTimeCorrectly() {
        val triggerTime = com.example.core.TimetableAlarmScheduler.computeNextTriggerTime(
            targetDayOfWeek = 3, // Wednesday
            startTimeMinutes = 10 * 60 + 10 // 10:10 AM
        )
        assertTrue(triggerTime > 0)
    }
}
