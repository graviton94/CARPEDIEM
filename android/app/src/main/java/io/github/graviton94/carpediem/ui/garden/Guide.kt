package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import kotlinx.coroutines.launch
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
// ③ 기록 · 추억 · 흐름 · 돌의 페이지에 처음 들어가면 그 페이지의 짧은 둘러보기 (같은 비춤). 설정의 ‘안내 다시 보기’로 ②③ 을 다시.

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
            // 가운데 (그림 · 제목 · 설명): 큰 글씨 · 가로 화면이면 이 부분만 스크롤
            val a = rememberPop(page)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()).pop(a), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
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
            }
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

// ───── 둘러보기: 한 곳씩 비춰 줌 (정원 · 기록 · 추억 · 흐름 · 돌의 페이지) ─────

/** 둘러보기가 비출 자리들 (화면 기준 사각형) 과, 스크롤 안에 있으면 그 자리를 화면으로 끌어오는 일. */
@Stable
class GuideTargets {
    val rects = mutableStateMapOf<String, Rect>()
    val reveal = mutableMapOf<String, suspend () -> Unit>()
}

/** 이 자리를 둘러보기에 알림. 스크롤 안이면 그 장을 보여 줄 때 화면 안으로 끌어온다. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.guideTarget(targets: GuideTargets?, key: String): Modifier {
    if (targets == null) return this
    val bring = remember { BringIntoViewRequester() }
    DisposableEffect(targets, key) { targets.reveal[key] = { bring.bringIntoView() }; onDispose { targets.reveal.remove(key); targets.rects.remove(key) } }
    return bringIntoViewRequester(bring).onGloballyPositioned { c -> targets.rects[key] = c.boundsInRoot() }
}

/** 둘러보기의 한 걸음: 비출 자리 (없으면 가운데 창만) · 제목 · 설명. */
class GuideStep(val target: String?, val title: Int, val body: Int)

/** 정원 둘러보기의 차례. 오늘의 문장이 질문인 날에도 같은 자리를 비춘다. */
val GardenGuideSteps = listOf(
    GuideStep(null, R.string.guide_welcomeTitle, R.string.guide_welcome),
    GuideStep("number", R.string.guide_numberTitle, R.string.guide_number),
    GuideStep("words", R.string.guide_wordsTitle, R.string.guide_words),
    GuideStep("haru", R.string.guide_haruTitle, R.string.guide_haru),
    GuideStep("actions", R.string.guide_actionsTitle, R.string.guide_actions),
    GuideStep("tabs", R.string.guide_tabsTitle, R.string.guide_tabs),
    GuideStep("settings", R.string.guide_settingsTitle, R.string.guide_settings),
    GuideStep(null, R.string.guide_endTitle, R.string.guide_end),
)

/** 늘 같은 자리 (페이지 오른쪽 위) 의 작은 (?): 누르면 그 페이지의 안내를 다시. */
@Composable
fun HelpButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.guide_again)
    Box(modifier.size(Tokens.Layout.tapTarget).clickable(onClickLabel = label, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(Tokens.Layout.tapTarget * 0.5f).crayonBox(null, G.Radius.chip, G.Stroke.chip, 1590), contentAlignment = Alignment.Center) {
            TokenText("?", Tokens.TypeScale.footnote, color = Theme.palette.secondary, weight = FontWeight.SemiBold)
        }
    }
}

/** 페이지마다 처음 들어왔을 때의 짧은 둘러보기 (key = PAGE_HINTS). 비출 자리가 없는 장은 건너뜀. */
val PageGuideSteps = mapOf(
    "write" to listOf(
        GuideStep("write.box", R.string.tour_writeBoxTitle, R.string.tour_writeBox),
        GuideStep("write.feeling", R.string.tour_writeFeelingTitle, R.string.tour_writeFeeling),
        GuideStep("write.record", R.string.tour_writeRecordTitle, R.string.tour_writeRecord),
    ),
    "memories" to listOf(
        GuideStep("mem.album", R.string.tour_memAlbumTitle, R.string.tour_memAlbum),
        GuideStep("mem.future", R.string.tour_memFutureTitle, R.string.tour_memFuture),
        GuideStep("mem.look", R.string.tour_memLookTitle, R.string.tour_memLook),
    ),
    "flow" to listOf(
        GuideStep("flow.bars", R.string.tour_flowBarsTitle, R.string.tour_flowBars),
        GuideStep("flow.calendar", R.string.tour_flowCalendarTitle, R.string.tour_flowCalendar),
        GuideStep("flow.special", R.string.tour_flowSpecialTitle, R.string.tour_flowSpecial),
    ),
    "stone" to listOf(
        GuideStep("stone.big", R.string.tour_stoneTitle, R.string.tour_stone),
        GuideStep("stone.calendar", R.string.tour_stoneCalendarTitle, R.string.tour_stoneCalendar),
        GuideStep("stone.action", R.string.tour_stoneActionTitle, R.string.tour_stoneAction),
    ),
)

