package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Decor
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.Hang
import io.github.graviton94.carpediem.core.Tree
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.sin
import io.github.graviton94.carpediem.design.Tokens.Garden.Decor as D

/** 정원의 자리 (누르면 그 자리의 한 장). LETTER = 말뚝에 꽂힌 편지 (누르면 편지가 펼쳐짐). */
enum class DecorPart { TREE, CARD, POST, LETTER, KITE, MOSS }

internal fun treeName(t: Tree) = when (t) { Tree.CHERRY -> R.string.decor_tree_cherry; Tree.ZELKOVA -> R.string.decor_tree_zelkova; Tree.GINKGO -> R.string.decor_tree_ginkgo; Tree.PINE -> R.string.decor_tree_pine }
internal fun stageName(stage: Int) = when (stage) { 0 -> R.string.decor_stage_0; 1 -> R.string.decor_stage_1; 2 -> R.string.decor_stage_2; else -> R.string.decor_stage_3 }
internal fun cardName(context: android.content.Context, key: String): String =
    context.resources.getIdentifier("decor_card_$key", "string", context.packageName).takeIf { it != 0 }?.let { context.getString(it) } ?: key

/** 밤 · 새벽: 한지 그림을 땅과 같은 남색 빛 아래로 (등불 · 달 · 별은 그대로). 땅에 덮는 빛 (Night.sky × groundAlpha) 과 같은 셈. */
internal fun nightFilter(dark: Boolean): ColorFilter? {
    if (!dark) return null
    val a = Tokens.Garden.Night.groundAlpha * D.nightKeep; val c = Tokens.Garden.Night.Colors.sky; val k = 1f - a
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(k, 0f, 0f, 0f, c.red * 255f * a, 0f, k, 0f, 0f, c.green * 255f * a, 0f, 0f, k, 0f, c.blue * 255f * a, 0f, 0f, 0f, 1f, 0f)))
}

/**
 * 누르는 자리: 그림 상자 전체가 아니라 그림이 실제로 있는 범위만 (작아도 minTap 은 남김).
 * 상자가 크면 (가지가 잘리지 않게 넉넉한 나무 상자) 하늘 · 빈 땅 · 위 글자까지 눌려 버리므로, 그림에는 누름을 두지 않고 이 자리를 따로 둔다.
 */
@Composable
private fun HitArea(img: androidx.compose.ui.graphics.ImageBitmap, x: Dp, y: Dp, k: Dp, boxW: Float, boxH: Float, atX: Float, atY: Float, a11y: String, onTap: (() -> Unit)?) {
    if (onTap == null) return
    val b = GardenArt.opaque(img); val w = k * boxW; val h = k * boxH
    val left = x - k * atX; val top = y - k * atY
    val minTap = 40.dp
    val ww = maxOf(w * b.width, minTap); val hh = maxOf(h * b.height, minTap)
    val cx = left + w * (b.left + b.width / 2); val cy = top + h * (b.top + b.height / 2)
    val quiet = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(Modifier.offset(cx - ww / 2, cy - hh / 2).size(ww, hh).clickable(interactionSource = quiet, indication = null, onClickLabel = a11y) { onTap() }
        .semantics { contentDescription = a11y; role = androidx.compose.ui.semantics.Role.Button })
}

/** 그림 상자 하나를 (기준점 = 화면의 x, y) 에 두는 자리. k = 화면 단위 하나당 그림 단위 배율. */
private fun Modifier.box(x: Dp, y: Dp, k: Dp, boxW: Float, boxH: Float, atX: Float, atY: Float) = offset(x - k * atX, y - k * atY).size(k * boxW, k * boxH)

/**
 * 하루 · 가족 돌 뒤에 놓이는 자리: ① 나무 (길의 시작) · ⑤ 나무 발치의 한 장 · ② 말뚝 (길의 끝) · ③ 연 (말뚝에 묶여 하늘에).
 * 흔들림은 느리게 (주기 3초 이상), 움직임을 끈 기기면 멈춤. kiteTop · kiteBottom = 연이 뜰 하늘 띠 (글자 아래 ~ 돌 머리 위).
 */
