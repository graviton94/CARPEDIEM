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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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

internal fun sleepy(now: LocalDateTime): Boolean { val h = now.hour; return h >= G.Motion.sleepFrom.toInt() || h < G.Motion.sleepTo.toInt() }

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
    val load by produceState(HaruLoad(null, true), seed, sprout) { value = HaruLoad(HaruArtStore.get(ctx, seed, sprout), false) }
    return load
}

/** 하루 한 명 (그림이 있으면 그림, 못 그렸으면 대체 그림). */
@Composable
internal fun Haru(load: HaruLoad, scale: Dp, modifier: Modifier, sleepy: Boolean = false, blinkKick: Int = 0) {
    val ctx = LocalContext.current
    val desc = load.art?.let { stringResource(R.string.garden_haruA11y, Labels.stone(ctx, it.meta.stone)) } ?: stringResource(R.string.garden_haru)
    val m = modifier.semantics { contentDescription = desc }
    when {
        load.art != null -> HaruFigure(load.art, scale, m, sleepy = sleepy, blinkKick = blinkKick)
        !load.loading -> HaruFallback(scale, m)
    }
}

// ───────────────────────── 홈 = 정원 ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenHome(state: AppState, profile: LifeProfile, now: LocalDateTime, onSettings: () -> Unit, onCollection: () -> Unit, onSupport: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val season = s.season
    val load = haruArt(state, season == Season.SPRING)
    val art = load.art
    val moments = gardenMoments(state, profile, s, now)
    var open by remember { mutableStateOf<Moment?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize().paperBackground()) {
        val u = Theme.unit
        val screenH = maxHeight
        val screenW = maxWidth
        val margin = Theme.deviceClass.pageMargin
        val pagePx = with(density) { screenH.toPx() }
        var topBottom by remember { mutableStateOf(0.dp) }
        val scroll = rememberScrollState()
        // 첫 장(정원)과 둘째 장(시간 · 달력) 사이에서 손을 떼면 가까운 장으로 부드럽게 넘어간다.
        LaunchedEffect(pagePx) {
            var from = scroll.value
            snapshotFlow { scroll.isScrollInProgress }.collect { moving ->
                val v = scroll.value
                if (moving) from = v
                else if (v in 1 until pagePx.toInt()) {
                    val goDown = if (from < pagePx) v > pagePx * G.Layout.pageSnap else v > pagePx * (1 - G.Layout.pageSnap)
                    // 넘어가는 중에 손가락이 닿으면 멈추고 손을 따른다
                    try { scroll.animateScrollTo(if (goDown) pagePx.toInt() else 0, tween(G.Motion.pageMs.toInt(), easing = FastOutSlowInEasing)) }
                    catch (e: CancellationException) { if (!isActive) throw e }
                }
            }
        }
        // 넘긴 정도 (0 = 정원, 1 = 둘째 장). 읽는 곳은 그리기 단계뿐이라 넘길 때 다시 구성하지 않는다.
        fun turned() = (scroll.value / pagePx).coerceIn(0f, 1f)

        // 키보드가 올라오면 넘기는 창 자체를 줄여, 입력칸을 키보드 위로 끌어올릴 수 있게
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(scroll)) {
            Box(Modifier.fillMaxWidth().height(screenH).graphicsLayer {
                val f = turned(); translationY = scroll.value * G.Layout.parallax; alpha = 1f - f * f
            }.clipToBounds()) {
                // 아래: 지나온 길 (땅 한 줄) 위에 하루와 놓인 것. 자리를 먼저 정해 하늘빛 · 별이 쓰게 한다.
                val haruScale = u * (G.Layout.haruWidth / G.Layout.haruArtWidth)
                val haruAbove = art?.let { haruScale * (it.meta.ground - it.meta.bbox.top) } ?: (u * G.Layout.haruWidth)
                val labels = u * (G.Layout.labelGap + G.Layout.labelRow * 3)
                val gy = maxOf(screenH * G.Layout.groundRatio, topBottom + u * G.Layout.minSkyGap + haruAbove).coerceAtMost(screenH - labels)

                Image(GardenArt.sky(ctx, season), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
                SkyTimeLayer(now, gy, topBottom, gy - haruAbove - u * G.Layout.minSkyGap, Modifier.fillMaxSize())

                // 위: 남은 시간 · 단위 · 오늘의 문장
                Column(
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
                    TokenText(stringResource(R.string.timeLeft), Tokens.TypeScale.subhead, color = p.secondary)
                    TokenText(Labels.number(s.remaining(state.unit)), Tokens.TypeScale.display(Theme.deviceClass), maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        LifeUnit.entries.forEachIndexed { i, unit -> GardenChip(Labels.unit(ctx, unit), unit == state.unit, seed = 800 + i) { state.changeUnit(unit) } }
                    }
                    state.quote?.let { q ->
                        Column(
                            Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4).clickable { state.nextQuote() },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
                        ) {
                            QuoteText(if (state.quoteLanguage == QuoteLanguage.ENGLISH) q.english else q.korean)
                            if (state.quoteLanguage == QuoteLanguage.BOTH) TokenText(q.english, Tokens.TypeScale.footnote.serif(), color = p.secondary, align = TextAlign.Center)
                            TokenText(stringResource(R.string.words_next), Tokens.TypeScale.caption2, color = p.secondary, weight = FontWeight.Normal)
                        }
                    }
                }

                val x0 = u * G.Layout.pathStart; val x1 = u * G.Layout.pathEnd
                val along = lerp((u * (G.Layout.pathStart + G.Layout.pathInset)).value, (u * (G.Layout.pathEnd - G.Layout.pathInset)).value, s.progress.toFloat().coerceIn(0f, 1f)).dp
                // 하루 몸이 길 끝을 넘지 않게 (큰 돌도 화면 안에). 대체 그림은 반지름 = haruWidth.
                val leftExt = art?.let { haruScale * (it.meta.box / 2 - it.meta.bbox.left) } ?: (u * G.Layout.haruWidth)
                val rightExt = art?.let { haruScale * (it.meta.bbox.right - it.meta.box / 2) } ?: (u * G.Layout.haruWidth)
                val hx = if (x0 + leftExt <= x1 - rightExt) along.coerceIn(x0 + leftExt, x1 - rightExt) else (x0 + x1) / 2

                // 해 · 달: 폰 시각을 따라 하늘을 가로지름. 위쪽 글자에도, 하루 머리에도 닿지 않음.
                val (day, t) = skyProgress(now)
                val r = u * G.Layout.sunRadius
                val base = gy - maxOf(u * G.Layout.sunBase, haruAbove + u * G.Layout.minSkyGap + r)
                val arc = min((u * G.Layout.sunArc).value, (base - topBottom - u * G.Layout.minSkyGap - r).value.coerceAtLeast(0f)).dp
                val sx = lerp((u * G.Layout.sunStart).value, (u * G.Layout.sunEnd).value, t).dp
                val sy = base - arc * sin(t * Math.PI).toFloat()
                Image(if (day) GardenArt.sun(ctx) else GardenArt.moon(ctx), null, Modifier.offset(sx - r, sy - r).size(r * 2))

                Image(GardenArt.strip(ctx, season), null, Modifier.offset(y = gy - u * G.Layout.stripLineY).fillMaxWidth().height(u * G.Layout.stripHeight), contentScale = ContentScale.FillBounds)

                // 놓인 것: 하루 왼쪽(지나온 쪽)에 최근 것부터. 자리가 없으면 거기까지만.
                val box = u * (G.Layout.objBox * G.Layout.objScale)
                var cursor = hx - leftExt - u * G.Layout.itemFromHaru
                for (m in moments) {
                    val cx = cursor - box * 0.3f
                    if (cx - box * 0.3f < x0) break
                    Image(GardenArt.obj(ctx, m.id), stringResource(objName(m.id)), Modifier.offset(cx - box / 2, gy - box * (G.Layout.objGround / G.Layout.objBox)).size(box).clickable { open = m })
                    cursor = cx - box * 0.3f - u * G.Layout.itemGap
                }

                Haru(load, haruScale, Modifier.offset(hx - haruScale * (G.Layout.haruBox / 2), gy - haruScale * G.Layout.haruGround), sleepy = sleepy(now), blinkKick = state.blinkKick)

                // 0세 · 기대수명 (한 줄) · 지금 나이와 계절 (다음 줄, 하루 아래)
                val px = with(density) { Triple(x0.toPx(), x1.toPx(), hx.toPx()) }
                Box(Modifier.offset(y = gy + u * G.Layout.labelGap).fillMaxWidth()) {
                    TokenText(stringResource(R.string.garden_age0), Tokens.TypeScale.caption1, Modifier.centerAt(px.first, 0f, px.second), color = p.secondary)
                    TokenText(stringResource(R.string.expectancy_value, Labels.years(s.expectancy)), Tokens.TypeScale.caption1, Modifier.centerAt(px.second, px.first, with(density) { screenW.toPx() }), color = p.secondary)
                }
                Box(Modifier.offset(y = gy + u * (G.Layout.labelGap + G.Layout.labelRow)).fillMaxWidth()) {
                    TokenText(stringResource(R.string.path_age, "${s.age}", Labels.season(ctx, season)), Tokens.TypeScale.caption1, Modifier.centerAt(px.third, px.first, px.second))
                }
                // 정원 아래쪽은 종이로 번져 둘째 장과 이어진다
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(screenH * G.Layout.fadeTail)
                    .background(Brush.verticalGradient(listOf(G.Colors.paper.copy(alpha = 0f), G.Colors.paper))))
                TokenText(stringResource(R.string.garden_down), Tokens.TypeScale.caption2, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Tokens.Space.sp3), color = p.secondary, weight = FontWeight.Normal)
            }

            // 둘째 장: 흐르는 시간 · 인생 달력 · 모은 것 · 응원하기. 넘길수록 떠오른다.
            Column(
                Modifier.fillMaxWidth().heightIn(min = screenH).graphicsLayer {
                    val f = turned(); alpha = f; translationY = (1f - f) * pagePx * (1f - G.Layout.parallax) * G.Layout.pageSnap
                }.statusBarsPadding().padding(horizontal = margin).padding(top = Tokens.Space.sp6)
                    .navigationBarsPadding().padding(bottom = Tokens.Space.sp10),
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
                CrayonCalendar(s.total(state.grid.unit), s.lived(state.grid.unit), cols, Modifier.graphicsLayer())
                TokenText(stringResource(R.string.calendar_legend, Labels.season(ctx, season)), Tokens.TypeScale.caption1, color = p.secondary)
                Spacer(Modifier.height(Tokens.Space.sp4))
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                    GardenButton(stringResource(R.string.collection), onCollection, filled = false, seed = 880, modifier = Modifier.weight(1f))
                    GardenButton(stringResource(R.string.support), onSupport, filled = false, seed = 884, modifier = Modifier.weight(1f))
                }
                // 맨 아래: 오늘의 한 줄 (기쁨도 슬픔도 실어 떠나보내기)
                Spacer(Modifier.height(Tokens.Space.sp6))
                CrayonRule(seed = 958)
                LetGoSection(state, now.toLocalDate())
            }
        }
        // 한 줄을 보낸 뒤: 깃털이 내려오며 한마디 창
        LetGoModal(state, Modifier.fillMaxSize())
    }

    open?.let { m ->
        ModalBottomSheet(onDismissRequest = { open = null }, containerColor = G.Colors.paper) {
            ItemSheet(m) { open = null }
        }
    }
}

