package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsets
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.isActive
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.zIndex
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.GridScale
import io.github.graviton94.carpediem.core.Sound
import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.Kind
import io.github.graviton94.carpediem.core.Family
import io.github.graviton94.carpediem.core.LifePeriod
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.Moment
import io.github.graviton94.carpediem.core.Moments
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** 해(낮) · 달(밤)이 지금 하늘의 어디쯤인지 (0 = 왼쪽 끝, 1 = 오른쪽 끝). 그날 실제 해 뜨고 지는 시각 기준 (SkyTime.sunPath). */
private fun skyProgress(now: LocalDateTime): Pair<Boolean, Float> = SkyTime.sunPath(now)

/** 가운데를 x 에 두되 [min, max] 안에서 벗어나지 않게. */
private fun Modifier.centerAt(xPx: Float, minPx: Float, maxPx: Float) = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0))
    layout(constraints.maxWidth, p.height) { p.place((xPx - p.width / 2f).coerceIn(minPx, max(minPx, maxPx - p.width)).toInt(), 0) }
}

/** 하루 그림을 불러온다. loading = 아직 그리는 중, 끝났는데 art 가 없으면 대체 그림을 쓴다. */
internal class HaruLoad(val art: HaruArt?, val loading: Boolean)

@Composable
internal fun haruArt(state: AppState, sprout: Boolean): HaruLoad {
    val ctx = LocalContext.current
    val seed = state.store.haruSeed
    // 번호로 바로 그리는 벡터 하루라 기다림이 없다
    return remember(seed, sprout) { HaruLoad(HaruArt.of(seed, sprout), false) }
}

/** 하루 한 명 (쓰다듬기 · 두 번 누르기는 HaruFigure). */
@Composable
internal fun Haru(load: HaruLoad, scale: Dp, modifier: Modifier, blinkKick: Int = 0, hat: Boolean = false, onOpen: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val art = load.art ?: return
    HaruFigure(art, scale, modifier, blinkKick = blinkKick, hat = hat, a11y = stringResource(R.string.garden_haruA11y, Labels.stone(ctx, art.meta.stone)), onOpen = onOpen)
}

/** 정원에 앉는 돌 하나 (내 돌 id = null). */
internal class Slot(val id: String?, val name: String, val art: HaruArt, val scale: Dp, val progress: Double?, val soon: Int?) {
    /** 생일 전날 저녁부터 그날 끝까지 모자. */
    val birthday get() = soon != null
}

/** 나와 가족의 돌 (나부터). 반려동물은 petScale 만큼 작게. */
@Composable
internal fun gardenSlots(state: AppState, profile: LifeProfile, s: LifeSnapshot, now: LocalDateTime, base: Dp): List<Slot> {
    val today = now.toLocalDate()
    val sprout = s.season == Season.SPRING
    val from = Tokens.Notify.birthdayFrom.toInt()
    val me = Slot(null, stringResource(R.string.family_me), HaruArt.of(state.store.haruSeed, sprout), base, s.progress, Family.birthdaySoon(profile.birthDate, now, from))
    return listOf(me) + state.people.map { p ->
        val prog = p.birth?.let { LifeSnapshot(it, state.store.expectancy(p), now).progress }
        Slot(p.id, p.name, HaruArt.of(p.seed, sprout), if (p.kind == Kind.PET) base * G.Family.petScale else base, prog, Family.birthdaySoon(p.birth, now, from))
    }
}

/**
 * 페이지 넘김. o = 이 장이 넘어간 정도 (0 = 펼쳐짐, 1 = 왼쪽으로 다 넘어감, -1 = 아직 오른쪽).
 * 손으로 넘길 때 = 내려앉는 종이: 새 장이 살짝 기운 채 들어와 바르게 내려앉고, 아래 장은 제자리에서 조금 그늘짐.
 * 이름표 · 버튼으로 옮길 때 (breath) = 숨 한 번: 지금 장이 조금 흐르며 옅어지고, 새 장이 반대편에서 스며듦.
 */
@Composable
private fun Modifier.pageTurn(pager: androidx.compose.foundation.pager.PagerState, page: Int, breath: () -> Boolean): Modifier {
    val shade = Theme.gc.scrim
    val d = LocalDensity.current.density
    val M = G.Motion
    return paperBackground().graphicsLayer {
        val o = (pager.currentPage - page) + pager.currentPageOffsetFraction
        val a = kotlin.math.abs(o)
        if (breath()) {
            // 두 장 모두 제자리에서: 옅어지는 쪽이 먼저, 사이에 빈 종이 한 박자
            val t = (a * 1.8f).coerceIn(0f, 1f); val out = t * t * (3 - 2 * t)
            translationX = o * size.width - o * M.breathDrift * d
            alpha = 1f - out
            val k = 1f - 0.015f * a; scaleX = k; scaleY = k
        } else if (o < 0f) {
            // 들어오는 장: 자리는 pager 가 옮겨 주고, 왼쪽 아래를 축으로 살짝 기울었다 내려앉음
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 1f)
            rotationZ = M.turnTilt * a
            translationY = -M.turnLift * d * kotlin.math.sin(a * Math.PI.toFloat())
        } else if (o > 0f) {
            // 아래 장: 제자리에 머물며 아주 조금 작아짐
            translationX = o * size.width
            val k = 1f - 0.02f * o; scaleX = k; scaleY = k
        }
    }.drawWithContent {
        drawContent()
        if (breath()) return@drawWithContent
        val o = (pager.currentPage - page) + pager.currentPageOffsetFraction
        if (o > 0f) drawRect(shade.copy(alpha = shade.alpha * (o * M.turnShade).coerceIn(0f, 1f)))
        else if (o < 0f) {
            // 새 장 왼쪽 가장자리의 옅은 그림자 한 줄
            val e = M.turnEdge * d
            drawRect(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, shade.copy(alpha = shade.alpha * 0.8f * (1f + o * 0.2f))), -e, 0f),
                androidx.compose.ui.geometry.Offset(-e, 0f), androidx.compose.ui.geometry.Size(e, size.height))
        }
    }
}

/** 이 시각부터 정원 위쪽에 ‘하루 닫기’ (밤 nightFrom 전이라도). */
private const val CLOSE_DAY_FROM = 19

/** 정원 아래 작은 한 줄: 깃털과 함께 ‘돌아온 한 줄’을 알림. */
@Composable
private fun RecallNote(text: String, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.button, G.Stroke.chip, 1190).clickable(onClick = onOpen)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp2),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
    ) {
        Image(GardenArt.obj(LocalContext.current, "feather"), null, Modifier.size(Theme.unit * G.LetGo.feather))
        TokenText(text, Tokens.TypeScale.footnote, Modifier.weight(1f), weight = FontWeight.Medium, maxLines = 2)
    }
}

/** 이름표: nameChars 글자까지, 넘으면 말줄임. */
internal fun shortName(n: String): String { val max = G.Family.nameChars.toInt(); return if (n.codePointCount(0, n.length) <= max) n else n.substring(0, n.offsetByCodePoints(0, max)) + "…" }

