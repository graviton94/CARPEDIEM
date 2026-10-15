package io.github.graviton94.carpediem.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import io.github.graviton94.carpediem.core.Decor
import io.github.graviton94.carpediem.core.Family
import io.github.graviton94.carpediem.core.GardenDecor
import io.github.graviton94.carpediem.core.Hang
import io.github.graviton94.carpediem.core.Kind as PersonKind
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.data.Store
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.HaruArt
import io.github.graviton94.carpediem.ui.garden.SkyTime
import io.github.graviton94.carpediem.ui.garden.drawHaru
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 정원 위젯의 그림 (1.1.7): 앱 정원과 같은 그림 파일 (assets/garden) · 같은 벡터 하루 (drawHaru).
 * 글자는 Glance 가 얹는다 (아주 조금만). 위젯은 움직이지 못하니 ‘때’ 로 바뀜: 하늘빛 · 해와 달의 실제 자리 · 밤의 별 · 잠든 하루.
 *  - garden (4×2): 내 정원 한 장. 나무 · 말뚝과 걸린 것 · 이번 계절의 한 장 · 하루와 가족 돌.
 *  - haru (2×2): 하루가 크게. 오늘 한 줄을 남겼으면 웃고, 무거운 마음이면 살짝 아래를 보고, 밤엔 잠듦.
 */
object GardenWidgetArt {
    private val L = Tokens.Garden.Layout
    private val D = Tokens.Garden.Decor
    private val N = Tokens.Garden.Night

    private fun asset(context: Context, name: String): Bitmap? = runCatching { context.assets.open("garden/$name").use { BitmapFactory.decodeStream(it) } }.getOrNull()
    private fun key(s: Season) = s.name.lowercase()
    private val paint get() = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** 위젯 한 장을 그릴 때 필요한 것 (가게에서 한 번 읽음). */
    private class Scene(context: Context, val now: LocalDateTime) {
        val store = Store(context)
        val profile = store.profile
        val season: Season = GardenDecor.realSeason(now.toLocalDate(), profile?.countryCode)
        val snapshot = profile?.let { LifeSnapshot(it.birthDate, it.expectancy(store.table), now) }
        val dark: Boolean
        init { SkyTime.useCountry(context, profile?.countryCode); dark = SkyTime.isDark(now) }
        /** 하루가 잠든 때 (늦은 밤 ~ 새벽). 저녁의 어두운 정원에서는 아직 깨어 있음. */
        val asleep: Boolean = now.hour >= SLEEP_FROM || now.hour < SLEEP_UNTIL
        val todayLine = store.lines.lastOrNull { it.date == now.toLocalDate() }
        val heavy = todayLine?.feeling in io.github.graviton94.carpediem.core.Letters.HEAVY
        val decor: Decor? = profile?.let { p ->
            val s = snapshot!!
            val rules = GardenDecor.Rules(listOf(D.stageDays1.toInt(), D.stageDays2.toInt(), D.stageDays3.toInt()), listOf(D.chimeBreaths.toInt(), D.bellBreaths.toInt(), D.lanternBreaths.toInt()),
                D.kiteLines.toInt(), D.ribbonLines.toInt(), D.ribbonMax.toInt(), D.budGazes.toInt(), D.budMax.toInt())
            runCatching { GardenDecor.of(now.toLocalDate(), p.countryCode, s.season, p.birthDate, s.expectancy, store.startDate, store.breaths.map { it.first }.distinct().size, store.lines, store.gazeDays.size, false, rules) }.getOrNull()
        }
        val sprout = snapshot?.season == Season.SPRING
    }