/** 정원에 놓인 것 (최근 것부터). 개발자 모드의 ‘모두 미리 보기’면 전부. */
internal fun gardenMoments(state: AppState, profile: LifeProfile, s: LifeSnapshot, now: LocalDateTime): List<Moment> =
    if (state.previewAll) Moments.all(now.toLocalDate())
    else Moments.earned(state.store.startDate, profile.birthDate, s.expectancy, now.toLocalDate(), state.store.firstSkip, state.store.returned, state.streaks)

/** 오늘의 문장: 두 줄에 안 들어가면 글자를 한 단계씩 줄인다. 어절 단위로 줄을 바꾼다 (TokenText). */
@Composable
private fun QuoteText(text: String) {
    val steps = listOf(Tokens.TypeScale.headline, Tokens.TypeScale.callout, Tokens.TypeScale.subhead)
    var step by remember(text) { mutableStateOf(0) }
    var ready by remember(text) { mutableStateOf(false) }
    val last = step == steps.lastIndex
    TokenText(
        text, steps[step].serif(), Modifier.fillMaxWidth().graphicsLayer { alpha = if (ready) 1f else 0f }, align = TextAlign.Center,
        maxLines = if (last) Int.MAX_VALUE else 2,
        onTextLayout = { r -> if (r.hasVisualOverflow && !last) step++ else ready = true },
    )
}

