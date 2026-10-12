package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.TokenText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** 이슬 유리를 흐리게 그릴 수 있는 폰 (안드로이드 12+). 그 아래는 흐림 없이 한지빛 김 + 물방울. */
internal val dewBlurs = android.os.Build.VERSION.SDK_INT >= 31

/**
 * 아침 이슬 (1.1.5): 첫 화면이 걷히면 정원이 김 서린 유리 너머에 있음 (흐린 정원 + 옅은 한지빛 + 작은 물방울).
 * 손끝이 지나간 자리만 맑아지고, 닦은 가장자리에서 가끔 물방울이 또르르. 하루 자리를 닦은 만큼 [onHaru] (0..1) 로 알려 하루가 눈을 뜸.
 * 하루가 거의 맑아지거나 화면을 넉넉히 닦으면 · 두 번 톡 · 뒤로 가기 · 화면 읽기의 ‘이슬 닦기’ → 남은 김이 스르르 ([onClear] auto = false).
 * 그냥 두면 한동안 뒤 저절로 천천히 걷힘 ([onClear] auto = true). 다 걷히면 [onGone].
 *
 * [garden] 은 정원 페이지를 그대로 담아 둔 그림 (흐림은 여기서 걸어 김 아래에 깜). [haru] 는 이 화면 좌표의 하루 자리 (px).
 */