    /** 밤엔 정원 그림을 어둡고 푸르게 (앱의 nightFilter 와 같은 결). */
    private fun nightPaint(dark: Boolean) = paint.apply {
        if (dark) colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            0.55f, 0f, 0f, 0f, 0f, 0f, 0.6f, 0f, 0f, 4f, 0f, 0f, 0.78f, 0f, 14f, 0f, 0f, 0f, 1f, 0f)))
    }

    /** 하늘 · 땅 · 때의 빛 · (밤이면) 별 · 해와 달. gy = 땅선. sunR = 해 · 달 반지름 (px). arc = 해가 지나는 길 (위 · 아래 y). */
    private fun sky(context: Context, c: Canvas, sc: Scene, w: Int, h: Int, gy: Float, sunR: Float, arcTop: Float, arcBase: Float, x0: Float, x1: Float) {
        c.drawColor((if (sc.dark) N.Colors.base else Tokens.Garden.Colors.paper).toArgb())
        val p = paint
        asset(context, "sky_${key(sc.season)}.jpg")?.let { sky ->
            // 하늘은 땅선 근처의 빛이 보이게 아래쪽을 맞춤 (앱 화면의 아래 하늘과 같은 자리)
            val sh = w * sky.height / sky.width.toFloat(); val top = min(0f, gy - sh * SKY_AT)
            c.drawBitmap(sky, null, RectF(0f, top, w.toFloat(), top + sh), p)
        }
        asset(context, "strip_${key(sc.season)}.webp")?.let { strip ->
            val sh = w * strip.height / strip.width.toFloat(); val lineY = sh * (L.stripLineY / L.stripHeight)
            c.drawBitmap(strip, null, RectF(0f, gy - lineY, w.toFloat(), max(gy - lineY + sh, h.toFloat())), p)
        }
        // 때의 빛 (앱 정원과 같은 규칙, 땅까지 옅게)
        val t = SkyTime.at(sc.now)
        if (t.alpha > 0f) {
            val top = t.color.copy(alpha = t.alpha).toArgb(); val low = t.color.copy(alpha = t.alpha * t.groundKeep).toArgb()
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, max(1f, gy), top, low, Shader.TileMode.CLAMP) })
        }
        // 밤: 별 몇 개 (밤마다 자리가 조금씩 바뀜)
        if (t.night > 0f) asset(context, "sparkle.png")?.let { star ->
            val r = java.util.Random(sc.now.toLocalDate().toEpochDay())
            val n = (w * h / (STAR_AREA * sunR * sunR)).toInt().coerceIn(4, 14)
            val sp = Paint(p).apply { alpha = (220 * t.night).toInt() }
            repeat(n) {
                val x = w * (0.04f + 0.92f * r.nextFloat()); val y = (arcBase - sunR) * r.nextFloat()
                val s = sunR * (0.35f + 0.35f * r.nextFloat())
                c.drawBitmap(star, null, RectF(x - s, y - s, x + s, y + s), sp)
            }
        }
        // 해 · 달: 그날 실제 뜨고 지는 시각에 따라 하늘을 건넘 (길은 그리지 않음)
        val (day, f) = SkyTime.sunPath(sc.now)
        val x = x0 + (x1 - x0) * f; val y = arcBase - (arcBase - arcTop) * sin(f * Math.PI).toFloat()
        val rect = RectF(x - sunR, y - sunR, x + sunR, y + sunR)
        if (day) asset(context, "sun.png")?.let { c.drawBitmap(it, null, rect, p) }
        else asset(context, "moon_full.png")?.let { moon(c, it, rect, SkyTime.moonPhase(sc.now), p) }
    }

    /** 달을 실제 모양으로 (앱의 MoonShape 와 같은 규칙): 어두운 쪽은 옅게, 밝은 쪽은 타원 경계로. */
    private fun moon(c: Canvas, img: Bitmap, r: RectF, phase: Double, paint: Paint) {
        val faint = Paint(paint).apply { alpha = (255 * N.moonDark).toInt() }
        c.drawBitmap(img, null, r, faint)
        val waxing = (phase < 0.5) != SkyTime.south
        val k = kotlin.math.cos(2 * Math.PI * phase).toFloat()
        val e = r.width() / 2f * N.moonDisk * kotlin.math.abs(k)
        val circle = android.graphics.Path().apply { addOval(r, android.graphics.Path.Direction.CW) }
        val half = android.graphics.Path().apply { addRect(if (waxing) RectF(r.centerX(), r.top, r.right, r.bottom) else RectF(r.left, r.top, r.centerX(), r.bottom), android.graphics.Path.Direction.CW) }
        val lit = android.graphics.Path(); lit.op(circle, half, android.graphics.Path.Op.INTERSECT)
        val ell = android.graphics.Path().apply { addOval(RectF(r.centerX() - e, r.top, r.centerX() + e, r.bottom), android.graphics.Path.Direction.CW) }
        lit.op(ell, if (k > 0f) android.graphics.Path.Op.DIFFERENCE else android.graphics.Path.Op.UNION)
        c.save(); c.clipPath(lit); c.drawBitmap(img, null, r, paint); c.restore()
    }

    /** 그림 상자 하나를 (기준점 = x, y) 에 k (px / 그림 단위) 로. */
    private fun put(c: Canvas, img: Bitmap?, x: Float, y: Float, k: Float, boxW: Float, boxH: Float, atX: Float, atY: Float, p: Paint) {
        img ?: return
        c.drawBitmap(img, null, RectF(x - atX * k, y - atY * k, x + (boxW - atX) * k, y + (boxH - atY) * k), p)
    }

    /** 돌 하나를 앱과 같은 벡터로: 가운데 cx, 땅 gy, 폭 widthPx. */
    private fun stone(c: Canvas, art: HaruArt, cx: Float, gy: Float, widthPx: Float, lid: Float = 0f, smile: Float = 0f, look: Offset = Offset.Zero, hat: Boolean = false) {
        val k = widthPx / art.meta.bbox.width
        val box = art.meta.box
        c.save(); c.translate(cx - art.meta.bbox.center.x * k, gy - art.meta.ground * k)
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(c),
            androidx.compose.ui.geometry.Size(box * k, box * k),
        ) { drawHaru(art, k, lid = lid, look = look, smile = smile, hat = hat) }
        c.restore()
    }

    /** 내 하루의 표정: 잠 (밤) · 웃음 (오늘 한 줄, 가벼운 마음) · 아래를 봄 (무거운 마음). */
    private fun face(sc: Scene, k: Float): Triple<Float, Float, Offset> = Triple(
        if (sc.asleep) 1f else 0f,
        if (!sc.asleep && sc.todayLine != null && !sc.heavy) 1f else 0f,
        if (sc.heavy && !sc.asleep) Offset(0f, k * LOOK_DOWN) else Offset.Zero,
    )

    private fun rounded(context: Context, bmp: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val rr = Tokens.Radius.lg.value * context.resources.displayMetrics.density
        Canvas(out).drawRoundRect(RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat()), rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) })
        bmp.recycle()
        return out
    }

    /** 4×2 정원: 앱 정원을 한 장으로. 나무 (왼쪽) · 말뚝 (오른쪽) · 사이 길에 하루와 가족. */
    fun garden(context: Context, wPx: Int, hPx: Int, now: LocalDateTime): Bitmap {
        val w = max(1, wPx); val h = max(1, hPx)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val sc = Scene(context, now)
        val u = w / G_WIDTH                         // 정원 단위 하나 (앱 화면 폭 390 = 위젯 폭)
        val gy = h * GARDEN_GROUND
        val sunR = u * SUN_R
        sky(context, c, sc, w, h, gy, sunR, h * 0.16f, gy - sunR * 2.2f, w * 0.30f, w * 0.80f)
        val p = nightPaint(sc.dark)
        // 나무 · 말뚝은 위젯 높이에 맞춰 줄임 (땅 위 높이의 GROW 만큼까지)
        val dk = min(1f, gy * DECOR_FIT / (D.treeAtY * u)) * u
        sc.decor?.let { d ->
            put(c, asset(context, "tree_${d.tree.key}_${key(sc.season)}_${d.stage.coerceIn(0, 3)}.webp"), u * D.treeX, gy, dk * D.treeScale, D.treeBoxW, D.treeBoxH, D.treeAtX, D.treeAtY, p)
            put(c, asset(context, "card_${d.card.key}.webp"), u * (D.treeX + D.cardFromTree), gy + u, dk * D.cardMini * 1.3f, D.cardBoxW, D.cardBoxH, D.cardBoxW / 2, D.cardAtY, p)
            val px = u * POST_X; val pk = dk * D.postScale
            put(c, asset(context, "post_${key(sc.season)}.webp"), px, gy, pk, D.postBoxW, D.postBoxH, D.postAtX, D.postAtY, p)
            val part = when (d.hang) { Hang.CHIME -> "chime"; Hang.BELL -> "bell"; Hang.LANTERN -> if (sc.dark) "lantern_lit" else "lantern"; Hang.NONE -> null }
            part?.let { put(c, asset(context, "post_$it.webp"), px, gy, pk, D.postBoxW, D.postBoxH, D.postAtX, D.postAtY, if (it == "lantern_lit") paint else p) }
        }
        // 하루와 가족: 길 가운데에 나란히 (하루가 맨 앞, 반려동물은 작게)
        val people = sc.store.people.filter { it.until == null }.take(MAX_FAMILY)
        val sizes = listOf(1f) + people.map { if (it.kind == PersonKind.PET) Tokens.Garden.Family.petScale else 1f }
        val base = u * STONE_W
        val gap = base * 0.18f
        val total = sizes.sumOf { (it * base).toDouble() }.toFloat() + gap * (sizes.size - 1)
        val fit = min(1f, u * (PATH_TO - PATH_FROM) / total)
        var x = u * (PATH_FROM + PATH_TO) / 2f - total * fit / 2f
        val (lid, smile, look) = face(sc, base)
        val hat = sc.profile?.let { Family.birthdaySoon(it.birthDate, now, Tokens.Notify.birthdayFrom.toInt()) } != null
        sizes.forEachIndexed { i, s ->
            val sw = base * s * fit
            val art = HaruArt.of(if (i == 0) sc.store.haruSeed else people[i - 1].seed, sc.sprout)
            if (i == 0) stone(c, art, x + sw / 2, gy, sw, lid, smile, look, hat)
            else stone(c, art, x + sw / 2, gy, sw, lid = if (sc.asleep) 1f else 0f, hat = Family.birthdaySoon(people[i - 1].birth, now, Tokens.Notify.birthdayFrom.toInt()) != null)
            x += sw + gap * fit
        }
        if (sc.dark) darken(c, w, h)
        return rounded(context, bmp)
    }

    /** 2×2 하루: 하루가 크게 땅 위에. 해 · 달은 위 오른쪽 하늘을 건넘. */
    fun haru(context: Context, wPx: Int, hPx: Int, now: LocalDateTime): Bitmap {
        val w = max(1, wPx); val h = max(1, hPx)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        val sc = Scene(context, now)
        val gy = h * HARU_GROUND
        val sunR = min(w, h) * 0.07f
        sky(context, c, sc, w, h, gy, sunR, h * 0.14f, h * 0.42f, w * 0.14f, w * 0.86f)
        val sw = min(w * HARU_W, gy * 0.7f)
        val art = HaruArt.of(sc.store.haruSeed, sc.sprout)
        val (lid, smile, look) = face(sc, sw)
        val hat = sc.profile?.let { Family.birthdaySoon(it.birthDate, now, Tokens.Notify.birthdayFrom.toInt()) } != null
        stone(c, art, w / 2f, gy, sw, lid, smile, look, hat)
        if (sc.dark) darken(c, w, h)
        return rounded(context, bmp)
    }

    /** 밤엔 하루 · 가족도 정원과 같이 살짝 어둡게 (한 겹 덮음). */
    private fun darken(c: Canvas, w: Int, h: Int) = c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { color = N.Colors.base.copy(alpha = NIGHT_VEIL).toArgb() })

    /** 위젯 그림의 폭 = 앱 정원 폭 (390 단위). 땅선 · 해 반지름 · 돌 폭 · 길 (정원 단위). */
    private const val G_WIDTH = 390f
    private const val GARDEN_GROUND = 0.84f
    private const val SUN_R = 13f
    private const val STONE_W = 46f
    private const val PATH_FROM = 96f
    private const val PATH_TO = 312f
    private const val POST_X = 350f
    private const val DECOR_FIT = 0.95f
    private const val MAX_FAMILY = 5
    private const val HARU_GROUND = 0.82f
    private const val HARU_W = 0.46f
    private const val SKY_AT = 0.55f
    private const val STAR_AREA = 90f
    private const val LOOK_DOWN = 0.04f
    private const val NIGHT_VEIL = 0.18f
    private const val SLEEP_FROM = 22
    private const val SLEEP_UNTIL = 6
}
