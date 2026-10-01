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

/** 해(낮) · 달(밤)이 지금 하늘의 어디쯤인지 (0 = 왼쪽 끝, 1 = 오른쪽 끝). 폰 시스템 시각 기준. */
private fun skyProgress(now: LocalDateTime): Pair<Boolean, Float> {
    val h = now.hour + now.minute / 60f
    val rise = G.Motion.sunrise; val set = G.Motion.sunset
    return if (h in rise..set) true to ((h - rise) / (set - rise))
    else false to (((h - set + 24f) % 24f) / (24f - (set - rise)))
}

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
               onBreath: (BreathKind, Int, Sound) -> Unit = { _, _, _ -> }, onGaze: () -> Unit = {}, bare: Boolean = false, onLook: () -> Unit = {}, onMemory: () -> Unit = {}) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val season = s.season
    val load = haruArt(state, season == Season.SPRING)
    val art = load.art
    val moments = gardenMoments(state, profile, s, now)
    var open by remember { mutableStateOf<Moment?>(null) }
    var breathSheet by remember { mutableStateOf(false) }
    val sleepy = !bare && isNight(now)
    // 캡처용: 이번 달 편지를 바로 펼침. 고르기만 그리기 중에, ‘연 편지’로 남기기는 그 뒤에
    var letterOpen by remember { mutableStateOf(if (state.debugOpenLetter && !bare) state.letterDue(now.toLocalDate()) else null) }
    LaunchedEffect(letterOpen) { letterOpen?.let { state.openLetter(it.id) } }

    // 네 장: 정원 · 기록 (오늘의 한 줄) · 추억 (마음의 기록 · 모은 것) · 흐름 (흐르는 시간 · 인생 달력 · 특별한 날). 옆으로 넘기거나 아래 이름표로.
    // bare = 돌멍하기: 정원 한 장, 글자 · 이름표 · 아래 버튼 없이
    val pager = rememberPagerState(initialPage = if (bare) 0 else state.homePage.coerceIn(0, 3)) { if (bare) 1 else 4 }
    if (!bare) LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { state.homePage = it } }
    val scope = rememberCoroutineScope()
    var recordView by remember { mutableStateOf(state.pendingRecord?.also { state.pendingRecord = null } ?: if (state.debugOpenYear) RecordView(state.yearDue(now.toLocalDate()) ?: now.year, null) else RecordView(now.year, now.monthValue)) }
    // 정원이 아닌 페이지에서 뒤로 가기: 앱을 닫지 않고 정원으로
    // 이름표 · 버튼으로 옮기는 동안은 ‘숨 한 번’, 손으로 넘길 땐 ‘내려앉는 종이’
    var breath by remember { mutableStateOf(false) }
    fun turnTo(i: Int) { scope.launch { breath = true; try { pager.animateScrollToPage(i, animationSpec = androidx.compose.animation.core.tween(G.Motion.pageMs.toInt())) } finally { breath = false } } }
    BackHandler(enabled = !bare && pager.currentPage != 0) { turnTo(0) }
    // 글을 쓰는 동안 (키보드가 떠 있으면) 옆으로 넘어가지 않고, 이름표도 쉬게
    val typing = WindowInsets.isImeVisible && pager.currentPage == 1   // 기록 페이지에서 쓰는 중일 때만
    fun toRecord(v: RecordView) { recordView = v; turnTo(2) }

    Box(Modifier.fillMaxSize().paperBackground()) {
      Column(Modifier.fillMaxSize()) {
        // 보이지 않는 페이지는 그리지 않는다 (반짝임 · 살랑임이 화면 밖에서 돌지 않게). 쓰던 글은 rememberSaveable 로 남음
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 0, userScrollEnabled = !bare && !typing,
            // 조금만 밀어도 넘어가게 (기본은 반 장)
            flingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(pager, snapPositionalThreshold = G.Motion.turnSnap)) { page ->
          // 손으로 넘길 땐 들어오는 장 (아직 오른쪽) 이 위에
          val incoming by remember(page) { derivedStateOf { (pager.currentPage - page) + pager.currentPageOffsetFraction < 0f } }
          Box(Modifier.fillMaxSize().zIndex(if (incoming) 1f else 0f).pageTurn(pager, page) { breath }) {
          when (page) {
            1 -> WritePage(state, now)
            2 -> MemoriesPage(state, profile, now, recordView, { recordView = it }, onMemory)
            3 -> FlowPage(state, profile, now)
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
                val fit = Family.fitScale(slots0.map { (it.art.meta.bbox.width * it.scale.value).toDouble() }, ((G.Layout.pathEnd - G.Layout.pathStart) * u.value).toDouble(), (G.Family.minGap * u.value).toDouble()).toFloat()
                val slots = if (fit < 1f) gardenSlots(state, profile, s, now, haruBase * fit) else slots0
                val headroom = slots.maxOf { sl -> sl.scale * (sl.art.meta.ground - sl.art.meta.bbox.top + if (sl.birthday) Tokens.Garden.Party.hatHeight else if (sl.art.sprout) Tokens.Garden.HaruDraw.sproutHeight else 0f) }
                val haruAbove = headroom
                val family = slots.size > 1
                // 이름표 두 줄 + 나이 한 줄 + 아래 버튼 · 카드와의 틈
                val labels = u * (G.Layout.labelGap + G.Layout.labelRow * 4)
                val gy = maxOf(screenH * G.Layout.groundRatio, topBottom + u * G.Layout.minSkyGap + haruAbove).coerceAtMost(screenH - labels - blockH)

                Image(GardenArt.sky(ctx, season), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
                // 땅 그림도 시간의 빛 아래에 (밤이면 땅까지 어두워짐)
                Image(GardenArt.strip(ctx, season), null, Modifier.offset(y = gy - u * G.Layout.stripLineY).fillMaxWidth().height(u * G.Layout.stripHeight), contentScale = ContentScale.FillBounds)
                // 정원만 보기(bare)는 위 글자가 없어도 별이 상태바 · 소리 버튼에 닿지 않게
                SkyTimeLayer(now, gy, if (bare) screenH * 0.14f else topBottom, gy - haruAbove - u * G.Layout.minSkyGap, Modifier.fillMaxSize())
                // 기억의 돌 가운데 ‘하늘에 별로 두기’를 켠 것: 하늘에 따뜻한 별 하나 (돌멍하기에는 두지 않음)
                if (!bare) {
                    // 위 글자 아래 ~ 돌 머리 위. 글자 높이를 아직 모르거나 띠가 없으면 하늘 위쪽의 작은 띠에
                    val starTop = if (topBottom > 0.dp) topBottom + u * G.Layout.minSkyGap else screenH * 0.14f
                    val starBottom = (gy - haruAbove - u * G.Layout.minSkyGap * 2).let { if (it > starTop + u * 12) it else starTop + u * 28 }
                    MemoryStars(state, screenW, starTop, starBottom, Theme.gc.night)
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
                        IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.settings) }) { Icon(Icons.Filled.Settings, null, tint = p.secondary) }
                    }
                    TokenText(stringResource(R.string.timeLeft) + " · " + stringResource(R.string.path_age, "${s.age}", Labels.season(ctx, season)), Tokens.TypeScale.subhead, color = p.secondary)
                    TokenText(Labels.number(s.remaining(state.unit)), Tokens.TypeScale.display(Theme.deviceClass), maxLines = 1)
                    // 잠들기 전 정원 (밤 10시 이후): 단위 고르기 · 영문 · 넘김 안내 같은 글자는 쉬게
                    if (!sleepy) Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        LifeUnit.entries.forEachIndexed { i, unit -> GardenChip(Labels.unit(ctx, unit), unit == state.unit, seed = 800 + i) { state.changeUnit(unit) } }
                    }
                    val question = state.question
                    if (question != null) QuestionBlock(state, question, sent = state.sentOn(now.toLocalDate())) {
                        // 한 줄로 답하기: 기록 페이지의 오늘의 한 줄로
                        state.answer(); turnTo(1)
                    } else state.quote?.let { q ->
                        Column(
                            Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4).clickable { state.nextQuote() },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
                        ) {
                            // 새 문장 (누르거나 날이 바뀌어) 은 옛 타자기처럼 한 글자씩. 자리는 처음부터 다 잡아 두어 줄이 흔들리지 않음
                            val main = if (state.quoteLanguage == QuoteLanguage.ENGLISH) q.english else q.korean
                            val second = if (state.quoteLanguage == QuoteLanguage.BOTH && !sleepy) q.english else null
                            val typed = rememberTyping(state, main, second)
                            QuoteText(main, typed)
                            if (second != null) TypedText(second, typed - main.length, Tokens.TypeScale.footnote.serif(), p.secondary, Modifier.fillMaxWidth())
                            if (!sleepy) TokenText(stringResource(R.string.words_next), Tokens.TypeScale.caption2, color = p.secondary, weight = FontWeight.Normal)
                        }
                    }
                    // 밤: 잠드는 명상 1분 · 아침 (오늘 아직 숨 쉬지 않았으면): 하루를 여는 숨 1분, 옅은 한 줄로
                    if (isNight(now)) TokenText(stringResource(R.string.breath_night), Tokens.TypeScale.footnote.serif(),
                        Modifier.clickable { onBreath(BreathKind.SLEEP, 1, state.sound) }.padding(Tokens.Space.sp2), color = p.secondary)
                    else if (Labels.part(now) == io.github.graviton94.carpediem.core.DayPart.MORNING && state.breaths.none { it.first == now.toLocalDate() })
                        TokenText(stringResource(R.string.breath_morning), Tokens.TypeScale.footnote.serif(),
                            Modifier.clickable { onBreath(BreathKind.CALM, 1, state.sound) }.padding(Tokens.Space.sp2), color = p.secondary)
                }

                val x0 = u * G.Layout.pathStart; val x1 = u * G.Layout.pathEnd
                // 돌 자리: 각자 인생의 길 위 원래 자리, 겹치면 옆으로 비켜 앉음 (core Family.place)
                val targets = slots.map { sl -> sl.progress?.let { lerp(G.Layout.pathStart + G.Layout.pathInset, G.Layout.pathEnd - G.Layout.pathInset, it.toFloat().coerceIn(0f, 1f)).toDouble() * u.value } }
                val widths = slots.map { sl -> (sl.art.meta.bbox.width * sl.scale.value).toDouble() }
                val xs = Family.place(targets, widths, 0, x0.value.toDouble(), x1.value.toDouble(), (G.Family.gap * u.value).toDouble(), (G.Family.minGap * u.value).toDouble()).map { it.toFloat().dp }

                // 해 · 달: 폰 시각을 따라 하늘을 가로지름. 위쪽 글자에도, 하루 머리에도 닿지 않음.
                val (day, t) = skyProgress(now)
                val r = u * G.Layout.sunRadius
                val base = gy - maxOf(u * G.Layout.sunBase, haruAbove + u * G.Layout.minSkyGap + r)
                val arc = min((u * G.Layout.sunArc).value, (base - topBottom - u * G.Layout.minSkyGap - r).value.coerceAtLeast(0f)).dp
                val sx = lerp((u * G.Layout.sunStart).value, (u * G.Layout.sunEnd).value, t).dp
                val sy = base - arc * sin(t * Math.PI).toFloat()
                Image(if (day) GardenArt.sun(ctx) else GardenArt.moon(ctx), null, Modifier.offset(sx - r, sy - r).size(r * 2))
                // 밤 · 새벽: 달빛 · 돌들 발치의 빛 · 가로등 · 반딧불
                val spanL = slots.indices.minOf { xs[it] - (widths[it].toFloat() / 2).dp }; val spanR = slots.indices.maxOf { xs[it] + (widths[it].toFloat() / 2).dp }
                NightLights(now, gy, spanL, spanR, if (day) null else androidx.compose.ui.unit.DpOffset(sx, sy), Modifier.fillMaxSize())


                // 놓인 것: 돌 사이 빈틈과 가장 왼쪽 돌의 왼편에, 최근 것부터. 자리가 없으면 거기까지만 (모은 것에는 모두).
                val box = u * (G.Layout.objBox * G.Layout.objScale)
                val step = box * 0.6f + u * G.Layout.itemGap
                val spans = slots.indices.map { i -> (xs[i] - u * widths[i].toFloat() / u.value / 2) to (xs[i] + u * widths[i].toFloat() / u.value / 2) }.sortedBy { it.first }
                val gaps = buildList {
                    add(x0 to spans.first().first - u * G.Layout.itemFromHaru)
                    for (k in 0 until spans.size - 1) add(spans[k].second + u * G.Layout.itemGap to spans[k + 1].first - u * G.Layout.itemGap)
                }
                val spots = gaps.flatMap { (a0, b0) -> buildList { var c = b0 - box * 0.3f; while (c - box * 0.3f >= a0) { add(c); c -= step } } }
                moments.zip(spots).forEach { (m, cx) ->
                    Image(GardenArt.obj(ctx, m.id), stringResource(objName(m.id)), Modifier.offset(cx - box / 2, gy - box * (G.Layout.objGround / G.Layout.objBox)).size(box).let { if (bare) it else it.clickable { open = m } })
                }

                // 돌들: 한 번 누르면 쓰다듬기, 두 번 누르면 그 돌의 페이지
                val todayLine = state.lines.lastOrNull { it.date == now.toLocalDate() }
                val sentTo = todayLine?.to
                // 무거운 마음을 보낸 날, 내 하루는 살짝 아래를 본다 (다음 날 평소대로)
                val heavyToday = todayLine?.feeling in io.github.graviton94.carpediem.core.Letters.HEAVY
                slots.forEachIndexed { i, sl ->
                    val cx = xs[i] - sl.scale * (sl.art.meta.bbox.center.x - sl.art.meta.box / 2)
                    val left = cx - sl.scale * (sl.art.meta.box / 2); val top = gy - sl.scale * G.Layout.haruGround
                    HaruFigure(sl.art, sl.scale, Modifier.offset(left, top), blinkKick = if (sl.id == null) state.blinkKick else 0, hat = sl.birthday, tiltOn = sl.id == null, lookDown = if (sl.id == null && heavyToday) G.Care.lookDown else 0f,
                        a11y = stringResource(R.string.garden_stoneA11y, sl.name, Labels.stone(ctx, sl.art.meta.stone)), onOpen = { onStone(sl.id) },
                        onLongPress = if (sl.id == null && !bare) ({ breathSheet = true }) else null)
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
                }

                // 이름표 (가족이 있을 때) · 0세 · 기대수명
                val px = with(density) { Pair(x0.toPx(), x1.toPx()) }
                // 돌이 oneRow 명보다 많으면 이름표를 두 줄로 번갈아 (서로 겹치지 않게)
                val labelRows = if (slots.size > G.Family.oneRow.toInt()) 2 else 1
                // 화면 왼쪽부터의 순서로 번갈아 (목록 순서가 아니라 실제로 옆에 앉은 돌끼리 다른 줄)
                val rank = xs.indices.sortedBy { xs[it].value }.withIndex().associate { it.value to it.index }
                if (family && !bare) Box(Modifier.offset(y = gy + u * G.Layout.labelGap).fillMaxWidth()) {
                    slots.forEachIndexed { i, sl -> TokenText(shortName(sl.name), Tokens.TypeScale.caption1, Modifier.offset(y = u * G.Layout.labelRow * ((rank[i] ?: i) % labelRows)).centerAt(with(density) { xs[i].toPx() }, 0f, with(density) { screenW.toPx() }), weight = FontWeight.Medium, maxLines = 1) }
                }
                if (!bare) Box(Modifier.offset(y = gy + u * (G.Layout.labelGap + if (family) G.Layout.labelRow * labelRows else 0f)).fillMaxWidth()) {
                    TokenText(stringResource(R.string.garden_age0), Tokens.TypeScale.caption1, Modifier.centerAt(px.first, 0f, px.second), color = p.secondary)
                    TokenText(stringResource(R.string.expectancy_value, Labels.years(s.expectancy)), Tokens.TypeScale.caption1, Modifier.centerAt(px.second, px.first, with(density) { screenW.toPx() }), color = p.secondary)
                }
                // 잠들기 전 정원: 화면 전체를 조금 더 어둡게
                if (sleepy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = G.SleepGarden.dim)))
                // 마음의 날씨: 오늘 보낸 마음이 하늘에 잠깐 (무거운 마음 = 몇 방울 비, 기쁨 · 희망 · 고마움 = 햇살 한 줄기)
                if (!bare) MoodWeather(state, now, gy - haruAbove)
                // 아래, 엄지가 닿는 곳: 생일 한 줄 · 도착한 것 한 장 · 정원에서 하는 일 (숨, 쉼 · 돌멍하기 · 돌 더하기)
                if (!bare) Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = margin).padding(bottom = Tokens.Space.sp3)
                        .onGloballyPositioned { c -> blockH = with(density) { c.size.height.toDp() } },
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
                ) {
                    slots.firstOrNull { it.soon != null }?.let { sl ->
                        val line = if (sl.id == null) stringResource(if (sl.soon == 0) R.string.bday_mineToday else R.string.bday_mineTomorrow)
                            else stringResource(if (sl.soon == 0) R.string.bday_today else R.string.bday_tomorrow, sl.name)
                        TokenText(line, Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                    }
                    val today = now.toLocalDate()
                    val letter = state.letterDue(today); val year = state.yearDue(today); val month = if (year == null) state.monthDue(today) else null
                    when {
                        letter != null -> LetterEnvelope(letter) { state.openLetter(letter.id); letterOpen = letter }
                        year != null -> YearCard(year) { state.openYear(year); toRecord(RecordView(year, null)) }
                        month != null -> MonthCard(month.second) { state.openMonth(month.first, month.second); toRecord(RecordView(month.first, month.second)) }
                    }
                    // 돌아온 한 줄 (몇 해 전 오늘 · 문득): 정원에서도 알 수 있게, 누르면 기록 페이지에서 펼쳐 봄
                    val recall = remember(state.lines, today, state.randomLine) {
                        io.github.graviton94.carpediem.core.Lines.yearsAgo(state.lines, today).firstOrNull()?.first?.let { ctx.getString(R.string.recall_notify, "$it") }
                            ?: state.randomLine?.let { ctx.getString(R.string.recall_randomNotify) }
                    }
                    recall?.let { t -> RecallNote(t) { turnTo(1) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        GardenButton(stringResource(R.string.breath), { breathSheet = true }, filled = false, seed = 888, modifier = Modifier.weight(1f), paper = true)
                        GardenButton(stringResource(R.string.gaze), onGaze, filled = false, seed = 889, modifier = Modifier.weight(1f), paper = true)
                        if (state.people.size < G.Family.max.toInt() - 1) GardenButton(stringResource(R.string.family_addShort), onAddPerson, filled = false, seed = 886, modifier = Modifier.weight(0.7f), paper = true)
                    }
                }
            }
            }
          }
          }
        }
        if (!bare && !typing) PageTabs(pager.currentPage) { turnTo(it) }
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
    }

    open?.let { m ->
        ModalBottomSheet(onDismissRequest = { open = null }, containerColor = Theme.gc.paper) {
            ItemSheet(m) { open = null }
        }
    }
    letterOpen?.let { LetterSheet(it, state.wishFor(it)) { letterOpen = null } }
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

