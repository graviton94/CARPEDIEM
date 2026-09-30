package io.github.graviton94.carpediem.ui.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

private fun feelingName(f: Feeling) = when (f) {
    Feeling.JOY -> R.string.feeling_joy; Feeling.HOPE -> R.string.feeling_hope; Feeling.CALM -> R.string.feeling_calm; Feeling.THANKS -> R.string.feeling_thanks
    Feeling.DISAPPOINT -> R.string.feeling_disappoint; Feeling.SAD -> R.string.feeling_sad; Feeling.WORRY -> R.string.feeling_worry
}


// ───── 정원(크레용) · 유리 두 디자인에서 함께 쓰는 작은 부품 ─────

/** 상자: 정원은 크레용 선, 유리는 옅은 유리판. strong = 조금 더 눈에 띄게 (돌아온 한 줄). */
@Composable
private fun Modifier.lineBox(seed: Int, strong: Boolean = false, pill: Boolean = false): Modifier {
    if (Theme.garden) return crayonBox(if (strong) G.Colors.chip else G.Colors.paper, if (pill) G.Radius.chip else G.Radius.box, G.Stroke.chip, seed)
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
@OptIn(ExperimentalLayoutApi::class)
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
    var flying by remember { mutableStateOf<String?>(null) }
    val fly = remember { Animatable(0f) }
    LaunchedEffect(flying) {
        if (flying == null) return@LaunchedEffect
        fly.snapTo(0f); fly.animateTo(1f, tween(G.Motion.letGoMs.toInt(), easing = LinearOutSlowInEasing)); flying = null
    }
    val sent = state.sentOn == today
    fun send() {
        if (text.isBlank()) return
        flying = text.trim(); state.letGo(text, feeling); text = ""; feeling = null; focus.clearFocus()
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(stringResource(R.string.letgo_title), Tokens.TypeScale.title3)
        TokenText(stringResource(R.string.letgo_sub), lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary)
        // 몇 해 전 오늘 보낸 한 줄: 먼저 조용히 알리고, 누르면 펼친다
        val recalls = remember(state.lines, today) { Lines.yearsAgo(state.lines, today) }
        recalls.forEach { (years, l) -> RecallCard(years, l) }
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
            else -> {
                TokenText(stringResource(R.string.letgo_feeling), Tokens.TypeScale.caption1, color = p.secondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    Feeling.entries.forEachIndexed { i, f ->
                        Chip(stringResource(feelingName(f)), feeling == f, seed = 970 + i) { feeling = if (feeling == f) null else f }
                    }
                }
                val style = lineType(Tokens.TypeScale.callout, Theme.garden).style(text.ifEmpty { stringResource(R.string.letgo_hint) }).copy(color = p.foreground)
                BasicTextField(
                    value = text,
                    onValueChange = { v -> val one = v.replace('\n', ' '); if (one.codePointCount(0, one.length) <= max) text = one },
                    singleLine = true, textStyle = style, cursorBrush = SolidColor(p.foreground),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth().lineBox(964).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) TokenText(stringResource(R.string.letgo_hint), lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary)
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

/** 몇 해 전 오늘의 한 줄. 처음엔 접혀 있고, 펼쳐 읽은 뒤 ‘다시 보내기’로 오늘은 접어 둔다. */
@Composable
private fun RecallCard(years: Int, line: DayLine) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    var opened by rememberSaveable(line.date) { mutableStateOf(false) }
    var gone by rememberSaveable(line.date) { mutableStateOf(false) }
    if (gone) return
    Column(
        Modifier.fillMaxWidth().lineBox(990 + years, strong = true).clickable { opened = true }
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Feather(u * G.LetGo.feather * 0.7f)
            TokenText(stringResource(R.string.recall_title, "$years"), Tokens.TypeScale.subhead, Modifier.weight(1f), weight = FontWeight.SemiBold)
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
 * 보낸 뒤 화면 아래에 잠깐 떠오르는 한마디 (마음에 맞춘 말). 깃털이 조금 날아간 뒤 나타나 몇 초 뒤 스르르 사라진다. 누르면 바로 닫힘.
 */
@Composable
fun GardenToast(state: AppState, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val u = Theme.unit
    val px = with(LocalDensity.current) { u.toPx() }
    val msg = state.toast
    val a = remember { Animatable(0f) }
    var shown by remember { mutableStateOf<String?>(null) }
    val m = G.Motion
    LaunchedEffect(msg) {
        if (msg == null) { a.animateTo(0f, tween(m.toastFadeMs.toInt())); shown = null; return@LaunchedEffect }
        delay(m.toastDelayMs.toLong()); shown = msg
        a.snapTo(0f); a.animateTo(1f, tween(m.toastFadeMs.toInt())); delay(m.toastMs.toLong()); a.animateTo(0f, tween(m.toastFadeMs.toInt()))
        shown = null; if (state.toast == msg) state.toast = null
    }
    val text = shown ?: return
    Row(
        modifier.fillMaxWidth().graphicsLayer { alpha = a.value; translationY = (1f - a.value) * px * G.LetGo.drift * 0.5f }
            .lineBox(998).clickable { state.toast = null }
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        Feather(u * G.LetGo.feather * 0.8f)
        TokenText(text, lineType(Tokens.TypeScale.callout, Theme.garden), Modifier.weight(1f))
    }
}

