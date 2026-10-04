package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Capsule
import io.github.graviton94.carpediem.core.CapsuleWhen
import io.github.graviton94.carpediem.core.Capsules
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.core.Rings
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * 추억을 미래로, 지난 해를 한 장으로.
 *   미래의 나에게 (10): 항아리에 담아 나무 밑에 묻음. 고른 날 (다음 생일 · 1년 뒤 · 석 달 뒤) 전엔 열 수 없음.
 *   생일 아침의 나이테 (07): 지난 생일부터 이번 생일 전날까지, 열두 달의 마음 색이 고리 하나.
 */

private fun whenName(w: CapsuleWhen) = when (w) { CapsuleWhen.SEASON -> R.string.capsule_season; CapsuleWhen.BIRTHDAY -> R.string.capsule_birthday; CapsuleWhen.YEAR -> R.string.capsule_year }

/** 언제 열까요: 석 달 뒤 · 다음 생일 · 1년 뒤. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WhenChips(selected: CapsuleWhen, seasonLabel: Int = R.string.capsule_season, onPick: (CapsuleWhen) -> Unit) {
    val p = Theme.palette
    TokenText(stringResource(R.string.capsule_when), Tokens.TypeScale.caption1, color = p.secondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        CapsuleWhen.entries.forEachIndexed { i, w -> GardenChip(stringResource(if (w == CapsuleWhen.SEASON) seasonLabel else whenName(w)), selected == w, 1500 + i) { onPick(w) } }
    }
}

/** 미래의 나에게 쓰기 (추억 · 나이테에서). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FutureLetterSheet(state: AppState, start: CapsuleWhen, today: LocalDate, onDismiss: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var w by rememberSaveable { mutableStateOf(start) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Theme.gc.paper) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            TokenText(stringResource(R.string.capsule_title), Tokens.TypeScale.title3.serif())
            TokenText(stringResource(R.string.capsule_sub), Tokens.TypeScale.footnote, color = p.secondary)
            BasicTextField(
                value = text, onValueChange = { v -> if (v.codePointCount(0, v.length) <= Capsules.MAX_CHARS && v.count { it == '\n' } < Lines.MAX_LINES) text = v },
                minLines = 3, maxLines = Lines.MAX_LINES, textStyle = Tokens.TypeScale.callout.style().copy(color = p.foreground), cursorBrush = SolidColor(p.foreground),
                modifier = Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1510).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                decorationBox = { inner -> Box { if (text.isEmpty()) TokenText(stringResource(R.string.capsule_hint), Tokens.TypeScale.callout, color = p.secondary); inner() } },
            )
            WhenChips(w, R.string.capsule_seasonPlain) { w = it }
            val opens = Capsules.opensOn(w, today, state.profile?.birthDate)
            TokenText(stringResource(R.string.capsule_opensOn, RecordText.day(ctx, opens)), Tokens.TypeScale.footnote, color = p.secondary)
            GardenButton(stringResource(R.string.capsule_bury), {
                if (text.isNotBlank()) state.bury(text, w, today)?.let { d -> state.say(ctx.getString(R.string.capsule_buried, RecordText.day(ctx, d))); onDismiss() }
            }, filled = text.isNotBlank(), seed = 1511)
        }
    }
}

/** 열린 항아리 한 통. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CapsuleSheet(c: Capsule, onDone: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDone, containerColor = Theme.gc.paper) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            Jar(Theme.unit * 34f, open = true)
            TokenText(stringResource(R.string.capsule_from, RecordText.day(ctx, c.written)), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
            TokenText(c.text, Tokens.TypeScale.title3.serif(), Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1520).padding(Tokens.Space.sp5), align = TextAlign.Center)
            GardenButton(stringResource(R.string.capsule_received), onDone, filled = true, seed = 1521)
        }
    }
}

/** 한지 항아리 (닫힘 · 열림). */
@Composable
internal fun Jar(size: Dp, open: Boolean = false) {
    val ink = Theme.gc.ink
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 10f
        drawOval(Color(0xFF8C6A44).copy(alpha = 0.35f), Offset(0.6f * k, 8.6f * k), Size(8.8f * k, 1.2f * k))
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(2.6f * k, 3.2f * k); lineTo(7.4f * k, 3.2f * k); quadraticTo(8.6f * k, 3.4f * k, 8.4f * k, 4.8f * k)
            lineTo(7.8f * k, 8.2f * k); quadraticTo(7.6f * k, 9.2f * k, 6.6f * k, 9.2f * k); lineTo(3.4f * k, 9.2f * k)
            quadraticTo(2.4f * k, 9.2f * k, 2.2f * k, 8.2f * k); lineTo(1.6f * k, 4.8f * k); quadraticTo(1.4f * k, 3.4f * k, 2.6f * k, 3.2f * k); close()
        }
        drawPath(body, Color(0xFFC9B79A)); drawPath(body, ink, style = Stroke(k * 0.3f))
        drawLine(Color.White.copy(alpha = 0.5f), Offset(3.4f * k, 5.6f * k), Offset(6.6f * k, 5.6f * k), k * 0.35f, StrokeCap.Round)
        if (open) {
            drawRoundRect(Color(0xFF8C6A44), Offset(5.4f * k, 1.0f * k), Size(4f * k, 1.2f * k), CornerRadius(0.5f * k))
            drawCircle(Color(0xFFF5B45C).copy(alpha = 0.35f), 2.4f * k, Offset(5f * k, 3f * k))
        } else {
            drawRoundRect(Color(0xFF8C6A44), Offset(2.2f * k, 2.4f * k), Size(5.6f * k, 1.2f * k), CornerRadius(0.5f * k))
            drawCircle(Color(0xFFB5651D), 0.45f * k, Offset(5f * k, 2.2f * k))
        }
    }
}

