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
    val wave = rememberInfiniteTransition(label = "decor")
    val sway by wave.animateFloat(-1f, 1f, infiniteRepeatable(tween(D.swayMs.toInt(), easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
    val drift by wave.animateFloat(0f, 1f, infiniteRepeatable(tween(D.kiteMs.toInt(), easing = LinearEasing)), label = "drift")
    // 누를 때 회색 상자가 번지지 않게 (그림이 곧 자리)
    val quiet = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    fun tap(p: DecorPart) = if (onTap == null) Modifier else Modifier.clickable(interactionSource = quiet, indication = null) { onTap(p) }

    // ① 나무: 길의 시작 (0세) 뒤. 인생의 계절이 막 바뀌었으면 옛 나무가 옅게 남았다가 천천히 바뀜
    val tk = u * D.treeScale
    val tx = u * D.treeX
    val treeA11y = stringResource(treeName(decor.tree)) + ", " + stringResource(stageName(decor.stage))
    decor.prevTree?.let { prev ->
        Image(GardenArt.tree(ctx, prev, decor.season, decor.stage), null, Modifier.box(tx, gy, tk, D.treeBoxW, D.treeBoxH, D.treeAtX, D.treeAtY).graphicsLayer { alpha = 1f - decor.blend }, colorFilter = filter)
    }
    Image(GardenArt.tree(ctx, decor.tree, decor.season, decor.stage), treeA11y,
        Modifier.box(tx, gy, tk, D.treeBoxW, D.treeBoxH, D.treeAtX, D.treeAtY).graphicsLayer { alpha = decor.blend }.then(tap(DecorPart.TREE)), colorFilter = filter)

    // ⑤ 나무 발치: 이번 계절의 한 장 (꽃 · 풀 · 열매 · 낙엽 · 눈사람 한 조각) 이 땅에 놓임
    val ck = u * D.cardMini
    Image(GardenArt.card(ctx, decor.card.key), cardName(ctx, decor.card.key),
        Modifier.box(u * (D.treeX + D.cardFromTree), gy + 1.dp, ck, D.cardBoxW, D.cardBoxH, D.cardBoxW / 2, D.cardAtY)
            .graphicsLayer { transformOrigin = TransformOrigin(0.5f, D.cardAtY / D.cardBoxH); rotationZ = D.cardTilt }.then(tap(DecorPart.CARD)), colorFilter = filter)

    // ② 말뚝: 길의 끝 (기대수명). 걸린 것은 가로대에서 따로 흔들림, 등불은 밤에 켜짐, 편지가 오면 봉투
    val pk = u * D.postScale
    val lit = dark && decor.hang == Hang.LANTERN
    val postA11y = listOfNotNull(stringResource(R.string.decor_post), when (decor.hang) { Hang.CHIME -> stringResource(R.string.decor_hang_chime); Hang.BELL -> stringResource(R.string.decor_hang_bell); Hang.LANTERN -> stringResource(R.string.decor_hang_lantern); Hang.NONE -> null },
        if (decor.letter) stringResource(R.string.decor_post_letter) else null).joinToString(", ")
    Box(Modifier.box(x1, gy, pk, D.postBoxW, D.postBoxH, D.postAtX, D.postAtY).then(tap(if (decor.letter) DecorPart.LETTER else DecorPart.POST))) {
        val glowC = Tokens.Garden.Decor.Colors.glow
        if (lit) Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * D.lanternX / D.postBoxW, size.height * (D.lanternY + 16f) / D.postBoxH)
            val r = size.width * D.glow / D.postBoxW
            drawCircle(Brush.radialGradient(listOf(glowC.copy(alpha = 0.55f), Color.Transparent), c, r), r, c)
        }
        Image(GardenArt.post(ctx, decor.season), postA11y, Modifier.fillMaxSize(), colorFilter = filter)
        @Composable fun swing(part: String, px: Float, py: Float, deg: Float, f: ColorFilter?) =
            Image(GardenArt.postPart(ctx, part), null, Modifier.fillMaxSize().graphicsLayer { transformOrigin = TransformOrigin(px / D.postBoxW, py / D.postBoxH); rotationZ = if (moving) deg else 0f }, colorFilter = f)
        if (decor.hang >= Hang.LANTERN) swing(if (lit) "lantern_lit" else "lantern", D.lanternX, D.lanternY, sway * 1f, if (lit) null else filter)
        if (decor.hang >= Hang.CHIME) swing("chime", D.chimeX, D.chimeY, sway * D.swayDeg, filter)
        if (decor.hang >= Hang.BELL) swing("bell", D.bellX, D.bellY, -sway * D.swayDeg * 0.7f, filter)
        if (decor.letter) Image(GardenArt.postPart(ctx, "letter"), null, Modifier.fillMaxSize().graphicsLayer { rotationZ = if (moving) sway * 0.6f else 0f }, colorFilter = filter)
    }

    // ③ 연: 낮에만, 말뚝 가로대 끝에 실로 묶여 오른쪽 하늘에. 꼬리 리본 = 서른 줄마다 하나, 그 줄들의 마음 색
    if (decor.kite && !dark && kiteBottom > kiteTop) {
        val kk = u * D.kiteScale
        val kx = u * D.kiteX
        val ky = kiteTop + (kiteBottom - kiteTop) * D.kiteHigh
        val density = LocalDensity.current.density
        val bob = if (moving) sin(drift * 2f * Math.PI.toFloat()) else 0f
        val tie = Offset(((x1 - pk * D.postAtX) + pk * D.tieX).value, ((gy - pk * D.postAtY) + pk * D.tieY).value)
        val ink = Tokens.Garden.Colors.ink
        Canvas(Modifier.fillMaxSize()) {
            val from = Offset(tie.x * density, tie.y * density)
            val to = Offset(kx.toPx(), (ky + kk * 19f).toPx() + bob * D.kiteDrift * density)
            val path = Path().apply { moveTo(from.x, from.y); quadraticTo((from.x + to.x) / 2 - 12f * density, (from.y + to.y) / 2 + 22f * density, to.x, to.y) }
            drawPath(path, ink.copy(alpha = 0.45f), style = Stroke(0.8f * density))
        }
        val colors = decor.ribbons.map { feelingColor(it) }
        Box(Modifier.box(kx, ky, kk, D.kiteBoxW, D.kiteBoxH, D.kiteAtX, D.kiteAtY)
            .graphicsLayer { transformOrigin = TransformOrigin(D.kiteAtX / D.kiteBoxW, D.kiteAtY / D.kiteBoxH); translationY = bob * D.kiteDrift * density; rotationZ = bob * D.kiteTilt }
            .then(tap(DecorPart.KITE))) {
            Image(GardenArt.kite(ctx), stringResource(R.string.decor_kite), Modifier.fillMaxSize())
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
    Image(GardenArt.moss(ctx, decor.season, decor.buds), stringResource(R.string.obj_moss),
        Modifier.box(x, gy + 1.dp, k, D.mossBoxW, D.mossBoxH, D.mossAtX, D.mossAtY).then(if (onTap == null) Modifier else Modifier.clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { onTap(DecorPart.MOSS) }),
        colorFilter = nightFilter(SkyTime.isDark(now)))
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
