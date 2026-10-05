package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Constellations
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Segments
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 마음의 기록 시트가 여는 곳: 한 달 (month) 또는 한 해 (month = null). */
data class RecordView(val year: Int, val month: Int?, val pick: LocalDate? = null)

internal object RecordText {
    private fun locale(ctx: android.content.Context): Locale = ctx.resources.configuration.locales[0]
    fun korean(ctx: android.content.Context) = locale(ctx).language == "ko"
    /** “9월” · “September” */
    fun month(ctx: android.content.Context, m: Int): String = java.time.Month.of(m).getDisplayName(TextStyle.FULL_STANDALONE, locale(ctx))
    fun constellation(ctx: android.content.Context, state: AppState, m: Int): String = state.store.constellations.of(m)?.name(io.github.graviton94.carpediem.data.Words.lang(ctx)).orEmpty()
    fun day(ctx: android.content.Context, d: LocalDate): String = d.format(java.time.format.DateTimeFormatter.ofPattern(ctx.getString(R.string.record_dayPattern), locale(ctx)))
}

/** 마음의 기록 시트 (지난 정원에서 열 때): 추억 페이지와 같은 판. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordSheet(state: AppState, start: RecordView, today: LocalDate, onClose: () -> Unit) {
    var view by remember { mutableStateOf(start) }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Theme.gc.paper, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp8),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        ) {
            RecordPanel(state, view, { view = it }, today)
            GardenButton(stringResource(R.string.garden_close), onClose, filled = false, seed = 1182)
        }
    }
}

/**
 * 마음의 기록: 위에 월 · 해. 월은 ‹ › 로 지난 달, 날을 누르면 그날의 한 줄. 해는 열두 달의 무늬를 한 화면에 (누르면 그 달).
 * 판은 크레용 테두리 안에, 위에 이름 · 별자리 · 수. 그림으로 보내기.
 */
