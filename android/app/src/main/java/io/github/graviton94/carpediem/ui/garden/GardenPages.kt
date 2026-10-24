package io.github.graviton94.carpediem.ui.garden

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.animateFloat
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
            CollectionBody(state, profile, now, onMemory, part = CollectPart.GRID)
        }
    }
}

/**
 * 추억 페이지 (와 모은 것 화면): 세 묶음.
 * ① 모은 것: 계절 조각 · 정원에 자란 것 · 만난 손님과 순간 · 별자리 · 절기 · 첫 정원의 꾸밈을 묶음별로 (그림 칸, ‘모두’는 받은 것만)
 * ② 미래의 나에게: 항아리에 묻는 편지
 * ③ 돌아보기: 한 해를 한 장으로 · 엔딩 크레딧 · 나이테 · 핀 씨앗 · 받은 편지 · 기억의 자리 (지난 해의 한 줄은 기록 페이지에서)
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CollectionBody(state: AppState, profile: LifeProfile, now: LocalDateTime, onMemory: () -> Unit, guide: GuideTargets? = null, onCredits: (Int) -> Unit = {}, part: CollectPart = CollectPart.GRID) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    val moments = gardenMoments(state, profile, s, now)
    var open by remember { mutableStateOf<Moment?>(null) }
    var card by remember { mutableStateOf<Pair<SeasonCard, GardenDecor.SeasonLines?>?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
            // 1.1.11: 모은 것 화면 = 모은 것 판만 (GRID), 추억 = 간직한 문장 · 돌아보기 (REST). 미래의 나에게는 기록 장으로 옮김
            if (part == CollectPart.GRID) {
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
                // 이름이 없는 것 (다른 버전의 기록에서 온 모르는 손님 · 순간) 은 내부 이름을 보이지 않고 건너뜀
                val name = ctx.resources.getIdentifier("chance_$k", "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) } ?: return@forEach
                fun res(n: String) = ctx.resources.getIdentifier(n, "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) }.orEmpty()
                val guest = k.startsWith("guest_")
                val art: @Composable () -> Unit = { MetArt(k, sk) }
                val visits = state.guestVisits[k.removePrefix("guest_")]?.takeIf { guest && it > 1 }
                val sub = listOfNotNull(stringResource(R.string.collect_firstMet, day.format(fmt)), visits?.let { stringResource(R.string.collect_visits, "$it") }).joinToString(" · ")
                tiles += Tile(art, name, day.format(fmt), day, Detail(art, name, sub, res("collect_desc_$k"),
                    stringResource(if (guest) R.string.collect_meet else R.string.collect_how),
                    if (guest) stringResource(R.string.collect_howGuest) else res("collect_how_$k"),
                    note = if (guest) stringResource(R.string.collect_howGuestRare) else null), kind = Shelf.MET)
            }
            // 아직 못 만난 것 (1.1.5): 옅은 실루엣 + ‘언젠가’, 누르면 분위기 힌트 한 줄만 (방법 · 진행률은 알려주지 않음). 늘 맨 뒤에
            val metKeys = met.map { it.first }.toSet()
            val ghostTitle = stringResource(R.string.collect_ghostTitle); val ghostSub = stringResource(R.string.collect_ghostSub); val ghostName = stringResource(R.string.collect_ghost)
            fun hint(n: String) = ctx.resources.getIdentifier(n, "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) }.orEmpty()
            GHOSTS.filter { it !in metKeys }.forEach { k ->
                val art: @Composable () -> Unit = { GhostArt { MetArt(k, sk) } }
                tiles += Tile(art, ghostName, "", null, Detail(art, ghostTitle, ghostSub, hint("collect_hint_$k"), "", ""), ghost = true, kind = Shelf.MET)
            }
            val hangHint = stringResource(R.string.collect_hint_hang)
            for ((i, part) in listOf("chime", "bell", "lantern").withIndex()) if (d.hang.ordinal <= i) {
                val art: @Composable () -> Unit = { GhostArt { Box(Modifier.fillMaxSize()) { Image(GardenArt.image(ctx, "post_$sk.webp"), null, Modifier.fillMaxSize()); Image(GardenArt.image(ctx, "post_$part.webp"), null, Modifier.fillMaxSize()) } } }
                tiles += Tile(art, ghostName, "", null, Detail(art, ghostTitle, ghostSub, hangHint, "", ""), ghost = true)
            }
            // 하루가 준 조약돌: 받은 날마다 한 장
            state.pebbles.forEach { d ->
                val art: @Composable () -> Unit = { PebbleArt() }
                val nm = stringResource(R.string.pebble_name)
                tiles += Tile(art, nm, d.format(fmt), d, Detail(art, nm, stringResource(R.string.collect_firstMet, d.format(fmt)), stringResource(R.string.pebble_desc),
                    stringResource(R.string.collect_how), stringResource(R.string.pebble_how)))
            }
            // 첫 정원 (옛 꾸밈): 원래의 한 장 (생긴 날 · 그날의 한 줄)
            moments.filter { state.previewAll || it.date.isBefore(io.github.graviton94.carpediem.core.Moments.LEGACY_UNTIL) }.forEach { m ->
                tiles += Tile({ Image(GardenArt.obj(ctx, m.id), null, Modifier.fillMaxSize()) }, stringResource(objName(m.id)), m.date.format(fmt), m.date, null) { open = m }
            }
            // 별자리 (1.1.6): 한 달에 한 줄을 남긴 날이 열흘이 되면 그 달의 별자리. 못 모은 것은 이름과 함께 옅게
            val lang = io.github.graviton94.carpediem.data.Words.lang(ctx)
            val clear = remember(state.lines) { io.github.graviton94.carpediem.core.Constellations.clearMonths(state.lines, STAR_DAYS) }
            val starsDesc = stringResource(R.string.collect_descStars, "$STAR_DAYS"); val starsHow = stringResource(R.string.collect_howStars, "$STAR_DAYS")
            val starsKind = stringResource(R.string.collect_kindStars)
            if (state.keepLines) for (m in 1..12) {
                val c = state.store.constellations.of(m) ?: continue
                val name = c.name(lang)
                val got = clear[m]
                val art: @Composable () -> Unit = { val img: @Composable () -> Unit = { Image(GardenArt.card(ctx, "stars_$m"), null, Modifier.fillMaxSize()) }; if (got != null) img() else GhostArt(img) }
                val whenText = RecordText.month(ctx, m)
                tiles += if (got != null) Tile(art, name, got.format(fmt), got, Detail(art, name, starsKind + " · " + whenText + " · " + got.format(fmt), starsDesc, how, starsHow), kind = Shelf.STARS)
                    else Tile(art, name, whenText, null, Detail(art, name, starsKind + " · " + whenText, starsDesc, how, starsHow), ghost = true, kind = Shelf.STARS)
            }
            // 절기 (1.1.6): 절기가 든 날 정원에 온 날. 이름은 처음부터 보이고, 만난 것만 진하게 (남반구 나라는 없음)
            val terms = remember(state.lines, state.breaths, state.gazeDays, today) { state.termsMet() }
            val south = profile.countryCode.uppercase() in GardenDecor.SOUTH
            val termKind = stringResource(R.string.collect_kindTerm); val termHow = stringResource(R.string.collect_howTerm)
            if (!south) io.github.graviton94.carpediem.core.SolarTerm.entries.forEach { t ->
                fun res(n: String) = ctx.resources.getIdentifier(n, "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) }.orEmpty()
                val name = res("termName_${t.key}")
                val got = terms[t]
                val art: @Composable () -> Unit = { val img: @Composable () -> Unit = { Image(GardenArt.card(ctx, "term_${t.key}"), null, Modifier.fillMaxSize()) }; if (got != null) img() else GhostArt(img) }
                tiles += if (got != null) Tile(art, name, got.format(fmt), got, Detail(art, name, termKind + " · " + got.format(fmt), res("term_${t.key}"), how, termHow), kind = Shelf.TERMS)
                    else Tile(art, name, "", null, Detail(art, name, termKind, "", how, termHow), ghost = true, kind = Shelf.TERMS)
            }
            // 묶음: 모두는 모은 것만 (못 만난 것으로 판이 가득 차지 않게), 묶음마다 모은 수 / 모두
            var shelf by rememberSaveable { mutableStateOf(Shelf.ALL) }
            val sorted = tiles.filter { if (shelf == Shelf.ALL) !it.ghost else it.kind == shelf }
                .sortedWith(if (shelf == Shelf.STARS || shelf == Shelf.TERMS) compareBy<Tile> { 0 } else compareBy<Tile> { it.ghost }.thenByDescending { it.date ?: java.time.LocalDate.MIN })
            var all by rememberSaveable { mutableStateOf(false) }
            var detail by remember { mutableStateOf<Detail?>(null) }
            val shown = if (all) sorted else sorted.take(COLLECT_FIRST)
            Column(Modifier.fillMaxWidth().guideTarget(guide, "mem.album"), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    fun count(k: Shelf) = tiles.count { it.kind == k && !it.ghost } to tiles.count { it.kind == k }
                    val shelves = listOfNotNull(Shelf.ALL, Shelf.GARDEN, Shelf.MET, Shelf.STARS.takeIf { state.keepLines }, Shelf.TERMS.takeIf { !south })
                    shelves.forEachIndexed { i, k ->
                        val (got, total) = count(k)
                        val label = when (k) {
                            Shelf.ALL -> stringResource(R.string.collect_shelfAll, "${tiles.count { !it.ghost }}")
                            Shelf.GARDEN -> stringResource(R.string.collect_shelfGarden, "$got")
                            Shelf.MET -> stringResource(R.string.collect_shelfMet, "$got", "$total")
                            Shelf.STARS -> stringResource(R.string.collect_shelfStars, "$got", "$total")
                            Shelf.TERMS -> stringResource(R.string.collect_shelfTerms, "$got", "$total")
                        }
                        GardenChip(label, shelf == k, 880 + i) { shelf = k; all = false }
                    }
                }
                if (sorted.isEmpty()) TokenText(stringResource(R.string.collection_empty), Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth().padding(vertical = Tokens.Space.sp4), color = p.secondary, align = TextAlign.Center)
                else TileGrid(shown.size) { i -> val t = shown[i]; CollectionTile(900 + i, t.art, t.title, t.dateText, t.onClick ?: { detail = t.detail }, ghost = t.ghost) }
                if (sorted.size > COLLECT_FIRST) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    GardenChip(if (all) stringResource(R.string.collect_less) else stringResource(R.string.collect_more, "${sorted.size}"), false, 899) { all = !all }
                }
            }
            detail?.let { dt -> CollectDetailSheet(dt) { detail = null } }
            } else {
            // ── 간직한 문장 (1.1.5): 정원의 문장을 길게 눌러 모은 것. 누르면 오늘 정원에 다시, 길게 누르면 뺌 ──
            KeptQuotes(state)
            // ── ③ 돌아보기 ──
            Spacer(Modifier.height(Tokens.Space.sp4))
            Column(Modifier.fillMaxWidth().guideTarget(guide, "mem.look"), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                TokenText(stringResource(R.string.lookback_title), Tokens.TypeScale.title3)
                TokenText(stringResource(R.string.lookback_sub), Tokens.TypeScale.footnote, color = p.secondary)
            }
            var yearSheet by remember { mutableStateOf<Int?>(null) }
            GardenYearAlbum(state, profile, now) { yearSheet = it }
            yearSheet?.let { y -> RecapShare(state, y, null, now.toLocalDate(), Theme.gc.night) { yearSheet = null } }
            KeepsakesSection(state, profile, now, onCredits, KeepPart.LOOKBACK)
            BloomedSeeds(state)
            ReceivedLetters(state, now.toLocalDate())
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
    }
    open?.let { m ->
        GardenSheet(onDismissRequest = { open = null }, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) { ItemSheet(m) { open = null } }
    }
    card?.let { (c, l) ->
        GardenSheet(onDismissRequest = { card = null }, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp6), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                Image(GardenArt.card(ctx, c.key), null, Modifier.size(u * 160f * G.Decor.cardBoxW / G.Decor.cardBoxH, u * 160f))
                TokenText(cardName(ctx, c.key), Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
                TokenText(stringResource(R.string.album_when, "${c.year}", io.github.graviton94.carpediem.ui.Labels.season(ctx, c.season), stringResource(treeName(c.tree))), Tokens.TypeScale.body, align = TextAlign.Center)
                l?.count?.takeIf { it > 0 }?.let { n -> TokenText(stringResource(R.string.album_lines, "$n"), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center) }
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
    androidx.compose.runtime.DisposableEffect(support) { support.onSupported = { id -> state.supported(id, now.toLocalDate()) }; support.onReady = { state.shopOpened() }; support.connect(); onDispose { support.onSupported = null; support.close() } }
    BackHandler(onBack = onBack)
    SkyBackground {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp10),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4),
        ) {
            PageBar(stringResource(R.string.support), onBack)
            // 고르고 → 아래 버튼으로 결제 (누르자마자 결제 창이 뜨지 않게). 처음엔 가운데 (커피)
            var picked by rememberSaveable { mutableStateOf(1) }
            val img = GardenArt.support(ctx)
            // 미리 놓아 보기 (1.1.5): 고른 응원이 하루 왼쪽 발치에 놓인 모습 (아직 놓인 건 아니라 살짝 반짝임)
            val shimmer = androidx.compose.animation.core.rememberInfiniteTransition(label = "preview").animateFloat(PREVIEW_ALPHA_LOW, 1f,
                androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(PREVIEW_PULSE_MS), androidx.compose.animation.core.RepeatMode.Reverse), label = "previewAlpha")
            BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(img.width / img.height.toFloat()).graphicsLayer().crayonBox(null, G.Radius.box, G.Stroke.box, seed = 910)) {
                Image(img, null, Modifier.fillMaxSize().padding(G.Stroke.box.let { Theme.unit * it }), contentScale = ContentScale.Crop)
                val scale = maxWidth * (G.Layout.supportHaru / G.Layout.haruArtWidth)
                val groundY = maxHeight * G.Layout.supportGround
                // 하루는 따로 한 겹: 깜빡임 · 숨이 그림 상자 (한지 테 · 큰 그림) 를 매 장면 다시 그리지 않게 (1.1.8, 버벅임)
                Haru(load, scale, Modifier.offset(maxWidth / 2 - scale * (G.Layout.haruBox / 2), groundY - scale * G.Layout.haruGround).graphicsLayer())
                val pw = maxWidth * PREVIEW_PIECE
                val haruHalf = scale * ((load.art?.meta?.bbox?.width ?: G.Layout.haruBox) / 2)
                val pieceImg = remember(picked) { GardenArt.card(ctx, when (picked) { 0 -> "tea"; 1 -> "coffee"; else -> "cake" }) }
                PieceImage(pieceImg, pw, Modifier.offset(maxWidth / 2 - haruHalf - pw * 0.55f, groundY - pw * 0.8f).graphicsLayer { alpha = shimmer.value })
            }
            // 미리 보기 한 줄: 처음이면 내 하루 곁에, 이미 같은 응원을 했으면 다른 돌 곁에도 하나 더 둘 수 있다고
            val pickedId = io.github.graviton94.carpediem.billing.Support.IDS[picked]
            val itemName = stringResource(when (picked) { 0 -> R.string.support_mark1; 1 -> R.string.support_mark2; else -> R.string.support_mark3 })
            val other = state.people.firstOrNull()?.name
            TokenText(if (state.supportCount(pickedId) > 0 && other != null) stringResource(R.string.support_previewMore, other, itemName) else stringResource(R.string.support_preview, itemName),
                Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
            TokenText(stringResource(R.string.support_title), Tokens.TypeScale.title3.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
            TokenText(stringResource(R.string.support_body), Tokens.TypeScale.callout, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
            val tiers = listOf(
                Triple(R.string.support_tier1, R.string.support_tier1_price, R.string.support_tier1_note),
                Triple(R.string.support_tier2, R.string.support_tier2_price, R.string.support_tier2_note),
                Triple(R.string.support_tier3, R.string.support_tier3_price, R.string.support_tier3_note),
            )
            val priceOf = { i: Int -> support.prices[io.github.graviton94.carpediem.billing.Support.IDS[i]]?.takeIf { it.isNotBlank() } }
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                tiers.forEachIndexed { i, (name, price, note) ->
                    val on = picked == i
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp6)
                            .crayonBox(if (on) Theme.gc.chip else Theme.gc.paper, G.Radius.box, if (on) G.Stroke.box else G.Stroke.chip, seed = 920 + i * 3)
                            .selectable(selected = on, role = androidx.compose.ui.semantics.Role.RadioButton) { picked = i }
                            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
                    ) {
                        SupportIcon(i, Theme.unit * 52f)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                            TokenText(stringResource(name), Tokens.TypeScale.headline)
                            TokenText(stringResource(note), Tokens.TypeScale.footnote, color = p.secondary)
                        }
                        TokenText(priceOf(i) ?: stringResource(price), Tokens.TypeScale.subhead, weight = FontWeight.SemiBold)
                    }
                }
            }
            val cta = stringResource(R.string.support_cta, stringResource(tiers[picked].first), priceOf(picked) ?: stringResource(tiers[picked].second))
            GardenButton(cta, {
                // Play 에서 상품을 받아 왔으면 결제 창. 아직이면 (막 열었거나 Play 에 닿지 않음) 다시 이어 보고 ‘잠시 뒤 다시’
                val act = ctx as? android.app.Activity
                soon = act == null || !support.buy(act, io.github.graviton94.carpediem.billing.Support.IDS[picked])
                if (soon) support.connect()
            }, filled = true, seed = 930)
            if (support.thanked) Box(Modifier.fillMaxWidth().crayonBox(Theme.gc.chip, G.Radius.box, G.Stroke.chip, seed = 941).padding(Tokens.Space.sp4)) {
                // 무엇이 하루 곁에 놓였는지 (정원에 돌아가면 보임)
                val k = io.github.graviton94.carpediem.billing.Support.IDS.indexOf(support.lastId)
                val item = if (k >= 0) stringResource(tiers[k].first) else null
                TokenText(if (item != null) stringResource(R.string.support_thanksItem, item) else stringResource(R.string.support_thanks), Tokens.TypeScale.subhead.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
            else if (support.pending) Box(Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, seed = 942).padding(Tokens.Space.sp4)) {
                TokenText(stringResource(R.string.support_pending), Tokens.TypeScale.subhead, Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
            else if (soon) Box(Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, seed = 940).padding(Tokens.Space.sp4)) {
                TokenText(stringResource(R.string.support_soon), Tokens.TypeScale.subhead, Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
            TokenText(stringResource(R.string.support_once) + "\n" + stringResource(R.string.support_use), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        }
    }
    // 응원이 끝나면 (1.1.8): 고마움 팝업. 내 하루 곁이 비어 있었으면 이미 놓였고 (정원에서 보기), 아니면 바로 내 하루 곁에 둘지 물음
    var doneFor by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(support.thanked, state.lastSupported) { if (support.thanked) doneFor = state.lastSupported }
    doneFor?.let { mark ->
        val item = "support:$mark"
        val me = io.github.graviton94.carpediem.core.Adornments.ME
        val placed = state.adornments[me] == item
        val name = adornName(ctx, item)
        io.github.graviton94.carpediem.ui.GardenAlert(
            onDismissRequest = { doneFor = null },
            title = { androidx.compose.material3.Text(stringResource(R.string.support_doneTitle)) },
            text = { androidx.compose.material3.Text(stringResource(if (placed) R.string.support_donePlaced else R.string.support_doneAsk, name)) },
            confirmButton = {
                if (placed) io.github.graviton94.carpediem.ui.AlertButton(stringResource(R.string.support_seeGarden), { doneFor = null; onBack() })
                else io.github.graviton94.carpediem.ui.AlertButton(stringResource(R.string.support_placeMe), { state.adorn(me, item); doneFor = null; state.say(ctx.getString(R.string.adorn_done, ctx.getString(R.string.garden_haru), name)) })
            },
            dismissButton = { io.github.graviton94.carpediem.ui.AlertButton(stringResource(if (placed) R.string.garden_close else R.string.support_later), { doneFor = null }, quiet = true) },
        )
    }
}

/** 미리 놓아 보기: 조각 크기 (그림 폭에 대한) · 반짝임 (가장 옅을 때 · 한 번 숨). */
private const val PREVIEW_PIECE = 0.16f
private const val PREVIEW_ALPHA_LOW = 0.6f
private const val PREVIEW_PULSE_MS = 1400

