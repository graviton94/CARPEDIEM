package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.Animatable
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

/**
 * 미니 하루 (1.1.5): 요즘 (지난 7일) 자주 머문 마음이 아주 작은 하루가 되어, 내 하루 발치 가운데 ~ 오른쪽에 하루와 같은 땅에 나란히 (최대 셋).
 * 그 마음 색으로 물든 조약돌, 눈은 마음 따라 (밝은 마음 웃는 눈 · 가운데 마음 동그란 눈 · 무거운 마음 감긴 눈으로 하루 쪽에 살짝 기댐).
 * 새 마음이 오면 땅에서 퐁. 누르면 아래 한마디 “이번 주엔 평온이 자주 머물렀어요.”
 */
@Composable
internal fun MiniHarus(state: AppState, feelings: List<Feeling>, cx: Dp, gy: Dp, haruW: Dp) {
    if (feelings.isEmpty()) return
    val ctx = LocalContext.current
    val names = feelings.map { ctx.getString(feelingName(it)) }
    val msg = stringResource(if (feelings.first() in Letters.HEAVY) R.string.mini_noteHeavy else R.string.mini_note, names.first())
    val a11y = stringResource(R.string.mini_a11y, names.joinToString(", "))
    val pop = remember(feelings) { Animatable(0f) }
    LaunchedEffect(feelings) { pop.animateTo(1f, tween(MINI_POP_MS)) }
    val ink = G.Colors.ink
    var right = cx
    feelings.forEachIndexed { k, f ->
        val w = haruW * (if (k == 0) MINI_FIRST else MINI_REST)
        val x = cx + haruW * MINI_FROM + haruW * MINI_STEP * k
        right = x + w / 2
        val face = when (f) { in Feeling.BRIGHT -> 0; in Letters.HEAVY -> 2; else -> 1 }
        val color = lerp(feelingColor(f), Color(0xFF7F7A71), 0.35f)
        Canvas(Modifier.offset(x - w / 2, gy - w * 0.7f).size(w, w * 0.7f).graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 1f); scaleX = pop.value; scaleY = pop.value
            rotationZ = if (face == 2) -MINI_LEAN else 0f
        }) { miniStone(color, ink, face) }
    }
    // 누르는 자리: 미니 하루들 위만 (하루 몸은 그대로 쓰다듬기)
    val tt = Tokens.Layout.tapTarget
    val left = cx + haruW * MINI_FROM - haruW * MINI_FIRST / 2
    Box(Modifier.offset(left, gy - tt * 0.8f).size(right - left, tt).semantics { contentDescription = a11y }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { state.say(msg) })
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
    drawPath(p, ink, style = Stroke(w * 0.05f, join = StrokeJoin.Round))
    val sw = w * 0.045f
    listOf(-.17f, .1f).forEach { ex ->
        val cx = ox + w * ex; val cy = oy - h * .56f; val r = w * .085f
        when (face) {
            0 -> drawArc(ink, 200f, 140f, false, Offset(cx - r, cy - r * .7f), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
            2 -> drawArc(ink, 20f, 140f, false, Offset(cx - r, cy - r * 1.2f), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw, cap = StrokeCap.Round))
            else -> drawCircle(ink, r * .55f, Offset(cx, cy))
        }
    }
}

/** 하루 폭에 대한 크기 · 자리: 첫째 (가장 자주) 는 조금 크게, 하루 가운데에서 오른쪽으로 나란히. 무거운 마음은 하루 쪽으로 기댐 (도). */
private const val MINI_FIRST = 0.22f
private const val MINI_REST = 0.18f
private const val MINI_FROM = 0.04f
private const val MINI_STEP = 0.2f
private const val MINI_LEAN = 10f
private const val MINI_POP_MS = 600
