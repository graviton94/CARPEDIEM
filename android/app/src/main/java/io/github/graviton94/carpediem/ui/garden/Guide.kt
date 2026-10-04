package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.TokenText
import io.github.graviton94.carpediem.ui.modalBox
import io.github.graviton94.carpediem.ui.pop
import io.github.graviton94.carpediem.ui.rememberPop
import kotlinx.coroutines.delay

// ───────────────────────── 처음 온 사람의 안내 ─────────────────────────
// 세 겹: ① 첫 화면 앞 소개 석 장 (IntroScreen) ② 첫 정원에서 둘러보기 (GuideTour, 하나씩 비춰 줌)
// ③ 기록 · 추억 · 흐름 · 돌의 페이지에 처음 들어가면 맨 위에 한 번 (PageHint). 설정의 ‘안내 다시 보기’로 ②③ 을 다시.

/** 첫 화면 앞 소개 석 장: 무엇을 하는 앱인지 · 하루에 할 일 · 내 돌과 기록이 머무는 곳. 건너뛸 수 있음. */
@Composable
fun IntroScreen(state: AppState, onDone: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    var page by rememberSaveable { mutableStateOf(0) }
    val pages = listOf(
        R.string.intro_title1 to R.string.intro_body1,
        R.string.intro_title2 to R.string.intro_body2,
        R.string.intro_title3 to R.string.intro_body3,
    )
    val last = page == pages.lastIndex
    BackHandler(enabled = page > 0) { page-- }
    Box(Modifier.fillMaxSize().paperBackground()) {
        Image(GardenArt.sky(ctx, Season.SPRING), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth, alignment = Alignment.TopCenter)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 건너뛰기 (오른쪽 위, 마지막 장에는 없음)
            Box(Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget), contentAlignment = Alignment.CenterEnd) {
                if (!last) TokenText(stringResource(R.string.guide_skip), Tokens.TypeScale.subhead,
                    Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onDone).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp3), color = p.secondary)
            }
            Spacer(Modifier.weight(1f))
            val a = rememberPop(page)
            Column(Modifier.fillMaxWidth().pop(a), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
                // 장마다 그림 하나: 해 · 깃털 · 내 돌 (첫 만남처럼 반짝임)
                Box(Modifier.height(u * 120), contentAlignment = Alignment.Center) {
                    when (page) {
                        0 -> Image(GardenArt.sun(ctx), null, Modifier.size(u * 96))
                        1 -> Image(GardenArt.obj(ctx, "feather"), null, Modifier.size(u * 96))
                        else -> haruArt(state, sprout = true).art?.let { BigStone(it, pet = false, hat = false, sparkle = true, size = 0.7f) }
                    }
                }
                TokenText(stringResource(pages[page].first), Tokens.TypeScale.title2.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                TokenText(stringResource(pages[page].second), Tokens.TypeScale.callout, Modifier.fillMaxWidth().widthIn(max = 420.dp), color = p.secondary, align = TextAlign.Center)
            }
            Spacer(Modifier.weight(1f))
            Dots(pages.size, page)
            Spacer(Modifier.height(Tokens.Space.sp4))
            GardenButton(stringResource(if (last) R.string.intro_start else R.string.guide_next), { if (last) onDone() else page++ }, filled = true, seed = 760 + page)
            Spacer(Modifier.height(Tokens.Space.sp6))
        }
    }
}

/** 몇째 장인지 작은 점으로. */
@Composable
private fun Dots(count: Int, current: Int) {
    val p = Theme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val s = Theme.unit * (if (i == current) 9 else 6)
            Box(Modifier.size(s).background(if (i == current) p.olive else p.secondary.copy(alpha = 0.35f), CircleShape))
        }
    }
}

// ───── 둘러보기: 정원에서 하나씩 비춰 줌 ─────

/** 둘러보기가 비출 자리들 (화면 기준 사각형). 정원의 각 부분이 guideTarget 으로 자기 자리를 알린다. */
@Stable
class GuideTargets {
    val rects = mutableStateMapOf<String, Rect>()
}

fun Modifier.guideTarget(targets: GuideTargets?, key: String): Modifier =
    if (targets == null) this else onGloballyPositioned { c -> targets.rects[key] = c.boundsInRoot() }

/** 둘러보기의 한 걸음: 비출 자리 (없으면 가운데 창만) · 제목 · 설명. */
class GuideStep(val target: String?, val title: Int, val body: Int)

/** 정원 둘러보기의 차례. 오늘의 문장이 질문인 날에도 같은 자리를 비춘다. */
val GardenGuideSteps = listOf(
    GuideStep(null, R.string.guide_welcomeTitle, R.string.guide_welcome),
    GuideStep("number", R.string.guide_numberTitle, R.string.guide_number),
    GuideStep("words", R.string.guide_wordsTitle, R.string.guide_words),
    GuideStep("haru", R.string.guide_haruTitle, R.string.guide_haru),
    GuideStep("path", R.string.guide_pathTitle, R.string.guide_path),
    GuideStep("actions", R.string.guide_actionsTitle, R.string.guide_actions),
    GuideStep("tabs", R.string.guide_tabsTitle, R.string.guide_tabs),
    GuideStep("settings", R.string.guide_settingsTitle, R.string.guide_settings),
    GuideStep(null, R.string.guide_endTitle, R.string.guide_end),
)

/**
 * 정원 둘러보기: 화면을 옅게 덮고 한 곳만 밝게 비춘 뒤, 그 옆에 짧은 설명 한 장.
 * 아무 데나 누르거나 ‘다음’으로 넘어가고, ‘건너뛰기’ · 뒤로 가기로 끝낸다. 마지막 장은 ‘한 줄 쓰러 가기’.
 */
