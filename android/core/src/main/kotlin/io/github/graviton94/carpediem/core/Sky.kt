package io.github.graviton94.carpediem.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** 나라마다 대표 도시 하나 (data/places.csv). 위치 권한 없이 해 · 달 시각을 셈. */
data class Place(val code: String, val lat: Double, val lon: Double)

class PlaceTable(csv: String) {
    val places: Map<String, Place> = Csv.records(csv).mapNotNull { r ->
        val code = r["code"] ?: return@mapNotNull null
        val lat = r["lat"]?.toDoubleOrNull() ?: return@mapNotNull null
        val lon = r["lon"]?.toDoubleOrNull() ?: return@mapNotNull null
        Place(code, lat, lon)
    }.associateBy { it.code }

    /** 표에 없으면 시간대에서 짐작 (위도 35, 경도 = 표준시 × 15). */
    fun of(code: String?, zone: ZoneId, at: Instant): Place =
        places[code?.uppercase()] ?: Place(code ?: "", 35.0, zone.rules.getStandardOffset(at).totalSeconds / 240.0)
}

/** 절기가 그날 정원에 놓는 아주 작은 변화 (그날 하루). */
enum class TermTouch { THAW, BUDS, BLOSSOM, DRIZZLE, GREEN, HIGH_SUN, HEAT, DEW, FROST, SNOW, LONG_NIGHT, ICE }

/** 스물넷 절기. 소한부터 해의 길 (황경) 285° 에서 15° 씩. */
enum class SolarTerm(val key: String, val touch: TermTouch) {
    SOHAN("sohan", TermTouch.ICE), DAEHAN("daehan", TermTouch.ICE),
    IPCHUN("ipchun", TermTouch.BUDS), USU("usu", TermTouch.THAW),
    GYEONGCHIP("gyeongchip", TermTouch.BUDS), CHUNBUN("chunbun", TermTouch.BLOSSOM),
    CHEONGMYEONG("cheongmyeong", TermTouch.BLOSSOM), GOGU("gogu", TermTouch.DRIZZLE),
    IPHA("ipha", TermTouch.GREEN), SOMAN("soman", TermTouch.GREEN),
    MANGJONG("mangjong", TermTouch.GREEN), HAJI("haji", TermTouch.HIGH_SUN),
    SOSEO("soseo", TermTouch.HEAT), DAESEO("daeseo", TermTouch.HEAT),
    IPCHU("ipchu", TermTouch.DEW), CHEOSEO("cheoseo", TermTouch.DEW),
    BAENGNO("baengno", TermTouch.DEW), CHUBUN("chubun", TermTouch.DEW),
    HALLO("hallo", TermTouch.DEW), SANGGANG("sanggang", TermTouch.FROST),
    IPDONG("ipdong", TermTouch.FROST), SOSEOL("soseol", TermTouch.SNOW),
    DAESEOL("daeseol", TermTouch.SNOW), DONGJI("dongji", TermTouch.LONG_NIGHT);

    /** 이 절기가 시작하는 해의 황경 (도). */
    val longitude: Double get() = (285.0 + 15.0 * ordinal) % 360.0
}

/** 해와 달 (정밀하지 않아도 되는 하늘: 몇 분 안팎). */
object Sky {
    private const val RAD = PI / 180

    private fun julian(t: Instant) = t.epochSecond / 86400.0 + t.nano / 8.64e13 + 2440587.5

    /** 해의 겉보기 황경 (도, 0 ~ 360). Meeus 25장 간이식. */
    fun sunLongitude(t: Instant): Double {
        val T = (julian(t) - 2451545.0) / 36525
        val l0 = 280.46646 + 36000.76983 * T + 0.0003032 * T * T
        val m = (357.52911 + 35999.05029 * T - 0.0001537 * T * T) * RAD
        val c = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(m) + (0.019993 - 0.000101 * T) * sin(2 * m) + 0.000289 * sin(3 * m)
        val om = (125.04 - 1934.136 * T) * RAD
        return norm(l0 + c - 0.00569 - 0.00478 * sin(om))
    }

    private fun norm(d: Double) = ((d % 360) + 360) % 360