@Composable
internal fun DecorBack(decor: Decor, now: LocalDateTime, gy: Dp, x0: Dp, x1: Dp, kiteTop: Dp, kiteBottom: Dp, onTap: ((DecorPart) -> Unit)?) {
    val ctx = LocalContext.current
    val u = Theme.unit
    val dark = SkyTime.isDark(now)
    val filter = nightFilter(dark)
    val moving = remember { !reducedMotion(ctx) }
    // 정원의 시계 (초): 그리는 단계에서만 읽어, 흔들릴 때 정원 전체가 다시 짜이지 않게. 걸린 것마다 박자 · 시작점이 다름
    val clock = rememberGardenClock(moving)
    val swLantern = remember { Sway(11) }; val swChime = remember { Sway(12) }; val swBell = remember { Sway(13) }; val swLetter = remember { Sway(14) }
    // 누르는 자리는 그림이 있는 범위만 (HitArea), 회색 상자가 번지지 않게
    fun tap(p: DecorPart): (() -> Unit)? = onTap?.let { f -> { f(p) } }

    // ① 나무: 길의 시작 (0세) 뒤. 인생의 계절이 막 바뀌었으면 옛 나무가 옅게 남았다가 천천히 바뀜
    val tk = u * D.treeScale
    val tx = u * D.treeX
    val treeA11y = stringResource(treeName(decor.tree)) + ", " + stringResource(stageName(decor.stage))
    decor.prevTree?.let { prev ->
        Image(GardenArt.tree(ctx, prev, decor.season, decor.stage), null, Modifier.box(tx, gy, tk, D.treeBoxW, D.treeBoxH, D.treeAtX, D.treeAtY).graphicsLayer { alpha = 1f - decor.blend }, colorFilter = filter)
    }
    val treeImg = GardenArt.tree(ctx, decor.tree, decor.season, decor.stage)
    Image(treeImg, null, Modifier.box(tx, gy, tk, D.treeBoxW, D.treeBoxH, D.treeAtX, D.treeAtY).graphicsLayer { alpha = decor.blend }, colorFilter = filter)
    HitArea(treeImg, tx, gy, tk, D.treeBoxW, D.treeBoxH, D.treeAtX, D.treeAtY, treeA11y, tap(DecorPart.TREE))

    // ⑤ 나무 발치: 이번 계절의 한 장 (꽃 · 풀 · 열매 · 낙엽 · 눈사람 한 조각) 이 땅에 놓임
    val ck = u * D.cardMini
    val cardImg = GardenArt.card(ctx, decor.card.key)
    Image(cardImg, null, Modifier.box(u * (D.treeX + D.cardFromTree), gy + 1.dp, ck, D.cardBoxW, D.cardBoxH, D.cardBoxW / 2, D.cardAtY)
            .graphicsLayer { transformOrigin = TransformOrigin(0.5f, D.cardAtY / D.cardBoxH); rotationZ = D.cardTilt }, colorFilter = filter)
    HitArea(cardImg, u * (D.treeX + D.cardFromTree), gy + 1.dp, ck, D.cardBoxW, D.cardBoxH, D.cardBoxW / 2, D.cardAtY, cardName(ctx, decor.card.key), tap(DecorPart.CARD))

    // ② 말뚝: 길의 끝 (기대수명). 걸린 것은 가로대에서 따로 흔들림, 등불은 밤에 켜짐, 편지가 오면 봉투
    val pk = u * D.postScale
    val lit = dark && decor.hang == Hang.LANTERN
    val postA11y = listOfNotNull(stringResource(R.string.decor_post), when (decor.hang) { Hang.CHIME -> stringResource(R.string.decor_hang_chime); Hang.BELL -> stringResource(R.string.decor_hang_bell); Hang.LANTERN -> stringResource(R.string.decor_hang_lantern); Hang.NONE -> null },
        if (decor.letter) stringResource(R.string.decor_post_letter) else null).joinToString(", ")
    Box(Modifier.box(x1, gy, pk, D.postBoxW, D.postBoxH, D.postAtX, D.postAtY)) {
        val glowC = Tokens.Garden.Decor.Colors.glow
        if (lit) Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * D.lanternX / D.postBoxW, size.height * (D.lanternY + 16f) / D.postBoxH)
            val r = size.width * D.glow / D.postBoxW
            drawCircle(Brush.radialGradient(listOf(glowC.copy(alpha = 0.55f), Color.Transparent), c, r), r, c)
        }
        Image(GardenArt.post(ctx, decor.season), null, Modifier.fillMaxSize(), colorFilter = filter)
        @Composable fun swing(part: String, px: Float, py: Float, sw: Sway, deg: Float, f: ColorFilter?) =
            Image(GardenArt.postPart(ctx, part), null, Modifier.fillMaxSize().graphicsLayer { transformOrigin = TransformOrigin(px / D.postBoxW, py / D.postBoxH); rotationZ = if (moving) sw.at(clock.value) * deg * (1f + 1.6f * windGust.floatValue) else 0f }, colorFilter = f)
        // 등이 걸리면 작은 종은 내려 둠 (가로대가 붐비지 않게)
        if (decor.hang >= Hang.LANTERN) swing(if (lit) "lantern_lit" else "lantern", D.lanternX, D.lanternY, swLantern, 1f, if (lit) null else filter)
        if (decor.hang >= Hang.CHIME) swing("chime", D.chimeX, D.chimeY, swChime, D.swayDeg, filter)
        if (decor.hang == Hang.BELL) swing("bell", D.bellX, D.bellY, swBell, D.swayDeg * 0.7f, filter)
        if (decor.letter) Image(GardenArt.postPart(ctx, "letter"), null, Modifier.fillMaxSize().graphicsLayer { rotationZ = if (moving) swLetter.at(clock.value) * 0.6f else 0f }, colorFilter = filter)
        // 밤: 연은 반으로 접혀 말뚝에 기대 있음 (낮에 다시 날아요)
        if (decor.kite && dark) Image(GardenArt.image(ctx, "kite_folded.webp"), null, Modifier.fillMaxSize(), colorFilter = filter)
    }
    HitArea(GardenArt.post(ctx, decor.season), x1, gy, pk, D.postBoxW, D.postBoxH, D.postAtX, D.postAtY, postA11y, tap(if (decor.letter) DecorPart.LETTER else DecorPart.POST))
    if (decor.kite && dark) HitArea(GardenArt.image(ctx, "kite_folded.webp"), x1, gy, pk, D.postBoxW, D.postBoxH, D.postAtX, D.postAtY, stringResource(R.string.decor_kite), tap(DecorPart.KITE))

    // ③ 연: 낮에만, 말뚝 가로대 끝에 실로 묶여 오른쪽 하늘에. 꼬리 리본 = 서른 줄마다 하나, 그 줄들의 마음 색
    if (decor.kite && !dark && kiteBottom > kiteTop) {
        val kk = u * D.kiteScale
        val kx = u * D.kiteX
        val ky = kiteTop + (kiteBottom - kiteTop) * D.kiteHigh
        val density = LocalDensity.current.density
        // 연은 느린 8자를 그리며 떠 있음 (kiteMs 에 한 바퀴)
        fun phase() = if (moving) clock.value * 1000f / D.kiteMs * 2f * Math.PI.toFloat() else 0f
        val tie = Offset(((x1 - pk * D.postAtX) + pk * D.tieX).value, ((gy - pk * D.postAtY) + pk * D.tieY).value)
        val ink = Tokens.Garden.Colors.ink
        Canvas(Modifier.fillMaxSize()) {
            val ph = phase()
            val from = Offset(tie.x * density, tie.y * density)
            val to = Offset(kx.toPx() + sin(ph) * D.kiteDrift * 0.8f * density, (ky + kk * 19f).toPx() + sin(2f * ph) * D.kiteDrift * 0.5f * density)
            val path = Path().apply { moveTo(from.x, from.y); quadraticTo((from.x + to.x) / 2 - 12f * density, (from.y + to.y) / 2 + 22f * density, to.x, to.y) }
            drawPath(path, ink.copy(alpha = 0.45f), style = Stroke(0.8f * density))
        }
        // 리본 색은 한지에 물든 듯 옅게 (많아야 넷)
        val colors = decor.ribbons.map { lerp(feelingColor(it), Tokens.Garden.Colors.paper, D.ribbonMute) }
        HitArea(GardenArt.kite(ctx), kx, ky, kk, D.kiteBoxW, D.kiteBoxH, D.kiteAtX, D.kiteAtY, stringResource(R.string.decor_kite), tap(DecorPart.KITE))
        Box(Modifier.box(kx, ky, kk, D.kiteBoxW, D.kiteBoxH, D.kiteAtX, D.kiteAtY)
            .graphicsLayer { val ph = phase(); transformOrigin = TransformOrigin(D.kiteAtX / D.kiteBoxW, D.kiteAtY / D.kiteBoxH)
                translationX = sin(ph) * D.kiteDrift * 0.8f * density; translationY = sin(2f * ph) * D.kiteDrift * 0.5f * density; rotationZ = sin(ph + 0.6f) * D.kiteTilt }
            ) {
            Image(GardenArt.kite(ctx), null, Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize()) {
                val f = size.width / D.kiteBoxW
                val tail = kiteTail()
                val n = colors.size
                colors.forEachIndexed { i, c ->
                    val p = tail[((i + 1f) / (n + 1f) * (tail.size - 1)).toInt()]
                    val o = Offset((D.kiteAtX + p.x) * f, (D.kiteAtY + p.y) * f)
                    fun bow(side: Float, col: Color) {
                        fun tri(dx: Float, dy: Float) = Path().apply { moveTo(o.x + dx, o.y + dy); lineTo(o.x + dx + side * 6f * f, o.y + dy - 3f * f); lineTo(o.x + dx + side * 6f * f, o.y + dy + 3f * f); close() }
                        drawPath(tri(0.5f * f, 0.8f * f), Color.Black.copy(alpha = 0.12f))
                        drawPath(tri(0f, 0f), col)
                    }
                    bow(-1f, c); bow(1f, lerp(c, Color.White, 0.25f))
                }
            }
        }
    }
}