/** 기록: 계절 첫날의 바람 · 오늘의 한 줄. */
@Composable
private fun WritePage(state: AppState, now: LocalDateTime) {
    val today = now.toLocalDate()
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = Theme.deviceClass.pageMargin).padding(top = Tokens.Space.sp6, bottom = Tokens.Space.sp8),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        state.wishDue(today)?.let { id -> WishCard(state, id, today) }
        LetGoSection(state, today)
    }
}

/** 추억: 마음의 기록 (월 · 해) 과 모은 것 (편지 · 고마움 책 · 놓인 것 · 지난 정원 · 기억의 자리). */
@Composable
private fun MemoriesPage(state: AppState, profile: LifeProfile, now: LocalDateTime, view: RecordView, onView: (RecordView) -> Unit, onMemory: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = Theme.deviceClass.pageMargin).padding(top = Tokens.Space.sp6, bottom = Tokens.Space.sp8),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        TokenText(stringResource(R.string.mood_title), Tokens.TypeScale.title3)
        if (state.keepLines) RecordPanel(state, view, onView, now.toLocalDate())
        else TokenText(stringResource(R.string.record_off), Tokens.TypeScale.footnote, color = Theme.palette.secondary)
        Spacer(Modifier.height(Tokens.Space.sp4))
        TokenText(stringResource(R.string.collection), Tokens.TypeScale.title3)
        CollectionBody(state, profile, now, onMemory)
    }
}