@Composable
internal fun RecordPanel(state: AppState, view: RecordView, onView: (RecordView) -> Unit, today: LocalDate) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val night = Theme.gc.night
    val book = state.store.constellations
    var picked by remember(view) { mutableStateOf(view.pick) }
    var deleting by remember { mutableStateOf<LocalDate?>(null) }
    deleting?.let { d ->
        io.github.graviton94.carpediem.ui.GardenAlert(
            onDismissRequest = { deleting = null },
            title = { androidx.compose.material3.Text(stringResource(R.string.edit_deleteDayAsk, RecordText.day(ctx, d))) },
            text = { androidx.compose.material3.Text(stringResource(R.string.edit_deleteHelp)) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { deleting = null; picked = null; state.deleteLine(d) }) { androidx.compose.material3.Text(stringResource(R.string.edit_delete), color = p.danger) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { deleting = null }) { androidx.compose.material3.Text(stringResource(R.string.cancel)) } },
        )
    }
    val first = remember(state.lines) { state.lines.minOfOrNull { it.date } ?: today }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Segments(listOf(true, false), view.month != null, { if (it) stringResource(R.string.record_month) else stringResource(R.string.record_year) }) { m ->
                onView(if (m) RecordView(view.year, view.month ?: if (view.year == today.year) today.monthValue else 12) else view.copy(month = null))
            }
        }
        val m = view.month
        Column(Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1171).padding(Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            if (m != null) {
                val days = remember(state.lines, view) { Constellations.monthDays(state.lines, view.year, m) }
                val sent = days.count { it.second != null }; val thanks = days.count { it.second?.feeling == Feeling.THANKS }
                val prev = LocalDate.of(view.year, m, 1).minusMonths(1); val next = LocalDate.of(view.year, m, 1).plusMonths(1)
                val canPrev = !prev.plusMonths(1).minusDays(1).isBefore(first.withDayOfMonth(1)); val canNext = !next.isAfter(today)
                val title = stringResource(R.string.record_monthTitle, RecordText.month(ctx, m))
                val shown = if (view.year == today.year) title else "${view.year} · $title"
                val sub = RecordText.constellation(ctx, state, m) + " · " + stringResource(R.string.year_count, "$sent", "$thanks")
                PanelHead(shown, sub, canPrev, canNext, { onView(RecordView(prev.year, prev.monthValue)) }, { onView(RecordView(next.year, next.monthValue)) })
                MonthGarden(book, days, state.store.haruSeed, night, today, Modifier.fillMaxWidth(), picked = picked) { d -> picked = if (picked == d.date) null else d.date }
                val pickedLine = picked?.let { d -> days.firstOrNull { it.first == d } }
                if (pickedLine != null) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        TokenText(RecordText.day(ctx, pickedLine.first), Tokens.TypeScale.caption1, color = p.secondary)
                        val l = pickedLine.second
                        // 오늘 보낸 한 줄은 떠나보낸 그대로 두고, 내일부터 여기서 다시 볼 수 있음
                        TokenText(when { l == null -> stringResource(R.string.record_rest); pickedLine.first == today -> stringResource(R.string.record_todayHidden); l.text.isBlank() -> stringResource(R.string.record_noText); else -> l.text },
                            Tokens.TypeScale.callout.serif(), color = if (l != null && pickedLine.first == today) p.secondary else p.foreground)
                        // 그날의 사진 (11): 지난 날만 (오늘은 한 줄처럼 내일부터)
                        if (l != null && pickedLine.first != today) WeatheredPhoto(state, pickedLine.first, today, Theme.unit * 150f, modifier = Modifier.padding(vertical = Tokens.Space.sp2))
                        // 빈 지난 날: 그날의 한 줄을 바로 (기록 페이지로)
                        if (l == null && state.canWriteOn(pickedLine.first, today)) TokenText(stringResource(R.string.record_writeDay), Tokens.TypeScale.footnote,
                            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { state.writeDay = pickedLine.first }.padding(vertical = Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold)
                        // 방금 지운 날이면 되돌리기 (앱을 켜 둔 동안)
                        if (l == null && state.lastDeleted?.date == pickedLine.first) TokenText(stringResource(R.string.edit_undo), Tokens.TypeScale.footnote,
                            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { state.undoDelete() }.padding(vertical = Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold)
                        // 지난 날의 한 줄 지우기 (오늘 것은 위 쓰는 칸에서)
                        if (l != null && pickedLine.first != today) TokenText(stringResource(R.string.edit_deleteDay), Tokens.TypeScale.footnote,
                            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { deleting = pickedLine.first }.padding(vertical = Tokens.Space.sp3), color = p.secondary)
                    }
                } else TokenText(stringResource(R.string.record_hint), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
                GardenButton(stringResource(R.string.share_image), {
                    io.github.graviton94.carpediem.share.ShareCards.send(ctx, io.github.graviton94.carpediem.share.ShareCards.month(ctx, book, days, state.store.haruSeed, night, today, shown, sub), "month-${view.year}-$m")
                }, filled = false, seed = 1173)
            } else {
                val y = view.year
                val days = remember(state.lines, y) { Lines.yearDays(state.lines, y) }
                val sent = days.count { it.second != null }
                val thanksAll = state.lines.filter { it.date.year == y && it.feeling == Feeling.THANKS && it.text.isNotBlank() }
                val thanks = remember(y, thanksAll.size) { thanksAll.map { it.text }.distinct().shuffled(kotlin.random.Random(y)).take(G.Year.thanks.toInt()) }
                val title = stringResource(R.string.year_title, "$y"); val count = stringResource(R.string.year_count, "$sent", "${thanksAll.size}")
                PanelHead(title, count, y > first.year, y < today.year, { onView(RecordView(y - 1, null)) }, { onView(RecordView(y + 1, null)) })
                YearTiles(state, y, today, night) { mo -> onView(RecordView(y, mo)) }
                TokenText(stringResource(R.string.record_yearHint), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
                thanks.forEach { TokenText("“$it”", Tokens.TypeScale.callout.serif(), Modifier.padding(horizontal = Tokens.Space.sp2)) }
                if (y < today.year || (today.monthValue == 12 && today.dayOfMonth == 31))
                    TokenText(stringResource(R.string.year_end), Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                GardenButton(stringResource(R.string.share_image), {
                    io.github.graviton94.carpediem.share.ShareCards.send(ctx, io.github.graviton94.carpediem.share.ShareCards.year(ctx, book, days, state.store.haruSeed, night, today, title, count, thanks), "year-$y")
                }, filled = false, seed = 1181)
            }
        }
    }
}

/** 판 위 한 줄: ‹ 이름 › 과 아래 작은 글. */
@Composable
private fun PanelHead(title: String, sub: String, canPrev: Boolean, canNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TokenText("‹", Tokens.TypeScale.title3, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(enabled = canPrev, role = Role.Button, onClick = onPrev)
            .padding(horizontal = Tokens.Space.sp3).semantics { contentDescription = ctx.getString(R.string.record_prev) }, color = if (canPrev) p.secondary else p.secondary.copy(alpha = 0.3f))
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText(title, Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
            TokenText(sub, Tokens.TypeScale.caption1, color = p.secondary, align = TextAlign.Center)
        }
        TokenText("›", Tokens.TypeScale.title3, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(enabled = canNext, role = Role.Button, onClick = onNext)
            .padding(horizontal = Tokens.Space.sp3).semantics { contentDescription = ctx.getString(R.string.record_next) }, color = if (canNext) p.secondary else p.secondary.copy(alpha = 0.3f))
    }
}