internal const val FRAME_NS = 33_000_000L

/** 정원의 시계 (초, 움직임을 끈 기기면 0 에 멈춤). 값은 graphicsLayer · Canvas 안에서만 읽을 것. */
@Composable
internal fun rememberGardenClock(moving: Boolean): androidx.compose.runtime.State<Float> {
    val clock = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    if (moving) androidx.compose.runtime.LaunchedEffect(Unit) {
        val start = androidx.compose.runtime.withFrameNanos { it }; var last = 0L
        // 느린 움직임뿐이라 1초에 30번이면 충분 (그리는 일을 반으로)
        while (true) androidx.compose.runtime.withFrameNanos { if (it - last >= FRAME_NS) { last = it; clock.floatValue = (it - start) / 1_000_000_000f } }
    }
    return clock
}

/** 따로따로 흔들림: 박자와 시작점이 서로 다른 느린 사인 둘 (대략 -1 … 1). 끝에서 멈칫하지 않고 이어짐. */
internal class Sway(seed: Int) {
    private val r = Crayon.Rng(seed * 7919 + 17)
    private val t1 = 3.1f + r.next() * 1.3f; private val t2 = 1.3f + r.next() * 0.6f
    private val p1 = r.next() * 6.2832f; private val p2 = r.next() * 6.2832f
    fun at(t: Float): Float = 0.8f * sin(6.2832f * t / t1 + p1) + 0.25f * sin(6.2832f * t / t2 + p2)
}

