package com.example.domain

import com.example.data.local.SubjectEntity
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

enum class AttendanceZone {
    SAFE_GREEN,     // > 80% or comfortably above target
    WARNING_YELLOW, // Within +3% buffer above target (e.g. 75% - 78%)
    DANGER_RED      // Below target (< 75%)
}

data class SubjectAttendanceAnalysis(
    val subject: SubjectEntity,
    val effectiveAttended: Int,
    val effectiveTotal: Int,
    val rawPercentage: Double,
    val roundedPercentage: Double,
    val mustAttendClasses: Int,
    val safeToSkipClasses: Int,
    val safeToSkipLabs: Int,
    val isAboveThreshold: Boolean,
    val zone: AttendanceZone,
    val statusBadge: String,
    val liquidPillCopy: String,
    val formulaExplanation: String
)

object AttendanceCalculator {

    fun analyze(subject: SubjectEntity): SubjectAttendanceAnalysis {
        // Include medical leave if excused + tutorial bonus attendance points
        val baseAttended = if (subject.medicalLeaveExcused) {
            subject.attendedClasses + subject.medicalExcusedClasses
        } else {
            subject.attendedClasses
        }
        val effectiveAttended = baseAttended + max(0, subject.tutorialBonusPoints)
        val effectiveTotal = max(subject.totalClasses, effectiveAttended)

        val rawPct = if (effectiveTotal == 0) {
            100.0
        } else {
            (effectiveAttended.toDouble() / effectiveTotal.toDouble()) * 100.0
        }

        val roundedPct = when (subject.roundingMode.uppercase()) {
            "FLOOR" -> floor(rawPct * 10.0) / 10.0
            "CEIL" -> ceil(rawPct * 10.0) / 10.0
            else -> (rawPct * 10.0).roundToInt() / 10.0
        }

        val targetRatio = (subject.targetPercentage.coerceIn(0.1, 99.99)) / 100.0
        val isSafe = roundedPct >= subject.targetPercentage

        // Spec Formula C.2: If Attendance < Target (T):
        // Classes Needed = ceil(((T * Total) - Attended) / (1 - T))
        val mustAttend = if (effectiveTotal == 0 || isSafe) {
            0
        } else {
            val numerator = (targetRatio * effectiveTotal) - effectiveAttended
            val denominator = max(0.0001, 1.0 - targetRatio)
            max(1, ceil(numerator / denominator - 1e-9).toInt())
        }

        // Spec Formula C.1: If Attendance >= Target (T):
        // Safe Bunks = floor((Attended - (T * Total)) / T)
        val safeToSkip = if (effectiveTotal == 0 || !isSafe) {
            0
        } else {
            val safe = floor(((effectiveAttended - (targetRatio * effectiveTotal)) / targetRatio) + 1e-9).toInt()
            max(0, safe)
        }

        val labWeight = max(1, subject.labWeightClasses)
        val safeLabs = safeToSkip / labWeight

        val zone = when {
            !isSafe -> AttendanceZone.DANGER_RED
            roundedPct <= subject.targetPercentage + 3.0 -> AttendanceZone.WARNING_YELLOW
            else -> AttendanceZone.SAFE_GREEN
        }

        val targetInt = subject.targetPercentage.toInt()
        val liquidCopy = if (effectiveTotal == 0) {
            "No classes recorded yet. Target is $targetInt%."
        } else if (isSafe) {
            "You can skip the next $safeToSkip class${if (safeToSkip == 1) "" else "es"} and still stay at $targetInt%."
        } else {
            "Attend the next $mustAttend class${if (mustAttend == 1) "" else "es"} straight to recover to $targetInt%."
        }

        val badge = when {
            effectiveTotal == 0 -> "NO CLASSES RECORDED"
            subject.isOptionalElective -> "OPTIONAL ELECTIVE (EXCLUDED)"
            mustAttend > 0 -> "ATTEND +$mustAttend STRAIGHT"
            safeToSkip > 0 -> "+$safeToSkip BUNKS LEFT"
            else -> "BUFFER 0 • ON EDGE"
        }

        val bonusSuffix = if (subject.tutorialBonusPoints > 0) " | +${subject.tutorialBonusPoints} Tut" else ""
        val formula = "A=$effectiveAttended / T=$effectiveTotal | Tgt=${targetInt}% | Lab=${labWeight}x$bonusSuffix"

        return SubjectAttendanceAnalysis(
            subject = subject,
            effectiveAttended = effectiveAttended,
            effectiveTotal = effectiveTotal,
            rawPercentage = rawPct,
            roundedPercentage = roundedPct,
            mustAttendClasses = mustAttend,
            safeToSkipClasses = safeToSkip,
            safeToSkipLabs = safeLabs,
            isAboveThreshold = isSafe,
            zone = zone,
            statusBadge = badge,
            liquidPillCopy = liquidCopy,
            formulaExplanation = formula
        )
    }
}