/**
 * 둘러보기: 화면을 옅게 덮고 한 곳만 밝게 비춘 뒤, 그 옆에 짧은 설명 한 장.
 * 아무 데나 누르거나 ‘다음’으로 넘어가고, ‘건너뛰기’ · 뒤로 가기로 끝낸다.
 * onWrite 가 있으면 (정원 둘러보기) 마지막 장은 ‘한 줄 쓰러 가기’ · ‘정원 더 둘러보기’, 없으면 ‘알겠어요’.
 */
@Composable
fun GuideTour(state: AppState, key: String, targets: GuideTargets, steps: List<GuideStep>, onWrite: (() -> Unit)? = null, onDone: () -> Unit) {
    val p = Theme.palette
    val density = LocalDensity.current
    // 몇째 장인지는 state 에 (한마디 창이 잠깐 떠서 둘러보기가 가려졌다 돌아와도 이어서)
    // 둘러보기마다 따로 (정원 · 기록 · … 이 서로의 장 번호를 쓰지 않게)
    val i = state.guideSteps[key] ?: 0
    fun setStep(v: Int) { state.guideSteps[key] = v }
    // 둘러보는 동안 알림 한마디는 기다림 (설명 창과 겹치지 않게), 페이지도 넘어가지 않음. 겹쳐 그려지는 동안에도 맞게 수로 셈
    DisposableEffect(Unit) { state.tourShown(true); onDispose { state.tourShown(false) } }
    // 그림이 자리를 잡을 때까지 조금 기다렸다가, 비출 자리가 있는 장만
    var shown by remember { mutableStateOf<List<GuideStep>?>(null) }
    LaunchedEffect(Unit) {
        delay(G.Motion.pageMs.toLong())
        shown = steps.filter { s -> s.target == null || targets.rects.any { (k, r) -> (k == s.target || k.startsWith(s.target + ".")) && r.width > 0f && r.height > 0f } }
    }
    val list = shown ?: return
    if (list.isEmpty()) { LaunchedEffect(Unit) { onDone() }; return }
    val at = i.coerceIn(0, list.lastIndex)
    val step = list[at]
    val last = at >= list.lastIndex
    fun next() { if (last) onDone() else setStep(at + 1) }
    BackHandler { onDone() }
    // 스크롤 아래에 있는 자리는 화면 안으로
    LaunchedEffect(step) { step.target?.let { k -> targets.reveal.filterKeys { it == k || it.startsWith("$k.") }.values.firstOrNull()?.invoke() } }
    var origin by remember { mutableStateOf(Offset.Zero) }
    // 한 자리가 여러 조각이면 (예: 숫자 + 단위 칩) 모두 감싸는 사각형
    val hole = step.target?.let { k ->
        targets.rects.filterKeys { it == k || it.startsWith("$k.") }.values.filter { it.width > 0f && it.height > 0f }
            .reduceOrNull { x, y -> Rect(minOf(x.left, y.left), minOf(x.top, y.top), maxOf(x.right, y.right), maxOf(x.bottom, y.bottom)) }
    }?.translate(-origin)?.inflate(with(density) { Tokens.Space.sp2.toPx() })
    val a = rememberPop(step)
    val scrim = Theme.gc.scrim.copy(alpha = 0.62f)
    val ring = p.olive
    BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { origin = it.boundsInRoot().topLeft }
            .pointerInput(step) { detectTapGestures { next() } },
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
                .onSizeChanged { cardH = it.height.toFloat() }.heightIn(max = with(density) { (hPx * 0.8f).toDp() }).pop(a).modalBox(1400 + at)
                .clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
                .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(Tokens.Space.sp5).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
        ) {
            if (list.size > 1) TokenText("${at + 1} / ${list.size}", Tokens.TypeScale.caption1, color = p.secondary)
            TokenText(stringResource(step.title), Tokens.TypeScale.title3.serif())
            TokenText(stringResource(step.body), Tokens.TypeScale.callout, color = p.foreground.copy(alpha = 0.82f))
            Spacer(Modifier.height(Tokens.Space.sp1))
            when {
                last && onWrite != null -> {
                    GardenButton(stringResource(R.string.guide_write), { onDone(); onWrite() }, filled = true, seed = 1420)
                    GardenButton(stringResource(R.string.guide_look), onDone, filled = false, seed = 1421)
                }
                else -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!last) TokenText(stringResource(R.string.guide_skip), Tokens.TypeScale.subhead,
                        Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onDone).padding(end = Tokens.Space.sp4, top = Tokens.Space.sp3, bottom = Tokens.Space.sp3), color = p.secondary)
                    Spacer(Modifier.weight(1f))
                    GardenButton(stringResource(if (last) R.string.guide_ok else R.string.guide_next), { next() }, filled = true, seed = 1410 + at, modifier = Modifier.widthIn(max = 160.dp))
                }
            }
        }
    }
}

