package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
import io.github.graviton94.carpediem.ui.pop
import io.github.graviton94.carpediem.ui.modalBox
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.LinearEasing
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.core.DayLine
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Care
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

internal fun feelingName(f: Feeling) = when (f) {
    Feeling.JOY -> R.string.feeling_joy; Feeling.HOPE -> R.string.feeling_hope; Feeling.CALM -> R.string.feeling_calm; Feeling.THANKS -> R.string.feeling_thanks
    Feeling.DISAPPOINT -> R.string.feeling_disappoint; Feeling.SAD -> R.string.feeling_sad; Feeling.WORRY -> R.string.feeling_worry
}


// ───── 정원(크레용) · 유리 두 디자인에서 함께 쓰는 작은 부품 ─────

/** 상자: 정원은 크레용 선, 유리는 옅은 유리판. strong = 조금 더 눈에 띄게 (돌아온 한 줄). */
@Composable
private fun Modifier.lineBox(seed: Int, strong: Boolean = false, pill: Boolean = false): Modifier {
    if (Theme.garden) return crayonBox(if (strong) Theme.gc.chip else Theme.gc.paper, if (pill) G.Radius.chip else G.Radius.box, G.Stroke.chip, seed)
    val p = Theme.palette
    val shape = RoundedCornerShape(if (pill) Tokens.Radius.pill else Tokens.Radius.md)
    return clip(shape).background(if (strong) p.olive.copy(alpha = 0.14f) else p.glass).border(Tokens.Stroke.line, p.glassEdge, shape)
}