internal fun objName(id: String) = when (id) {
    "moss" -> R.string.obj_moss; "teacup" -> R.string.obj_teacup; "cairn" -> R.string.obj_cairn; "pine" -> R.string.obj_pine
    "flower" -> R.string.obj_flower; "pond" -> R.string.obj_pond; "leaf" -> R.string.obj_leaf; "candle" -> R.string.obj_candle
    "dandelion" -> R.string.obj_dandelion; "feather" -> R.string.obj_feather; "snail" -> R.string.obj_snail
    "pinwheel" -> R.string.obj_pinwheel; "paperboat" -> R.string.obj_paperboat; "kite" -> R.string.obj_kite; else -> R.string.obj_acorn
}
internal fun objWhen(id: String) = when (id) {
    "moss" -> R.string.obj_moss_when; "teacup" -> R.string.obj_teacup_when; "cairn" -> R.string.obj_cairn_when; "pine" -> R.string.obj_pine_when
    "flower" -> R.string.obj_flower_when; "pond" -> R.string.obj_pond_when; "leaf" -> R.string.obj_leaf_when; "candle" -> R.string.obj_candle_when
    "dandelion" -> R.string.obj_dandelion_when; "feather" -> R.string.obj_feather_when; "snail" -> R.string.obj_snail_when
    "pinwheel" -> R.string.obj_pinwheel_when; "paperboat" -> R.string.obj_paperboat_when; "kite" -> R.string.obj_kite_when; else -> R.string.obj_acorn_when
}
internal fun objLine(id: String) = when (id) {
    "moss" -> R.string.obj_moss_line; "teacup" -> R.string.obj_teacup_line; "cairn" -> R.string.obj_cairn_line; "pine" -> R.string.obj_pine_line
    "flower" -> R.string.obj_flower_line; "pond" -> R.string.obj_pond_line; "leaf" -> R.string.obj_leaf_line; "candle" -> R.string.obj_candle_line
    "dandelion" -> R.string.obj_dandelion_line; "feather" -> R.string.obj_feather_line; "snail" -> R.string.obj_snail_line
    "pinwheel" -> R.string.obj_pinwheel_line; "paperboat" -> R.string.obj_paperboat_line; "kite" -> R.string.obj_kite_line; else -> R.string.obj_acorn_line
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
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(Tokens.Space.sp10))
            TokenText(stringResource(R.string.garden_meet_title), Tokens.TypeScale.title2.serif(), align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp2))
            TokenText(stringResource(R.string.garden_meet_sub), Tokens.TypeScale.subhead, color = p.secondary, align = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            val scale = u * (G.Layout.meetHaruWidth / G.Layout.haruArtWidth)
            Box(Modifier.fillMaxWidth().height(u * G.Layout.haruBox * (G.Layout.meetHaruWidth / G.Layout.haruArtWidth) * 0.8f), contentAlignment = Alignment.BottomCenter) {
                Image(GardenArt.strip(ctx, Season.SPRING), null, Modifier.offset(y = u * (G.Layout.stripHeight - G.Layout.stripLineY)).width(screenW).height(u * G.Layout.stripHeight), contentScale = ContentScale.FillBounds)
                if (load.loading) TokenText(stringResource(R.string.garden_drawing), Tokens.TypeScale.footnote, Modifier.padding(bottom = Tokens.Space.sp8), color = p.secondary)
                else Box(Modifier.offset(y = scale * (G.Layout.haruBox - G.Layout.haruGround))) {
                    Haru(load, scale, Modifier)
                    art?.let { Sparkles(it, scale) }
                }
            }
            Spacer(Modifier.height(Tokens.Space.sp6))
            art?.let { TokenText("${Labels.stone(ctx, it.meta.stone)} · ${stringResource(R.string.garden_no, haruNo(state.store.haruSeed))}", Tokens.TypeScale.caption1, color = p.secondary) }
            Spacer(Modifier.weight(1f))
            GardenButton(stringResource(R.string.garden_meet_go), onDone, filled = true, seed = 750)
            Spacer(Modifier.height(Tokens.Space.sp6))
        }
    }
}