// ───────────────────────── 홈 = 정원 ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GardenHome(state: AppState, profile: LifeProfile, now: LocalDateTime, onSettings: () -> Unit, onCollection: () -> Unit, onSupport: () -> Unit, onStone: (String?) -> Unit, onAddPerson: () -> Unit,
               onBreath: (BreathKind, Int, Sound) -> Unit = { _, _, _ -> }, onGaze: () -> Unit = {}, bare: Boolean = false, onLook: () -> Unit = {}, onMemory: () -> Unit = {},
               onCloseDay: () -> Unit = {}, onCredits: (Int) -> Unit = {}) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val season = s.season
    val load = haruArt(state, season == Season.SPRING)
    val art = load.art
    // 정원의 자리 여섯 (core GardenDecor): 하늘 · 땅 · 나무 옷은 실제 계절, 나무 종류는 인생의 계절
    val day0 = now.toLocalDate()
    val decor = state.decor(profile, s, day0)
    val real = decor.season
    var decorOpen by remember { mutableStateOf<DecorPart?>(null) }
    var breathSheet by remember { mutableStateOf(false) }
    var askBreath by remember { mutableStateOf<Pair<BreathKind, Int>?>(null) }
    val sleepy = !bare && isNight(now)
    // 캡처용: 이번 달 편지를 바로 펼침. 고르기만 그리기 중에, ‘연 편지’로 남기기는 그 뒤에
    var gardenYearOpen by remember { mutableStateOf<Int?>(null) }
    var capsuleOpen by remember { mutableStateOf<io.github.graviton94.carpediem.core.Capsule?>(null) }
    var ringOpen by remember { mutableStateOf<Int?>(null) }
    var seedOpen by remember { mutableStateOf(false) }
    // 오늘 펼쳐 본 나이테 (같은 날 아래 한 줄로 다시 권하지 않음)
    var ringsSeen by remember { mutableStateOf(emptySet<Int>()) }
    var letterOpen by remember { mutableStateOf(if (state.debugOpenLetter && !bare) state.letterDue(now.toLocalDate()) else null) }
    LaunchedEffect(letterOpen) { letterOpen?.let { state.openLetter(it.id) } }

    // 네 장: 정원 · 기록 (오늘의 한 줄) · 추억 (마음의 기록 · 모은 것) · 흐름 (흐르는 시간 · 인생 달력 · 특별한 날). 옆으로 넘기거나 아래 이름표로.
    // bare = 돌멍하기: 정원 한 장, 글자 · 이름표 · 아래 버튼 없이
    val pager = rememberPagerState(initialPage = if (bare) 0 else state.homePage.coerceIn(0, 3)) { if (bare) 1 else 4 }
    if (!bare) LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { state.homePage = it } }
    // 다른 날의 한 줄을 고르면 (마음의 기록에서도) 기록 페이지로
    if (!bare) LaunchedEffect(state.writeDay) { if (state.writeDay != null && pager.currentPage != 1) pager.animateScrollToPage(1) }
    // 정원 페이지가 보일 때만: 이번 계절의 한 장을 받고, 새로 생긴 것이 있으면 한 줄 (같은 것은 한 번만). 다른 페이지에 있다 돌아오면 그때
    val onGarden = bare || pager.currentPage == 0
    if (!bare && onGarden) LaunchedEffect(decor.stage, decor.tree, decor.hang, decor.kite, decor.ribbons.size, decor.buds, decor.card.id, decor.letter) { state.noticeDecor(decor) }
    // 정원을 열 때의 우연한 순간 (달팽이 · 비눗방울 · 나비, 각각 한 번씩)
    if (!bare && onGarden) LaunchedEffect(day0) { state.openChance(day0, real) }
    // 절기가 든 날 (S1): 처음 열 때 한 줄, 정원엔 그날 하루 작은 변화
    val termToday = remember(day0, state.profile?.countryCode) { state.termToday(day0) }
    if (!bare && onGarden) LaunchedEffect(day0) { state.noticeTerm(day0) }
    // 오늘 마친 숨의 흔적 (E3)
    val trace = remember(day0, state.breaths) { state.breathTrace(day0) }
    // 돌에게 건넨 이번 계절의 조각 (R1): 사람 id → 조각
    val offered = remember(state.offerings, decor.card) { io.github.graviton94.carpediem.core.Offerings.shown(state.offerings, decor.card) }
    val scope = rememberCoroutineScope()
    var recordView by remember { mutableStateOf(state.pendingRecord?.also { state.pendingRecord = null } ?: if (state.debugOpenYear) RecordView(state.yearDue(now.toLocalDate()) ?: now.year, null) else RecordView(now.year, now.monthValue)) }
    // 정원이 아닌 페이지에서 뒤로 가기: 앱을 닫지 않고 정원으로
    // 이름표 · 버튼으로 옮기는 동안은 ‘숨 한 번’, 손으로 넘길 땐 ‘내려앉는 종이’
    var breath by remember { mutableStateOf(false) }
    fun turnTo(i: Int) { scope.launch { breath = true; try { pager.animateScrollToPage(i, animationSpec = androidx.compose.animation.core.tween(G.Motion.pageMs.toInt())) } finally { breath = false } } }
    BackHandler(enabled = !bare && pager.currentPage != 0) { turnTo(0) }
    // 글을 쓰는 동안 (키보드가 떠 있으면) 옆으로 넘어가지 않고, 이름표도 쉬게
    val typing = WindowInsets.isImeVisible && pager.currentPage == 1   // 기록 페이지에서 쓰는 중일 때만
    fun toRecord(v: RecordView) { recordView = v; turnTo(1) }
    // 처음 온 사람의 둘러보기: 비출 자리들 (bare = 돌멍하기에는 없음)
    val guide = remember { GuideTargets() }
    val touring = !bare && !state.guideDone
    // 걱정한 밤 다음 아침의 한마디 (06): 오늘 처음 정원을 열 때 한 번 (보여 준 날을 바로 적어 둠)
    var comfort by remember { mutableStateOf<String?>(null) }
    var greet by remember { mutableStateOf(false) }
    if (!bare) LaunchedEffect(day0, state.guideDone, state.guest) {
        if (state.guideDone && state.greetingDue(day0)) { greet = true; state.greeted(day0) }
        if (state.guideDone && state.comfortDue(day0)) { comfort = dayLine(ctx, "comfort_", day0); state.comfortSeen(day0) }
    }

    Box(Modifier.fillMaxSize().paperBackground()) {
      Column(Modifier.fillMaxSize()) {
        // 보이지 않는 페이지는 그리지 않는다 (반짝임 · 살랑임이 화면 밖에서 돌지 않게). 쓰던 글은 rememberSaveable 로 남음
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 0, userScrollEnabled = !bare && !typing && !state.touring,
            // 조금만 밀어도 넘어가게 (기본은 반 장)
            flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(pager, snapPositionalThreshold = G.Motion.turnSnap)) { page ->
          // 손으로 넘길 땐 들어오는 장 (아직 오른쪽) 이 위에
          val incoming by remember(page) { derivedStateOf { (pager.currentPage - page) + pager.currentPageOffsetFraction < 0f } }
          Box(Modifier.fillMaxSize().zIndex(if (incoming) 1f else 0f).pageTurn(pager, page) { breath }) {
          when (page) {
            1 -> WritePage(state, now, recordView, guide, onSettings) { recordView = it }
            2 -> MemoriesPage(state, profile, now, guide, onMemory, onCredits)
            3 -> FlowPage(state, profile, now, guide)
            else -> BoxWithConstraints(Modifier.fillMaxSize()) {
        val u = Theme.unit
        val screenH = maxHeight
        val screenW = maxWidth
        val margin = Theme.deviceClass.pageMargin
        var topBottom by remember { mutableStateOf(0.dp) }
        var blockH by remember { mutableStateOf(0.dp) }
            Box(Modifier.fillMaxSize().clipToBounds()) {
                // 아래: 지나온 길 (땅 한 줄) 위에 나와 가족의 돌 · 놓인 것. 자리를 먼저 정해 하늘빛 · 별 · 해가 쓰게 한다.
                // 돌이 많아 길에 다 앉지 못하면 모두 같은 비율로 조금씩 작게 (나 포함 9개까지)
                val haruBase = u * (G.Layout.haruWidth / G.Layout.haruArtWidth)
                val slots0 = gardenSlots(state, profile, s, now, haruBase)
                // 길이 모자라면 먼저 살짝 겹쳐 앉고 (돌 폭의 overlap 까지), 그래도 모자랄 때만 줄임
                val w0 = slots0.map { (it.art.meta.bbox.width * it.scale.value).toDouble() }
                // 줄일 때와 앉힐 때 같은 간격을 써야 길 밖으로 밀려나지 않음 (줄이기 전 폭으로 정한 겹침)
                val og0 = Family.overlapGap(w0, G.Family.overlap.toDouble())
                val fit = Family.fitScale(w0, ((G.Layout.pathEnd - G.Layout.pathStart) * u.value).toDouble(), og0).toFloat()
                val slots = if (fit < 1f) gardenSlots(state, profile, s, now, haruBase * fit) else slots0
                val headroom = slots.maxOf { sl -> sl.scale * (sl.art.meta.ground - sl.art.meta.bbox.top + if (sl.birthday) Tokens.Garden.Party.hatHeight else if (sl.art.sprout) Tokens.Garden.HaruDraw.sproutHeight else 0f) }
                val haruAbove = headroom
                val family = slots.size > 1
                // 이름표 두 줄 + 나이 한 줄 + 아래 버튼 · 카드와의 틈
                val labels = u * (G.Layout.labelGap + G.Layout.labelRow * 4)
                val gy = maxOf(screenH * G.Layout.groundRatio, topBottom + u * G.Layout.minSkyGap + haruAbove).coerceAtMost(screenH - labels - blockH)

                Image(GardenArt.sky(ctx, real), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
                // 땅 그림도 시간의 빛 아래에 (밤이면 땅까지 어두워짐)
                // 하늘의 우연한 순간 (무지개 · 오로라): 먼 산 뒤
                if (!bare) state.chance?.let { c -> ChanceLayer(c, now, real, gy, 0.dp, u * G.Decor.treeX, u * G.Layout.pathEnd, topBottom + u * G.Layout.minSkyGap, back = true) { seen -> state.chanceDone(seen, c) } }
                Image(GardenArt.strip(ctx, real), null, Modifier.offset(y = gy - u * G.Layout.stripLineY).fillMaxWidth().guideTarget(guide.takeIf { !bare }, "path.ground").height(u * G.Layout.stripHeight), contentScale = ContentScale.FillBounds)
                // 정원만 보기(bare)는 위 글자가 없어도 별이 상태바 · 소리 버튼에 닿지 않게
                SkyTimeLayer(now, gy, if (bare) screenH * 0.14f else topBottom, gy - haruAbove - u * G.Layout.minSkyGap, Modifier.fillMaxSize())
                // 기억의 돌 가운데 ‘하늘에 별로 두기’를 켠 것: 하늘에 따뜻한 별 하나 (돌멍하기에는 두지 않음)
                if (!bare) {
                    // 위 글자 아래 ~ 돌 머리 위. 글자 높이를 아직 모르거나 띠가 없으면 하늘 위쪽의 작은 띠에
                    val starTop = if (topBottom > 0.dp) topBottom + u * G.Layout.minSkyGap else screenH * 0.14f
                    val starBottom = minOf(gy - haruAbove - u * G.Layout.minSkyGap * 2, gy - u * G.Layout.hillTop).let { if (it > starTop + u * 12) it else starTop + u * 28 }
                    MemoryStars(state, screenW, starTop, starBottom, Theme.gc.night, day0)
                }

                // 위: 남은 시간 · 단위 · 오늘의 문장
                if (!bare) Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = margin).onGloballyPositioned { c -> topBottom = with(density) { (c.boundsInParent().bottom).toDp() } },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
                ) {
                    // 늘 보이는 이름과 한 줄 소개 (첫 화면과 같은 말)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            TokenText("Carpe Diem", Tokens.TypeScale.headline.serif())
                            TokenText(stringResource(R.string.tagline), Tokens.TypeScale.caption1.serif(), color = p.secondary)
                        }
                        IconButton(onClick = onSettings, modifier = Modifier.guideTarget(guide, "settings").semantics { contentDescription = ctx.getString(R.string.settings) }) { Icon(Icons.Filled.Settings, null, tint = p.secondary) }
                    }
                    // 윗줄 첫 말이 단위 (남은 날 · 주 · 달 · 해): 밤에 단위 버튼이 쉬어도 숫자가 무엇인지 알 수 있게
                    val leftName = stringResource(when (state.unit) { LifeUnit.DAYS -> R.string.timeLeft_days; LifeUnit.WEEKS -> R.string.timeLeft_weeks; LifeUnit.MONTHS -> R.string.timeLeft_months; LifeUnit.YEARS -> R.string.timeLeft_years })
                    TokenText(leftName + " · " + stringResource(R.string.path_age, "${s.age}", Labels.season(ctx, season)), Tokens.TypeScale.subhead, color = p.secondary, align = TextAlign.Center)
                    // 큰 글씨 설정에서도 한 줄: 폭에 맞춰 숫자가 작아짐
                    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().guideTarget(guide, "number"), contentAlignment = Alignment.Center) {
                        val num = Labels.number(s.remaining(state.unit)); val big = Tokens.TypeScale.display(Theme.deviceClass)
                        val fs = density.fontScale; val fit = (maxWidth.value / (num.length * 0.62f * big.size.value * fs)).coerceAtMost(1f)
                        TokenText(num, if (fit < 1f) big.copy(size = (big.size.value * fit).sp) else big, maxLines = 1)
                    }
                    // 잠들기 전 정원 (밤 10시 이후): 단위 고르기 · 영문 · 넘김 안내 같은 글자는 쉬게. 글씨가 크면 두 줄로
                    if (!sleepy) androidx.compose.foundation.layout.FlowRow(Modifier.guideTarget(guide, "number.units"), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        LifeUnit.entries.forEachIndexed { i, unit -> GardenChip(Labels.unit(ctx, unit), unit == state.unit, seed = 800 + i) { state.changeUnit(unit) } }
                    }
                    val question = state.question
                    // 오늘의 질문은 답하기 전까지만: 오늘 한 줄을 남겼으면 (답했든 아니든) 다시 오늘의 문장으로
                    if (question != null && !state.sentOn(now.toLocalDate())) Box(Modifier.guideTarget(guide, "words")) { QuestionBlock(state, question, sent = false) {
                        // 한 줄로 답하기: 기록 페이지의 오늘의 한 줄로
                        state.answer(); turnTo(1)
                    } } else state.quote?.let { q ->
                        Column(
                            Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4).guideTarget(guide, "words").clickable { state.nextQuote() },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
                        ) {
                            // 새 문장 (누르거나 날이 바뀌어) 은 옛 타자기처럼 한 글자씩. 자리는 처음부터 다 잡아 두어 줄이 흔들리지 않음
                            val phone = io.github.graviton94.carpediem.data.Words.lang(ctx)
                            val main = io.github.graviton94.carpediem.data.Words.main(q, state.quoteLanguage, phone)
                            val second = if (!sleepy) io.github.graviton94.carpediem.data.Words.second(q, state.quoteLanguage, phone) else null
                            val typed = rememberTyping(state, main, second)
                            QuoteText(main, typed)
                            if (second != null) TypedText(second, typed - main.length, Tokens.TypeScale.footnote.serif(), p.secondary, Modifier.fillMaxWidth())
                            if (!sleepy) TokenText(stringResource(R.string.words_next), Tokens.TypeScale.caption1, color = p.secondary, weight = FontWeight.Normal)
                        }
                    }
                    // 걱정한 밤 다음 아침 (06): 하루의 한마디만 (묻지 않음, 누르거나 조금 지나면 사라짐)
                    // 돌아온 날 (09): 빠진 날 대신 손님 이야기 (한마디보다 먼저)
                    if (greet) state.guest?.let { g -> GreetingCard(g) { greet = false } }
                    else comfort?.let { c -> ComfortWords(c) { comfort = null } }
                    // 저녁 7시 이후 · 밤: 하루 닫기 (한 줄 → 고마움 → 등불) · 아침: 씨앗 하나 (04), 심었거나 넘겼으면 하루를 여는 숨 1분
                    // 숨은 누르면 바로 가지 않고 “… 하러 갈까요?” 한 번 묻기
                    if (isNight(now) || now.hour >= CLOSE_DAY_FROM) CloseDayEntry(state.sentOn(day0), onCloseDay)
                    else if (Labels.part(now) == io.github.graviton94.carpediem.core.DayPart.MORNING && state.breaths.none { it.first == now.toLocalDate() })
                        TokenText(stringResource(R.string.breath_morning), Tokens.TypeScale.footnote.serif(),
                            Modifier.clickable { askBreath = BreathKind.CALM to R.string.breath_morning }.padding(Tokens.Space.sp2), color = p.secondary)
                    askBreath?.let { (kind, name) ->
                        io.github.graviton94.carpediem.ui.GardenAlert(
                            onDismissRequest = { askBreath = null },
                            title = { Text(stringResource(R.string.breath_ask, stringResource(name))) },
                            text = { Text(stringResource(R.string.breath_askHelp)) },
                            confirmButton = { androidx.compose.material3.TextButton(onClick = { askBreath = null; onBreath(kind, 1, state.sound) }) { Text(stringResource(R.string.breath_askGo)) } },
                            dismissButton = { androidx.compose.material3.TextButton(onClick = { askBreath = null }) { Text(stringResource(R.string.breath_askStay), color = p.secondary) } },
                        )
                    }
                }

                val x0 = u * G.Layout.pathStart; val x1 = u * G.Layout.pathEnd
                // 돌 자리: 각자 인생의 길 위 원래 자리, 겹치면 옆으로 비켜 앉음 (core Family.place)
                val targets = slots.map { sl -> sl.progress?.let { lerp(G.Layout.pathStart + G.Layout.pathInset, G.Layout.pathEnd - G.Layout.pathInset, it.toFloat().coerceIn(0f, 1f)).toDouble() * u.value } }
                val widths = slots.map { sl -> (sl.art.meta.bbox.width * sl.scale.value).toDouble() }
                val xs = Family.place(targets, widths, 0, x0.value.toDouble(), x1.value.toDouble(), (G.Family.gap * u.value).toDouble(), og0).map { it.toFloat().dp }

                // 해 · 달: 폰 시각을 따라 하늘을 가로지름. 위쪽 글자에도, 하루 머리에도 닿지 않음.
                val (day, t) = skyProgress(now)
                val r = u * G.Layout.sunRadius
                val base = gy - maxOf(u * G.Layout.sunBase, haruAbove + u * G.Layout.minSkyGap + r)
                val arc = min((u * G.Layout.sunArc).value, (base - topBottom - u * G.Layout.minSkyGap - r).value.coerceAtLeast(0f)).dp
                val sx = lerp((u * G.Layout.sunStart).value, (u * G.Layout.sunEnd).value, t).dp
                val sy = base - arc * sin(t * Math.PI).toFloat()
                if (day) Image(GardenArt.sun(ctx), null, Modifier.offset(sx - r, sy - r).size(r * 2))
                else MoonShape(GardenArt.moonFull(ctx), SkyTime.moonPhase(now), Modifier.offset(sx - r, sy - r).size(r * 2))
                // 돌멍하기: 움직이는 정원 (GazeLife) 이 같은 자리를 쓰게
                if (bare) androidx.compose.runtime.SideEffect { gazeGeom.value = GazeGeom(gy, xs[0], sx, sy, day, haruAbove) }
                // 밤 · 새벽: 달빛 · 돌들 발치의 빛 · 가로등 · 반딧불
                val spanL = slots.indices.minOf { xs[it] - (widths[it].toFloat() / 2).dp }; val spanR = slots.indices.maxOf { xs[it] + (widths[it].toFloat() / 2).dp }
                NightLights(now, gy, spanL, spanR, if (day) null else androidx.compose.ui.unit.DpOffset(sx, sy), Modifier.fillMaxSize())
                // 가끔 별똥별 (밤 · 새벽, 글자와 하루 머리 사이 하늘)
                ShootingStars(now, topBottom, gy - haruAbove - u * G.Layout.minSkyGap, Modifier.fillMaxSize())
                // 절기가 든 날의 작은 변화 (S1) · 오늘 마친 숨의 흔적 (E3): 그날만, 돌 · 꾸밈 뒤에
                termToday?.let { tt -> TermTouches(tt.touch, now, gy, topBottom, gy - haruAbove - u * G.Layout.minSkyGap, with(androidx.compose.ui.platform.LocalDensity.current) { Offset(sx.toPx(), sy.toPx()) }, day, Modifier.fillMaxSize()) }
                BreathTraces(trace, now, gy, xs[0], (widths[0].toFloat() / 2).dp, Modifier.fillMaxSize())
                // 오늘 심은 아침 씨앗 (04): 하루 왼쪽 발치에 새싹 (저녁에 텄다고 하면 작은 꽃)
                if (!bare) SeedSprout(state, day0, xs[0] - (widths[0].toFloat() / 2).dp - u * 3f, gy)
                // 나무 밑에 묻은 항아리 (10): 열리는 날까지 작은 흙더미
                if (!bare) JarMound(state, day0, u * G.Decor.treeX + u * 14f, gy)
                // 돌아온 날의 손님 (09): 그날 하루 말뚝 곁에


                // 자리 여섯: 나무 (길의 시작) · 발치의 한 장 · 말뚝 (길의 끝) · 연 (하늘) — 돌들 뒤에. 하루 밑엔 이끼 방석.
                // 자리는 화면 폭 · 땅 · 길의 양 끝에 붙어 있어, 돌이 어디 앉든 가족이 몇이든 움직이지 않는다.
                // 그림은 미리 (화면 스레드 밖에서) 읽어 두고, 다 읽은 뒤에 그림 (한꺼번에 읽으면 멈춘 듯 보임)
                val decorNames = remember(decor) { GardenArt.decorNames(decor) }
                var decorReady by remember(decorNames) { mutableStateOf(GardenArt.loaded(decorNames)) }
                LaunchedEffect(decorNames) { if (!decorReady) { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { decorNames.forEach { GardenArt.opaque(GardenArt.image(ctx, it)) } }; decorReady = true } }   // 누르는 자리 (그림 범위) 도 화면 스레드 밖에서
                if (decorReady) {
                    DecorBack(decor, now, gy, x0, x1, topBottom + u * G.Layout.minSkyGap, gy - haruAbove - u * G.Layout.minSkyGap,
                        if (bare) null else { part -> if (part == DecorPart.LETTER) state.letterDue(day0)?.let { l -> state.openLetter(l.id); letterOpen = l } else decorOpen = part }, warm = BreathKind.SLEEP in trace)
                    MossSeat(decor, now, xs[0], widths[0].toFloat().dp, gy, if (bare) null else { part -> decorOpen = part })
                }

                // 돌들: 한 번 누르면 쓰다듬기, 두 번 누르면 그 돌의 페이지
                val todayLine = state.lines.lastOrNull { it.date == now.toLocalDate() }
                val sentTo = todayLine?.to
                // 무거운 마음을 보낸 날, 내 하루는 살짝 아래를 본다 (다음 날 평소대로)
                val heavyToday = todayLine?.feeling in io.github.graviton94.carpediem.core.Letters.HEAVY
                // 노래하는 돌: 하루를 길게 누르면 한 음, 가족 돌들이 왼쪽부터 차례로 저마다의 음으로 대답 (빛 동그라미 + 살짝 뜀)
                val S = G.Song
                val singOrder = listOf(0) + slots.indices.drop(1).sortedBy { xs[it].value }
                fun startAt(i: Int): Float { val k = singOrder.indexOf(i); return if (k <= 0) 0f else S.firstMs + (k - 1) * S.gapMs }
                fun slotSeed(i: Int): Long = slots[i].id?.let { id -> state.people.firstOrNull { it.id == id }?.seed } ?: state.store.haruSeed
                val song = remember { androidx.compose.animation.core.Animatable(-1f) }   // 노래가 시작된 뒤 지난 ms (-1 = 쉼)
                fun sing() {
                    val total = startAt(singOrder.last()) + S.ringMs
                    if (state.sound != Sound.NONE) io.github.graviton94.carpediem.sound.StoneSong.play(singOrder.map { i -> io.github.graviton94.carpediem.sound.StoneSong.pitch(slotSeed(i), i == 0) to startAt(i).toLong() })
                    scope.launch { song.snapTo(0f); song.animateTo(total, androidx.compose.animation.core.tween(total.toInt(), easing = androidx.compose.animation.core.LinearEasing)); song.snapTo(-1f) }
                }
                val hopPx = with(density) { (u * S.hop).toPx() }
                val gazeClock = rememberGardenClock(bare && remember { !reducedMotion(ctx) })
                // 왼쪽부터 그려, 겹쳐 앉으면 오른쪽 돌이 앞 (누름도 앞 돌이 먼저 받음)
                slots.indices.sortedBy { xs[it].value }.forEach { i -> val sl = slots[i]; androidx.compose.runtime.key(sl.id ?: "me") {
                    val cx = xs[i] - sl.scale * (sl.art.meta.bbox.center.x - sl.art.meta.box / 2)
                    val left = cx - sl.scale * (sl.art.meta.box / 2); val top = gy - sl.scale * G.Layout.haruGround
                    HaruFigure(sl.art, sl.scale, Modifier.offset(left, top).guideTarget(guide.takeIf { sl.id == null && !bare }, "haru").graphicsLayer {
                        val a = (song.value - startAt(i)) / 320f
                        translationY = if (song.value >= 0f && a in 0f..1f) -hopPx * sin(a * Math.PI).toFloat() else 0f
                        // 돌멍하기: 돌마다 조금씩 다른 박자로 숨 쉼 (바닥을 축으로 1.5%)
                        if (bare) { val b = gazeBreath(gazeClock.value * (if (i == 0) 1f else 0.93f + 0.05f * (i % 3)) + i * 1.7f)
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, sl.art.meta.ground / sl.art.meta.box); scaleY = 1f + Tokens.Garden.Gaze.swell * b; scaleX = 1f + Tokens.Garden.Gaze.swell * 0.4f * b }
                    }, blinkKick = if (sl.id == null) state.blinkKick else 0, hat = sl.birthday, tiltOn = sl.id == null, lookDown = if (sl.id == null && heavyToday) G.Care.lookDown else 0f,
                        a11y = stringResource(R.string.garden_stoneA11y, sl.name, Labels.stone(ctx, sl.art.meta.stone)), onOpen = { onStone(sl.id) },
                        onLongPress = if (sl.id == null && !bare) ({ sing() }) else null)
                    // 생일 당일: 돌 앞에 작은 케이크 (전날 저녁엔 모자만)
                    if (sl.soon == 0) {
                        val cw = u * Tokens.Garden.Party.cakeWidth
                        androidx.compose.foundation.Canvas(Modifier.offset(xs[i] + u * widths[i].toFloat() / u.value * 0.18f - cw / 2, gy - cw + u * 1.5f).size(cw)) { birthdayCake() }
                    }
                    // 오늘 이 돌에게 한 줄을 보냈으면 곁에 깃털이 하루 동안 머묾
                    if (sentTo != null && sentTo == sl.id) {
                        val fw = u * G.Family.feather
                        Image(GardenArt.obj(ctx, "feather"), null, Modifier.offset(xs[i] + u * widths[i].toFloat() / u.value / 2 - fw * 0.3f, gy - sl.scale * (sl.art.meta.ground - sl.art.meta.bbox.top) - fw * 0.4f).size(fw))
                    }
                    // 이번 계절에 건넨 조각 (R1): 그 돌 왼쪽 발치에 계절이 끝날 때까지. 누르면 ‘가을에 엄마에게 놓은 감’
                    sl.id?.let { pid -> offered[pid] }?.let { o ->
                        val ow = u * G.Family.offerSize
                        val label = stringResource(R.string.offer_label, Labels.season(ctx, o.card.season), sl.name, cardName(ctx, o.card.key))
                        Image(GardenArt.card(ctx, o.card.key), label, Modifier.offset(xs[i] - u * widths[i].toFloat() / u.value / 2 - ow * 0.45f, gy - ow * 0.9f).size(ow)
                            .clickable(enabled = !bare) { state.say(label) }, colorFilter = nightFilter(SkyTime.isDark(now)))
                    }
                } }

                androidx.compose.foundation.Canvas(Modifier.fillMaxSize().graphicsLayer()) {
                    val e = song.value; if (e < 0f) return@Canvas
                    val ring = S.ringSize * u.toPx()
                    slots.indices.forEach { i ->
                        val a = (e - startAt(i)) / S.ringMs; if (a !in 0f..1f) return@forEach
                        val sl = slots[i]
                        val c = Offset(xs[i].toPx(), (gy - sl.scale * (sl.art.meta.ground - sl.art.meta.bbox.center.y)).toPx())
                        drawCircle(Tokens.Garden.Night.Colors.firefly.copy(alpha = (1f - a) * 0.3f), ring * 0.5f * (0.3f + a), c)
                        drawCircle(Tokens.Garden.Colors.now.copy(alpha = (1f - a) * 0.8f), ring * (0.25f + 0.75f * a), c, style = androidx.compose.ui.graphics.drawscope.Stroke(u.toPx() * 1.2f))
                    }
                }
                // 달팽이 손님: 오랜만에 돌아온 날, 한 시간쯤 돌들 앞 길을 천천히 건넘
                if (!bare) SnailGuest(state.store.snailAt, now, gy, u * G.Decor.treeX)
                // 돌아온 날의 손님 (09): 그날 하루 말뚝 발치에 (돌들 앞)
                if (!bare) state.guest?.let { g -> GuestFigure(g, x1 - u * 6f, gy + u * 3f) }
                // 우연한 순간 (한 번에 하나, 몇 초 뒤 사라짐)
                if (!bare) state.chance?.let { c -> ChanceLayer(c, now, real, gy, xs[0], u * G.Decor.treeX, x1, topBottom + u * G.Layout.minSkyGap, back = false) { seen -> state.chanceDone(seen, c) } }
                // 이름표 (가족이 있을 때) · 0세 · 기대수명
                val px = with(density) { Pair(x0.toPx(), x1.toPx()) }
                // 돌이 oneRow 명보다 많으면 이름표를 두 줄로 번갈아 (서로 겹치지 않게)
                val labelRows = if (slots.size > G.Family.oneRow.toInt()) 2 else 1
                // 화면 왼쪽부터의 순서로 번갈아 (목록 순서가 아니라 실제로 옆에 앉은 돌끼리 다른 줄)
                val rank = xs.indices.sortedBy { xs[it].value }.withIndex().associate { it.value to it.index }
                if (family && !bare) Box(Modifier.offset(y = gy + u * G.Layout.labelGap).fillMaxWidth()) {
                    slots.forEachIndexed { i, sl -> TokenText(shortName(sl.name), Tokens.TypeScale.caption1, Modifier.offset(y = u * G.Layout.labelRow * ((rank[i] ?: i) % labelRows)).centerAt(with(density) { xs[i].toPx() }, 0f, with(density) { screenW.toPx() }), weight = FontWeight.Medium, maxLines = 1) }
                }
                if (!bare) Box(Modifier.offset(y = gy + u * (G.Layout.labelGap + if (family) G.Layout.labelRow * labelRows else 0f)).fillMaxWidth().guideTarget(guide, "path")
                    .semantics(mergeDescendants = true) { contentDescription = ctx.getString(R.string.garden_pathA11y, "${s.age}", Labels.years(s.expectancy)) }) {
                    TokenText(stringResource(R.string.garden_age0), Tokens.TypeScale.caption1, Modifier.centerAt(px.first, 0f, px.second), color = p.secondary)
                    TokenText(stringResource(R.string.expectancy_value, Labels.years(s.expectancy)), Tokens.TypeScale.caption1, Modifier.centerAt(px.second, px.first, with(density) { screenW.toPx() }), color = p.secondary)
                }
                // 잠들기 전 정원: 화면 전체를 조금 더 어둡게
                if (sleepy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = G.SleepGarden.dim)))
                // 마음의 날씨: 오늘 보낸 마음이 하늘에 잠깐 (무거운 마음 = 몇 방울 비, 기쁨 · 희망 · 고마움 = 햇살 한 줄기)
                if (!bare && state.chance == null) MoodWeather(state, now, gy - haruAbove)   // 한 번에 하나만
                // 아래, 엄지가 닿는 곳: 생일 한 줄 · 도착한 것 한 장 · 정원에서 하는 일 (숨, 쉼 · 돌멍하기 · 돌 더하기)
                if (!bare) Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = margin).padding(bottom = Tokens.Space.sp3)
                        .onGloballyPositioned { c -> blockH = with(density) { c.size.height.toDp() } },
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
                ) {
                    // 버튼 위에는 하나만 (정원을 가리지 않게): 생일 한 줄 → 지난 해 · 지난 달의 정원 → 돌아온 한 줄 차례로
                    val bday = slots.firstOrNull { it.soon != null }
                    val today = now.toLocalDate()
                    // 계절의 편지는 말뚝에 꽂힌 봉투로 (누르면 펼침). 여기엔 지난 해 · 지난 달의 정원만
                    val year = if (bday == null) state.yearDue(today) else null; val month = if (bday == null && year == null) state.monthDue(today) else null
                    val gardenYear = if (bday == null && year == null && month == null) state.gardenYearDue(today) else null
                    // 돌아온 한 줄 (몇 해 전 오늘 · 문득): 정원에서도 알 수 있게, 누르면 기록 페이지에서 펼쳐 봄
                    val recall = remember(state.lines, today, state.randomLine) {
                        io.github.graviton94.carpediem.core.Lines.yearsAgo(state.lines, today).firstOrNull()?.first?.let { ctx.getString(R.string.recall_notify, "$it") }
                            ?: state.randomLine?.let { ctx.getString(R.string.recall_randomNotify) }
                    }
                    // 열린 항아리 (10) · 생일 아침의 나이테 (07): 생일 한 줄보다 먼저 (생일이면 같은 아침에 열림)
                    val capsule = state.capsuleDue(today)
                    val creditsYear = state.creditsDue(today)
                    val ringNew = remember(state.lines, today) { io.github.graviton94.carpediem.core.Rings.newToday(profile.birthDate, today, state.lines) }
                    when {
                        capsule != null -> RecallNote(stringResource(R.string.capsule_opened)) { capsuleOpen = capsule }
                        // 12월 마지막 열흘 (08): 올해의 엔딩 크레딧 (한 번 보면 다시 권하지 않음)
                        creditsYear != null -> RecallNote(stringResource(R.string.credits_ready)) { state.creditsSeen(creditsYear); onCredits(creditsYear) }
                        ringNew != null && ringNew !in ringsSeen -> RecallNote(stringResource(R.string.ring_new, "$ringNew")) { ringsSeen = ringsSeen + ringNew; ringOpen = ringNew }
                        bday != null -> {
                            val line = if (bday.id == null) stringResource(if (bday.soon == 0) R.string.bday_mineToday else R.string.bday_mineTomorrow)
                                else stringResource(if (bday.soon == 0) R.string.bday_today else R.string.bday_tomorrow, bday.name)
                            TokenText(line, Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                        }
                        year != null -> YearCard(year) { state.openYear(year); toRecord(RecordView(year, null)) }
                        month != null -> MonthCard(month.second) { state.openMonth(month.first, month.second); toRecord(RecordView(month.first, month.second)) }
                        gardenYear != null -> RecallNote(stringResource(R.string.gardenYear_ask)) { state.gardenYearSeen(gardenYear); gardenYearOpen = gardenYear }
                        // 아침 씨앗 (04): 아침에 한 줄로, 누르면 고르는 장
                        state.seedDue(now) && !touring -> RecallNote(stringResource(R.string.seed_note)) { seedOpen = true }
                        recall != null -> RecallNote(recall) { turnTo(1) }
                        // 첫 일주일 길잡이: 하루에 하나, 해 본 것은 건너뜀 (누르면 그 일로)
                        else -> state.firstWeekNudge(today)?.takeIf { state.guideDone }?.let { k ->
                            val text = stringResource(when (k) { "breath" -> R.string.nudge_breath; "stone" -> R.string.nudge_stone; "gaze" -> R.string.nudge_gaze
                                "special" -> R.string.nudge_special; "widget" -> R.string.nudge_widget; else -> R.string.nudge_backup })
                            // 보여 준 날을 적어 둠: 하루 지나면 해 보지 않았어도 다음 권유로
                            LaunchedEffect(k) { state.nudgeShown(k, today) }
                            RecallNote(text) {
                                when (k) {
                                    "breath" -> { breathSheet = true }
                                    "stone" -> onAddPerson()
                                    "gaze" -> onGaze()
                                    "special" -> turnTo(3)
                                    "widget" -> { if (!io.github.graviton94.carpediem.widget.Widgets.pin(ctx, "line")) state.say(ctx.getString(R.string.nudge_widgetHow)) }
                                    else -> { state.settingsFocus = "backup"; onSettings() }
                                }
                                state.nudgeSeen(k)
                            }
                        }
                    }
                    // 큰 글씨면 버튼을 두 줄로 (글자가 잘리지 않게)
                    val addOn = state.people.size < G.Family.max.toInt() - 1
                    val bigText = density.fontScale >= G.Layout.bigFont
                    Row(Modifier.guideTarget(guide, "actions"), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        GardenButton(stringResource(R.string.breath), { breathSheet = true }, filled = false, seed = 888, modifier = Modifier.weight(1f), paper = true)
                        GardenButton(stringResource(R.string.gaze), onGaze, filled = false, seed = 889, modifier = Modifier.weight(1f), paper = true)
                        if (addOn && !bigText) GardenButton(stringResource(R.string.family_addShort), onAddPerson, filled = false, seed = 886, modifier = Modifier.weight(0.7f), paper = true)
                    }
                    if (addOn && bigText) GardenButton(stringResource(R.string.family_addShort), onAddPerson, filled = false, seed = 886, modifier = Modifier.fillMaxWidth().guideTarget(guide, "actions.add"), paper = true)
                }
            }
            }
          }
          }
        }
        if (!bare && !typing) Box(Modifier.guideTarget(guide, "tabs")) { PageTabs(pager.currentPage) { turnTo(it) } }
      }
        // 한 줄을 보낸 뒤: 깃털이 내려오며 한마디 창
        if (!bare) LetGoModal(state, Modifier.fillMaxSize()) { c ->
            when (c) {
                io.github.graviton94.carpediem.ui.Care.CALM_BREATH -> onBreath(BreathKind.CALM, 1, state.sound)
                io.github.graviton94.carpediem.ui.Care.BOX_BREATH -> onBreath(BreathKind.BOX, 1, state.sound)
                io.github.graviton94.carpediem.ui.Care.LOOK -> onLook()
                io.github.graviton94.carpediem.ui.Care.SLEEP_BREATH -> onBreath(BreathKind.SLEEP, 1, state.sound)
                io.github.graviton94.carpediem.ui.Care.MORNING_BREATH -> onBreath(BreathKind.CALM, 1, state.sound)
                io.github.graviton94.carpediem.ui.Care.SEND_TO -> {}
            }
        }
        // 처음 온 사람: 정원을 하나씩 비추며 둘러보기 (정원 페이지에서, 한마디 창이 없을 때)
        if (touring && pager.currentPage == 0 && state.toast == null) GuideTour(state, "garden", guide, GardenGuideSteps, onWrite = { state.pageHintSeen("write"); state.focusWrite = true; turnTo(1) }) { state.finishGuide() }
        // 기록 · 추억 · 흐름: 처음 들어오면 그 페이지의 짧은 둘러보기 (정원 둘러보기를 마친 뒤, 넘기는 중이 아닐 때)
        val pageKey = listOf(null, "write", "memories", "flow").getOrNull(pager.currentPage)
        if (!bare && state.guideDone && pageKey != null && pageKey !in state.pageHints && state.toast == null && !pager.isScrollInProgress && !typing)
            androidx.compose.runtime.key(pageKey) { GuideTour(state, pageKey, guide, PageGuideSteps.getValue(pageKey)) { state.pageHintSeen(pageKey) } }
    }

    decorOpen?.let { part ->
        ModalBottomSheet(onDismissRequest = { decorOpen = null }, containerColor = Theme.gc.paper) { DecorSheet(part, decor, state, now) }
    }
    letterOpen?.let { LetterSheet(it, state.wishFor(it)) { letterOpen = null } }
    gardenYearOpen?.let { y -> GardenYearSheet(state, profile, now, y) { gardenYearOpen = null } }
    capsuleOpen?.let { c -> CapsuleSheet(c) { state.openCapsule(c); capsuleOpen = null } }
    ringOpen?.let { a -> RingSheet(state, profile, a, now) { ringOpen = null } }
    if (seedOpen) SeedSheet(state, now.toLocalDate()) { seedOpen = false }
    if (breathSheet) BreathSheet(state, now, { k, m, snd -> breathSheet = false; onBreath(k, m, snd) }) { breathSheet = false }
}