/** 계절 바람이 부는 동안의 세기 (0 … 1): 걸린 것 · 이끼 풀이 그만큼 더 흔들림. */
internal val windGust = androidx.compose.runtime.mutableFloatStateOf(0f)

/** 연 꼬리의 점들 (연 몸 가운데 기준, 그림 단위): hanji_garden.js kite() 의 꼬리와 같은 곡선. */
private fun kiteTail(): List<Offset> {
    val segs = listOf(floatArrayOf(0f, 18f, 6f, 34f, -6f, 50f, 2f, 70f), floatArrayOf(2f, 70f, 8f, 84f, -2f, 96f, 4f, 110f))
    val out = ArrayList<Offset>()
    segs.forEachIndexed { s, q ->
        for (i in (if (s == 0) 0 else 1)..24) {
            val t = i / 24f; val a = 1 - t
            out.add(Offset(a * a * a * q[0] + 3 * a * a * t * q[2] + 3 * a * t * t * q[4] + t * t * t * q[6], a * a * a * q[1] + 3 * a * a * t * q[3] + 3 * a * t * t * q[5] + t * t * t * q[7]))
        }
    }
    return out
}

internal fun feelingColor(f: Feeling?): Color { val m = Tokens.Garden.Mood.Colors; return when (f) { Feeling.JOY -> m.joy; Feeling.HOPE -> m.hope; Feeling.CALM -> m.calm; Feeling.THANKS -> m.thanks; Feeling.DISAPPOINT -> m.disappoint; Feeling.SAD -> m.sad; Feeling.WORRY -> m.worry; null -> m.none } }

