package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.DayLine
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.Letter
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.core.Question
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.cos
import kotlin.math.sin

// ───────────────────────── 오늘의 질문 ─────────────────────────

/**
 * 문장 자리에 오는 가벼운 질문. ‘한 줄로 답하기’는 둘째 장의 오늘의 한 줄로 넘어가고, ‘오늘은 문장으로’는 그날 문장으로 돌아간다.
 * 이미 한 줄을 보낸 날은 질문만 조용히.
 */
@Composable
internal fun QuestionBlock(state: AppState, q: Question, sent: Boolean, onAnswer: () -> Unit) {
    val p = Theme.palette
    val lang = state.quoteLanguage
    Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
        TokenText(stringResource(R.string.question_label), Tokens.TypeScale.caption2, color = p.secondary, weight = FontWeight.Normal)
        TokenText(if (lang == QuoteLanguage.ENGLISH) q.english else q.korean, Tokens.TypeScale.headline.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
        if (lang == QuoteLanguage.BOTH) TokenText(q.english, Tokens.TypeScale.footnote.serif(), Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        if (!sent) Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically) {
            TokenText(stringResource(R.string.question_answer), Tokens.TypeScale.footnote, Modifier.clickable(onClick = onAnswer).padding(Tokens.Space.sp2), weight = FontWeight.SemiBold)
            TokenText("·", Tokens.TypeScale.footnote, color = p.secondary)
            TokenText(stringResource(R.string.question_skip), Tokens.TypeScale.footnote, Modifier.clickable { state.skipQuestion() }.padding(Tokens.Space.sp2), color = p.secondary)
        }
    }
}

// ───────────────────────── 마음의 하늘 ─────────────────────────

internal fun moodColor(f: Feeling?): Color = when (f) {
    Feeling.JOY -> G.Mood.Colors.joy; Feeling.HOPE -> G.Mood.Colors.hope; Feeling.CALM -> G.Mood.Colors.calm; Feeling.THANKS -> G.Mood.Colors.thanks
    Feeling.DISAPPOINT -> G.Mood.Colors.disappoint; Feeling.SAD -> G.Mood.Colors.sad; Feeling.WORRY -> G.Mood.Colors.worry; null -> G.Mood.Colors.none
}

/** 손으로 그린 동그라미 하나의 점들: 날마다 조금씩 다른 울퉁불퉁함 (seed = 날짜). */
internal fun handCircle(c: Offset, r: Float, seed: Int): List<Offset> {
    val rng = Crayon.Rng(seed); val ph = FloatArray(3) { rng.next() * 6.283f }; val w = G.Mood.wobble
    val squash = 1f + (rng.next() - 0.5f) * w
    return List(28) { k ->
        val a = k * 6.283f / 28
        val k1 = 1f + w * (0.5f * sin(a * 2 + ph[0]) + 0.3f * sin(a * 3 + ph[1]) + 0.2f * sin(a * 5 + ph[2]))
        Offset(c.x + cos(a) * r * k1 * squash, c.y + sin(a) * r * k1 / squash)
    }
}

/**
 * 지난 30일의 마음을 손으로 그린 동그라미로 (글도 숫자도 없이, 오른쪽 아래가 오늘). 쉰 날은 빈 테두리, 마음을 안 고른 날은 종이빛.
 * 누르면 아무것도 열리지 않는다 (돌아보기만). 기록 남기기를 끄면 보이지 않는다.
 */
@Composable
internal fun MoodSky(state: AppState, today: LocalDate) {
    if (!state.keepLines) return
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = with(LocalDensity.current) { Theme.unit.toPx() }
    val days = remember(state.lines, today) { Lines.lastDays(state.lines, today, G.Mood.days.toInt()) }
    val cols = G.Mood.columns.toInt(); val rows = (days.size + cols - 1) / cols
    val lineMask = Crayon.tooth(GardenArt.toothLine(ctx), u); val fillMask = Crayon.tooth(GardenArt.toothFill(ctx), u)
    val night = Theme.gc.night; val ink = Theme.gc.ink; val future = Theme.gc.future
    val a11y = stringResource(R.string.mood_a11y, "${days.count { it.second?.text?.isNotBlank() == true || it.second?.feeling != null }}")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        TokenText(stringResource(R.string.mood_title), Tokens.TypeScale.title3)
        androidx.compose.foundation.layout.Spacer(Modifier.fillMaxWidth().aspectRatio(cols / rows.toFloat()).semantics { contentDescription = a11y }.drawWithCache {
            val cell = size.width / cols
            val shapes = days.mapIndexed { i, (d, l) ->
                val c = Offset((i % cols + 0.5f) * cell, (i / cols + 0.5f) * cell)
                Triple(handCircle(c, cell / 2 / G.Mood.gap, d.toEpochDay().toInt()), l, d.toEpochDay().toInt())
            }
            val paths = shapes.map { (pts, l, _) -> Triple(Crayon.path(pts), l, pts) }
            onDrawBehind {
                // 결 한 겹에 모두 칠하고, 선도 한 겹에 (동그라미마다 겹을 만들면 무거워짐)
                with(Crayon) {
                    textured(fillMask) {
                        paths.forEach { (path, l, _) ->
                            if (l != null) { val base = moodColor(l.feeling); drawPath(path, if (night) lerp(base, Color.Black, G.Mood.nightDarken) else base) }
                        }
                    }
                    textured(lineMask) {
                        paths.forEachIndexed { i, (_, l, pts) ->
                            stroke(pts, G.Mood.line * u, if (l != null) ink.copy(alpha = if (night) 0.5f else 0.55f) else future, shapes[i].third, passes = 1)
                        }
                    }
                }
            }
        })
        TokenText(stringResource(R.string.mood_sub), Tokens.TypeScale.caption1, color = p.secondary)
    }
}

