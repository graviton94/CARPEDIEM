package io.github.graviton94.carpediem.ui.garden

import androidx.compose.ui.graphics.graphicsLayer
import io.github.graviton94.carpediem.ui.GardenAlert
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Kind
import io.github.graviton94.carpediem.core.Memories
import io.github.graviton94.carpediem.core.Person
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.SkyBackground
import io.github.graviton94.carpediem.ui.TokenText
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** “2011년 봄” 처럼 계절까지만. */
@Composable
private fun seasonText(d: LocalDate): String = stringResource(R.string.memory_season, "${d.year}", Labels.season(LocalContext.current, Memories.seasonOf(d)))

/** 적어 준 날만: “2011년 봄부터 2024년 겨울까지” · “…부터” · “…까지” · 없으면 null. */
@Composable
private fun rangeText(p: Person): String? {
    val from = Memories.from(p); val until = p.until
    return when {
        from != null && until != null -> stringResource(R.string.memory_range, seasonText(from), seasonText(until))
        from != null -> stringResource(R.string.memory_from, seasonText(from))
        until != null -> stringResource(R.string.memory_until, seasonText(until))
        else -> null
    }
}

/**
 * 기억의 자리 (모은 것 안): 곁을 떠난 가족 · 반려동물의 돌이 이끼 방석 위에 눈을 감고 앉아 있다.
 * 숫자 · 알림 없이, 보고 싶을 때만 찾아오는 곳. 여기서만 그 돌에게 한 줄을 보낼 수 있다 (오늘의 한 줄 · 회상 · 편지와 섞이지 않음).
 */
@Composable
fun MemoryScreen(state: AppState, now: LocalDateTime, onAdd: () -> Unit, onBack: () -> Unit) {
    val p = Theme.palette
    BackHandler(onBack = onBack)
    SkyBackground {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp10),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp6),
        ) {
            PageBar(stringResource(R.string.memory), onBack)
            TokenText(stringResource(R.string.memory_sub), Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
            state.memories.forEachIndexed { i, m -> MemoryStone(state, m, now.toLocalDate(), 1300 + i * 10) }
            if (state.memories.size < Memories.MAX) TokenText(stringResource(R.string.memory_addMore), Tokens.TypeScale.footnote,
                Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(vertical = Tokens.Space.sp3), color = p.secondary, align = TextAlign.Center)
            else TokenText(stringResource(R.string.memory_full), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        }
    }
}