@Composable
internal fun MorningDew(garden: GraphicsLayer, haru: Rect, onHaru: (Float) -> Unit, onClear: (auto: Boolean) -> Unit, onGone: () -> Unit) {
    val g = Theme.gc
    val density = LocalDensity.current
    // 크기는 화면 폭의 1% 를 한 칸으로 (손끝 폭 · 물방울이 폰 크기와 함께 커지게)
    val u = with(density) { (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp / 100f).toPx() }
    val scope = rememberCoroutineScope()
    val fog = remember { Animatable(1f) }          // 김 전체 (걷히면 0)
    val words = remember { Animatable(0f) }        // 유리 위 두 줄
    var tick by remember { mutableIntStateOf(0) }  // 닦은 자리가 바뀌면 다시 그리기
    var cleared by remember { mutableStateOf(false) }
    var lastWipe by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var wiped by remember { mutableFloatStateOf(0f) }
    val haruNow by rememberUpdatedState(haru)
    val path = remember { android.graphics.Path() }
    val trails = remember { android.graphics.Path() }
    val trickles = remember { ArrayList<FloatArray>() }   // x, y, 속도, 반지름, 멈출 y
    // 닦은 칸 (화면을 COLS × ROWS 로 나눠, 손끝이 닿은 칸만 셈: 픽셀을 읽지 않음)
    val cells = remember { BooleanArray(COLS * ROWS) }
    val beads = remember { val r = Random(BEAD_SEED); List(BEADS) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.25f + r.nextFloat() * r.nextFloat() * 1.1f) } }
    // 닦은 자리: 경로를 김에서 도려냄 (DstOut). 가장자리는 넓고 옅은 획 → 좁고 짙은 획을 겹쳐 부드럽게
    val wipePath = remember { path.asComposePath() }
    val trailPath = remember { trails.asComposePath() }
    val wipeStrokes = remember(u) { WIPE_PASSES.map { (k, a) -> androidx.compose.ui.graphics.drawscope.Stroke(u * WIPE_W * k, cap = StrokeCap.Round, join = StrokeJoin.Round) to a } }
    val trailStroke = remember(u) { androidx.compose.ui.graphics.drawscope.Stroke(u * TRAIL_W, cap = StrokeCap.Round) }
    val layerPaint = remember { Paint() }
    garden.renderEffect = if (dewBlurs) androidx.compose.ui.graphics.BlurEffect(u * BLUR, u * BLUR, androidx.compose.ui.graphics.TileMode.Clamp) else null

    fun clear(auto: Boolean) {
        if (cleared) return
        cleared = true
        onClear(auto)
        scope.launch { fog.animateTo(0f, tween(if (auto) AUTO_FADE_MS else FADE_MS, easing = LinearEasing)); onGone() }
    }
    val box = remember { floatArrayOf(1f, 1f) }   // 그린 크기 (px)
    fun mark(p: Offset) {
        val cw = box[0] / COLS; val ch = box[1] / ROWS; val r = u * WIPE_W / 2
        for (cy in 0 until ROWS) for (cx in 0 until COLS) {
            val k = cy * COLS + cx; if (cells[k]) continue
            val c = Offset((cx + 0.5f) * cw, (cy + 0.5f) * ch)
            if ((c - p).getDistance() <= r + minOf(cw, ch) / 2) cells[k] = true
        }
        // 화면 전체 · 하루 자리 각각 닦인 비율
        var all = 0; var hIn = 0; var hAll = 0
        val hr = haruNow
        for (k in cells.indices) {
            val c = Offset((k % COLS + 0.5f) * cw, (k / COLS + 0.5f) * ch)
            if (cells[k]) all++
            if (hr.contains(c)) { hAll++; if (cells[k]) hIn++ }
        }
        wiped = all.toFloat() / cells.size
        val hf = if (hAll > 0) hIn.toFloat() / hAll else 0f
        onHaru(hf)
        if (hf >= HARU_DONE || wiped >= ALL_DONE) clear(false)
    }
    fun wipe(a: Offset, b: Offset) {
        if (cleared) return
        if (a == b) { path.moveTo(a.x, a.y); path.lineTo(a.x + 0.1f, a.y) } else { path.moveTo(a.x, a.y); path.lineTo(b.x, b.y) }
        lastWipe = System.currentTimeMillis()
        mark(b)
        // 닦은 가장자리에서 가끔 물방울이 또르르
        if (Random.nextFloat() < TRICKLE_CHANCE && trickles.size < TRICKLE_MAX) {
            val side = if (Random.nextBoolean()) -1 else 1
            trickles += floatArrayOf(b.x + side * u * WIPE_W * 0.55f, b.y + u * 2f, 0f, u * (0.7f + Random.nextFloat() * 0.6f), b.y + u * (6f + Random.nextFloat() * 14f))
        }
        tick++
    }
    // 유리 위 두 줄은 첫 화면이 걷힌 뒤 천천히. 그냥 두면 한동안 뒤 저절로 걷힘
    LaunchedEffect(Unit) { delay(WORDS_DELAY_MS); words.animateTo(1f, tween(WORDS_MS)) }
    LaunchedEffect(Unit) { while (!cleared) { delay(1000); if (System.currentTimeMillis() - lastWipe > IDLE_MS) clear(true) } }
    // 흘러내리는 물방울
    LaunchedEffect(Unit) {
        while (fog.value > 0f) {
            withFrameMillis { }
            if (trickles.isEmpty()) continue
            val it = trickles.iterator()
            while (it.hasNext()) {
                val d = it.next()
                d[2] = minOf(d[2] + u * 0.02f, u * 0.9f)
                val ny = d[1] + d[2]
                trails.moveTo(d[0], d[1]); trails.lineTo(d[0], ny); d[1] = ny
                if (d[1] > d[4]) it.remove()
            }
            tick++
        }
    }
    BackHandler(enabled = !cleared) { clear(false) }
    val a11y = stringResource(R.string.dew_a11y)
    BoxWithConstraints(Modifier.fillMaxSize().graphicsLayer { alpha = fog.value }
        .semantics { contentDescription = a11y; onClick(a11y) { clear(false); true } }
        .pointerInput(Unit) {
            var lastUp = 0L; var lastAt = Offset.Zero
            awaitEachGesture {
                val down = awaitFirstDown()
                if (down.uptimeMillis - lastUp < DOUBLE_TAP_MS && (down.position - lastAt).getDistance() < u * 12f) { clear(false); return@awaitEachGesture }
                var prev = down.position; wipe(prev, prev); down.consume()
                while (true) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed) { lastUp = c.uptimeMillis; lastAt = c.position; break }
                    wipe(prev, c.position); prev = c.position; c.consume()
                }
            }
        }) {
        Canvas(Modifier.fillMaxSize()) {
            tick; box[0] = size.width; box[1] = size.height
            drawIntoCanvas { c ->
                c.saveLayer(Rect(Offset.Zero, size), layerPaint)
                if (dewBlurs) drawLayer(garden)
                drawRect(g.paper.copy(alpha = if (dewBlurs) VEIL else VEIL_FLAT))
                beads.forEach { b ->
                    val x = b[0] * size.width; val y = b[1] * size.height; val r = b[2] * 1.3f * u
                    drawCircle(Color.White.copy(alpha = 0.55f), r, Offset(x, y))
                    drawCircle(g.ink.copy(alpha = 0.12f), r * 0.9f, Offset(x + r * 0.25f, y + r * 0.3f))
                    drawCircle(Color.White.copy(alpha = 0.9f), r * 0.3f, Offset(x - r * 0.3f, y - r * 0.3f))
                }
                wipeStrokes.forEach { (st, a) -> drawPath(wipePath, Color.Black.copy(alpha = a), style = st, blendMode = BlendMode.DstOut) }
                drawPath(trailPath, Color.Black.copy(alpha = TRAIL_ALPHA), style = trailStroke, blendMode = BlendMode.DstOut)
                c.restore()
            }
            trickles.forEach { d ->
                drawCircle(Color.White.copy(alpha = 0.8f), d[3], Offset(d[0], d[1]))
                drawCircle(g.ink.copy(alpha = 0.35f), d[3], Offset(d[0], d[1]), style = androidx.compose.ui.graphics.drawscope.Stroke(u * 0.25f))
            }
        }
        // 유리 위 두 줄 (닦기 시작하면 옅어짐)
        Column(Modifier.fillMaxWidth().offset(y = maxHeight * WORDS_AT).padding(horizontal = Theme.deviceClass.pageMargin)
            .graphicsLayer { alpha = words.value * (1f - wiped * 4f).coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            TokenText(stringResource(R.string.dew_line), Tokens.TypeScale.callout.serif(), color = g.ink, align = TextAlign.Center)
            TokenText(stringResource(R.string.dew_hint), Tokens.TypeScale.footnote, color = g.inkSoft, align = TextAlign.Center)
        }
    }
}