// ───────────────────────── 계절의 편지 ─────────────────────────

/** 봉투 한 장 (손으로 그린 선: 몸통 + 접힌 덮개). */
@Composable
private fun Envelope(modifier: Modifier) {
    val ink = Theme.gc.ink
    val ctx = LocalContext.current
    val u = with(LocalDensity.current) { Theme.unit.toPx() }
    val lineMask = Crayon.tooth(GardenArt.toothLine(ctx), u); val fillMask = Crayon.tooth(GardenArt.toothFill(ctx), u)
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val body = Crayon.roundRect(u * 1.5f, u * 1.5f, w - u * 3, h - u * 3, u * 2.5f, 7, u * 0.5f)
        val flap = listOf(Offset(u * 2.5f, u * 3f), Offset(w / 2, h * 0.58f), Offset(w - u * 2.5f, u * 3f))
        with(Crayon) {
            textured(fillMask) { fill(body, G.Mood.Colors.none, u) }
            textured(lineMask) { stroke(body, u * 1.4f, ink, 11); stroke(flap, u * 1.2f, ink, 13, closed = false) }
        }
        drawCircle(G.Party.Colors.cream, u * 2.2f, Offset(w / 2, h * 0.58f))   // 작은 봉인
    }
}

/** 둘째 장 맨 위: 이번 달에 도착해 아직 펼치지 않은 편지. 누르면 편지가 펼쳐진다. */
@Composable
internal fun LetterEnvelope(letter: Letter, onOpen: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    Row(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 1100).clickable(onClick = onOpen)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        Envelope(Modifier.size(u * G.Letter.envelope, u * G.Letter.envelope * 0.7f))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            TokenText(stringResource(R.string.letter_arrived, Labels.season(ctx, letter.season)), Tokens.TypeScale.subhead, weight = FontWeight.SemiBold)
            TokenText(stringResource(R.string.recall_open), Tokens.TypeScale.footnote, color = p.secondary)
        }
    }
}

