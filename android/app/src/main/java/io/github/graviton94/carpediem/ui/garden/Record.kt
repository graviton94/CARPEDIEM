package io.github.graviton94.carpediem.ui.garden

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
data class RecordView(val year: Int, val month: Int?)

internal object RecordText {
    private fun locale(ctx: android.content.Context): Locale = ctx.resources.configuration.locales[0]
    fun korean(ctx: android.content.Context) = locale(ctx).language == "ko"
    /** “9월” · “September” */
    fun month(ctx: android.content.Context, m: Int): String = java.time.Month.of(m).getDisplayName(TextStyle.FULL_STANDALONE, locale(ctx))
    fun constellation(ctx: android.content.Context, state: AppState, m: Int): String = state.store.constellations.of(m)?.let { if (korean(ctx)) it.korean else it.english }.orEmpty()
    fun day(ctx: android.content.Context, d: LocalDate): String = d.format(java.time.format.DateTimeFormatter.ofPattern(ctx.getString(R.string.record_dayPattern), locale(ctx)))
}

/** 둘째 장: 이번 달의 마음의 기록 (그 달의 별자리, 낮엔 꽃 · 밤엔 별). 누르면 시트. 기록 남기기를 끄면 보이지 않는다. */
@Composable
internal fun MoodRecord(state: AppState, today: LocalDate, onOpen: (RecordView) -> Unit) {
    if (!state.keepLines) return
    val p = Theme.palette
    val ctx = LocalContext.current
    val days = remember(state.lines, today) { Constellations.monthDays(state.lines, today.year, today.monthValue) }
    val kept = days.count { it.second != null }
    val a11y = stringResource(R.string.mood_a11y, RecordText.month(ctx, today.monthValue), "$kept")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        TokenText(stringResource(R.string.mood_title), Tokens.TypeScale.title3)
        MonthGarden(state.store.constellations, days, Theme.gc.night, today,
            Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1170).clickable(role = Role.Button) { onOpen(RecordView(today.year, today.monthValue)) }
                .semantics { contentDescription = a11y })
        TokenText(stringResource(R.string.mood_sub, RecordText.month(ctx, today.monthValue), RecordText.constellation(ctx, state, today.monthValue)), Tokens.TypeScale.caption1, color = p.secondary)
    }
}