/** 열두 달 칸의 얇은 테두리: 크레용 층 대신 가벼운 선 (한 해에 열두 칸, 여러 해가 한꺼번에 그려져도 가볍게). */
@Composable
private fun Modifier.tileEdge(weight: Float): Modifier {
    val u = Theme.unit
    return this.border(u * G.Stroke.chip * 0.6f * weight, Theme.gc.ink.copy(alpha = 0.45f * weight + 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(u * G.Radius.chip))
}

/** 한 해: 열두 달의 무늬를 4 × 3 칸에 (지나간 달만, 오지 않은 달은 빈 칸). 누르면 그 달. */
@Composable
internal fun YearTiles(state: AppState, y: Int, today: LocalDate, night: Boolean, onMonth: (Int) -> Unit) {
    val ctx = LocalContext.current
    val p = Theme.palette
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        (0 until 3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                (1..4).forEach { k ->
                    val m = row * 4 + k
                    val passed = LocalDate.of(y, m, 1) <= today
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        if (passed) {
                            val days = remember(state.lines, y, m) { Constellations.monthDays(state.lines, y, m) }
                            MonthGarden(state.store.constellations, days, state.store.haruSeed, night, today,
                                Modifier.fillMaxWidth().tileEdge(1f).clickable(role = Role.Button) { onMonth(m) },
                                animate = false, sizes = TILE_SIZES)
                        } else Spacer(Modifier.fillMaxWidth().aspectRatio(G.Year.monthAspect).tileEdge(0.5f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                            TokenText(RecordText.month(ctx, m), Tokens.TypeScale.caption1, color = if (passed) p.foreground else p.secondary)
                            // 마음의 날씨: 그 달에 가장 많았던 마음을 작은 하늘로 (숫자 없이)
                            if (passed) remember(state.lines, y, m) { Lines.monthMood(state.lines, y, m) }?.let { MoodSky(it) }
                        }
                    }
                }
            }
        }
    }
}

/** 마음의 날씨 한 칸: 기쁨 · 희망 · 고마움 = 해, 고요 = 구름, 실망 · 슬픔 · 걱정 = 구름과 빗방울. 색은 그 마음의 색. */
@Composable
internal fun MoodSky(f: Feeling) {
    val ctx = LocalContext.current
    val c = moodColor(f)
    val name = stringResource(feelingName(f))
    val label = stringResource(R.string.mood_weatherA11y, name)
    androidx.compose.foundation.Canvas(Modifier.size(Theme.unit * 12).semantics { contentDescription = label }) {
        val w = size.width; val h = size.height
        when (f) {
            Feeling.JOY, Feeling.HOPE, Feeling.THANKS -> {
                drawCircle(c, w * 0.26f, center)
                repeat(8) { k -> val a = k * Math.PI / 4; val r1 = w * 0.36f; val r2 = w * 0.48f
                    drawLine(c, androidx.compose.ui.geometry.Offset(center.x + (r1 * kotlin.math.cos(a)).toFloat(), center.y + (r1 * kotlin.math.sin(a)).toFloat()),
                        androidx.compose.ui.geometry.Offset(center.x + (r2 * kotlin.math.cos(a)).toFloat(), center.y + (r2 * kotlin.math.sin(a)).toFloat()), strokeWidth = w * 0.07f) }
            }
            else -> {
                val heavy = f != Feeling.CALM
                val cy = if (heavy) h * 0.38f else h * 0.5f
                drawCircle(c, w * 0.2f, androidx.compose.ui.geometry.Offset(w * 0.34f, cy + h * 0.04f))
                drawCircle(c, w * 0.25f, androidx.compose.ui.geometry.Offset(w * 0.58f, cy - h * 0.04f))
                drawRect(c, androidx.compose.ui.geometry.Offset(w * 0.18f, cy), androidx.compose.ui.geometry.Size(w * 0.62f, h * 0.18f))
                if (heavy) listOf(0.32f, 0.52f, 0.72f).forEach { x ->
                    drawLine(c, androidx.compose.ui.geometry.Offset(w * x, h * 0.72f), androidx.compose.ui.geometry.Offset(w * (x - 0.05f), h * 0.92f), strokeWidth = w * 0.07f)
                }
            }
        }
    }
}