/** ④ 하루의 자리: 하루 밑의 이끼 방석 (봉오리 0 ~ 5, 계절마다 꽃 · 토끼풀 · 버섯 · 눈). width = 하루의 폭. */
@Composable
internal fun MossSeat(decor: Decor, now: LocalDateTime, x: Dp, width: Dp, gy: Dp, onTap: ((DecorPart) -> Unit)?) {
    val ctx = LocalContext.current
    // 이끼 그림 상자 90 단위 가운데 68 단위가 방석
    val k = width * D.mossWidth / 68f
    val img = GardenArt.moss(ctx, decor.season, decor.buds)
    Image(img, null, Modifier.box(x, gy + 1.dp, k, D.mossBoxW, D.mossBoxH, D.mossAtX, D.mossAtY), colorFilter = nightFilter(SkyTime.isDark(now)))
    HitArea(img, x, gy + 1.dp, k, D.mossBoxW, D.mossBoxH, D.mossAtX, D.mossAtY, stringResource(R.string.obj_moss), onTap?.let { f -> { f(DecorPart.MOSS) } })
}

/** 자리를 눌렀을 때의 한 장: 그림 · 이름 · 지금까지 쌓인 것 · 다음 · 이 자리의 규칙 한 줄. */
@Composable
internal fun DecorSheet(part: DecorPart, decor: Decor, state: AppState, now: LocalDateTime) {
    val ctx = LocalContext.current
    val p = Theme.palette
    val u = Theme.unit
    val today = now.toLocalDate()
    val days = ChronoUnit.DAYS.between(state.store.startDate, today).coerceAtLeast(0)
    val breathDays = state.breaths.map { it.first }.distinct().size
    val written = state.lines.count { it.text.isNotBlank() }
    val img = when (part) {
        DecorPart.TREE -> GardenArt.tree(ctx, decor.tree, decor.season, decor.stage)
        DecorPart.CARD -> GardenArt.card(ctx, decor.card.key)
        DecorPart.POST, DecorPart.LETTER -> GardenArt.post(ctx, decor.season)
        DecorPart.KITE -> GardenArt.kite(ctx)
        DecorPart.MOSS -> GardenArt.moss(ctx, decor.season, decor.buds)
    }
    val title = when (part) {
        DecorPart.TREE -> stringResource(treeName(decor.tree))
        DecorPart.CARD -> stringResource(R.string.decor_card)
        DecorPart.POST, DecorPart.LETTER -> stringResource(R.string.decor_post)
        DecorPart.KITE -> stringResource(R.string.decor_kite)
        DecorPart.MOSS -> stringResource(R.string.obj_moss)
    }
    val line = when (part) {
        DecorPart.TREE -> stringResource(R.string.decor_tree_line, "$days", stringResource(stageName(decor.stage)))
        DecorPart.CARD -> stringResource(R.string.decor_card_line, "${decor.card.year}", Labels.season(ctx, decor.card.season), cardName(ctx, decor.card.key))
        DecorPart.POST, DecorPart.LETTER -> if (breathDays == 0) stringResource(R.string.decor_post_none) else stringResource(R.string.decor_post_line, "$breathDays")
        DecorPart.KITE -> if (decor.kite) stringResource(R.string.decor_kite_line, "$written", "${decor.ribbons.size}") else stringResource(R.string.decor_kite_none, "${D.kiteLines.toInt()}")
        DecorPart.MOSS -> stringResource(R.string.decor_moss_line, "${state.gazeDays.size}", "${decor.buds}")
    }
    val next = when (part) {
        DecorPart.TREE -> listOf(D.stageDays1, D.stageDays2, D.stageDays3).map { it.toInt() }.firstOrNull { days < it }?.let { stringResource(R.string.decor_tree_next, "$it") }
        DecorPart.POST, DecorPart.LETTER -> listOf(D.bellBreaths, D.lanternBreaths).map { it.toInt() }.firstOrNull { breathDays in 1 until it }?.let { stringResource(R.string.decor_post_next, "$it") }
        else -> null
    }
    val help = when (part) {
        DecorPart.TREE -> R.string.decor_tree_help; DecorPart.CARD -> R.string.decor_card_help; DecorPart.POST, DecorPart.LETTER -> R.string.decor_post_help
        DecorPart.KITE -> R.string.decor_kite_help; DecorPart.MOSS -> R.string.decor_moss_help
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp6), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        Image(img, null, Modifier.height(u * if (part == DecorPart.CARD) 150f else 110f))
        TokenText(title, Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
        TokenText(line, Tokens.TypeScale.body, align = TextAlign.Center)
        next?.let { TokenText(it, Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center) }
        TokenText(stringResource(help), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
    }
}