@Composable
private fun Chip(text: String, selected: Boolean, seed: Int, onClick: () -> Unit) {
    if (Theme.garden) { GardenChip(text, selected, seed, onClick); return }
    val p = Theme.palette
    Box(Modifier.heightIn(min = Tokens.Layout.tapTarget), contentAlignment = Alignment.Center) {
        Box(Modifier.clip(RoundedCornerShape(Tokens.Radius.pill)).background(if (selected) p.olive else p.dim).clickable(onClick = onClick)
            .padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1)) {
            TokenText(text, Tokens.TypeScale.subhead, color = if (selected) p.onOlive else p.foreground, weight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun Action(text: String, filled: Boolean, seed: Int, onClick: () -> Unit) {
    if (Theme.garden) { GardenButton(text, onClick, filled = filled, seed = seed); return }
    val p = Theme.palette
    Box(Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp2).clip(RoundedCornerShape(Tokens.Radius.pill))
        .background(if (filled) p.olive else p.dim).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        TokenText(text, Tokens.TypeScale.headline, color = if (filled) p.onOlive else p.foreground)
    }
}

/** 깃털 그림 (정원만). 유리 디자인은 그림 없이 글만. */
@Composable
private fun Feather(size: Dp) { if (Theme.garden) Image(GardenArt.obj(LocalContext.current, "feather"), null, Modifier.size(size)) }

/** 오늘의 한 줄에서 글꼴: 정원은 명조(세리프), 유리는 기본 글꼴로 조금 더 모던하게. */
private fun lineType(t: io.github.graviton94.carpediem.design.TypeToken, garden: Boolean) = if (garden) t.serif() else t

/**
 * 오늘의 한 줄: 기쁨도 슬픔도 한 줄에 실어 떠나보낸다. 하루에 한 번.
 * 보내면 글이 깃털에 실려 하늘로 올라가며 옅어지고, 그 뒤로는 오늘 쓴 글을 다시 보여 주지 않는다 (기기 안에만 남음).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LetGoSection(state: AppState, today: LocalDate, modifier: Modifier = Modifier) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val px = with(LocalDensity.current) { u.toPx() }
    val focus = LocalFocusManager.current
    val max = G.LetGo.maxChars.toInt()
    var text by rememberSaveable { mutableStateOf("") }
    var feeling by rememberSaveable { mutableStateOf<Feeling?>(null) }
    // 누구에게 (선택): 생일인 사람이 있으면 먼저 골라 둠
    val birthdayId = state.people.firstOrNull { io.github.graviton94.carpediem.core.Family.isBirthday(it.birth, today) }?.id
    var to by rememberSaveable(birthdayId) { mutableStateOf(birthdayId) }
    var flying by remember { mutableStateOf<String?>(null) }
    val fly = remember { Animatable(0f) }
    LaunchedEffect(flying) {
        if (flying == null) return@LaunchedEffect
        fly.snapTo(0f); fly.animateTo(1f, tween(G.Motion.letGoMs.toInt(), easing = LinearOutSlowInEasing)); flying = null
    }
    // 다른 날의 한 줄: 고른 지난 날 (null = 오늘). 그날엔 질문 · 돌아온 한 줄 없이 쓰기만
    val day = state.writeDay
    var picking by remember { mutableStateOf(false) }
    val sent = if (day == null) state.sentOn(today) else false
    val formView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    fun send() {
        if (text.isBlank()) return
        val who = to?.takeIf { id -> state.people.any { it.id == id } }
        flying = text.trim()
        if (day != null) state.letGoOn(day, text, feeling, who) else state.letGo(text, feeling, who)
        text = ""; feeling = null; focus.clearFocus()
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(if (day == null) stringResource(R.string.letgo_title) else stringResource(R.string.letgo_dayTitle, RecordText.day(ctx, day)), Tokens.TypeScale.title3)
        // 오늘 | 다른 날 (기본은 늘 오늘)
        if (Theme.garden && state.profile != null) Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Chip(stringResource(R.string.letgo_today), day == null, seed = 950) { state.writeDay = null }
            Chip(day?.let { RecordText.day(ctx, it) } ?: stringResource(R.string.letgo_other), day != null, seed = 951) { picking = true }
        }
        // 때에 맞는 소개 (아침 · 저녁 · 밤), 낮은 원래 말
        val sub = remember(today) { io.github.graviton94.carpediem.ui.Labels.timed(ctx, "letgo_sub", io.github.graviton94.carpediem.ui.Labels.part(state.fixedNow ?: java.time.LocalDateTime.now())) }
        TokenText(sub ?: stringResource(R.string.letgo_sub), lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary)
        // 몇 해 전 오늘 보낸 한 줄: 먼저 조용히 알리고, 누르면 펼친다
        val recalls = remember(state.lines, today, day) { if (day != null) emptyList() else Lines.yearsAgo(state.lines, today) }
        recalls.forEach { (years, l) -> RecallCard(stringResource(R.string.recall_title, "$years"), l, 990 + years) }
        // 정한 주기 없이 문득 찾아온 지난 한 줄
        state.randomLine?.takeIf { r -> day == null && recalls.none { it.second.date == r.date } }?.let { r ->
            RecallCard(stringResource(R.string.recall_random, r.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))), r, 996)
        }
        val line = flying
        when {
            // 떠나보내는 중: 깃털과 함께 위로, 옆으로 살짝 흔들리며 옅어진다
            line != null -> Box(Modifier.fillMaxWidth().heightIn(min = u * G.LetGo.rise * 0.6f), contentAlignment = Alignment.BottomCenter) {
                Row(
                    Modifier.graphicsLayer {
                        val f = fly.value
                        translationY = -G.LetGo.rise * px * f
                        translationX = G.LetGo.drift * px * sin(f * PI * 1.5).toFloat()
                        rotationZ = -6f * f; alpha = (1f - f) * (1f - f)
                    }.lineBox(960, pill = true).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp2),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
                ) {
                    Feather(u * G.LetGo.feather)
                    TokenText(line, lineType(Tokens.TypeScale.callout, Theme.garden), maxLines = 2)
                }
            }
            // 오늘은 이미 보냄
            sent -> {
                val shown = remember { Animatable(0f) }
                LaunchedEffect(Unit) { shown.animateTo(1f, tween(G.Motion.pageMs.toInt())) }
                Row(Modifier.fillMaxWidth().graphicsLayer { alpha = shown.value }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                    Feather(u * G.LetGo.feather)
                    TokenText(stringResource(R.string.letgo_done), Tokens.TypeScale.subhead, Modifier.weight(1f))
                }
            }
            else -> Column(Modifier.fillMaxWidth().bringIntoViewRequester(formView), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                // 질문에 답하는 중이면 입력칸 위에 질문 한 줄
                state.answering?.takeIf { day == null }?.let { q ->
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        TokenText(stringResource(R.string.question_label), Tokens.TypeScale.caption1, color = p.secondary)
                        TokenText(io.github.graviton94.carpediem.data.Words.main(q, state.quoteLanguage, io.github.graviton94.carpediem.data.Words.lang(ctx)), lineType(Tokens.TypeScale.headline, Theme.garden))
                    }
                }
                TokenText(stringResource(R.string.letgo_feeling), Tokens.TypeScale.caption1, color = p.secondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    Feeling.entries.forEachIndexed { i, f ->
                        Chip(stringResource(feelingName(f)), feeling == f, seed = 970 + i) { feeling = if (feeling == f) null else f }
                    }
                }
                if (Theme.garden && state.people.isNotEmpty()) {
                    TokenText(stringResource(R.string.letgo_to), Tokens.TypeScale.caption1, color = p.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        state.people.forEachIndexed { i, person -> Chip(person.name, to == person.id, seed = 980 + i) { to = if (to == person.id) null else person.id } }
                    }
                }
                // 쓰는 중에는 기본 글꼴 (글자를 칠 때마다 글꼴이 바뀌지 않게)
                val style = Tokens.TypeScale.callout.style().copy(color = p.foreground)
                BasicTextField(
                    value = text,
                    onValueChange = { v -> val one = v.replace('\n', ' '); if (one.codePointCount(0, one.length) <= max) text = one },
                    singleLine = true, textStyle = style, cursorBrush = SolidColor(p.foreground),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                    // 키보드가 올라온 뒤 입력칸 · 보내기 버튼이 보이게 끌어올린다
                    modifier = Modifier.fillMaxWidth().onFocusEvent { f -> if (f.isFocused) scope.launch { delay(G.Motion.keyboardMs.toLong()); formView.bringIntoView() } }
                        .lineBox(964).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) TokenText(stringResource(if (day == null) R.string.letgo_hint else R.string.letgo_dayHint), Tokens.TypeScale.callout, color = p.secondary)
                            inner()
                        }
                    },
                )
                TokenText("${text.codePointCount(0, text.length)} / $max", Tokens.TypeScale.caption2, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.End)
                Action(stringResource(if (day == null) R.string.letgo_send else R.string.letgo_daySend), filled = text.isNotBlank(), seed = 968) { send() }
                if (day != null) TokenText(stringResource(R.string.letgo_backToday), Tokens.TypeScale.footnote,
                    Modifier.fillMaxWidth().clickable { state.writeDay = null }.padding(vertical = Tokens.Space.sp1), color = p.secondary, align = TextAlign.Center)
            }
        }
        TokenText(stringResource(R.string.letgo_privacy), Tokens.TypeScale.caption1, color = p.secondary)
    }
    // 날짜 고르기: 생일부터 어제까지, 이미 한 줄이 있는 날은 고를 수 없음 (하루에 한 줄)
    if (picking) {
        val zone = java.time.ZoneOffset.UTC
        val birth = state.profile?.birthDate ?: today
        val taken = remember(state.lines) { state.lines.map { it.date }.toSet() }
        val dp = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = (day ?: today.minusDays(1)).atStartOfDay().toInstant(zone).toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                    return d.isBefore(today) && !d.isBefore(birth) && d !in taken
                }
                override fun isSelectableYear(year: Int): Boolean = year in birth.year..today.year
            })
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms -> val d = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate(); if (state.canWriteOn(d, today)) state.writeDay = d }
                    picking = false
                }) { androidx.compose.material3.Text(stringResource(R.string.done)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { picking = false }) { androidx.compose.material3.Text(stringResource(R.string.cancel)) } },
        ) { androidx.compose.material3.DatePicker(state = dp) }
    }
}

/** 돌아온 한 줄 (몇 해 전 오늘 · 문득). 처음엔 접혀 있고, 펼쳐 읽은 뒤 ‘다시 보내기’로 오늘은 접어 둔다. */
@Composable
private fun RecallCard(title: String, line: DayLine, seed: Int) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    var opened by rememberSaveable(line.date) { mutableStateOf(false) }
    var gone by rememberSaveable(line.date) { mutableStateOf(false) }
    if (gone) return
    Column(
        Modifier.fillMaxWidth().lineBox(seed, strong = true).clickable { opened = true }
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Feather(u * G.LetGo.feather * 0.7f)
            TokenText(title, Tokens.TypeScale.subhead, Modifier.weight(1f), weight = FontWeight.SemiBold)
        }
        if (!opened) TokenText(stringResource(R.string.recall_open), Tokens.TypeScale.footnote, color = p.secondary)
        else {
            val meta = listOfNotNull(line.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), line.feeling?.let { stringResource(feelingName(it)) }).joinToString(" · ")
            TokenText(meta, Tokens.TypeScale.caption1, color = p.secondary)
            TokenText(line.text, lineType(Tokens.TypeScale.headline, Theme.garden))
            TokenText(stringResource(R.string.recall_close), Tokens.TypeScale.footnote, Modifier.clickable { gone = true }.padding(vertical = Tokens.Space.sp1), color = p.olive, weight = FontWeight.SemiBold)
        }
    }
}