/**
 * 기록 찾기: 마음의 기록 위의 작은 칸. 낱말 · 마음 이름 · 보낸 사람으로 찾고, 누르면 그 달 판의 그날로.
 * 오늘 보낸 글은 내일부터 찾아짐 (떠나보낸 그대로).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun RecordSearch(state: AppState, today: LocalDate, onOpen: (RecordView) -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    var q by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    androidx.compose.foundation.text.BasicTextField(
        value = q, onValueChange = { q = it.replace('\n', ' ').take(40) }, singleLine = true,
        textStyle = Tokens.TypeScale.callout.style().copy(color = p.foreground), cursorBrush = androidx.compose.ui.graphics.SolidColor(p.foreground),
        modifier = Modifier.fillMaxWidth().keepAboveKeyboard().crayonBox(null, G.Radius.chip, G.Stroke.chip, 1195).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        decorationBox = { inner -> Box { if (q.isEmpty()) TokenText(stringResource(R.string.search_hint), Tokens.TypeScale.callout, color = p.secondary); inner() } },
    )
    if (q.isBlank()) return
    fun open(v: RecordView) { q = ""; onOpen(v) }
    val found = remember(state.lines, q, today) {
        Lines.search(state.lines, q, today, { ctx.getString(feelingName(it)) }, { id -> state.people.firstOrNull { it.id == id }?.name ?: state.memories.firstOrNull { it.id == id }?.name })
    }
    if (found.isEmpty()) {
        TokenText(stringResource(R.string.search_none), Tokens.TypeScale.footnote, color = p.secondary)
        // 마음으로 찾아보기: 누르면 그 마음 이름으로
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Feeling.entries.forEachIndexed { i, f -> val n = stringResource(feelingName(f)); GardenChip(n, false, 1500 + i) { q = n } }
        }
    } else {
        TokenText(stringResource(R.string.search_count, "${found.size}"), Tokens.TypeScale.footnote, color = p.secondary)
        found.take(30).forEach { l ->
            Column(Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable { open(RecordView(l.date.year, l.date.monthValue, l.date)) }.padding(vertical = Tokens.Space.sp2),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                val meta = listOfNotNull(RecordText.day(ctx, l.date).let { if (l.date.year != today.year) "${l.date.year} · $it" else it }, l.feeling?.let { ctx.getString(feelingName(it)) }).joinToString(" · ")
                TokenText(meta, Tokens.TypeScale.caption1, color = p.secondary)
                TokenText(l.text.ifBlank { stringResource(R.string.record_noText) }, Tokens.TypeScale.callout.serif(), maxLines = 3, color = if (l.text.isBlank()) p.secondary else p.foreground)
            }
        }
    }
}

/** 홈 둘째 장 위의 한 장: 한 해 (12월 31일 ~ 1월 7일) 또는 지난 달 (달이 바뀐 뒤 사흘). 낮엔 꽃 세 송이, 밤엔 별 세 개. */
@Composable
internal fun GardenCard(title: String, seed: Int, onOpen: () -> Unit) {
    val p = Theme.palette
    Row(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, seed).clickable(onClick = onOpen).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        TinyGarden(Theme.gc.night, Modifier.size(Theme.unit * 56, Theme.unit * 40).crayonBox(null, G.Radius.chip, G.Stroke.chip, seed + 1))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            TokenText(title, Tokens.TypeScale.subhead, weight = FontWeight.SemiBold)
            TokenText(stringResource(R.string.recall_open), Tokens.TypeScale.footnote, color = p.secondary)
        }
    }
}

@Composable
internal fun YearCard(year: Int, onOpen: () -> Unit) =
    GardenCard(stringResource(if (Theme.gc.night) R.string.year_cardNight else R.string.year_card, "$year"), 1180, onOpen)

@Composable
internal fun MonthCard(month: Int, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    GardenCard(stringResource(if (Theme.gc.night) R.string.record_cardNight else R.string.record_card, RecordText.month(ctx, month)), 1176, onOpen)
}

/** 모은 것 아래: 지난 정원. 해마다 열두 달의 칸, 해 이름을 누르면 한 해 · 칸을 누르면 그 달. */
@Composable
internal fun PastGardens(state: AppState, today: LocalDate) {
    val years = remember(state.lines, today, state.keepLines) { state.gardenYears() }
    if (years.isEmpty()) return
    val night = Theme.gc.night
    var open by remember { mutableStateOf<RecordView?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(stringResource(R.string.year_list), Tokens.TypeScale.title3)
        years.forEach { y ->
            TokenText(stringResource(R.string.year_title, "$y"), Tokens.TypeScale.subhead, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(role = Role.Button) { open = RecordView(y, null) }.padding(vertical = Tokens.Space.sp2), weight = FontWeight.SemiBold)
            YearTiles(state, y, today, night) { m -> open = RecordView(y, m) }
        }
    }
    open?.let { RecordSheet(state, it, today) { open = null } }
}