/** 정원의 나무 밑: 아직 묻혀 있는 항아리가 있으면 작은 흙더미 + 뚜껑 끝. 누르면 열리는 날을 한 줄로. */
@Composable
internal fun JarMound(state: AppState, today: LocalDate, x: Dp, gy: Dp) {
    val sealed = remember(state.capsules, today) { Capsules.sealed(state.capsules, today) }
    if (sealed.isEmpty()) return
    val ctx = LocalContext.current
    val u = Theme.unit
    val w = u * 12f
    val label = stringResource(R.string.capsule_mound, RecordText.day(ctx, sealed.minOf { it.opens }))
    Canvas(Modifier.offset(x - w / 2, gy - w * 0.45f).size(w, w * 0.5f).semantics { contentDescription = label }.clickable { state.say(label) }) {
        val k = size.width / 12f
        drawOval(Color(0xFF7A5A38).copy(alpha = 0.75f), Offset(0.5f * k, 2.4f * k), Size(11f * k, 3.4f * k))
        drawRoundRect(Color(0xFF8C6A44), Offset(4.2f * k, 1.6f * k), Size(3.6f * k, 1.1f * k), CornerRadius(0.5f * k))
        drawCircle(Color(0xFFB5651D), 0.4f * k, Offset(6f * k, 1.4f * k))
    }
}

// ───────────────────────── 생일 아침의 나이테 ─────────────────────────