/**
 * 한 줄을 보낸 뒤: 마음에 맞춘 한마디가 담긴 창이 조용히 떠오른다. ‘확인’으로 닫는다.
 * 창 밖을 눌러도 닫히지 않는다 (한마디를 읽을 틈). 뒤로 가기는 닫기.
 */
@Composable
fun LetGoModal(state: AppState, modifier: Modifier = Modifier, onCare: (Care) -> Unit = {}) {
    val msg = state.toast ?: return
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    // 창은 앱의 다른 떠오르는 것과 같은 나타남
    val card = io.github.graviton94.carpediem.ui.rememberPop(msg)
    BackHandler { state.toast = null; state.care = null }
    val feather = GardenArt.obj(ctx, "feather")
    val scrim = Theme.gc.scrim
    BoxWithConstraints(
        modifier.fillMaxSize()
            .drawBehind { drawRect(scrim.copy(alpha = scrim.alpha * card.value)) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // 깃털 비는 쓰지 않음 (보내면 창만 조용히 떠오름)
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = Theme.deviceClass.pageMargin)
                .pop(card).modalBox().padding(Tokens.Space.sp6).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        ) {
            Image(feather, null, Modifier.size(u * G.LetGo.feather * 1.3f))
            TokenText(state.toastTitle ?: stringResource(R.string.letgo_modalTitle), lineType(Tokens.TypeScale.title3, Theme.garden), align = TextAlign.Center)
            TokenText(msg, lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp2))
            Action(stringResource(R.string.letgo_ok), filled = true, seed = 999) { state.toast = null; state.care = null }
            // 그림으로 보내기: 오늘 보낸 한 줄을 카드 한 장으로 (글을 남겼을 때만)
            val todayLine = state.lines.lastOrNull { it.date == (state.fixedNow ?: java.time.LocalDateTime.now()).toLocalDate() }
            if (Theme.garden && todayLine != null && todayLine.text.isNotBlank()) TokenText(stringResource(R.string.share_image), Tokens.TypeScale.footnote,
                Modifier.clickable {
                    io.github.graviton94.carpediem.share.ShareCards.send(ctx, io.github.graviton94.carpediem.share.ShareCards.line(ctx, todayLine,
                        io.github.graviton94.carpediem.share.ShareCards.feelingName(ctx, todayLine.feeling), state.store.haruSeed), "line-${todayLine.date}")
                }.padding(vertical = Tokens.Space.sp1), color = p.secondary, align = TextAlign.Center)
            // 돌봄 권하기: 확인 아래 작은 한 줄 (지나쳐도 되는 곳에)
            state.care?.let { c -> CareLine(state, c) { state.toast = null; state.care = null; onCare(c) } }
        }
    }
}

/** 한마디 창 맨 아래 권유 한 줄. 누구에게는 가족 이름 칩, 나머지는 누르면 바로 그 일로. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareLine(state: AppState, c: Care, onGo: () -> Unit) {
    val p = Theme.palette
    if (c == Care.SEND_TO) {
        TokenText(stringResource(R.string.care_sendTo), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            state.people.forEachIndexed { i, person -> Chip(person.name, false, seed = 1200 + i) { state.sendTodayTo(person.id); state.toast = null; state.care = null } }
        }
        return
    }
    val text = stringResource(when (c) { Care.CALM_BREATH -> R.string.care_calm; Care.BOX_BREATH -> R.string.care_box; Care.SLEEP_BREATH -> R.string.care_sleep; Care.MORNING_BREATH -> R.string.care_morning; else -> R.string.care_look })
    TokenText(text, Tokens.TypeScale.footnote, Modifier.clickable(onClick = onGo).padding(vertical = Tokens.Space.sp2), color = p.olive, weight = FontWeight.SemiBold, align = TextAlign.Center)
}