/** 아래 이름표: 정원 · 기록 · 추억 · 흐름. */
@Composable
private fun PageTabs(current: Int, onPick: (Int) -> Unit) {
    val p = Theme.palette
    val names = listOf(R.string.tab_garden, R.string.tab_write, R.string.tab_memories, R.string.tab_flow)
    Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
        CrayonRule(seed = 1300)
        Row(Modifier.fillMaxWidth()) {
            names.forEachIndexed { i, id ->
                val on = i == current
                Column(
                    Modifier.weight(1f).heightIn(min = Tokens.Layout.tapTarget).clickable(role = androidx.compose.ui.semantics.Role.Tab) { onPick(i) }
                        .semantics { selected = on }.padding(vertical = Tokens.Space.sp1),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    TokenText(stringResource(id), Tokens.TypeScale.subhead, color = if (on) p.foreground else p.secondary, weight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    Box(Modifier.padding(top = Tokens.Space.sp1).size(Theme.unit * G.Layout.tabMark, Theme.unit * 0.8f)
                        .background(if (on) p.olive else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(Theme.unit)))
                }
            }
        }
    }
}

/** 기록: 계절 첫날의 바람 · 오늘의 한 줄, 그 아래 쌓인 한 줄들 (마음의 기록: 월 · 해). 보낸 뒤에도 이번 달 정원에 오늘이 피는 것을 본다. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun WritePage(state: AppState, now: LocalDateTime, view: RecordView, guide: GuideTargets, onSettings: () -> Unit, onView: (RecordView) -> Unit) {
    val today = now.toLocalDate()
    // 마음의 기록에서 빈 날을 고르면 위의 쓰는 칸으로 올라감
    val scroll = rememberScrollState()
    LaunchedEffect(state.writeDay) { if (state.writeDay != null) scroll.animateScrollTo(0) }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(scroll).statusBarsPadding()
            .padding(horizontal = Theme.deviceClass.pageMargin).padding(top = Tokens.Space.sp6, bottom = Tokens.Space.sp8),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        state.wishDue(today)?.let { id -> WishCard(state, id, today) }
        LetGoSection(state, today, guide = guide)
        Spacer(Modifier.height(Tokens.Space.sp4))
        TokenText(stringResource(R.string.mood_title), Tokens.TypeScale.title3)
        if (state.keepLines) {
            // 찾은 줄을 누르면 그 달 판의 그날로, 판이 보이게 끌어옴
            val panel = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
            val sc = rememberCoroutineScope()
            RecordSearch(state, today) { v -> onView(v); sc.launch { delay(G.Motion.pageMs.toLong() / 2); panel.bringIntoView() } }
            Box(Modifier.guideTarget(guide, "write.record").bringIntoViewRequester(panel)) { RecordPanel(state, view, onView, today) }
        }
        // 기록 남기기를 꺼 두었으면: 어디서 켜는지 (누르면 설정으로)
        else TokenText(stringResource(R.string.record_off) + " " + stringResource(R.string.record_offHow), Tokens.TypeScale.footnote,
            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onSettings).padding(vertical = Tokens.Space.sp3), color = Theme.palette.secondary)
    }
}

/** 추억: 모은 것 (계절 앨범 · 한 해 한 장 · 만난 순간 · 편지 · 고마움 책 · 지난 정원 · 기억의 자리). */
@Composable
private fun MemoriesPage(state: AppState, profile: LifeProfile, now: LocalDateTime, guide: GuideTargets, onMemory: () -> Unit, onCredits: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = Theme.deviceClass.pageMargin).padding(top = Tokens.Space.sp6, bottom = Tokens.Space.sp8),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        TokenText(stringResource(R.string.collection), Tokens.TypeScale.title3)
        CollectionBody(state, profile, now, onMemory, guide)
        // 아침 씨앗 가운데 핀 것만 (04): 쉰 씨앗은 남기지 않음
        BloomedSeeds(state)
        // 미래의 나에게 (10) · 나이테 (07)
        KeepsakesSection(state, profile, now, onCredits)
    }
}

