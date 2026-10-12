package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.Letters
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * 미니 하루 (1.1.5): 요즘 (지난 7일) 자주 머문 마음이 아주 작은 하루가 되어, 내 하루 발치 가운데 ~ 오른쪽에 (최대 셋).
 * 온 순서대로 쌓임: 첫째 왼쪽 아래 · 둘째 오른쪽 아래 · 셋째 둘 사이 위. 아래 줄은 땅에 살짝 묻혀 하루 밑선을 가림.
 * 그 마음 색으로 물든 조약돌, 눈은 마음 따라 (밝은 마음 웃는 눈 · 가운데 마음 동그란 눈 · 무거운 마음 감긴 눈으로 하루 쪽에 살짝 기댐).
 * 혼자일 땐 하루 곁에 더 다정하게 밀착해 살짝 기대고, 누르면 조약돌이 살짝 퐁 뛰며 한마디 “이번 주엔 평온이 자주 머물렀어요.”
 */
@Composable
internal fun MiniHarus(state: AppState, feelings: List<Feeling>, cx: Dp, gy: Dp, haruW: Dp) {
    if (feelings.isEmpty()) return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val names = feelings.map { ctx.getString(feelingName(it)) }
    val msg = stringResource(if (feelings.first() in Letters.HEAVY) R.string.mini_noteHeavy else R.string.mini_note, names.first())
    val a11y = stringResource(R.string.mini_a11y, names.joinToString(", "))
    val pop = remember(feelings) { Animatable(0f) }
    val bounce = remember { Animatable(0f) }
    LaunchedEffect(feelings) { pop.animateTo(1f, tween(MINI_POP_MS)) }
    val ink = G.Colors.ink
    // 쌓는 차례는 온 순서 (지난 7일 안에서 먼저 머문 마음이 먼저): 1 왼쪽 아래 · 2 오른쪽 아래 · 3 둘 사이 위
    val order = remember(feelings, state.lines) {
        val today = (state.fixedNow ?: java.time.LocalDateTime.now()).toLocalDate()
        feelings.sortedBy { f -> state.lines.filter { it.feeling == f && it.date.isAfter(today.minusDays(MINI_DAYS)) }.minOfOrNull { it.date } ?: today }
    }
    val solo = feelings.size == 1
    val w = haruW * MINI_W
    val bx = cx + haruW * (if (solo) MINI_SOLO_AT else MINI_AT)
    // 아래 줄은 하루 밑선을 살짝 가릴 만큼 땅에 묻혀 앉음
    val ground = gy + haruW * MINI_SINK
    fun spot(k: Int): Pair<Dp, Dp> = when {
        solo -> bx to ground
        k == 0 -> bx - w * MINI_SPREAD to ground
        k == 1 -> bx + w * MINI_SPREAD to ground
        else -> bx to ground - w * MINI_STACK
    }
    var left = Dp.Infinity; var right = -Dp.Infinity; var top = ground
    order.forEachIndexed { k, f ->
        val (x, base) = spot(k)
        left = minOf(left, x - w / 2); right = maxOf(right, x + w / 2); top = minOf(top, base - w * 0.7f)
        val face = when (f) { in Feeling.BRIGHT -> 0; in Letters.HEAVY -> 2; else -> 1 }
        val color = lerp(feelingColor(f), Color(0xFF7F7A71), 0.35f)
        val bVal = bounce.value
        val lift = if (bVal in 0.001f..0.999f) sin((bVal + k * 0.15f).coerceAtMost(1f) * PI.toFloat()) else 0f
        Canvas(Modifier.offset(x - w / 2, base - w * 0.7f).size(w, w * 0.7f).graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 1f)
            scaleX = pop.value; scaleY = pop.value
            rotationZ = when {
                face == 2 && k < 2 -> -MINI_LEAN
                solo -> -MINI_SOLO_LEAN
                else -> 0f
            }
            translationY = -lift * (haruW * MINI_BOUNCE_LIFT).toPx()
        }) { miniStone(color, ink, face) }
    }
    // 누르는 자리: 미니 하루들 위만 (하루 몸은 그대로 쓰다듬기, 누르면 퐁 뛰며 말)
    val tt = Tokens.Layout.tapTarget
    val tapTop = minOf(top, ground - tt * 0.8f)
    Box(Modifier.offset(left, tapTop).size(right - left, maxOf(tt, ground - tapTop + w * 0.1f)).semantics { contentDescription = a11y }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            scope.launch { bounce.snapTo(0f); bounce.animateTo(1f, tween(MINI_BOUNCE_MS, easing = LinearEasing)); bounce.snapTo(0f) }
            state.say(msg)
        })
}

/** 하루와 같은 조약돌 모양 (상자 아래 가운데가 땅). face: 0 웃는 눈 · 1 동그란 눈 · 2 감긴 눈. */
private fun DrawScope.miniStone(fill: Color, ink: Color, face: Int) {
    val w = size.width; val h = w * 0.62f; val ox = w / 2; val oy = size.height
    val p = Path().apply {
        moveTo(ox - w * .5f, oy - h * .35f)
        cubicTo(ox - w * .48f, oy - h * 1.02f, ox + w * .44f, oy - h * 1.08f, ox + w * .5f, oy - h * .42f)
        cubicTo(ox + w * .55f, oy - h * .02f, ox + w * .2f, oy + h * .02f, ox - w * .1f, oy)
        cubicTo(ox - w * .4f, oy, ox - w * .53f, oy - h * .1f, ox - w * .5f, oy - h * .35f)
        close()
    }
    drawPath(p, fill)
    drawPath(p, ink, style = Stroke(w * 0.075f, join = StrokeJoin.Round))
    val sw = w * 0.06f
    listOf(-.17f, .1f).forEach { ex ->
        val cx = ox + w * ex; val cy = oy - h * .56f; val r = w * .085f
        when (face) {
            0 -> drawArc(ink, 200f, 140f, false, Offset(cx - r, cy - r * .7f), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
            2 -> drawArc(ink, 20f, 140f, false, Offset(cx - r, cy - r * 1.2f), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
            else -> drawCircle(ink, r * .55f, Offset(cx, cy))
        }
    }
}

/**
 * 하루 폭에 대한 크기 · 자리: 모두 같은 크기, 하루 가운데에서 조금 오른쪽에 쌓임 (아래 둘 · 위 하나). 아래 줄은 땅에 살짝 묻힘 (MINI_SINK).
 * 혼자일 땐 하루 곁에 살짝 기대고 (MINI_SOLO_LEAN), 아래 줄의 무거운 마음은 하루 쪽으로 기댐 (MINI_LEAN).
 */
private const val MINI_W = 0.2f
private const val MINI_AT = 0.16f
private const val MINI_SOLO_AT = 0.1f
private const val MINI_SPREAD = 0.47f
private const val MINI_STACK = 0.4f
private const val MINI_SINK = 0.035f
private const val MINI_DAYS = 7L
private const val MINI_LEAN = 10f
private const val MINI_SOLO_LEAN = 7f
private const val MINI_POP_MS = 600
private const val MINI_BOUNCE_MS = 520
private const val MINI_BOUNCE_LIFT = 0.14f