@Composable
private fun Sparkles(art: HaruArt, scale: Dp) {
    val ctx = LocalContext.current
    val img: ImageBitmap = GardenArt.sparkle(ctx)
    val bb = art.meta.bbox; val s = G.Layout.sparkle
    listOf(Triple(bb.left - s * 1.4f, bb.top + s, s), Triple(bb.right + s * 1.1f, bb.top - s * 0.2f, s * 0.8f)).forEach { (x, y, r) ->
        Image(img, null, Modifier.offset(scale * (x - r), scale * (y - r)).size(scale * (r * 2)))
    }
}

/** 하루 번호를 사람이 읽는 꼴로 (예: 2A61·F07C). */
fun haruNo(seed: Long): String { val h = seed.toString(16).uppercase().padStart(8, '0').takeLast(8); return h.take(4) + "·" + h.drop(4) }

// ───────────────────────── 작은 부품 ─────────────────────────

@Composable
fun GardenChip(text: String, selected: Boolean, seed: Int, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onClick), contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.crayonBox(if (selected) G.Colors.chip else null, G.Radius.chip, G.Stroke.chip, seed).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1)) {
            TokenText(text, Tokens.TypeScale.subhead, weight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
fun GardenButton(text: String, onClick: () -> Unit, filled: Boolean, seed: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp2).crayonBox(if (filled) G.Colors.button else null, G.Radius.button, G.Stroke.box, seed).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { TokenText(text, Tokens.TypeScale.headline) }
}