/** 흐름: 흐르는 시간 · 인생 달력 · 특별한 날. */
@Composable
private fun FlowPage(state: AppState, profile: LifeProfile, now: LocalDateTime, guide: GuideTargets) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val season = s.season
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = Theme.deviceClass.pageMargin).padding(top = Tokens.Space.sp6, bottom = Tokens.Space.sp8),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
                TokenText(stringResource(R.string.flow), Tokens.TypeScale.title3, Modifier.guideTarget(guide, "flow.bars"))
                LifePeriod.entries.forEachIndexed { i, period ->
                    val pp = s.period(period)
                    Row {
                        TokenText(Labels.period(ctx, period), Tokens.TypeScale.subhead)
                        Spacer(Modifier.weight(1f))
                        TokenText("${Labels.percent(pp.progress, 0)} · ${Labels.remaining(ctx, pp)}", Tokens.TypeScale.caption1, color = p.secondary)
                    }
                    CrayonBar(pp.progress.toFloat(), G.Colors.bars[i], Modifier.guideTarget(guide, "flow.bars.$i"), seed = 830 + i * 3)
                }
                Spacer(Modifier.height(Tokens.Space.sp4))
                var menu by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TokenText(stringResource(R.string.calendar), Tokens.TypeScale.title3)
                    Spacer(Modifier.weight(1f))
                    Box {
                        Row(Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { menu = true }, verticalAlignment = Alignment.CenterVertically) {
                            TokenText(Labels.grid(ctx, state.grid), Tokens.TypeScale.subhead, color = p.secondary, weight = FontWeight.SemiBold)
                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = p.secondary)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            GridScale.entries.forEach { g -> DropdownMenuItem(text = { Text(Labels.grid(ctx, g)) }, onClick = { state.changeGrid(g); menu = false }) }
                        }
                    }
                }
                val cols = when (state.grid) { GridScale.WEEKS -> Tokens.Grid.weeksColumns; GridScale.MONTHS -> Tokens.Grid.monthsColumns; GridScale.YEARS -> Tokens.Grid.yearsColumns }
                // 칸이 수천 개라 한 번 그려 두고(레이어) 넘길 때는 옮기기만 한다
                // 특별한 날 꽃: 그날이 든 칸 (그날까지 지나온 단위 수)
                val byCell = remember(state.specialDays, state.grid, profile) {
                    state.specialDays.groupBy { d -> LifeSnapshot(profile.birthDate, s.expectancy, d.date.atStartOfDay()).lived(state.grid.unit) }
                }
                // 꽃이 있는 칸을 누르면 그 칸 위에 작은 말풍선 (이름 · 날짜), 다시 누르거나 다른 칸을 누르면 닫힘
                var bubble by remember(state.grid) { mutableStateOf<Pair<Int, Offset>?>(null) }
                val density = LocalDensity.current
                Box(Modifier.fillMaxWidth().guideTarget(guide, "flow.calendar")) {
                    CrayonCalendar(s.total(state.grid.unit), s.lived(state.grid.unit), cols, Modifier.graphicsLayer(), flowers = byCell.keys) { i, at ->
                        bubble = if (bubble?.first == i || i !in byCell) null else i to at
                    }
                    bubble?.let { (i, at) ->
                        val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        val text = byCell[i].orEmpty().joinToString("\n") { "${it.name} · ${it.date.format(fmt)}" }
                        var w by remember { mutableStateOf(0) }
                        val gapPx = with(density) { (Theme.unit * 6).toPx() }
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val maxX = with(density) { maxWidth.toPx() } - w
                            Box(Modifier.offset { androidx.compose.ui.unit.IntOffset((at.x - w / 2f).coerceIn(0f, maxX.coerceAtLeast(0f)).toInt(), (at.y + gapPx).toInt()) }
                                .onGloballyPositioned { w = it.size.width }
                                .crayonBox(Theme.gc.paper, G.Radius.chip, G.Stroke.chip, 1420 + i).clickable { bubble = null }
                                .padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp2)) {
                                TokenText(text, Tokens.TypeScale.footnote, weight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                TokenText(stringResource(R.string.calendar_legend, Labels.season(ctx, season)), Tokens.TypeScale.caption1, color = p.secondary)
                Box(Modifier.guideTarget(guide, "flow.special")) { SpecialDaysRow(state, profile.birthDate) }
    }
}

/** 정원에 놓인 것 (최근 것부터). 개발자 모드의 ‘모두 미리 보기’면 전부. */
internal fun gardenMoments(state: AppState, profile: LifeProfile, s: LifeSnapshot, now: LocalDateTime): List<Moment> =
    if (state.previewAll) Moments.all(now.toLocalDate())
    else Moments.earned(state.store.startDate, profile.birthDate, s.expectancy, now.toLocalDate(), state.store.firstSkip, state.store.returned, state.streaks, state.firstBreath)

/** 오늘의 문장: 두 줄에 안 들어가면 글자를 한 단계씩 줄인다. 어절 단위로 줄을 바꾼다 (TokenText). */
/** 몇 글자까지 쳤는지 (문장 + 둘째 줄을 이어서). 이미 다 쳐 본 문장이거나 움직임을 끈 기기면 한 번에. */
@Composable
private fun rememberTyping(state: AppState, main: String, second: String?): Int {
    val key = main + "\n" + second.orEmpty()
    val ctx = LocalContext.current
    val still = remember { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val done = still || state.debugTyped || state.typedQuote == key
    var n by remember(key) { mutableStateOf(if (done) Int.MAX_VALUE else 0) }
    LaunchedEffect(key) {
        if (done) return@LaunchedEffect
        state.typedQuote = key
        val all = main.length + (second?.length ?: 0)
        val r = java.util.Random(key.hashCode().toLong())
        val ms = G.Motion.typeMs
        while (n < all) {
            val c = if (n < main.length) main[n] else second!![n - main.length]
            // 글자마다 조금씩 다른 박자, 쉼표 · 마침표 뒤엔 한 숨. 둘째 줄 (영문) 은 조금 빠르게
            val base = if (n < main.length) ms else ms * 0.5f
            delay((base * (0.7f + 0.6f * r.nextFloat()) * (if (c in ",.!?·…") G.Motion.typePause else 1f)).toLong())
            n++
        }
    }
    return n
}

/** 친 만큼만 보이는 글 (안 친 글자는 투명하게 자리만). */
@Composable
internal fun TypedText(text: String, shown: Int, token: io.github.graviton94.carpediem.design.TypeToken, color: Color, modifier: Modifier = Modifier,
                      maxLines: Int = Int.MAX_VALUE, onTextLayout: (androidx.compose.ui.text.TextLayoutResult) -> Unit = {}) {
    val k = shown.coerceIn(0, text.length)
    // 줄은 띄어쓰기에서만 바뀌게 (낱말 · 어절 가운데서 끊기지 않게, 모든 Android 버전에서), 마지막 줄에 한 낱말만 남지 않게 끝 두 낱말은 붙여 둠
    val (shaped, at) = remember(text) { keepWords(text) }
    val shownText = androidx.compose.ui.text.buildAnnotatedString {
        append(shaped)
        if (k < text.length) addStyle(androidx.compose.ui.text.SpanStyle(color = Color.Transparent), at[k], shaped.length)
    }
    val style = token.style(text).copy(lineBreak = androidx.compose.ui.text.style.LineBreak(androidx.compose.ui.text.style.LineBreak.Strategy.Balanced,
        androidx.compose.ui.text.style.LineBreak.Strictness.Normal, androidx.compose.ui.text.style.LineBreak.WordBreak.Phrase))
    Text(shownText, modifier.semantics { contentDescription = text }, color = color, style = style, textAlign = TextAlign.Center, maxLines = maxLines, onTextLayout = onTextLayout)
}

/** 낱말 안 글자 사이에 WORD JOINER 를 넣고 마지막 띄어쓰기는 붙는 띄어쓰기로. at[i] = 원래 i 번째 글자가 바뀐 글에서 놓인 자리. */
internal fun keepWords(text: String): Pair<String, IntArray> {
    val lastSpace = text.trimEnd().lastIndexOf(' ').takeIf { text.trim().count { it == ' ' } >= 2 } ?: -1
    val b = StringBuilder(); val at = IntArray(text.length + 1)
    text.forEachIndexed { i, c ->
        at[i] = b.length
        b.append(if (i == lastSpace) '\u00A0' else c)
        val n = text.getOrNull(i + 1)
        if (n != null && !c.isWhitespace() && !n.isWhitespace() && !c.isHighSurrogate()) b.append('\u2060')
    }
    at[text.length] = b.length
    return b.toString() to at
}

@Composable
private fun QuoteText(text: String, shown: Int = Int.MAX_VALUE) {
    val steps = listOf(Tokens.TypeScale.headline, Tokens.TypeScale.callout, Tokens.TypeScale.subhead)
    var step by remember(text) { mutableStateOf(0) }
    var ready by remember(text) { mutableStateOf(false) }
    val last = step == steps.lastIndex
    TypedText(
        text, shown, steps[step].serif(), Theme.palette.foreground, Modifier.fillMaxWidth().graphicsLayer { alpha = if (ready) 1f else 0f },
        maxLines = if (last) Int.MAX_VALUE else 2,
        onTextLayout = { r -> if (r.hasVisualOverflow && !last) step++ else ready = true },
    )
}

internal fun objName(id: String) = when (id) {
    "moss" -> R.string.obj_moss; "teacup" -> R.string.obj_teacup; "cairn" -> R.string.obj_cairn; "pine" -> R.string.obj_pine
    "flower" -> R.string.obj_flower; "pond" -> R.string.obj_pond; "leaf" -> R.string.obj_leaf; "candle" -> R.string.obj_candle
    "dandelion" -> R.string.obj_dandelion; "bookmark" -> R.string.obj_bookmark; "snail" -> R.string.obj_snail
    "pinwheel" -> R.string.obj_pinwheel; "paperboat" -> R.string.obj_paperboat; "kite" -> R.string.obj_kite; "windchime" -> R.string.obj_windchime; else -> R.string.obj_acorn
}
internal fun objWhen(id: String) = when (id) {
    "moss" -> R.string.obj_moss_when; "teacup" -> R.string.obj_teacup_when; "cairn" -> R.string.obj_cairn_when; "pine" -> R.string.obj_pine_when
    "flower" -> R.string.obj_flower_when; "pond" -> R.string.obj_pond_when; "leaf" -> R.string.obj_leaf_when; "candle" -> R.string.obj_candle_when
    "dandelion" -> R.string.obj_dandelion_when; "bookmark" -> R.string.obj_bookmark_when; "snail" -> R.string.obj_snail_when
    "pinwheel" -> R.string.obj_pinwheel_when; "paperboat" -> R.string.obj_paperboat_when; "kite" -> R.string.obj_kite_when; "windchime" -> R.string.obj_windchime_when; else -> R.string.obj_acorn_when
}
internal fun objLine(id: String) = when (id) {
    "moss" -> R.string.obj_moss_line; "teacup" -> R.string.obj_teacup_line; "cairn" -> R.string.obj_cairn_line; "pine" -> R.string.obj_pine_line
    "flower" -> R.string.obj_flower_line; "pond" -> R.string.obj_pond_line; "leaf" -> R.string.obj_leaf_line; "candle" -> R.string.obj_candle_line
    "dandelion" -> R.string.obj_dandelion_line; "bookmark" -> R.string.obj_bookmark_line; "snail" -> R.string.obj_snail_line
    "pinwheel" -> R.string.obj_pinwheel_line; "paperboat" -> R.string.obj_paperboat_line; "kite" -> R.string.obj_kite_line; "windchime" -> R.string.obj_windchime_line; else -> R.string.obj_acorn_line
}

/** 놓인 것을 누르면: 생긴 날과 한 줄. 개수나 빈칸은 보이지 않는다. */
@Composable
internal fun ItemSheet(m: Moment, onClose: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(stringResource(objName(m.id)), Tokens.TypeScale.title3)
        Image(GardenArt.obj(ctx, m.id), null, Modifier.size(u * G.Layout.objBox * 0.8f))
        TokenText(stringResource(R.string.garden_itemDate, m.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)), stringResource(objWhen(m.id))), Tokens.TypeScale.footnote, color = p.secondary)
        TokenText(stringResource(objLine(m.id)), Tokens.TypeScale.callout.serif(), align = TextAlign.Center)
        GardenButton(stringResource(R.string.garden_close), onClose, filled = false, seed = 876)
    }
}

