package io.github.graviton94.carpediem.ui.garden

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.Moment
import io.github.graviton94.carpediem.core.GardenDecor
import io.github.graviton94.carpediem.core.SeasonCard
import androidx.compose.foundation.background
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.SkyBackground
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** 정원 쪽 화면의 위 줄: 돌아가기 + 제목 (가운데). */
@Composable
internal fun PageBar(title: String, onBack: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    Box(Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget), contentAlignment = Alignment.Center) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).semantics { contentDescription = ctx.getString(R.string.back) }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = p.secondary)
        }
        TokenText(title, Tokens.TypeScale.headline)
    }
}

// ───────────────────────── 모은 것 ─────────────────────────

/**
 * 정원에 놓인 것을 한곳에서. 받은 것만 보인다 (빈칸 · 개수 · 남은 것 목록은 없다 — 모으는 놀이가 아니라 지나온 날의 흔적).
 * 누르면 정원에서와 같은 한 장 (생긴 날과 한 줄).
 */
@Composable
fun CollectionScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, onMemory: () -> Unit = {}, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    SkyBackground {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp10),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4),
        ) {
            PageBar(stringResource(R.string.collection), onBack)
            CollectionBody(state, profile, now, onMemory)
        }
    }
}

/**
 * 추억 페이지 (와 모은 것 화면): 세 묶음.
 * ① 모은 것: 계절 조각 · 정원에 자란 것 · 만난 손님과 순간 · 첫 정원의 꾸밈을 한 판에 (그림 칸, 받은 것만)
 * ② 미래의 나에게: 항아리에 묻는 편지
 * ③ 돌아보기: 한 해를 한 장으로 · 엔딩 크레딧 · 나이테 · 핀 씨앗 · 받은 편지 · 고마움 책 · 지난 정원 · 기억의 자리
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionBody(state: AppState, profile: LifeProfile, now: LocalDateTime, onMemory: () -> Unit, guide: GuideTargets? = null, onCredits: (Int) -> Unit = {}) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val moments = gardenMoments(state, profile, s, now)
    var open by remember { mutableStateOf<Moment?>(null) }
    var card by remember { mutableStateOf<Pair<SeasonCard, GardenDecor.SeasonLines?>?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
            TokenText(stringResource(R.string.collection_sub), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary)
            // ── ① 모은 것: 한 판 (같은 크기 칸, 최근 것부터 여섯, 나머지는 펼쳐서). 칸을 누르면 설명과 얻는 방법 ──
            val today = now.toLocalDate()
            val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            val earlier = stringResource(R.string.collect_before)
            fun dateText(d: java.time.LocalDate?) = d?.format(fmt) ?: earlier
            val tiles = ArrayList<Tile>()
            // 계절 조각
            val here = remember(today) { state.decor(profile, s, today).card }
            val cards = (if (state.previewAll) io.github.graviton94.carpediem.core.Season.entries.map { SeasonCard(here.year, it, here.tree) } else state.seasonCards.mapNotNull { SeasonCard.parse(it) })
                .distinctBy { it.year to it.season }
            val stats = remember(state.lines, profile.countryCode) { GardenDecor.seasonLines(state.lines, profile.countryCode) }
            cards.forEach { c ->
                val l = stats[c.year to c.season]
                val whenText = stringResource(R.string.album_cell, "${c.year}", io.github.graviton94.carpediem.ui.Labels.season(ctx, c.season))
                val name = cardName(ctx, c.key)
                val art: @Composable () -> Unit = { Image(GardenArt.card(ctx, c.key), null, Modifier.fillMaxSize()) }
                val start = java.time.LocalDate.of(c.year, when (c.season) { io.github.graviton94.carpediem.core.Season.SPRING -> 3; io.github.graviton94.carpediem.core.Season.SUMMER -> 6; io.github.graviton94.carpediem.core.Season.AUTUMN -> 9; else -> 12 }, 1)
                tiles += Tile(art, name, whenText, start, Detail(art, name,
                    stringResource(R.string.collect_kindCard) + " · " + whenText + (l?.count?.takeIf { it > 0 }?.let { " · " + stringResource(R.string.album_lines, "$it") } ?: ""),
                    stringResource(R.string.collect_descCard), stringResource(R.string.collect_how), stringResource(R.string.collect_howCard)))
            }
            // 정원에 자란 것 (지금 정원에 있는 것만, 처음 보인 날)
            val d = remember(today, state.lines.size, state.breaths.size) { state.decor(profile, s, today) }
            val sk = d.season.name.lowercase()
            val dates = state.decorDates
            val grownKind = stringResource(R.string.collect_kindGrown)
            val how = stringResource(R.string.collect_how)
            if (d.stage > 0) {
                val art: @Composable () -> Unit = { Image(GardenArt.image(ctx, "tree_${d.tree.key}_${sk}_${d.stage.coerceIn(0, 3)}.webp"), null, Modifier.fillMaxSize()) }
                val name = stringResource(treeName(d.tree))
                val steps = listOf(G.Decor.stageDays1, G.Decor.stageDays2, G.Decor.stageDays3).mapIndexed { i, n -> stringResource(R.string.collect_chipDays, "${n.toInt()}") to (d.stage > i) }
                tiles += Tile(art, name, dateText(dates["stage"]), dates["stage"], Detail(art, name, grownKind + " · " + dateText(dates["stage"]),
                    stringResource(R.string.collect_descTree), how, stringResource(R.string.collect_howTree), chips = steps.filterIndexed { i, _ -> i < d.stage }))
            }
            if (d.hang != io.github.graviton94.carpediem.core.Hang.NONE) {
                val art: @Composable () -> Unit = { Box(Modifier.fillMaxSize()) {
                    Image(GardenArt.image(ctx, "post_$sk.webp"), null, Modifier.fillMaxSize())
                    Image(GardenArt.image(ctx, "post_${d.hang.name.lowercase()}.webp"), null, Modifier.fillMaxSize())
                } }
                val names = listOf(R.string.collect_chime, R.string.collect_bell, R.string.collect_lantern).map { stringResource(it) }
                val name = names[d.hang.ordinal - 1]
                val steps = listOf(G.Decor.chimeBreaths, G.Decor.bellBreaths, G.Decor.lanternBreaths).mapIndexed { i, n -> stringResource(R.string.collect_chipHang, "${n.toInt()}", names[i]) to (i == d.hang.ordinal - 1) }
                tiles += Tile(art, name, dateText(dates["hang"]), dates["hang"], Detail(art, name, stringResource(R.string.collect_kindHang) + " · " + dateText(dates["hang"]),
                    stringResource(listOf(R.string.collect_descChime, R.string.collect_descBell, R.string.collect_descLantern)[d.hang.ordinal - 1]), how, stringResource(R.string.collect_howHang),
                    chips = steps.take(d.hang.ordinal)))
            }
            if (d.kite) {
                val art: @Composable () -> Unit = { Image(GardenArt.image(ctx, "kite.webp"), null, Modifier.fillMaxSize(0.95f)) }
                val name = stringResource(R.string.collect_kite)
                tiles += Tile(art, name, dateText(dates["kite"]), dates["kite"], Detail(art, name, grownKind + " · " + dateText(dates["kite"]),
                    stringResource(R.string.collect_descKite), how, stringResource(R.string.collect_howKite, "${G.Decor.kiteLines.toInt()}")))
            }
            if (d.ribbons.isNotEmpty()) {
                val art: @Composable () -> Unit = {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize(0.7f)) {
                        val n = d.ribbons.size.coerceAtMost(8); val gap = size.width / (n + 1)
                        d.ribbons.take(8).forEachIndexed { i, f ->
                            val x = gap * (i + 1)
                            drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(x, size.height * 0.1f); quadraticTo(x - gap * 0.6f, size.height * 0.5f, x - gap * 0.2f + (i % 2) * gap * 0.3f, size.height * 0.9f) },
                                feelingColor(f), style = androidx.compose.ui.graphics.drawscope.Stroke(size.width * 0.045f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                    }
                }
                val name = stringResource(R.string.collect_ribbon, "${d.ribbons.size}")
                tiles += Tile(art, name, dateText(dates["ribbon"]), dates["ribbon"], Detail(art, name, grownKind + " · " + dateText(dates["ribbon"]),
                    stringResource(R.string.collect_descRibbon), how, stringResource(R.string.collect_howRibbon, "${G.Decor.ribbonLines.toInt()}", "${G.Decor.ribbonMax.toInt()}")))
            }
            if (d.buds > 0) {
                val art: @Composable () -> Unit = { Image(GardenArt.image(ctx, "moss_${sk}_${d.buds.coerceIn(0, 5)}.webp"), null, Modifier.fillMaxSize()) }
                val name = stringResource(R.string.collect_bud, "${d.buds}")
                tiles += Tile(art, name, dateText(dates["bud"]), dates["bud"], Detail(art, name, grownKind + " · " + dateText(dates["bud"]),
                    stringResource(R.string.collect_descBud), how, stringResource(R.string.collect_howBud, "${G.Decor.budGazes.toInt()}", "${G.Decor.budMax.toInt()}")))
            }
            // 만난 손님 · 순간 (처음 만난 날, 손님은 온 번수도)
            val met = state.chancesMet.mapNotNull { r -> r.split(':', limit = 2).takeIf { it.size == 2 }?.let { (k, d2) -> runCatching { k to java.time.LocalDate.parse(d2) }.getOrNull() } }
                .sortedBy { it.second }.distinctBy { it.first }
            met.forEach { (k, day) ->
                val name = ctx.resources.getIdentifier("chance_$k", "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) } ?: k
                fun res(n: String) = ctx.resources.getIdentifier(n, "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) }.orEmpty()
                val guest = k.startsWith("guest_")
                val art: @Composable () -> Unit = { MetArt(k, sk) }
                val visits = state.guestVisits[k.removePrefix("guest_")]?.takeIf { guest && it > 1 }
                val sub = listOfNotNull(stringResource(R.string.collect_firstMet, day.format(fmt)), visits?.let { stringResource(R.string.collect_visits, "$it") }).joinToString(" · ")
                tiles += Tile(art, name, day.format(fmt), day, Detail(art, name, sub, res("collect_desc_$k"),
                    stringResource(if (guest) R.string.collect_meet else R.string.collect_how),
                    if (guest) stringResource(R.string.collect_howGuest) else res("collect_how_$k"),
                    note = if (guest) stringResource(R.string.collect_howGuestRare) else null))
            }
            // 첫 정원 (옛 꾸밈): 원래의 한 장 (생긴 날 · 그날의 한 줄)
            moments.filter { state.previewAll || it.date.isBefore(io.github.graviton94.carpediem.core.Moments.LEGACY_UNTIL) }.forEach { m ->
                tiles += Tile({ Image(GardenArt.obj(ctx, m.id), null, Modifier.fillMaxSize()) }, stringResource(objName(m.id)), m.date.format(fmt), m.date, null) { open = m }
            }
            val sorted = tiles.sortedByDescending { it.date ?: java.time.LocalDate.MIN }
            var all by rememberSaveable { mutableStateOf(false) }
            var detail by remember { mutableStateOf<Detail?>(null) }
            val shown = if (all) sorted else sorted.take(COLLECT_FIRST)
            Column(Modifier.fillMaxWidth().guideTarget(guide, "mem.album"), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                if (sorted.isEmpty()) TokenText(stringResource(R.string.collection_empty), Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth().padding(vertical = Tokens.Space.sp4), color = p.secondary, align = TextAlign.Center)
                else TileGrid(shown.size) { i -> val t = shown[i]; CollectionTile(900 + i, t.art, t.title, t.dateText, t.onClick ?: { detail = t.detail }) }
                if (sorted.size > COLLECT_FIRST) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GardenChip(if (all) stringResource(R.string.collect_less) else stringResource(R.string.collect_more, "${sorted.size}"), false, 899) { all = !all }
                }
            }
            detail?.let { dt -> CollectDetailSheet(dt) { detail = null } }
            // ── ② 미래의 나에게 ──
            KeepsakesSection(state, profile, now, onCredits, KeepPart.FUTURE)
            // ── ③ 돌아보기 ──
            Spacer(Modifier.height(Tokens.Space.sp4))
            TokenText(stringResource(R.string.lookback_title), Tokens.TypeScale.title3)
            TokenText(stringResource(R.string.lookback_sub), Tokens.TypeScale.footnote, color = p.secondary)
            var yearSheet by remember { mutableStateOf<Int?>(null) }
            Box(Modifier.guideTarget(guide, "mem.year")) { GardenYearAlbum(state) { yearSheet = it } }
            yearSheet?.let { y -> GardenYearSheet(state, profile, now, y) { yearSheet = null } }
            KeepsakesSection(state, profile, now, onCredits, KeepPart.LOOKBACK)
            BloomedSeeds(state)
            ReceivedLetters(state, now.toLocalDate())
            ThanksAndLetGo(state)
            PastGardens(state, now.toLocalDate())
            // 기억의 자리: 기억의 돌이 있을 때만 (앱이 먼저 권하지 않음)
            if (state.memories.isNotEmpty()) Row(
                Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4).heightIn(min = Tokens.Layout.tapTarget).crayonBox(null, G.Radius.box, G.Stroke.chip, 1160).clickable(onClick = onMemory)
                    .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
            ) {
                TokenText(stringResource(R.string.memory), Tokens.TypeScale.headline, Modifier.weight(1f))
                TokenText(stringResource(R.string.memory_sub), Tokens.TypeScale.footnote, color = p.secondary)
            }
    }
    open?.let { m ->
        ModalBottomSheet(onDismissRequest = { open = null }, containerColor = Theme.gc.paper, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) { ItemSheet(m) { open = null } }
    }
    card?.let { (c, l) ->
        ModalBottomSheet(onDismissRequest = { card = null }, containerColor = Theme.gc.paper, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp6), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                Image(GardenArt.card(ctx, c.key), null, Modifier.size(u * 160f * G.Decor.cardBoxW / G.Decor.cardBoxH, u * 160f))
                TokenText(cardName(ctx, c.key), Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
                TokenText(stringResource(R.string.album_when, "${c.year}", io.github.graviton94.carpediem.ui.Labels.season(ctx, c.season), stringResource(treeName(c.tree))), Tokens.TypeScale.body, align = TextAlign.Center)
                l?.count?.takeIf { it > 0 }?.let { n -> TokenText(stringResource(R.string.album_lines, "$n"), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center) }
            }
        }
    }
}

/**
 * 계절 앨범: 받은 계절 한 장을 해마다 한 줄 (최근 해부터), 봄 · 여름 · 가을 · 겨울 순. 받지 못한 계절은 빈 종이 한 장 (세지 않음, 아쉬워하지 않게 옅게).
 * 장마다 그 계절에 남긴 한 줄 수, 테두리는 가장 많았던 마음의 색.
 */