/** 응원 한 가지의 그림: 0 차 한 잔 · 1 커피 한 잔 · 2 케이크 한 조각 (정원의 계절 조각과 같은 한지 조각, 돌 곁 꾸밈으로도 쓰임). */
@Composable
internal fun SupportIcon(kind: Int, size: androidx.compose.ui.unit.Dp) {
    val ctx = LocalContext.current
    val img = remember(kind) { GardenArt.card(ctx, when (kind) { 0 -> "tea"; 1 -> "coffee"; else -> "cake" }) }
    PieceImage(img, size)
}


/** 세 칸씩 놓는 판 (마지막 줄은 빈칸으로 채움). */
@Composable
private fun TileGrid(count: Int, tile: @Composable (Int) -> Unit) {
    val cols = COLLECT_COLS
    (0 until count).chunked(cols).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            row.forEach { i -> Box(Modifier.weight(1f)) { tile(i) } }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** 모은 것 한 칸: 같은 높이. 그림 (정사각) · 이름 한 줄 (길면 …) · 날짜 한 줄, 오른쪽 위 (?) (누르면 상세). */
@Composable
private fun CollectionTile(seed: Int, art: @Composable () -> Unit, title: String, date: String, onClick: () -> Unit, ghost: Boolean = false) {
    val u = Theme.unit
    val p = Theme.palette
    val a11y = if (ghost) stringResource(R.string.collect_ghostA11y) else null
    // 못 만난 것: 테두리까지 옅게 (그림은 GhostArt 가 한지빛으로 씻어 둠)
    Box(Modifier.fillMaxWidth().height(u * COLLECT_TILE_H).graphicsLayer { alpha = if (ghost) GHOST_TILE else 1f }.crayonBox(null, G.Radius.box, G.Stroke.chip, seed).clickable(onClick = onClick)
        .let { m -> if (a11y != null) m.semantics { contentDescription = a11y } else m }) {
        Column(Modifier.fillMaxSize().padding(horizontal = Tokens.Space.sp2, vertical = Tokens.Space.sp2), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            Box(Modifier.size(u * COLLECT_ART), contentAlignment = Alignment.Center) { art() }
            TokenText(title, Tokens.TypeScale.caption1, Modifier.fillMaxWidth(), color = if (ghost) p.secondary else p.foreground, weight = if (ghost) FontWeight.Normal else FontWeight.SemiBold, align = TextAlign.Center, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            TokenText(date, Tokens.TypeScale.caption2, color = p.secondary, align = TextAlign.Center, maxLines = 1)
        }
        // 크레용 테두리 안쪽으로 넉넉히 (가장자리에 붙지 않게)
        TokenText("?", Tokens.TypeScale.caption2, Modifier.align(Alignment.TopEnd).padding(top = Tokens.Space.sp2 + Tokens.Space.sp1, end = Tokens.Space.sp2 + Tokens.Space.sp1).size(u * 16f)
            .border(u * 1.2f, p.secondary.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape), color = p.secondary, align = TextAlign.Center)
    }
}

/** 별자리를 모으는 날 수 (한 달 안에 한 줄을 남긴 날). 정원 밤하늘의 별자리도 같은 값. */
internal const val STAR_DAYS = 10

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
private class Tile(val art: @Composable () -> Unit, val title: String, val dateText: String, val date: java.time.LocalDate?, val detail: Detail?, val ghost: Boolean = false,
                   val kind: Shelf = Shelf.GARDEN, val onClick: (() -> Unit)? = null)

/** 모은 것의 칸 묶음 (1.1.6): 모두 (모은 것만) · 정원 · 손님과 순간 · 별자리 · 절기. 못 만난 것은 그 묶음에서만 옅게. */
private enum class Shelf { ALL, GARDEN, MET, STARS, TERMS }

/** (?) 상세: 큰 그림 · 이름 · 종류와 날짜 · 설명 · 얻는 방법 (단계가 있으면 지나온 단계 칩, 지금 것만 진하게). 아직 얻지 않은 것은 보이지 않음. */
private class Detail(val art: @Composable () -> Unit, val name: String, val sub: String, val desc: String, val howTitle: String, val how: String,
                     val chips: List<Pair<String, Boolean>> = emptyList(), val note: String? = null)

/** 간직한 문장: 새것이 위, 셋까지 보이고 그보다 많으면 ‘전체 보기’. 없으면 모으는 법 한 줄. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun KeptQuotes(state: AppState) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val fmt = remember { java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM) }
    var all by rememberSaveable { mutableStateOf(false) }
    val list = state.keptQuotes.mapNotNull { (n, d) -> state.keptQuote(n)?.let { Triple(n, it, d) } }
    Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        TokenText(stringResource(R.string.keep_title), Tokens.TypeScale.title3)
        TokenText(stringResource(if (list.isEmpty()) R.string.keep_empty else R.string.keep_sub), Tokens.TypeScale.footnote, color = p.secondary)
        val phone = io.github.graviton94.carpediem.data.Words.lang(ctx)
        (if (all) list else list.take(KEEP_FIRST)).forEachIndexed { i, (n, q, d) ->
            val text = io.github.graviton94.carpediem.data.Words.main(q, state.quoteLanguage, phone)
            Box(Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1800 + i)
                .combinedClickable(onLongClickLabel = stringResource(R.string.keep_off), onLongClick = { state.toggleKeep(n) }) { state.showKept(n) }
                .drawWithContent { drawContent(); keepLeaf(androidx.compose.ui.geometry.Offset(size.width - u.toPx() * 16f, u.toPx() * 15f), u.toPx() * 0.8f, 1f) }
                .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3)) {
                Column(Modifier.padding(end = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                    TokenText(text, Tokens.TypeScale.callout.serif())
                    TokenText(stringResource(R.string.keep_on_date, d.format(fmt)), Tokens.TypeScale.caption2, color = p.secondary)
                }
            }
        }
        if (list.size > KEEP_FIRST) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GardenChip(if (all) stringResource(R.string.collect_less) else stringResource(R.string.collect_more, "${list.size}"), false, 1799) { all = !all }
        }
    }
}
private const val KEEP_FIRST = 3

/** 못 만난 것의 그림: 한지빛으로 씻어 옅은 실루엣처럼. */
@Composable
private fun GhostArt(content: @Composable () -> Unit) {
    val paper = Theme.gc.paper
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = GHOST_ART; compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
        .drawWithContent { drawContent(); drawRect(paper.copy(alpha = GHOST_WASH), blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop) }, contentAlignment = Alignment.Center) { content() }
}
/** 만날 수 있는 손님 · 우연한 순간 (‘chance_’ 이름과 같음). */
private val GHOSTS = listOf("guest_tit", "guest_squirrel", "guest_hedgehog", "guest_rabbit", "guest_owl", "wind", "rainbow", "fireflies", "aurora", "snail", "bubbles", "butterflies")
private const val GHOST_TILE = 0.7f
private const val GHOST_ART = 0.35f
private const val GHOST_WASH = 0.55f

/** 모은 것: 한 줄에 넷, 두 줄 (여덟) 까지 보이고 그보다 많으면 ‘전체 보기’. */
private const val COLLECT_COLS = 4
private const val COLLECT_FIRST = 8
private const val COLLECT_TILE_H = 110f
private const val COLLECT_ART = 46f

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CollectDetailSheet(d: Detail, onClose: () -> Unit) {
    val p = Theme.palette
    val u = Theme.unit
    GardenSheet(onDismissRequest = onClose, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Box(Modifier.size(u * 120f), contentAlignment = Alignment.Center) { d.art() }
            TokenText(d.name, Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
            TokenText(d.sub, Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
            if (d.desc.isNotEmpty()) TokenText(d.desc, Tokens.TypeScale.callout, Modifier.fillMaxWidth().padding(top = Tokens.Space.sp2), align = TextAlign.Center)
            // 못 만난 것은 힌트 한 줄만 (얻는 방법 칸 없이)
            if (d.howTitle.isNotEmpty()) CrayonRule(Modifier.padding(vertical = Tokens.Space.sp2), seed = 1730)
            if (d.howTitle.isNotEmpty()) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
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

/** 하루가 준 조약돌 (모은 것 칸 · 상세의 그림): 회갈색 조약돌 하나와 작은 빛. */
@Composable
internal fun PebbleArt() {
    val ink = Theme.gc.ink
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(Tokens.Space.sp4)) {
        val w = size.minDimension; val h = w * 0.66f
        val tl = androidx.compose.ui.geometry.Offset((size.width - w) / 2, (size.height - h) / 2 + h * 0.15f)
        val sz = androidx.compose.ui.geometry.Size(w, h)
        drawOval(androidx.compose.ui.graphics.Color(0xFFA29A8B), tl, sz)
        drawOval(ink, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.05f))
        drawArc(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), 200f, 70f, false, tl + androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.18f),
            androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.035f))
    }
}