// ───────────────────────── 하루를 만남 (온보딩 다음 한 장) ─────────────────────────

/** 반짝이는 첫 만남에만. 평소 정원에는 반짝이가 없다. */
@Composable
fun MeetScreen(state: AppState, onDone: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val load = haruArt(state, sprout = true)
    val art = load.art
    BoxWithConstraints(Modifier.fillMaxSize().paperBackground()) {
        val u = Theme.unit
        val screenW = maxWidth
        Image(GardenArt.sky(ctx, Season.SPRING), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
        SkyTimeLayer(state.fixedNow ?: java.time.LocalDateTime.now(), maxHeight * G.Layout.groundRatio, 0.dp, 0.dp, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(Tokens.Space.sp10))
            TokenText(stringResource(R.string.garden_meet_title), Tokens.TypeScale.title2.serif(), align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp2))
            TokenText(stringResource(R.string.garden_meet_sub), Tokens.TypeScale.subhead, color = p.secondary, align = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            // 땅 한 줄 위에 하루 (돌의 페이지와 같은 배치), 첫 만남에만 반짝이
            art?.let { BigStone(it, pet = false, hat = false, sparkle = true) }
            Spacer(Modifier.height(Tokens.Space.sp6))
            art?.let { TokenText(Labels.stone(ctx, it.meta.stone), Tokens.TypeScale.caption1, color = p.secondary) }
            Spacer(Modifier.weight(1f))
            GardenButton(stringResource(R.string.garden_meet_go), onDone, filled = true, seed = 750)
            Spacer(Modifier.height(Tokens.Space.sp6))
        }
    }
}