@Composable
private fun MemoryStone(state: AppState, m: Person, today: LocalDate, seed: Int) {
    val p = Theme.palette
    val u = Theme.unit
    val focus = LocalFocusManager.current
    var text by rememberSaveable(m.id) { mutableStateOf("") }
    var justSent by remember(m.id) { mutableStateOf(false) }
    var showLines by remember(m.id) { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var full by remember { mutableStateOf(false) }
    val sent = state.memoryLines.filter { it.to == m.id }
    val sentToday = sent.any { it.date == today }
    Column(Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.box, seed).padding(Tokens.Space.sp5),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        // 이끼 방석 위, 눈을 지긋이 감은 돌
        val art = HaruArt.of(m.seed, false)
        val scale = u * (G.Layout.meetHaruWidth * 0.7f * (if (m.kind == Kind.PET) G.Family.petScale else 1f) / G.Layout.haruArtWidth)
        val a11y = stringResource(R.string.memory_a11y, m.name)
        // 돌이 보이는 높이만큼만 자리를 잡고, 그림 칸(200)은 그 위로 겹쳐 그린다 (부모 크기에 눌리지 않게 unbounded)
        val mossH = u * 14
        val showH = scale * (G.Layout.haruGround - art.meta.bbox.top) + mossH / 2
        Box(Modifier.fillMaxWidth().height(showH).semantics { contentDescription = a11y }) {
            val moss = G.Colors.moss
            val ink = Theme.gc.ink
            Canvas(Modifier.align(Alignment.BottomCenter).size(scale * art.meta.bbox.width * 1.5f, mossH)) {
                drawOval(moss, size = size)
                drawOval(ink.copy(alpha = 0.5f), size = size, style = androidx.compose.ui.graphics.drawscope.Stroke(u.toPx() * 1.2f))
            }
            val dx = -(scale * (art.meta.bbox.center.x - art.meta.box / 2))
            Box(Modifier.align(Alignment.TopCenter).wrapContentSize(Alignment.TopCenter, unbounded = true).offset(x = dx, y = -(scale * art.meta.bbox.top)).size(scale * art.meta.box)) {
                HaruFigure(art, scale, Modifier, lid = 1f, tiltOn = false)
            }
        }
        Spacer(Modifier.height(Tokens.Space.sp2))
        TokenText(m.name, Tokens.TypeScale.title3.serif())
        rangeText(m)?.let { TokenText(it, Tokens.TypeScale.footnote, color = p.secondary) }
        CrayonRule(seed = seed + 1)
        // 한 줄 보내기 (그 돌마다 하루 한 번)
        when {
            justSent -> TokenText(stringResource(R.string.memory_sent, m.name), Tokens.TypeScale.subhead.serif(), align = TextAlign.Center)
            sentToday -> TokenText(stringResource(R.string.memory_sentToday), Tokens.TypeScale.footnote, color = p.secondary)
            else -> {
                fun send() { if (state.sendToMemory(m.id, text)) { text = ""; justSent = true; focus.clearFocus() } }
                BasicTextField(
                    value = text, onValueChange = { v -> val one = v.replace('\n', ' '); if (one.codePointCount(0, one.length) <= G.LetGo.maxChars.toInt()) text = one },
                    singleLine = true, textStyle = Tokens.TypeScale.callout.style().copy(color = p.foreground), cursorBrush = SolidColor(p.foreground),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, seed + 2).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                    decorationBox = { inner -> Box { if (text.isEmpty()) TokenText(stringResource(R.string.memory_sendHint), Tokens.TypeScale.callout, color = p.secondary); inner() } },
                )
                GardenButton(stringResource(R.string.memory_send, m.name), { send() }, filled = text.isNotBlank(), seed = seed + 3)
            }
        }
        if (sent.any { it.text.isNotBlank() }) {
            TokenText(if (showLines) stringResource(R.string.memory_count, "${sent.size}") else stringResource(R.string.memory_open), Tokens.TypeScale.footnote,
                Modifier.clickable { showLines = !showLines }.padding(vertical = Tokens.Space.sp1), color = p.secondary)
            if (showLines) sent.filter { it.text.isNotBlank() }.reversed().forEach { l ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                    TokenText(l.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), Tokens.TypeScale.caption1, color = p.secondary)
                    TokenText(l.text, Tokens.TypeScale.callout.serif())
                }
            }
        }
        // 하늘에 별로 두기 (기본 켬)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TokenText(stringResource(R.string.memory_star), Tokens.TypeScale.subhead)
                TokenText(stringResource(R.string.memory_starHelp), Tokens.TypeScale.caption1, color = p.secondary)
            }
            Switch(m.star, { state.setStar(m.id, it) }, colors = SwitchDefaults.colors(checkedTrackColor = p.olive))
        }
        // 기억의 주에 한 줄 알림 (R2, 기본 꺼짐): 적어 둔 떠난 날이 있을 때만
        if (m.until != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                TokenText(stringResource(R.string.memory_weekRow), Tokens.TypeScale.subhead)
                TokenText(stringResource(R.string.memory_weekHelp), Tokens.TypeScale.footnote, color = p.secondary)
            }
            Switch(m.id in state.memoryWeekOn, { state.setMemoryWeek(m.id, it) }, colors = SwitchDefaults.colors(checkedTrackColor = p.olive))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically) {
            TokenText(stringResource(R.string.memory_back), Tokens.TypeScale.footnote, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { if (!state.backToGarden(m.id)) full = true }.padding(Tokens.Space.sp3), color = p.secondary)
            TokenText("·", Tokens.TypeScale.footnote, color = p.secondary)
            TokenText(stringResource(R.string.stone_removeAction), Tokens.TypeScale.footnote, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { confirmRemove = true }.padding(Tokens.Space.sp3), color = p.secondary)
        }
        if (full) TokenText(stringResource(R.string.memory_backFull), Tokens.TypeScale.caption1, color = p.secondary)
    }
    if (confirmRemove) GardenAlert(
        onDismissRequest = { confirmRemove = false },
        title = { Text(stringResource(R.string.memory_removeConfirm, m.name)) },
        confirmButton = { TextButton(onClick = { confirmRemove = false; state.removeMemory(m.id) }) { Text(stringResource(R.string.stone_removeAction), color = p.danger) } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * 정원 하늘의 작은 별 (기억의 돌 가운데 ‘하늘에 별로 두기’를 켠 것). 다른 별보다 조금 따뜻하고, 밤에 조금 더 밝다.
 * 자리는 돌마다 정해져 있고 (id), 누르면 이름만 잠깐 조용히 보인다. top ~ bottom = 별이 앉을 하늘 띠.
 */
@Composable
internal fun MemoryStars(state: AppState, width: Dp, top: Dp, bottom: Dp, night: Boolean, today: LocalDate = LocalDate.now()) {
    val stars = state.memories.filter { it.star }
    if (stars.isEmpty() || bottom <= top) return
    val u = Theme.unit
    // 기억의 주 (R2): 적어 둔 날 앞뒤 사흘엔 그 별이 숨 쉬듯 조금 더 밝음 (날수 · 햇수는 쓰지 않음)
    val week = remember(stars, today) { stars.filter { io.github.graviton94.carpediem.core.MemoryWeek.of(it, today) != null }.map { it.id }.toSet() }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val clock = rememberGardenClock(week.isNotEmpty() && remember { !reducedMotion(ctx) })
    var shown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(shown) { if (shown != null) { delay(3200); shown = null } }
    val size = u * 22
    stars.forEach { m ->
        val r = Crayon.Rng(m.id.hashCode())
        val x = width * (0.14f + 0.72f * r.next()); val y = top + (bottom - top) * r.next()
        val a11y = stringResource(R.string.memory_starNote, m.name)
        Canvas(Modifier.offset(x - size / 2, y - size / 2).size(size).semantics { contentDescription = a11y }.clickable { shown = m.id }.graphicsLayer()) {
            val c = Offset(this.size.width / 2, this.size.height / 2); val k = this.size.width / 22f
            val warm = G.Night.Colors.firefly
            // 그 주엔 빛이 넓고 밝게, 6초에 한 번 숨 쉬듯
            val wk = if (m.id in week) 0.5f + 0.5f * kotlin.math.sin(clock.value * 6.2832f / 6f) else -1f
            val halo = if (wk >= 0f) 10f * k * (1.25f + 0.25f * wk) else 10f * k
            val ha = (if (night) 0.35f else 0.22f) * (if (wk >= 0f) 1.5f + 0.4f * wk else 1f)
            drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = ha.coerceAtMost(0.75f)), Color.Transparent), c, halo), halo, c)
            val s = Path().apply {
                moveTo(c.x, c.y - 6f * k); lineTo(c.x + 1.6f * k, c.y - 1.6f * k); lineTo(c.x + 6f * k, c.y); lineTo(c.x + 1.6f * k, c.y + 1.6f * k)
                lineTo(c.x, c.y + 6f * k); lineTo(c.x - 1.6f * k, c.y + 1.6f * k); lineTo(c.x - 6f * k, c.y); lineTo(c.x - 1.6f * k, c.y - 1.6f * k); close()
            }
            drawPath(s, warm.copy(alpha = if (night || wk >= 0f) 0.95f else 0.75f))
        }
        if (shown == m.id) Box(Modifier.offset(y = y + size / 2).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            TokenText(stringResource(R.string.memory_starNote, m.name), Tokens.TypeScale.caption1,
                Modifier.crayonBox(Theme.gc.paper, G.Radius.chip, G.Stroke.chip, 1350).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1))
        }
    }
}