/**
 * 처음 켤 때의 첫 화면: 하늘 아래 이름 · 인사, 그리고 ‘새로 시작하기’ · ‘기록 불러오기’.
 * 예전 폰의 기록이 있는 사람은 프로필을 새로 만들지 않고 바로 파일에서 이어 쓴다 (들여오면 화면을 새로 지음).
 */
@Composable
fun WelcomeScreen(state: AppState, onStart: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var pickHelp by remember { mutableStateOf(false) }
    var restoreFrom by remember { mutableStateOf<android.net.Uri?>(null) }
    val openFile = androidx.activity.compose.rememberLauncherForActivityResult(object : androidx.activity.result.contract.ActivityResultContracts.OpenDocument() {
        override fun createIntent(context: android.content.Context, input: Array<String>) = super.createIntent(context, input).apply {
            putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"))
        }
    }) { uri -> restoreFrom = uri }
    Box(Modifier.fillMaxSize().paperBackground()) {
        Image(GardenArt.sky(ctx, Season.SPRING), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth, alignment = Alignment.TopCenter)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                    val name = stringResource(R.string.title_name)
                    if (name != "Carpe Diem") TokenText("Carpe Diem", Tokens.TypeScale.footnote.serif(), color = p.secondary)
                    TokenText(name, Tokens.TypeScale.largeTitle.serif(), align = TextAlign.Center)
                    TokenText(stringResource(R.string.title_helloFirst), Tokens.TypeScale.callout.serif(), color = p.secondary, align = TextAlign.Center)
                }
            }
            GardenButton(stringResource(R.string.welcome_start), onStart, filled = true, seed = 770)
            Spacer(Modifier.height(Tokens.Space.sp3))
            GardenButton(stringResource(R.string.welcome_restore), { pickHelp = true }, filled = false, seed = 771, paper = true)
            Spacer(Modifier.height(Tokens.Space.sp2))
            TokenText(stringResource(R.string.welcome_restoreSub), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp6))
        }
    }
    // 어떤 파일을 고르는지 한 번 알려 준 뒤 ‘다운로드’ 에서 열기
    if (pickHelp) io.github.graviton94.carpediem.ui.GardenAlert(
        onDismissRequest = { pickHelp = false },
        title = { androidx.compose.material3.Text(stringResource(R.string.backup_pickTitle)) },
        text = { androidx.compose.material3.Text(stringResource(R.string.backup_pickHelp)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { pickHelp = false; openFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { androidx.compose.material3.Text(stringResource(R.string.backup_pickGo)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { pickHelp = false }) { androidx.compose.material3.Text(stringResource(R.string.cancel)) } },
    )
    restoreFrom?.let { uri ->
        val name = remember(uri) { io.github.graviton94.carpediem.ui.fileName(ctx, uri) }
        io.github.graviton94.carpediem.ui.GardenAlert(
            onDismissRequest = { restoreFrom = null },
            title = { androidx.compose.material3.Text(stringResource(R.string.welcome_restore)) },
            text = { androidx.compose.material3.Text(listOfNotNull(name?.let { stringResource(R.string.backup_picked, it) }, stringResource(R.string.backup_notOurs).takeIf { name != null && !name.startsWith("haru") }).joinToString("\n\n")) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = {
                restoreFrom = null
                scope.launch {
                    val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)?.use { state.store.restore(it) } }.getOrNull() == true }
                    if (ok) {
                        state.say(ctx.getString(R.string.backup_done))
                        io.github.graviton94.carpediem.widget.Widgets.refresh(ctx)
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ (ctx as? android.app.Activity)?.takeIf { !it.isFinishing && !it.isDestroyed }?.recreate() }, (Tokens.Garden.Motion.noteMs / 2).toLong())
                    } else state.say(ctx.getString(R.string.backup_fail))
                }
            }) { androidx.compose.material3.Text(stringResource(R.string.backup_pickGo)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { restoreFrom = null }) { androidx.compose.material3.Text(stringResource(R.string.cancel)) } },
        )
    }
}
