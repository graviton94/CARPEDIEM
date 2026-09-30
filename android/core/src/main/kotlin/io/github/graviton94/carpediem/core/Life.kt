package io.github.graviton94.carpediem.core

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor
import kotlin.math.roundToLong

enum class Sex { OTHER, MALE, FEMALE }

enum class LifeUnit { DAYS, WEEKS, MONTHS, YEARS }

/** 인생 달력 한 칸의 크기. 열 수는 디자인 토큰에서 온다. */
enum class GridScale(val unit: LifeUnit) { MONTHS(LifeUnit.MONTHS), YEARS(LifeUnit.YEARS), WEEKS(LifeUnit.WEEKS) }

enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    companion object {
        fun of(progress: Double): Season = entries[(progress * 4).toInt().coerceIn(0, 3)]
    }
}

enum class LifePeriod { DAY, WEEK, MONTH, YEAR }

data class LifeProfile(
    val birthDate: LocalDate,
    val countryCode: String,
    val sex: Sex,
    /** null 이면 나라 · 성별 평균(자동), 값이 있으면 사용자가 직접 넣은 기대수명. */
    val customExpectancy: Double?,
) {
    fun expectancy(table: LifeExpectancyTable): Double = customExpectancy ?: table.expectancy(countryCode, sex)
}

/** 특정 시각 기준으로 계산한 인생의 흐름. 날짜 경계는 기기 시간대의 자정. */
class LifeSnapshot(birthDate: LocalDate, val expectancy: Double, val now: LocalDateTime) {
    val birth: LocalDate = birthDate
    val end: LocalDate = endDate(birthDate, expectancy)
    private val today: LocalDate = now.toLocalDate()

    /** 지나온 비율 0..1 */
    val progress: Double
        get() {
            val total = Duration.between(birth.atStartOfDay(), end.atStartOfDay()).seconds.toDouble()
            if (total <= 0) return 1.0
            return (Duration.between(birth.atStartOfDay(), now).seconds / total).coerceIn(0.0, 1.0)
        }

    val season: Season get() = Season.of(progress)
    val isBeyondExpectancy: Boolean get() = !now.isBefore(end.atStartOfDay())
    val age: Int get() = lived(LifeUnit.YEARS)

    fun lived(unit: LifeUnit) = count(unit, birth, today, birth.atStartOfDay(), now)
    fun remaining(unit: LifeUnit) = count(unit, today, end, now, end.atStartOfDay())
    fun total(unit: LifeUnit) = count(unit, birth, end, birth.atStartOfDay(), end.atStartOfDay())

    private fun count(unit: LifeUnit, from: LocalDate, to: LocalDate, fromTime: LocalDateTime, toTime: LocalDateTime): Int {
        if (!toTime.isAfter(fromTime)) return 0
        val v = when (unit) {
            LifeUnit.DAYS -> ChronoUnit.DAYS.between(from, to)
            LifeUnit.WEEKS -> ChronoUnit.DAYS.between(from, to) / 7
            LifeUnit.MONTHS -> ChronoUnit.MONTHS.between(from, to)
            LifeUnit.YEARS -> ChronoUnit.YEARS.between(from, to)
        }
        return v.coerceAtLeast(0).toInt()
    }

    fun period(period: LifePeriod): PeriodProgress {
        val (start, stop) = when (period) {
            LifePeriod.DAY -> today to today.plusDays(1)
            LifePeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { it to it.plusWeeks(1) }
            LifePeriod.MONTH -> today.withDayOfMonth(1).let { it to it.plusMonths(1) }
            LifePeriod.YEAR -> today.withDayOfYear(1).let { it to it.plusYears(1) }
        }
        return PeriodProgress(period, start.atStartOfDay(), stop.atStartOfDay(), now)
    }

    companion object {
        /** 출생일 + 기대수명. 소수 부분은 평균 태양년(365.2425일)으로 환산. */
        fun endDate(birth: LocalDate, expectancy: Double): LocalDate {
            val years = expectancy.coerceAtLeast(0.0)
            val whole = floor(years).toLong()
            val extraDays = ((years - whole) * 365.2425).roundToLong()
            return birth.plusYears(whole).plusDays(extraDays)
        }
    }
}

class PeriodProgress(val period: LifePeriod, val start: LocalDateTime, val end: LocalDateTime, val now: LocalDateTime) {
    val progress: Double
        get() {
            val total = Duration.between(start, end).seconds.toDouble()
            return if (total <= 0) 0.0 else (Duration.between(start, now).seconds / total).coerceIn(0.0, 1.0)
        }

    val secondsLeft: Long get() = Duration.between(now, end).seconds.coerceAtLeast(0)
    val hoursLeft: Int get() = (secondsLeft / 3600).toInt()
    val minutesLeft: Int get() = (secondsLeft / 60).toInt()

    /** 오늘 이후로 이 기간에 남은 날 수 (오늘 제외). */
    val remainingDaysAfterToday: Int
        get() = (ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate()) - 1).coerceAtLeast(0).toInt()
}
