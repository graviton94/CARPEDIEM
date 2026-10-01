package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
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
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    val sent = state.sentOn(today)
    val formView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    fun send() {
        if (text.isBlank()) return
        flying = text.trim(); state.letGo(text, feeling, to?.takeIf { id -> state.people.any { it.id == id } }); text = ""; feeling = null; focus.clearFocus()
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(stringResource(R.string.letgo_title), Tokens.TypeScale.title3)
        // 때에 맞는 소개 (아침 · 저녁 · 밤), 낮은 원래 말
        val sub = remember(today) { io.github.graviton94.carpediem.ui.Labels.timed(ctx, "letgo_sub", io.github.graviton94.carpediem.ui.Labels.part(state.fixedNow ?: java.time.LocalDateTime.now())) }
        TokenText(sub ?: stringResource(R.string.letgo_sub), lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary)
        // 몇 해 전 오늘 보낸 한 줄: 먼저 조용히 알리고, 누르면 펼친다
        val recalls = remember(state.lines, today) { Lines.yearsAgo(state.lines, today) }
        recalls.forEach { (years, l) -> RecallCard(stringResource(R.string.recall_title, "$years"), l, 990 + years) }
        // 정한 주기 없이 문득 찾아온 지난 한 줄
        state.randomLine?.takeIf { r -> recalls.none { it.second.date == r.date } }?.let { r ->
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
                state.answering?.let { q ->
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        TokenText(stringResource(R.string.question_label), Tokens.TypeScale.caption1, color = p.secondary)
                        TokenText(if (state.quoteLanguage == io.github.graviton94.carpediem.data.QuoteLanguage.ENGLISH) q.english else q.korean, lineType(Tokens.TypeScale.headline, Theme.garden))
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
                            if (text.isEmpty()) TokenText(stringResource(R.string.letgo_hint), Tokens.TypeScale.callout, color = p.secondary)
                            inner()
                        }
                    },
                )
                TokenText("${text.codePointCount(0, text.length)} / $max", Tokens.TypeScale.caption2, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.End)
                Action(stringResource(R.string.letgo_send), filled = text.isNotBlank(), seed = 968) { send() }
            }
        }
        TokenText(stringResource(R.string.letgo_privacy), Tokens.TypeScale.caption1, color = p.secondary)
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

private class Flake(val x: Float, val delay: Float, val span: Float, val size: Float, val phase: Float, val spin: Float)

/**
 * 한 줄을 보낸 뒤: 깃털이 위에서 아래로 흩날리며 내려오고, 마음에 맞춘 한마디가 담긴 창이 뜬다. ‘확인’으로 닫는다.
 * 창 밖을 눌러도 닫히지 않는다 (한마디를 읽을 틈). 뒤로 가기는 닫기.
 */
@Composable
fun LetGoModal(state: AppState, modifier: Modifier = Modifier, onCare: (Care) -> Unit = {}) {
    val msg = state.toast ?: return
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val px = with(LocalDensity.current) { u.toPx() }
    val m = G.Motion
    val fall = remember(msg) { Animatable(0f) }
    val card = remember(msg) { Animatable(0f) }
    LaunchedEffect(msg) {
        launch { fall.animateTo(1f, tween(m.fallMs.toInt(), easing = LinearEasing)) }
        delay(m.cardDelayMs.toLong()); card.animateTo(1f, tween(m.modalFadeMs.toInt(), easing = LinearOutSlowInEasing))
    }
    BackHandler { state.toast = null; state.care = null }
    val feather = GardenArt.obj(ctx, "feather")
    val scrim = Theme.gc.scrim
    val flakes = remember(msg) {
        val r = Crayon.Rng(msg.hashCode())
        List(G.LetGo.feathers.toInt()) { Flake(0.06f + 0.88f * r.next(), r.next() * 0.35f, 0.5f + 0.25f * r.next(), 0.7f + 0.6f * r.next(), r.next() * 6.28f, (r.next() - 0.5f) * 60f) }
    }
    BoxWithConstraints(
        modifier.fillMaxSize()
            .drawBehind { drawRect(scrim.copy(alpha = scrim.alpha * card.value)) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        val w = constraints.maxWidth.toFloat(); val h = constraints.maxHeight.toFloat()
        flakes.forEach { f ->
            val side = u * G.LetGo.feather * f.size
            val sidePx = side.value * px / u.value
            Image(feather, null, Modifier.size(side).graphicsLayer {
                val t = ((fall.value - f.delay) / f.span).coerceIn(0f, 1f)
                translationX = f.x * w - sidePx / 2 + sin(t * PI.toFloat() * 3f + f.phase) * G.LetGo.sway * px
                translationY = -sidePx + (h + sidePx) * t
                rotationZ = f.spin * sin(t * PI.toFloat() * 2f + f.phase)
                alpha = if (t <= 0f || t >= 1f) 0f else minOf(1f, (1f - t) * 3f)
            })
        }
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = Theme.deviceClass.pageMargin)
                .graphicsLayer { alpha = card.value; val k = 0.94f + 0.06f * card.value; scaleX = k; scaleY = k }
                .modalBox().padding(Tokens.Space.sp6).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        ) {
            Image(feather, null, Modifier.size(u * G.LetGo.feather * 1.3f))
            TokenText(state.toastTitle ?: stringResource(R.string.letgo_modalTitle), lineType(Tokens.TypeScale.title3, Theme.garden), align = TextAlign.Center)
            TokenText(msg, lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp2))
            Action(stringResource(R.string.letgo_ok), filled = true, seed = 999) { state.toast = null; state.care = null }
            // 돌봄 권하기: 확인 아래 작은 한 줄 (지나쳐도 되는 곳에)
            state.care?.let { c -> CareLine(state, c) { state.toast = null; state.care = null; onCare(c) } }
        }
    }
}

/** 창 바탕: 정원은 종이 위 크레용 선, 유리는 불투명한 판 (뒤가 비치면 글이 흐려서). */
@Composable
private fun Modifier.modalBox(): Modifier {
    if (Theme.garden) return crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.box, 997)
    val p = Theme.palette
    val shape = RoundedCornerShape(Tokens.Radius.lg)
    return clip(shape).background(p.base).border(Tokens.Stroke.line, p.glassEdge, shape)
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