/** 나이테 한 장: 안쪽은 지난 해들 (옅게), 바깥 고리는 이번 해의 열두 달 마음 색. 한 해 걸어보기 · 다음 생일에 열 편지. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RingSheet(state: AppState, profile: LifeProfile, age: Int, now: LocalDateTime, onDismiss: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val ring = remember(age, state.lines) { Rings.of(profile.birthDate, age, state.lines, state.store.haruSeed) }
    val older = remember(age, state.lines) { Rings.done(profile.birthDate, now.toLocalDate(), state.lines).filter { it < age }.size }
    var walk by remember { mutableStateOf(false) }
    var letter by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Theme.gc.paper) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            TokenText("${RecordText.day(ctx, ring.start)} — ${RecordText.day(ctx, ring.end)}", Tokens.TypeScale.caption1, color = p.secondary)
            TokenText(stringResource(R.string.ring_title, "$age"), Tokens.TypeScale.title3.serif())
            RingArt(ring.months, older, age, Theme.unit * 120f)
            Column(Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1530).padding(Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                TokenText(stringResource(R.string.ring_count, "${ring.lines}", "${ring.thanks}"), Tokens.TypeScale.subhead)
                ring.top?.let { TokenText(stringResource(R.string.ring_top, Labels.feeling(ctx, it)), Tokens.TypeScale.subhead) }
                ring.pick?.let { l -> TokenText("“${l.text.replace('\n', ' ')}” · ${RecordText.day(ctx, l.date)}", Tokens.TypeScale.footnote.serif(), color = p.secondary) }
            }
            GardenButton(stringResource(if (walk) R.string.ring_walkClose else R.string.ring_walk), { walk = !walk }, filled = false, seed = 1531)
            if (walk) {
                val lines = remember(ring) { state.lines.filter { !it.date.isBefore(ring.start) && !it.date.isAfter(ring.end) && it.text.isNotBlank() } }
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    var lastSeason: io.github.graviton94.carpediem.core.Season? = null
                    lines.forEach { l ->
                        val season = io.github.graviton94.carpediem.core.Memories.seasonOf(l.date)
                        if (season != lastSeason) { lastSeason = season; TokenText(Labels.season(ctx, season), Tokens.TypeScale.caption1, Modifier.padding(top = Tokens.Space.sp2), color = p.olive) }
                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                            Box(Modifier.padding(top = Tokens.Space.sp1).size(Theme.unit * 3f).crayonBox(moodColor(l.feeling), 99f, 0.6f, 1540))
                            TokenText("${RecordText.day(ctx, l.date)}  ${l.text}", Tokens.TypeScale.footnote)
                        }
                    }
                }
            }
            GardenButton(stringResource(R.string.ring_letter), { letter = true }, filled = true, seed = 1532)
        }
    }
    if (letter) FutureLetterSheet(state, CapsuleWhen.BIRTHDAY, now.toLocalDate()) { letter = false }
}

/** 나이테 그림: older 개의 옅은 안쪽 고리 + 바깥 고리 열두 칸 (마음 색, 빈 달은 종이빛). */
@Composable
private fun RingArt(months: List<io.github.graviton94.carpediem.core.Feeling?>, older: Int, age: Int, size: Dp) {
    val ink = Theme.gc.ink
    val paper = Theme.gc.paper
    val serif = Tokens.TypeScale.title3.serif()
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val c = Offset(this.size.width / 2, this.size.height / 2)
            val outer = this.size.minDimension / 2 * 0.94f
            val band = outer * 0.16f
            val core = outer * 0.3f
            // 안쪽: 지난 해들 (많으면 최근 여섯만)
            val n = older.coerceAtMost(6)
            for (i in 0 until n) {
                val r = core + (outer - band - core) * (i + 1) / (n + 1)
                drawCircle(Color(0xFFB59A72).copy(alpha = 0.35f), r, c, style = Stroke(outer * 0.04f))
            }
            drawCircle(Color(0xFFE8D6B3), core, c)
            // 바깥 고리: 열두 달
            val r = outer - band / 2
            months.forEachIndexed { i, f ->
                drawArc(if (f == null) paper else moodColor(f), -90f + i * 30f + 1f, 28f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(band))
            }
            drawCircle(ink.copy(alpha = 0.25f), outer, c, style = Stroke(outer * 0.012f))
        }
        TokenText("$age", serif, color = Theme.palette.secondary)
    }
}

/** 추억: 미래의 나에게 · 열어 본 항아리 · 나이테. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeepsakesSection(state: AppState, profile: LifeProfile, now: LocalDateTime) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val today = now.toLocalDate()
    var writing by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<Capsule?>(null) }
    var ringAge by remember { mutableStateOf<Int?>(null) }
    Spacer(Modifier.height(Tokens.Space.sp4))
    TokenText(stringResource(R.string.capsule_section), Tokens.TypeScale.title3)
    TokenText(stringResource(R.string.capsule_sectionSub), Tokens.TypeScale.footnote, color = p.secondary)
    val sealed = remember(state.capsules, today) { Capsules.sealed(state.capsules, today) }
    if (sealed.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        Jar(Theme.unit * 10f)
        TokenText(stringResource(R.string.capsule_sealed, "${sealed.size}", RecordText.day(ctx, sealed.minOf { it.opens })), Tokens.TypeScale.footnote)
    }
    GardenButton(stringResource(R.string.capsule_write), { writing = true }, filled = false, seed = 1550)
    val opened = remember(state.capsules) { Capsules.openedOnes(state.capsules) }
    opened.take(20).forEachIndexed { i, c ->
        Row(Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable { reading = c }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Jar(Theme.unit * 8f, open = true)
            TokenText(stringResource(R.string.capsule_openedRow, RecordText.day(ctx, c.written), RecordText.day(ctx, c.opens)), Tokens.TypeScale.footnote, Modifier.weight(1f))
        }
    }
    // 나이테: 다 지나간 해 가운데 한 줄이 있는 해
    val rings = remember(state.lines, today) { Rings.done(profile.birthDate, today, state.lines) }
    if (rings.isNotEmpty()) {
        Spacer(Modifier.height(Tokens.Space.sp4))
        TokenText(stringResource(R.string.ring_section), Tokens.TypeScale.title3)
        TokenText(stringResource(R.string.ring_sectionSub), Tokens.TypeScale.footnote, color = p.secondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            rings.take(12).forEachIndexed { i, a -> GardenChip(stringResource(R.string.ring_chip, "$a"), false, 1560 + i) { ringAge = a } }
        }
    }
    if (writing) FutureLetterSheet(state, CapsuleWhen.BIRTHDAY, today) { writing = false }
    reading?.let { c -> CapsuleSheet(c) { reading = null } }
    ringAge?.let { a -> RingSheet(state, profile, a, now) { ringAge = null } }
}