    /** 그날 (zone 의 하루 안에) 절기가 들면 그 절기, 아니면 null. */
    fun termOn(date: LocalDate, zone: ZoneId): SolarTerm? {
        val a = sunLongitude(date.atStartOfDay(zone).toInstant())
        val b = sunLongitude(date.plusDays(1).atStartOfDay(zone).toInstant())
        val span = norm(b - a)
        return SolarTerm.entries.firstOrNull { norm(it.longitude - a) < span }
    }

    /** 오늘이 속한 절기 (가장 최근에 든 것). */
    fun termOf(date: LocalDate, zone: ZoneId): SolarTerm {
        val lon = sunLongitude(date.plusDays(1).atStartOfDay(zone).toInstant().minusSeconds(1))
        return SolarTerm.entries[(floor(norm(lon - 285.0) / 15.0).toInt()) % 24]
    }

    /** 해 뜨고 지는 시각 (NOAA 간이식, 지평선 −0.833°). 백야 · 극야면 null. */
    class SunDay(val rise: ZonedDateTime, val set: ZonedDateTime)

    fun sun(date: LocalDate, place: Place, zone: ZoneId): SunDay? {
        val noonUtc = date.atStartOfDay(ZoneId.of("UTC")).plusHours(12).toInstant()
        val n = julian(noonUtc) - 2451545.0
        val g = (357.529 + 0.98560028 * n) * RAD
        val q = 280.459 + 0.98564736 * n
        val lambda = (q + 1.915 * sin(g) + 0.020 * sin(2 * g)) * RAD
        val e = (23.439 - 0.00000036 * n) * RAD
        val decl = asin(sin(e) * sin(lambda))
        val ra = atan2(cos(e) * sin(lambda), cos(lambda)) / RAD
        // 시간 방정식 (분)
        val eot = 4 * (norm(q - ra + 180) - 180)
        val lat = place.lat * RAD
        val cosH = (sin(-0.833 * RAD) - sin(lat) * sin(decl)) / (cos(lat) * cos(decl))
        if (cosH < -1 || cosH > 1) return null
        val h = acos(cosH) / RAD // 도
        val noonMin = 720 - 4 * place.lon - eot // UTC 분
        fun at(min: Double) = date.atStartOfDay(ZoneId.of("UTC")).plusSeconds((min * 60).toLong()).withZoneSameInstant(zone)
        return SunDay(at(noonMin - 4 * h), at(noonMin + 4 * h))
    }

    /** 극지방에서 해가 지지 않는 날이면 true (null 이 나온 날 낮인지 밤인지). */
    fun polarDay(date: LocalDate, place: Place): Boolean {
        val n = julian(date.atStartOfDay(ZoneId.of("UTC")).plusHours(12).toInstant()) - 2451545.0
        val g = (357.529 + 0.98560028 * n) * RAD
        val lambda = (280.459 + 0.98564736 * n + 1.915 * sin(g)) * RAD
        val decl = asin(sin(23.439 * RAD) * sin(lambda))
        return place.lat * decl > 0
    }

    /** 달의 나이 (0 = 그믐/삭, 0.5 = 보름, 1 직전 = 다시 그믐). 평균 삭망월로. */
    fun moonPhase(t: Instant): Double {
        val days = julian(t) - 2451550.1
        return ((days / 29.530588853) % 1 + 1) % 1
    }

    /** 달 모양 이름: 삭 · 초승 · 상현 · 차가는 · 보름 · 기우는 · 하현 · 그믐. */
    enum class MoonShape { NEW, CRESCENT, FIRST_QUARTER, WAXING, FULL, WANING, LAST_QUARTER, OLD }

    fun moonShape(phase: Double): MoonShape = MoonShape.entries[(floor(phase * 8 + 0.5).toInt()) % 8]

    /** 밝은 쪽 비율 (0 ~ 1). */
    fun moonLit(phase: Double) = (1 - cos(2 * PI * phase)) / 2

    /** 낮인지: 해가 뜬 뒤 · 지기 전. 백야 · 극야는 그대로. */
    fun isDay(now: ZonedDateTime, place: Place): Boolean {
        val s = sun(now.toLocalDate(), place, now.zone) ?: return polarDay(now.toLocalDate(), place)
        return !now.isBefore(s.rise) && now.isBefore(s.set)
    }
}