/** 흐름: 흐르는 시간 · 인생 달력 · 특별한 날. */
@Composable
private fun FlowPage(state: AppState, profile: LifeProfile, now: LocalDateTime) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val season = s.season
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
            .padding(horizontal = Theme.deviceClass.pageMargin).padding(top = Tokens.Space.sp6, bottom = Tokens.Space.sp8),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
                TokenText(stringResource(R.string.flow), Tokens.TypeScale.title3)
                LifePeriod.entries.forEachIndexed { i, period ->
                    val pp = s.period(period)
                    Row {
                        TokenText(Labels.period(ctx, period), Tokens.TypeScale.subhead)
                        Spacer(Modifier.weight(1f))
                        TokenText("${Labels.percent(pp.progress, 0)} · ${Labels.remaining(ctx, pp)}", Tokens.TypeScale.caption1, color = p.secondary)
                    }
                    CrayonBar(pp.progress.toFloat(), G.Colors.bars[i], seed = 830 + i * 3)
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
                Box(Modifier.fillMaxWidth()) {
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
                SpecialDaysRow(state, profile.birthDate)
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
    val done = still || state.typedQuote == key
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
private fun TypedText(text: String, shown: Int, token: io.github.graviton94.carpediem.design.TypeToken, color: Color, modifier: Modifier = Modifier,
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
    "dandelion" -> R.string.obj_dandelion; "feather" -> R.string.obj_feather; "snail" -> R.string.obj_snail
    "pinwheel" -> R.string.obj_pinwheel; "paperboat" -> R.string.obj_paperboat; "kite" -> R.string.obj_kite; "windchime" -> R.string.obj_windchime; else -> R.string.obj_acorn
}
internal fun objWhen(id: String) = when (id) {
    "moss" -> R.string.obj_moss_when; "teacup" -> R.string.obj_teacup_when; "cairn" -> R.string.obj_cairn_when; "pine" -> R.string.obj_pine_when
    "flower" -> R.string.obj_flower_when; "pond" -> R.string.obj_pond_when; "leaf" -> R.string.obj_leaf_when; "candle" -> R.string.obj_candle_when
    "dandelion" -> R.string.obj_dandelion_when; "feather" -> R.string.obj_feather_when; "snail" -> R.string.obj_snail_when
    "pinwheel" -> R.string.obj_pinwheel_when; "paperboat" -> R.string.obj_paperboat_when; "kite" -> R.string.obj_kite_when; "windchime" -> R.string.obj_windchime_when; else -> R.string.obj_acorn_when
}
internal fun objLine(id: String) = when (id) {
    "moss" -> R.string.obj_moss_line; "teacup" -> R.string.obj_teacup_line; "cairn" -> R.string.obj_cairn_line; "pine" -> R.string.obj_pine_line
    "flower" -> R.string.obj_flower_line; "pond" -> R.string.obj_pond_line; "leaf" -> R.string.obj_leaf_line; "candle" -> R.string.obj_candle_line
    "dandelion" -> R.string.obj_dandelion_line; "feather" -> R.string.obj_feather_line; "snail" -> R.string.obj_snail_line
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