@Composable
internal fun Sparkles(art: HaruArt, scale: Dp) {
    val ctx = LocalContext.current
    val img: ImageBitmap = GardenArt.sparkle(ctx)
    val bb = art.meta.bbox; val s = G.Layout.sparkle
    listOf(Triple(bb.left - s * 1.4f, bb.top + s, s), Triple(bb.right + s * 1.1f, bb.top - s * 0.2f, s * 0.8f)).forEach { (x, y, r) ->
        Image(img, null, Modifier.offset(scale * (x - r), scale * (y - r)).size(scale * (r * 2)))
    }
}


// ───────────────────────── 작은 부품 ─────────────────────────

@Composable
fun GardenChip(text: String, selected: Boolean, seed: Int, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onClick), contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.crayonBox(if (selected) Theme.gc.chip else null, G.Radius.chip, G.Stroke.chip, seed).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1)) {
            TokenText(text, Tokens.TypeScale.subhead, weight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
fun GardenButton(text: String, onClick: () -> Unit, filled: Boolean, seed: Int, modifier: Modifier = Modifier, paper: Boolean = false) {
    Box(
        modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp2).crayonBox(if (filled) Theme.gc.button else if (paper) Theme.gc.paper else null, G.Radius.button, G.Stroke.box, seed).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TokenText(text, Tokens.TypeScale.headline) }
}