/** 모은 것을 어디에 (1.1.11): 모은 것 화면의 판 · 추억 장의 나머지 (간직한 문장 · 돌아보기). */
internal enum class CollectPart { GRID, REST }

/**
 * 추억 맨 아래의 모은 것 한 줄 (1.1.11): 제목 · 묶음마다 모은 수 (별자리 n/12 · 절기 n/24 · 손님과 순간 n/m), 누르면 모은 것 화면.
 * 판은 자리를 많이 차지해서 추억에서는 접어 둠.
 */
@Composable
internal fun CollectionRow(state: AppState, profile: LifeProfile, now: LocalDateTime, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val p = Theme.palette
    val today = now.toLocalDate()
    val stars = remember(state.lines) { io.github.graviton94.carpediem.core.Constellations.clearMonths(state.lines, STAR_DAYS).size }
    val terms = remember(state.lines, state.breaths, state.gazeDays, today) { state.termsMet().size }
    val met = state.chancesMet.mapNotNull { it.substringBefore(':', "").takeIf { k -> k.isNotEmpty() } }.toSet()
    val south = profile.countryCode.uppercase() in GardenDecor.SOUTH
    val parts = listOfNotNull(
        stringResource(R.string.collect_shelfStars, "$stars", "12").takeIf { state.keepLines },
        stringResource(R.string.collect_shelfTerms, "$terms", "${io.github.graviton94.carpediem.core.SolarTerm.entries.size}").takeIf { !south },
        stringResource(R.string.collect_shelfMet, "${met.size}", "${(met + GHOSTS).size}"),
    )
    Row(
        modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).crayonBox(null, G.Radius.box, G.Stroke.chip, 1170).clickable(onClick = onOpen)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            TokenText(stringResource(R.string.collection), Tokens.TypeScale.headline)
            TokenText(parts.joinToString(" · "), Tokens.TypeScale.footnote, color = p.secondary)
        }
        TokenText("›", Tokens.TypeScale.headline, color = p.secondary)
    }
}