@Composable
fun GuideTour(state: AppState, targets: GuideTargets, steps: List<GuideStep>, onWrite: () -> Unit, onDone: () -> Unit) {
    val p = Theme.palette
    val density = LocalDensity.current
    // 몇째 장인지는 state 에 (한마디 창이 잠깐 떠서 둘러보기가 가려졌다 돌아와도 이어서)
    var i by state.guideStepState
    // 정원 그림이 자리를 잡을 때까지 조금 기다렸다가
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(G.Motion.pageMs.toLong()) ; ready = true }
    if (!ready) return
    val step = steps[i.coerceIn(0, steps.lastIndex)]
    val last = i >= steps.lastIndex
    fun next() { if (last) onDone() else i++ }
    BackHandler { onDone() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    // 한 자리가 여러 조각이면 (예: 숫자 + 단위 칩) 모두 감싸는 사각형
    val hole = step.target?.let { k ->
        targets.rects.filterKeys { it == k || it.startsWith("$k.") }.values.filter { it.width > 0f && it.height > 0f }
            .reduceOrNull { x, y -> Rect(minOf(x.left, y.left), minOf(x.top, y.top), maxOf(x.right, y.right), maxOf(x.bottom, y.bottom)) }
    }?.translate(-origin)?.inflate(with(density) { Tokens.Space.sp2.toPx() })
    val a = rememberPop(i)
    val scrim = Theme.gc.scrim.copy(alpha = 0.62f)
    val ring = p.olive
    BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft }
            .pointerInput(i) { detectTapGestures { next() } },
    ) {
        val hPx = with(density) { maxHeight.toPx() }
        // 덮개와 비추는 구멍 (구멍은 지움으로 뚫는다)
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(scrim)
            hole?.let { r ->
                val rad = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                drawRoundRect(Color.Black, r.topLeft, r.size, rad, blendMode = BlendMode.Clear)
                drawRoundRect(ring.copy(alpha = 0.9f * a.value), r.topLeft, r.size, rad, style = Stroke(2.dp.toPx()))
            }
        }
        // 설명 한 장: 비춘 곳이 아래쪽이면 그 위에, 위쪽이면 그 아래에. 비출 곳이 없으면 가운데
        var cardH by remember { mutableStateOf(0f) }
        val gap = with(density) { Tokens.Space.sp3.toPx() }
        val top = when {
            hole == null -> (hPx - cardH) / 2f
            hole.center.y > hPx / 2f -> (hole.top - gap - cardH)
            else -> hole.bottom + gap
        }.let { t ->
            // 창이 화면보다 크면 (가로 화면 · 큰 글씨) 위에 붙임
            val minTop = with(density) { Tokens.Space.sp10.toPx() }
            t.coerceIn(minTop, maxOf(minTop, hPx - cardH - with(density) { Tokens.Space.sp6.toPx() }))
        }
        Column(
            Modifier.offset(y = with(density) { top.toDp() }).fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin)
                .onSizeChanged { cardH = it.height.toFloat() }.pop(a).modalBox(1400 + i)
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                .padding(Tokens.Space.sp5).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
        ) {
            TokenText("${i + 1} / ${steps.size}", Tokens.TypeScale.caption1, color = p.secondary)
            TokenText(stringResource(step.title), Tokens.TypeScale.title3.serif())
            TokenText(stringResource(step.body), Tokens.TypeScale.callout, color = p.secondary)
            Spacer(Modifier.height(Tokens.Space.sp1))
            if (last) {
                GardenButton(stringResource(R.string.guide_write), { onDone(); onWrite() }, filled = true, seed = 1420)
                GardenButton(stringResource(R.string.guide_look), onDone, filled = false, seed = 1421)
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TokenText(stringResource(R.string.guide_skip), Tokens.TypeScale.subhead,
                    Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onDone).padding(end = Tokens.Space.sp4, top = Tokens.Space.sp3, bottom = Tokens.Space.sp3), color = p.secondary)
                Spacer(Modifier.weight(1f))
                GardenButton(stringResource(R.string.guide_next), { next() }, filled = true, seed = 1410 + i, modifier = Modifier.widthIn(max = 160.dp))
            }
        }
    }
}

// ───── 페이지마다 처음 한 번 ─────

/** 처음 들어온 페이지의 맨 위에 한 장: 이 페이지에서 무엇을 하는지. ‘알겠어요’로 다시 나오지 않음. */
@Composable
fun PageHint(state: AppState, key: String, title: Int, body: Int, modifier: Modifier = Modifier) {
    if (key in state.pageHints) return
    val p = Theme.palette
    val a = rememberPop(key)
    Column(
        modifier.fillMaxWidth().pop(a).crayonBox(Theme.gc.chip, G.Radius.box, G.Stroke.chip, 1450 + key.length).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
    ) {
        TokenText(stringResource(title), Tokens.TypeScale.subhead, weight = FontWeight.SemiBold)
        TokenText(stringResource(body), Tokens.TypeScale.footnote, color = p.foreground)
        TokenText(stringResource(R.string.guide_ok), Tokens.TypeScale.footnote,
            Modifier.align(Alignment.End).heightIn(min = Tokens.Layout.tapTarget).clickable { state.pageHintSeen(key) }.padding(horizontal = Tokens.Space.sp2, vertical = Tokens.Space.sp3),
            color = p.olive, weight = FontWeight.SemiBold)
    }
}