@Composable
private fun SeasonAlbum(state: AppState, profile: LifeProfile, now: LocalDateTime, onOpen: (Pair<SeasonCard, GardenDecor.SeasonLines?>) -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val today = now.toLocalDate()
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val here = state.decor(profile, s, today).card
    // 시험용 미리 보기: 지금 나무로 올해 네 장 모두
    val cards = (if (state.previewAll) io.github.graviton94.carpediem.core.Season.entries.map { SeasonCard(here.year, it, here.tree) } else state.seasonCards.mapNotNull { SeasonCard.parse(it) } + here)
        .distinctBy { it.year to it.season }
    val stats = remember(state.lines, profile.countryCode) { GardenDecor.seasonLines(state.lines, profile.countryCode) }
    TokenText(stringResource(R.string.album), Tokens.TypeScale.headline, Modifier.fillMaxWidth())
    cards.groupBy { it.year }.toSortedMap(compareByDescending { it }).forEach { (year, list) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically) {
            TokenText("$year", Tokens.TypeScale.footnote.serif(), Modifier.size(Theme.unit * 34f, Theme.unit * 20f), color = p.secondary)
            io.github.graviton94.carpediem.core.Season.entries.forEach { se ->
                val c = list.firstOrNull { it.season == se }
                val l = stats[year to se]
                // 화면 읽기: 칸 하나를 한 번에 (“2026년 봄, 벚꽃, 12줄”)
                val seName = io.github.graviton94.carpediem.ui.Labels.season(ctx, se)
                val cellA11y = listOfNotNull(stringResource(R.string.album_cell, "$year", seName), c?.let { cardName(ctx, it.key) }, l?.count?.takeIf { it > 0 && c != null }?.let { stringResource(R.string.album_lines, "$it") }).joinToString(", ")
                // 아직 오지 않은 계절은 더 옅게 (지나간 빈 칸과 구별)
                val ahead = year == here.year && se.ordinal > here.season.ordinal
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = cellA11y }.graphicsLayer { alpha = if (ahead) 0.45f else 1f },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                    if (c != null) {
                        Image(GardenArt.card(ctx, c.key), null, Modifier.fillMaxWidth().aspectRatio(G.Decor.cardBoxW / G.Decor.cardBoxH).clickable { onOpen(c to l) })
                        // 그 계절의 한 줄 수, 앞의 작은 점 = 가장 많았던 마음
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                            l?.feeling?.let { f -> Box(Modifier.size(Tokens.Space.sp1 + Tokens.Space.sp1 / 2).background(feelingColor(f), androidx.compose.foundation.shape.CircleShape)) }
                            // 0줄은 적지 않음
                            l?.count?.takeIf { it > 0 }?.let { n -> TokenText(stringResource(R.string.album_lines, "$n"), Tokens.TypeScale.caption2, color = p.secondary, align = TextAlign.Center) }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(G.Decor.cardBoxW / G.Decor.cardBoxH).crayonBox(null, G.Radius.box, G.Stroke.chip, seed = 1300 + year % 50 * 4 + se.ordinal))
                        TokenText(io.github.graviton94.carpediem.ui.Labels.season(ctx, se), Tokens.TypeScale.caption2, color = p.secondary.copy(alpha = 0.6f), align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ───────────────────────── 응원하기 ─────────────────────────

/**
 * 응원하기: 보상 없이, 한 번 결제 3단계 (docs/plan.md M3). 그림 가운데에 내 하루가 앉아 있다.
 * 결제는 Google Play (billing/Support.kt). Play 에 상품이 아직 없으면 누르면 시험판 안내가 나온다.
 */
@Composable
fun SupportScreen(state: AppState, now: LocalDateTime, onBack: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val load = haruArt(state, sprout = false)
    var soon by remember { mutableStateOf(false) }
    val support = remember { io.github.graviton94.carpediem.billing.Support(ctx) }
    androidx.compose.runtime.DisposableEffect(support) { support.connect(); onDispose { support.close() } }
    BackHandler(onBack = onBack)
    SkyBackground {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp10),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4),
        ) {
            PageBar(stringResource(R.string.support), onBack)
            val img = GardenArt.support(ctx)
            BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(img.width / img.height.toFloat()).crayonBox(null, G.Radius.box, G.Stroke.box, seed = 910)) {
                Image(img, null, Modifier.fillMaxSize().padding(G.Stroke.box.let { Theme.unit * it }), contentScale = ContentScale.Crop)
                val scale = maxWidth * (G.Layout.supportHaru / G.Layout.haruArtWidth)
                Haru(load, scale, Modifier.offset(maxWidth / 2 - scale * (G.Layout.haruBox / 2), maxHeight * G.Layout.supportGround - scale * G.Layout.haruGround))
            }
            TokenText(stringResource(R.string.support_title), Tokens.TypeScale.title3.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
            TokenText(stringResource(R.string.support_body), Tokens.TypeScale.callout, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
            listOf(
                Triple(R.string.support_tier1, R.string.support_tier1_price, "teacup"),
                Triple(R.string.support_tier2, R.string.support_tier2_price, "teacup"),
                Triple(R.string.support_tier3, R.string.support_tier3_price, "candle"),
            ).forEachIndexed { i, (name, price, obj) ->
                val id = io.github.graviton94.carpediem.billing.Support.IDS[i]
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp4)
                        .crayonBox(if (i == 1) Theme.gc.chip else null, G.Radius.button, G.Stroke.box, seed = 920 + i * 3).clickable {
                            // Play 에 상품이 있으면 결제 창, 없으면 시험판 안내
                            val act = ctx as? android.app.Activity
                            soon = act == null || !support.buy(act, id)
                        }
                        .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp2),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
                ) {
                    Image(GardenArt.obj(ctx, obj), null, Modifier.size(Theme.unit * G.Layout.collectionCell * 0.42f))
                    TokenText(stringResource(name), Tokens.TypeScale.headline, Modifier.weight(1f))
                    TokenText(support.prices[id]?.takeIf { it.isNotBlank() } ?: stringResource(price), Tokens.TypeScale.headline, color = p.secondary)
                }
            }
            if (support.thanked) Box(Modifier.fillMaxWidth().crayonBox(Theme.gc.chip, G.Radius.box, G.Stroke.chip, seed = 941).padding(Tokens.Space.sp4)) {
                TokenText(stringResource(R.string.support_thanks), Tokens.TypeScale.subhead.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
            else if (soon) Box(Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, seed = 940).padding(Tokens.Space.sp4)) {
                TokenText(stringResource(R.string.support_soon), Tokens.TypeScale.subhead, Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
            TokenText(stringResource(R.string.support_once), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        }
    }
}


/** 세 칸씩 놓는 판 (마지막 줄은 빈칸으로 채움). */
@Composable
private fun TileGrid(count: Int, tile: @Composable (Int) -> Unit) {
    val cols = 3
    (0 until count).chunked(cols).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            row.forEach { i -> Box(Modifier.weight(1f)) { tile(i) } }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** 모은 것 한 칸: 같은 높이. 그림 (정사각) · 이름 한 줄 (길면 …) · 날짜 한 줄, 오른쪽 위 (?) (누르면 상세). */
@Composable
private fun CollectionTile(seed: Int, art: @Composable () -> Unit, title: String, date: String, onClick: () -> Unit) {
    val u = Theme.unit
    val p = Theme.palette
    Box(Modifier.fillMaxWidth().height(u * COLLECT_TILE_H).crayonBox(null, G.Radius.box, G.Stroke.chip, seed).clickable(onClick = onClick)) {
        Column(Modifier.fillMaxSize().padding(horizontal = Tokens.Space.sp2, vertical = Tokens.Space.sp2), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            Box(Modifier.size(u * 64f), contentAlignment = Alignment.Center) { art() }
            TokenText(title, Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), weight = FontWeight.SemiBold, align = TextAlign.Center, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            TokenText(date, Tokens.TypeScale.caption2, color = p.secondary, align = TextAlign.Center, maxLines = 1)
        }
        // 크레용 테두리 안쪽으로 넉넉히 (가장자리에 붙지 않게)
        TokenText("?", Tokens.TypeScale.caption2, Modifier.align(Alignment.TopEnd).padding(top = Tokens.Space.sp2 + Tokens.Space.sp1, end = Tokens.Space.sp2 + Tokens.Space.sp1).size(u * 16f)
            .border(u * 1.2f, p.secondary.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape), color = p.secondary, align = TextAlign.Center)
    }
}

/** 만난 손님 · 순간의 그림 (정원에서 쓰는 그림 그대로). */
@Composable
private fun MetArt(key: String, season: String) {
    val ctx = LocalContext.current
    when {
        key.startsWith("guest_") -> Image(GardenArt.image(ctx, "${key}.webp"), null, Modifier.fillMaxSize(0.85f))
        key == "rainbow" -> Image(GardenArt.image(ctx, "moment_rainbow.webp"), null, Modifier.fillMaxSize())
        key == "aurora" -> Image(GardenArt.image(ctx, "moment_aurora.webp"), null, Modifier.fillMaxSize())
        key == "snail" -> Image(GardenArt.image(ctx, "moment_snail.webp"), null, Modifier.fillMaxSize(0.8f))
        key == "butterflies" -> Box(Modifier.fillMaxSize(0.7f)) {
            listOf("fly_wing_l", "fly_wing_r", "fly_body").forEach { n -> Image(GardenArt.image(ctx, "$n.webp"), null, Modifier.fillMaxSize()) }
        }
        key == "wind" -> Image(GardenArt.image(ctx, "wind_$season.webp"), null, Modifier.fillMaxSize(0.5f))
        else -> androidx.compose.foundation.Canvas(Modifier.fillMaxSize(0.7f)) {
            val r = size.minDimension
            if (key == "bubbles") listOf(Offset(0.35f, 0.6f) to 0.2f, Offset(0.65f, 0.4f) to 0.14f, Offset(0.55f, 0.75f) to 0.1f).forEach { (o, k) ->
                drawCircle(Color(0xFF9DB9C9), r * k, Offset(o.x * r, o.y * r), style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.02f)) }
            else listOf(Offset(0.3f, 0.5f), Offset(0.6f, 0.35f), Offset(0.7f, 0.7f), Offset(0.45f, 0.75f)).forEach { o ->
                drawCircle(Brush.radialGradient(listOf(Color(0xCCF6EC96), Color(0x00F6EC96)), Offset(o.x * r, o.y * r), r * 0.12f), r * 0.12f, Offset(o.x * r, o.y * r)) }
        }
    }
}

/** 정원에 자란 것 한 칸: 그림과 생겼을 때의 한 줄. */
private class Grown(val caption: Int, val art: @Composable () -> Unit)

/** 모은 것 한 칸: 그림 · 이름 · 날짜, 누르면 상세 (옛 꾸밈은 원래의 한 장). date = 줄 세우는 날 (없으면 맨 뒤). */
private class Tile(val art: @Composable () -> Unit, val title: String, val dateText: String, val date: java.time.LocalDate?, val detail: Detail?, val onClick: (() -> Unit)? = null)

/** (?) 상세: 큰 그림 · 이름 · 종류와 날짜 · 설명 · 얻는 방법 (단계가 있으면 지나온 단계 칩, 지금 것만 진하게). 아직 얻지 않은 것은 보이지 않음. */
private class Detail(val art: @Composable () -> Unit, val name: String, val sub: String, val desc: String, val howTitle: String, val how: String,
                     val chips: List<Pair<String, Boolean>> = emptyList(), val note: String? = null)

private const val COLLECT_FIRST = 6
private const val COLLECT_TILE_H = 120f

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CollectDetailSheet(d: Detail, onClose: () -> Unit) {
    val p = Theme.palette
    val u = Theme.unit
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Theme.gc.paper, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Box(Modifier.size(u * 120f), contentAlignment = Alignment.Center) { d.art() }
            TokenText(d.name, Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
            TokenText(d.sub, Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
            if (d.desc.isNotEmpty()) TokenText(d.desc, Tokens.TypeScale.callout, Modifier.fillMaxWidth().padding(top = Tokens.Space.sp2), align = TextAlign.Center)
            CrayonRule(Modifier.padding(vertical = Tokens.Space.sp2), seed = 1730)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                TokenText(d.howTitle, Tokens.TypeScale.footnote, color = p.olive, weight = FontWeight.SemiBold)
                if (d.how.isNotEmpty()) TokenText(d.how, Tokens.TypeScale.subhead)
                d.note?.let { TokenText(it, Tokens.TypeScale.footnote, color = p.secondary) }
                if (d.chips.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(Modifier.padding(top = Tokens.Space.sp1), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                    d.chips.forEachIndexed { i, (label, now) -> GardenChip(label, now, 1740 + i) {} }
                }
            }
            GardenButton(stringResource(R.string.collect_close), onClose, filled = false, seed = 1750, modifier = Modifier.fillMaxWidth().padding(top = Tokens.Space.sp3))
        }
    }
}