/** 닦은 칸을 세는 바둑판. */
private const val COLS = 12
private const val ROWS = 24
/** 하루 자리가 이만큼 · 화면이 이만큼 닦이면 남은 김이 스르르. */
private const val HARU_DONE = 0.5f
private const val ALL_DONE = 0.4f
/** 손끝 폭 · 가장자리 번짐 · 흐림 (화면 폭의 %). */
private const val WIPE_W = 9f
/** 닦은 획 셋 (폭 배율, 짙기): 가장자리가 번진 듯. */
private val WIPE_PASSES = listOf(1.35f to 0.35f, 1.0f to 0.7f, 0.7f to 1f)
private const val BLUR = 1.5f
private const val TRAIL_W = 0.7f
private const val TRAIL_ALPHA = 0.35f
/** 김의 한지빛 (흐린 정원 위) · 흐림이 없는 폰은 더 짙게. */
private const val VEIL = 0.42f
private const val VEIL_FLAT = 0.78f
private const val BEADS = 90
private const val BEAD_SEED = 7
private const val TRICKLE_CHANCE = 0.06f
private const val TRICKLE_MAX = 14
private const val FADE_MS = 1400
private const val AUTO_FADE_MS = 3200
private const val IDLE_MS = 15_000L
private const val DOUBLE_TAP_MS = 300L
private const val WORDS_DELAY_MS = 1200L
private const val WORDS_MS = 1000
private const val WORDS_AT = 0.4f