/**
 * 편지 한 장: 달마다 묶어 날짜 · 마음 · 글. 슬픔 · 걱정 · 실망은 기본으로 ‘흘려보낸 마음 N번’만 (‘함께 보기’로 펼침).
 * 끝에 한 줄 인사. 펼치면 연 편지로 남는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LetterSheet(letter: Letter, onClose: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    var heavy by remember(letter.id) { mutableStateOf(false) }
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val month = DateTimeFormatter.ofPattern("MMMM", ctx.resources.configuration.locales[0])
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Theme.gc.paper, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp8),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        ) {
            TokenText(stringResource(R.string.letter_title, "${letter.arrives.year}", Labels.season(ctx, letter.season)), Tokens.TypeScale.title3.serif())
            TokenText(stringResource(R.string.letter_range, letter.from.format(date), letter.until.format(date)), Tokens.TypeScale.caption1, color = p.secondary)
            CrayonRule(seed = 1110)
            val shown = (if (heavy) letter.lines + letter.heavy else letter.lines).sortedBy { it.date }
            shown.groupBy { it.date.withDayOfMonth(1) }.forEach { (m, list) ->
                TokenText(m.format(month), Tokens.TypeScale.subhead, color = p.secondary, weight = FontWeight.SemiBold)
                list.forEach { l -> LetterLine(l, date) }
            }
            if (letter.heavy.isNotEmpty() && !heavy) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                TokenText(stringResource(R.string.letter_heavy, "${letter.heavy.size}"), Tokens.TypeScale.footnote, color = p.secondary)
                TokenText("·", Tokens.TypeScale.footnote, color = p.secondary)
                TokenText(stringResource(R.string.letter_showHeavy), Tokens.TypeScale.footnote, Modifier.clickable { heavy = true }.padding(vertical = Tokens.Space.sp2), weight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(Tokens.Space.sp2))
            TokenText(stringResource(R.string.letter_end), Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp2))
            GardenButton(stringResource(R.string.letter_fold), onClose, filled = false, seed = 1112)
        }
    }
}

@Composable
private fun LetterLine(l: DayLine, date: DateTimeFormatter) {
    val p = Theme.palette
    Column(Modifier.fillMaxWidth().padding(start = Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
        TokenText(listOfNotNull(l.date.format(date), l.feeling?.let { stringResource(feelingName(it)) }).joinToString(" · "), Tokens.TypeScale.caption1, color = p.secondary)
        TokenText(l.text, Tokens.TypeScale.callout.serif())
    }
}

/** 모은 것 화면 아래: 받은 편지 (최근 것부터). */
@Composable
internal fun ReceivedLetters(state: AppState, today: LocalDate) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val list = remember(state.lines, today, state.keepLines) { state.received(today) }
    if (list.isEmpty()) return
    var open by remember { mutableStateOf<Letter?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(stringResource(R.string.letters), Tokens.TypeScale.title3)
        TokenText(stringResource(R.string.letters_sub), Tokens.TypeScale.caption1, color = p.secondary)
        list.forEachIndexed { i, l ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).crayonBox(null, G.Radius.box, G.Stroke.chip, 1120 + i).clickable { state.openLetter(l.id); open = l }
                    .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp2),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
            ) {
                Envelope(Modifier.size(u * G.Letter.envelope * 0.7f, u * G.Letter.envelope * 0.49f))
                TokenText(stringResource(R.string.letter_title, "${l.arrives.year}", Labels.season(ctx, l.season)), Tokens.TypeScale.subhead, Modifier.weight(1f))
            }
        }
    }
    open?.let { LetterSheet(it) { open = null } }
}

// ───────────────────────── 고마움 책 · 흘려보낸 마음 ─────────────────────────

/**
 * 모은 것 아래: 고마움으로 보낸 줄만 모은 작은 책 (옆으로 한 장씩), 그리고 무거운 마음은 글 없이 흘려보낸 횟수 한 줄만.
 * 기록 남기기를 끄면 보이지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThanksAndLetGo(state: AppState) {
    if (!state.keepLines) return
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val thanks = remember(state.lines) { state.lines.filter { it.feeling == Feeling.THANKS && it.text.isNotBlank() }.sortedBy { it.date } }
    val heavy = remember(state.lines) { state.lines.count { it.feeling in io.github.graviton94.carpediem.core.Letters.HEAVY } }
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        if (thanks.isNotEmpty()) {
            TokenText(stringResource(R.string.thanks_book), Tokens.TypeScale.title3)
            Row(
                Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 1130).clickable { open = true }
                    .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
            ) {
                androidx.compose.foundation.Image(GardenArt.obj(ctx, "feather"), null, Modifier.size(u * G.LetGo.feather * 0.8f))
                TokenText(stringResource(R.string.thanks_sub), Tokens.TypeScale.subhead, Modifier.weight(1f))
            }
        }
        if (heavy > 0) TokenText(stringResource(R.string.letgo_count, "$heavy"), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
    }
    if (open) ModalBottomSheet(onDismissRequest = { open = false }, containerColor = Theme.gc.paper, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        ThanksPages(thanks) { open = false }
    }
}

/** 고마움 책의 장들: 한 장에 한 줄, 마지막 장은 쌓인 날을 알려 주는 한 줄. */
@Composable
private fun ThanksPages(list: List<DayLine>, onClose: () -> Unit) {
    val p = Theme.palette
    val pages = list.size + 1
    val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = 0) { pages }
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = Tokens.Space.sp6), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
        TokenText(stringResource(R.string.thanks_book), Tokens.TypeScale.title3.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
        androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Theme.deviceClass.pageMargin), pageSpacing = Tokens.Space.sp3) { i ->
            Column(
                Modifier.fillMaxWidth().heightIn(min = Theme.unit * 180).crayonBox(null, G.Radius.box, G.Stroke.box, 1140 + i % 7).padding(Tokens.Space.sp6),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (i < list.size) {
                    TokenText(list[i].date.format(date), Tokens.TypeScale.caption1, color = p.secondary)
                    TokenText(list[i].text, Tokens.TypeScale.headline.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                } else TokenText(stringResource(R.string.thanks_end), Tokens.TypeScale.headline.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
            }
        }
        TokenText(stringResource(R.string.thanks_page, "${minOf(pager.currentPage + 1, list.size)}", "${list.size}"), Tokens.TypeScale.caption1, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        Box(Modifier.padding(horizontal = Theme.deviceClass.pageMargin)) { GardenButton(stringResource(R.string.garden_close), onClose, filled = false, seed = 1150) }
    }
}