/** 마음의 기록: 월 · 해. 월은 ‹ › 로 지난 달, 날을 누르면 그날의 한 줄. 해는 열두 별자리의 띠. 그림으로 보내기. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordSheet(state: AppState, start: RecordView, today: LocalDate, onClose: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val night = Theme.gc.night
    val book = state.store.constellations
    var view by remember { mutableStateOf(start) }
    var picked by remember(view) { mutableStateOf<LocalDate?>(null) }
    val first = remember(state.lines) { state.lines.minOfOrNull { it.date } ?: today }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Theme.gc.paper, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp8),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Segments(listOf(true, false), view.month != null, { if (it) stringResource(R.string.record_month) else stringResource(R.string.record_year) }) { m ->
                    view = if (m) RecordView(view.year, view.month ?: if (view.year == today.year) today.monthValue else 12) else view.copy(month = null)
                }
            }
            val m = view.month
            if (m != null) {
                val days = remember(state.lines, view) { Constellations.monthDays(state.lines, view.year, m) }
                val sent = days.count { it.second != null }; val thanks = days.count { it.second?.feeling == Feeling.THANKS }
                val prev = LocalDate.of(view.year, m, 1).minusMonths(1); val next = LocalDate.of(view.year, m, 1).plusMonths(1)
                val canPrev = !prev.plusMonths(1).minusDays(1).isBefore(first.withDayOfMonth(1)); val canNext = !next.isAfter(today)
                val title = stringResource(R.string.record_monthTitle, RecordText.month(ctx, m))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TokenText("‹", Tokens.TypeScale.title3, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(enabled = canPrev, role = Role.Button) { view = RecordView(prev.year, prev.monthValue) }
                        .padding(horizontal = Tokens.Space.sp3).semantics { contentDescription = ctx.getString(R.string.record_prev) }, color = if (canPrev) p.secondary else p.secondary.copy(alpha = 0.3f))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        TokenText(if (view.year == today.year) title else "${view.year} · $title", Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
                        TokenText(RecordText.constellation(ctx, state, m) + " · " + stringResource(R.string.year_count, "$sent", "$thanks"), Tokens.TypeScale.caption1, color = p.secondary, align = TextAlign.Center)
                    }
                    TokenText("›", Tokens.TypeScale.title3, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(enabled = canNext, role = Role.Button) { view = RecordView(next.year, next.monthValue) }
                        .padding(horizontal = Tokens.Space.sp3).semantics { contentDescription = ctx.getString(R.string.record_next) }, color = if (canNext) p.secondary else p.secondary.copy(alpha = 0.3f))
                }
                MonthGarden(book, days, night, today, Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1171), picked = picked) { d -> picked = if (picked == d.date) null else d.date }
                val pickedLine = picked?.let { d -> days.firstOrNull { it.first == d } }
                if (pickedLine != null) {
                    Column(Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 1172).padding(Tokens.Space.sp3), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        TokenText(RecordText.day(ctx, pickedLine.first), Tokens.TypeScale.caption1, color = p.secondary)
                        val l = pickedLine.second
                        TokenText(when { l == null -> stringResource(R.string.record_rest); l.text.isBlank() -> stringResource(R.string.record_noText); else -> l.text }, Tokens.TypeScale.callout.serif())
                    }
                } else TokenText(stringResource(R.string.record_hint), Tokens.TypeScale.caption1, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
                GardenButton(stringResource(R.string.share_image), {
                    io.github.graviton94.carpediem.share.ShareCards.send(ctx, io.github.graviton94.carpediem.share.ShareCards.month(ctx, book, days, night, today,
                        if (view.year == today.year) title else "${view.year} · $title", RecordText.constellation(ctx, state, m) + " · " + ctx.getString(R.string.year_count, "$sent", "$thanks")), "month-${view.year}-$m")
                }, filled = false, seed = 1173)
            } else {
                val y = view.year
                val days = remember(state.lines, y) { Lines.yearDays(state.lines, y) }
                val sent = days.count { it.second != null }
                val thanksAll = state.lines.filter { it.date.year == y && it.feeling == Feeling.THANKS && it.text.isNotBlank() }
                val thanks = remember(y, thanksAll.size) { thanksAll.map { it.text }.distinct().shuffled(kotlin.random.Random(y)).take(G.Year.thanks.toInt()) }
                val title = stringResource(R.string.year_title, "$y"); val count = stringResource(R.string.year_count, "$sent", "${thanksAll.size}")
                var shown by remember { mutableIntStateOf(1) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val canPrev = y > first.year; val canNext = y < today.year
                    TokenText("‹", Tokens.TypeScale.title3, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(enabled = canPrev, role = Role.Button) { view = RecordView(y - 1, null) }.padding(horizontal = Tokens.Space.sp3),
                        color = if (canPrev) p.secondary else p.secondary.copy(alpha = 0.3f))
                    TokenText(title, Tokens.TypeScale.title3.serif(), Modifier.weight(1f), align = TextAlign.Center)
                    TokenText("›", Tokens.TypeScale.title3, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(enabled = canNext, role = Role.Button) { view = RecordView(y + 1, null) }.padding(horizontal = Tokens.Space.sp3),
                        color = if (canNext) p.secondary else p.secondary.copy(alpha = 0.3f))
                }
                YearFlow(book, days, night, today, Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1174)) { shown = it }
                TokenText(RecordText.month(ctx, shown) + " · " + RecordText.constellation(ctx, state, shown), Tokens.TypeScale.caption1, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
                TokenText(count, Tokens.TypeScale.footnote, color = p.secondary)
                thanks.forEach { TokenText("“$it”", Tokens.TypeScale.callout.serif()) }
                if (y < today.year || (today.monthValue == 12 && today.dayOfMonth == 31)) {
                    Spacer(Modifier.height(Tokens.Space.sp2))
                    TokenText(stringResource(R.string.year_end), Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                }
                GardenButton(stringResource(R.string.share_image), {
                    io.github.graviton94.carpediem.share.ShareCards.send(ctx, io.github.graviton94.carpediem.share.ShareCards.year(ctx, book, days, night, today, title, count, thanks), "year-$y")
                }, filled = false, seed = 1181)
            }
            GardenButton(stringResource(R.string.garden_close), onClose, filled = false, seed = 1182)
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

/** 모은 것 아래: 지난 정원. 해마다 열두 달의 칸 (지나간 달만, 오지 않은 달은 빈 칸), 해 이름을 누르면 한 해. */
@Composable
internal fun PastGardens(state: AppState, today: LocalDate) {
    val years = remember(state.lines, today, state.keepLines) { state.gardenYears() }
    if (years.isEmpty()) return
    val ctx = LocalContext.current
    val p = Theme.palette
    val night = Theme.gc.night
    var open by remember { mutableStateOf<RecordView?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(stringResource(R.string.year_list), Tokens.TypeScale.title3)
        years.forEach { y ->
            TokenText(stringResource(R.string.year_title, "$y"), Tokens.TypeScale.subhead, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(role = Role.Button) { open = RecordView(y, null) }.padding(vertical = Tokens.Space.sp2), weight = FontWeight.SemiBold)
            (0 until 3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    (1..4).forEach { k ->
                        val m = row * 4 + k
                        val passed = LocalDate.of(y, m, 1) <= today
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                            if (passed) {
                                val days = remember(state.lines, y, m) { Constellations.monthDays(state.lines, y, m) }
                                MonthGarden(state.store.constellations, days, night, today,
                                    Modifier.fillMaxWidth().crayonBox(null, G.Radius.chip, G.Stroke.chip, 1190 + m).clickable(role = Role.Button) { open = RecordView(y, m) },
                                    animate = false, sizes = TILE_SIZES)
                            } else Spacer(Modifier.fillMaxWidth().aspectRatio(G.Year.monthAspect).crayonBox(null, G.Radius.chip, G.Stroke.chip * 0.5f, 1190 + m))
                            TokenText(RecordText.month(ctx, m), Tokens.TypeScale.caption1, color = if (passed) p.foreground else p.secondary)
                        }
                    }
                }
            }
        }
    }
    open?.let { RecordSheet(state, it, today) { open = null } }
}
