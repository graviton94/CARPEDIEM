package io.github.graviton94.carpediem.ui.garden

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Breath
import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.BreathStep
import io.github.graviton94.carpediem.core.DayPart
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.core.SeedState
import io.github.graviton94.carpediem.core.Seeds
import io.github.graviton94.carpediem.core.Sound
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.sound.Soundscape
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

/*
 * 하루를 열고 닫는 작은 의식들.
 *   아침 씨앗 (04): 아침 정원 위쪽에 작은 다짐 하나 → 저녁에 ‘싹이 텄나요?’ (텄으면 꽃, 아니면 흙 속에서 쉼)
 *   걱정한 밤 다음 아침 (06): 하루가 위로 한마디만 (묻지도 세지도 않음)
 *   하루 닫기 (03): 한 줄 → 고마운 것 하나 (숨 세 번) → 등불 아래 숨 두 번 → 등불 끄기
 *   하루의 숨결을 손끝으로 (05): 숨을 떨림으로 (화면은 어둡게)
 */

private const val SEED_CHOICES = 3

/** res 의 seed_choice_0 … 고르기 글들. */
private fun seedChoices(ctx: Context): List<String> =
    generateSequence(0) { it + 1 }.map { ctx.resources.getIdentifier("seed_choice_$it", "string", ctx.packageName) }.takeWhile { it != 0 }.map { ctx.getString(it) }.toList()

/** prefix_0 … 가운데 오늘의 것 (날마다 다음 것). */
internal fun dayLine(ctx: Context, prefix: String, day: LocalDate, salt: Int = 0): String? {
    val ids = generateSequence(0) { it + 1 }.map { ctx.resources.getIdentifier("$prefix$it", "string", ctx.packageName) }.takeWhile { it != 0 }.toList()
    return if (ids.isEmpty()) null else ctx.getString(ids[io.github.graviton94.carpediem.core.Nudges.pick(day, ids.size, salt)])
}

// ───────────────────────── 아침 씨앗 ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeedCard(state: AppState, today: LocalDate) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val all = remember { seedChoices(ctx) }
    val picks = remember(today, all) { Seeds.choices(all.size, today, SEED_CHOICES).map { all[it] } }
    var pick by remember(today) { mutableStateOf<String?>(null) }
    var own by remember(today) { mutableStateOf(false) }
    var text by remember(today) { mutableStateOf("") }
    val chosen = if (own) text.takeIf { it.isNotBlank() } else pick
    Column(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 1400).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
    ) {
        TokenText(stringResource(R.string.seed_title), Tokens.TypeScale.headline.serif())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            picks.forEachIndexed { i, c -> GardenChip(c, !own && pick == c, 1401 + i) { own = false; pick = if (pick == c) null else c } }
            GardenChip(stringResource(R.string.seed_own), own, 1405) { own = !own; pick = null }
        }
        if (own) BasicTextField(
            value = text, onValueChange = { v -> val one = v.replace('\n', ' '); if (one.codePointCount(0, one.length) <= Seeds.MAX_CHARS) text = one },
            singleLine = true, textStyle = Tokens.TypeScale.callout.style().copy(color = p.foreground), cursorBrush = SolidColor(p.foreground),
            modifier = Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1406).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
            decorationBox = { inner -> Box { if (text.isEmpty()) TokenText(stringResource(R.string.seed_hint), Tokens.TypeScale.callout, color = p.secondary); inner() } },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            GardenButton(stringResource(R.string.seed_plant), { chosen?.let { state.plantSeed(it, today); state.say(ctx.getString(R.string.seed_planted)) } },
                filled = chosen != null, seed = 1407, modifier = Modifier.weight(1f))
            TokenText(stringResource(R.string.seed_later), Tokens.TypeScale.footnote,
                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { state.skipSeed(today) }.padding(Tokens.Space.sp3), color = p.secondary)
        }
    }
}

/** 저녁 (한 줄을 보낸 뒤 · 하루 닫기): ‘아침에 심은 ○○, 싹이 텄나요?’ 아니라고 답할 칸은 없음. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeedAsk(state: AppState, today: LocalDate, now: LocalDateTime) {
    val seed = state.seedToAsk(today) ?: return
    if (Labels.part(now) != DayPart.EVENING && Labels.part(now) != DayPart.NIGHT) return
    val p = Theme.palette
    Column(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.chip, G.Radius.box, G.Stroke.chip, 1410).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
    ) {
        TokenText(stringResource(R.string.seed_askLabel, seed.text), Tokens.TypeScale.footnote, color = p.secondary)
        TokenText(stringResource(R.string.seed_ask), Tokens.TypeScale.headline.serif())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            GardenChip(stringResource(R.string.seed_yes), false, 1411) { state.answerSeed(true, today) }
            GardenChip(stringResource(R.string.seed_no), false, 1412) { state.answerSeed(false, today) }
        }
    }
}

/** 정원 흙의 오늘 씨앗: 심었으면 새싹, 피었으면 작은 꽃 (그날만). x = 줄기 자리, gy = 땅. */
@Composable
internal fun SeedSprout(state: AppState, today: LocalDate, x: Dp, gy: Dp) {
    val s = Seeds.of(state.seeds, today)?.takeIf { state.seedsOn } ?: return
    if (s.state == SeedState.RESTING) return
    val u = Theme.unit
    val w = u * 10f
    val ink = Theme.gc.ink
    Canvas(Modifier.offset(x - w / 2, gy - w).size(w)) {
        val k = size.width / 10f
        val stem = Offset(5f * k, 9.6f * k)
        val top = Offset(5f * k, if (s.state == SeedState.BLOOMED) 3.6f * k else 5.4f * k)
        drawLine(Color(0xFF5F7236), stem, top, k * 0.7f)
        // 잎 둘
        drawOval(Color(0xFF8FA25A), Offset(2.2f * k, 6.2f * k), Size(2.8f * k, 1.4f * k))
        drawOval(Color(0xFF8FA25A), Offset(5f * k, 5.6f * k), Size(2.8f * k, 1.4f * k))
        if (s.state == SeedState.BLOOMED) {
            for (i in 0 until 5) { val a = i * 1.2566f; drawCircle(Color(0xFFF2B35A), 1.1f * k, Offset(top.x + 1.4f * k * kotlin.math.cos(a), top.y + 1.4f * k * kotlin.math.sin(a))) }
            drawCircle(Color(0xFFB5651D), 0.8f * k, top)
        }
        drawCircle(ink.copy(alpha = 0.18f), 1.4f * k, Offset(5f * k, 9.8f * k), style = Stroke(k * 0.4f))
    }
}

// ───────────────────────── 걱정한 밤 다음 아침 ─────────────────────────

/** 하루의 한마디 (06): 고를 것도 답할 것도 없이. 누르거나 조금 지나면 사라짐. */
@Composable
internal fun ComfortWords(text: String, onDone: () -> Unit) {
    val p = Theme.palette
    LaunchedEffect(text) { delay(COMFORT_MS); onDone() }
    Column(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 1420).clickable(onClick = onDone)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
    ) {
        TokenText(stringResource(R.string.comfort_from), Tokens.TypeScale.caption1, color = p.secondary)
        TokenText(text, Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
    }
}

private const val COMFORT_MS = 25_000L

// ───────────────────────── 하루 닫기 ─────────────────────────

/** 저녁 · 밤 정원의 입구: 작은 등 + ‘하루 닫기 · 1분’. */
@Composable
internal fun CloseDayEntry(sent: Boolean, onOpen: () -> Unit) {
    val p = Theme.palette
    Row(
        Modifier.crayonBox(Theme.gc.paper.copy(alpha = 0.85f), G.Radius.button, G.Stroke.chip, 1430).clickable(onClick = onOpen)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp2),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        Lantern(Theme.unit * 9f)
        Column {
            TokenText(stringResource(R.string.closeDay_entry), Tokens.TypeScale.subhead.serif(), weight = FontWeight.SemiBold)
            TokenText(stringResource(if (sent) R.string.closeDay_entrySent else R.string.closeDay_entrySub), Tokens.TypeScale.caption1, color = p.secondary)
        }
    }
}

/** 작은 한지 등 (손그림 느낌의 단순한 꼴). */
@Composable
private fun Lantern(size: Dp, glow: Float = 1f) {
    val ink = Theme.gc.ink
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 10f
        drawLine(ink.copy(alpha = 0.6f), Offset(5f * k, 0f), Offset(5f * k, 2f * k), k * 0.4f)
        if (glow > 0f) drawCircle(Color(0xFFF5B45C).copy(alpha = 0.25f * glow), 5f * k, Offset(5f * k, 6f * k))
        drawRoundRect(Color(0xFFF2B35A).copy(alpha = 0.35f + 0.65f * glow), Offset(2.6f * k, 2f * k), Size(4.8f * k, 7f * k), androidx.compose.ui.geometry.CornerRadius(1.6f * k))
        drawRoundRect(Color(0xFFFFE2A8).copy(alpha = 0.3f + 0.7f * glow), Offset(3.5f * k, 3f * k), Size(3f * k, 5f * k), androidx.compose.ui.geometry.CornerRadius(1f * k))
        drawRoundRect(ink, Offset(2.6f * k, 2f * k), Size(4.8f * k, 7f * k), androidx.compose.ui.geometry.CornerRadius(1.6f * k), style = Stroke(k * 0.45f))
    }
}

private enum class CloseStep { LINE, THANKS, LANTERN, END, DARK }

/**
 * 하루 닫기 (03). 위에 걸음 셋 (한 줄 · 고마움 · 등불), 어느 걸음에서든 ‘여기까지’로 끝낼 수 있다 (그래도 ‘여기까지도 좋아요’).
 * 한 줄을 이미 남겼으면 ✓ 로 지나가고, 등불 숨을 마치면 숨의 흔적 (마음 등불) 을 남긴다. 몇 번 했는지는 세지 않는다.
 */
@Composable
fun CloseDayScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, onDone: () -> Unit) {
    val p = Theme.palette
    val today = now.toLocalDate()
    var step by rememberSaveable { mutableStateOf(CloseStep.LINE) }
    var stopped by remember { mutableStateOf(false) }
    KeepScreenOn(step != CloseStep.DARK)
    BackHandler { if (step == CloseStep.DARK || stopped) onDone() else stopped = true }
    Box(Modifier.fillMaxSize().paperBackground()) {
        when (step) {
            CloseStep.LINE -> LineStep(state, today, now, onNext = { step = CloseStep.THANKS })
            CloseStep.THANKS -> ShortBreath(state, BreathKind.THANKS, 3, R.string.closeDay_thanksTitle) { step = CloseStep.LANTERN }
            CloseStep.LANTERN -> ShortBreath(state, BreathKind.SLEEP, 2, R.string.closeDay_lanternTitle) { state.recordBreath(BreathKind.SLEEP, today); step = CloseStep.END }
            CloseStep.END -> EndStep { step = CloseStep.DARK }
            CloseStep.DARK -> DarkStep(onDone)
        }
        // 걸음 셋 + 여기까지
        if (step.ordinal <= CloseStep.LANTERN.ordinal) Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically,
        ) {
            TokenText(stringResource(R.string.closeDay_stop), Tokens.TypeScale.footnote,
                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { stopped = true }.padding(Tokens.Space.sp3), color = p.secondary)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                for (i in 0 until 3) Box(Modifier.size(Theme.unit * 7f, Theme.unit * 1.2f)
                    .background(if (i <= step.ordinal) Color(0xFFF5B45C) else p.secondary.copy(alpha = 0.25f), androidx.compose.foundation.shape.RoundedCornerShape(Theme.unit)))
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(Tokens.Layout.tapTarget))
        }
        if (stopped) Box(Modifier.fillMaxSize().background(Theme.gc.scrim).clickable { stopped = false }, contentAlignment = Alignment.Center) {
            Column(Modifier.padding(horizontal = Theme.deviceClass.pageMargin).crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.box, 1440).padding(Tokens.Space.sp6),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                TokenText(stringResource(R.string.closeDay_enough), Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
                GardenButton(stringResource(R.string.breath_home), onDone, filled = true, seed = 1441)
                GardenButton(stringResource(R.string.closeDay_keep), { stopped = false }, filled = false, seed = 1442)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineStep(state: AppState, today: LocalDate, now: LocalDateTime, onNext: () -> Unit) {
    val p = Theme.palette
    var text by rememberSaveable { mutableStateOf("") }
    var feeling by rememberSaveable { mutableStateOf<Feeling?>(null) }
    val sent = state.sentOn(today)
    val max = G.LetGo.maxChars.toInt()
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = Theme.deviceClass.pageMargin).padding(top = Tokens.Layout.tapTarget + Tokens.Space.sp6, bottom = Tokens.Space.sp6),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        if (sent) {
            TokenText(stringResource(R.string.closeDay_lineDone), Tokens.TypeScale.title3.serif())
            TokenText(stringResource(R.string.closeDay_lineDoneSub), Tokens.TypeScale.callout, color = p.secondary)
            SeedAsk(state, today, now)
            Spacer(Modifier.height(Tokens.Space.sp4))
            GardenButton(stringResource(R.string.closeDay_next), onNext, filled = true, seed = 1450)
        } else {
            TokenText(stringResource(R.string.closeDay_lineTitle), Tokens.TypeScale.title3.serif())
            TokenText(stringResource(R.string.letgo_feeling), Tokens.TypeScale.footnote, color = p.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                Feeling.entries.forEachIndexed { i, f -> GardenChip(stringResource(feelingName(f)), feeling == f, 1451 + i) { feeling = if (feeling == f) null else f } }
            }
            BasicTextField(
                value = text, onValueChange = { v -> if (v.codePointCount(0, v.length) <= max && v.count { it == '\n' } < Lines.MAX_LINES) text = v },
                minLines = 2, maxLines = Lines.MAX_LINES, textStyle = Tokens.TypeScale.callout.style().copy(color = p.foreground), cursorBrush = SolidColor(p.foreground),
                modifier = Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1460).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                decorationBox = { inner -> Box { if (text.isEmpty()) TokenText(stringResource(R.string.letgo_hint), Tokens.TypeScale.callout, color = p.secondary); inner() } },
            )
            GardenButton(stringResource(R.string.letgo_send), {
                if (text.isNotBlank()) {
                    state.letGo(text, feeling, today = today)
                    // 하루 닫기 안에서는 한마디 창 · 권하기 없이 다음 걸음으로 (정원의 바람은 돌아가면)
                    state.toast = null; state.toastTitle = null; state.care = null; text = ""
                }
            }, filled = text.isNotBlank(), seed = 1461)
            TokenText(stringResource(R.string.closeDay_skip), Tokens.TypeScale.footnote,
                Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onNext).padding(vertical = Tokens.Space.sp3), color = p.secondary, align = TextAlign.Center)
        }
    }
}

/** 짧은 숨 (숨 n 번): 하루와 숨의 그림 · 말, 끝나면 조금 머물렀다가 다음 걸음으로. */
@Composable
private fun ShortBreath(state: AppState, kind: BreathKind, cycles: Int, title: Int, onDone: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val view = LocalView.current
    val plan = remember(kind, cycles) { Breath.cycles(rhythm(kind), cycles) }
    val total = plan.last().let { it.startMs + it.lengthMs }
    var elapsed by remember(kind) { mutableLongStateOf(0L) }
    val animate = remember { !reducedMotion(ctx) }
    val touch = rememberTouchBreath(state.breathTouch)
    LaunchedEffect(kind) {
        touch?.play(plan, 0)
        var last = withFrameMillis { it }
        while (elapsed < total) {
            val t = withFrameMillis { it }
            if (t - last < 33) continue
            elapsed += t - last; last = t
        }
        delay(1200); onDone()
    }
    val step by remember(plan) { androidx.compose.runtime.derivedStateOf { Breath.at(plan, elapsed)?.first?.step } }
    LaunchedEffect(step) {
        if (state.sound != Sound.NONE) when (step) { BreathStep.IN -> Soundscape.bowl(G.Sound.bowlInHz.toDouble()); BreathStep.OUT -> Soundscape.bowl(G.Sound.bowlOutHz.toDouble()); else -> {} }
        if (touch == null && step == BreathStep.IN) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val u = Theme.unit
        val b = G.Breath
        val screenW = maxWidth
        val groundY = maxHeight * b.haruAt
        val art = remember { HaruArt.of(state.store.haruSeed, false) }
        val scale = u * (b.haruWidth / G.Layout.haruArtWidth)
        val k = with(androidx.compose.ui.platform.LocalDensity.current) { scale.toPx() }
        BreathScene(kind, art, scale, groundY, animate, plan, { elapsed }, { fullAt(plan, elapsed) })
        val boxH = scale * (G.Layout.haruGround - art.meta.bbox.top + G.Layout.sparkle)
        Column(Modifier.align(Alignment.TopCenter).offset(y = groundY - boxH).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.graphicsLayer {
                if (animate) { transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f); val f = fullAt(plan, elapsed); val s = 1f + b.swell * f; scaleX = s; scaleY = s; translationY = -b.rise * k * f }
            }) { BigStoneOnly(art, scale, if (elapsed > 2500) 1f else 0f) }
            if (kind != BreathKind.CALM) CrayonRule(Modifier.padding(horizontal = screenW * b.ruleInset), seed = 1470)
        }
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin), horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText(stringResource(title), Tokens.TypeScale.footnote.serif(), color = p.secondary, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp3))
            Box(Modifier.heightIn(min = Tokens.Space.sp10 * 2), contentAlignment = Alignment.Center) {
                step?.let { BreathCue(stringResource(cueName(kind, it)), p.secondary) }
            }
        }
    }
}

@Composable
private fun EndStep(onOff: () -> Unit) {
    val p = Theme.palette
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(1400)) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin).graphicsLayer { alpha = shown.value },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Lantern(Theme.unit * 40f)
        Spacer(Modifier.height(Tokens.Space.sp8))
        TokenText(stringResource(R.string.closeDay_end), Tokens.TypeScale.title3.serif(), Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
        Spacer(Modifier.height(Tokens.Space.sp2))
        TokenText(stringResource(R.string.closeDay_endSub), Tokens.TypeScale.callout, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        Spacer(Modifier.height(Tokens.Space.sp8))
        GardenButton(stringResource(R.string.closeDay_off), onOff, filled = true, seed = 1480)
        Spacer(Modifier.weight(1f))
    }
}

/** 등불을 끈 뒤: 천천히 어두워지고 ‘잘 자요’. 누르면 정원으로. */
@Composable
private fun DarkStep(onDone: () -> Unit) {
    val dark = remember { Animatable(0f) }
    LaunchedEffect(Unit) { dark.animateTo(1f, tween(2600)) }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = dark.value }.background(Color.Black).pointerInput(Unit) { detectTapGestures { onDone() } }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText(stringResource(R.string.closeDay_goodnight), Tokens.TypeScale.headline.serif(), color = Color.White.copy(alpha = 0.55f))
            Spacer(Modifier.height(Tokens.Space.sp10))
            TokenText(stringResource(R.string.breath_sleepBack), Tokens.TypeScale.footnote, color = Color.White.copy(alpha = 0.3f))
        }
    }
}

// ───────────────────────── 하루의 숨결을 손끝으로 ─────────────────────────

/** 숨을 떨림으로 (05). 세기를 조절할 수 있는 폰은 차오르고 잦아드는 떨림, 아니면 짧은 박동. */
internal class TouchBreath(private val vib: Vibrator) {
    fun play(plan: List<Breath.Phase>, from: Long) {
        if (!vib.hasVibrator()) return
        val (t, a) = Breath.touchWave(plan, from, slice = 150)
        if (t.isEmpty()) return
        val effect = if (vib.hasAmplitudeControl()) VibrationEffect.createWaveform(t, a, -1) else {
            // 세기 조절이 없으면: 들이쉼 · 내쉼 동안 짧은 박동 (조용한 때는 쉼)
            val times = ArrayList<Long>(); var wait = 0L
            t.indices.forEach { i -> if (a[i] > 0) { times.add(wait); times.add(18L); wait = t[i] - 18L } else wait += t[i] }
            if (times.isEmpty()) return
            VibrationEffect.createWaveform(times.toLongArray(), -1)
        }
        runCatching { vib.vibrate(effect) }
    }
    fun stop() { runCatching { vib.cancel() } }
}

private fun vibrator(ctx: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

internal fun canTouch(ctx: Context): Boolean = vibrator(ctx)?.hasVibrator() == true

/** on 이면 떨림 하나 (화면을 떠나면 멈춤). */
@Composable
internal fun rememberTouchBreath(on: Boolean): TouchBreath? {
    val ctx = LocalContext.current
    val t = remember(on) { if (on) vibrator(ctx)?.takeIf { it.hasVibrator() }?.let { TouchBreath(it) } else null }
    DisposableEffect(t) { onDispose { t?.stop() } }
    return t
}

/** 손끝 숨 동안 화면을 아주 어둡게 (켜 둔 채). 화면을 떠나면 원래대로. */
@Composable
internal fun DimWindow(on: Boolean) {
    val ctx = LocalContext.current
    DisposableEffect(on) {
        val w = (ctx as? Activity)?.window
        if (on && w != null) { val lp = w.attributes; lp.screenBrightness = 0.01f; w.attributes = lp }
        onDispose { if (w != null) { val lp = w.attributes; lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; w.attributes = lp } }
    }
}

/** 손끝 숨에서 쓰는 벽시계 (화면이 잠깐 꺼졌다 와도 떨림과 같은 자리). */
internal fun wallNow(): Long = SystemClock.elapsedRealtime()

/** 추억: 피어난 씨앗 (04). 핀 것만, 최근 것부터. 쉰 씨앗 · 개수 비교는 두지 않는다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BloomedSeeds(state: AppState) {
    val bloomed = remember(state.seeds) { Seeds.bloomed(state.seeds) }
    if (bloomed.isEmpty() || !state.seedsOn) return
    val p = Theme.palette
    val ctx = LocalContext.current
    Spacer(Modifier.height(Tokens.Space.sp4))
    TokenText(stringResource(R.string.seed_garden), Tokens.TypeScale.title3)
    TokenText(stringResource(R.string.seed_gardenSub), Tokens.TypeScale.footnote, color = p.secondary)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        bloomed.take(BLOOMED_SHOWN).forEachIndexed { i, s ->
            Row(Modifier.crayonBox(Theme.gc.paper, G.Radius.chip, G.Stroke.chip, 1490 + i % 7).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                Canvas(Modifier.size(Theme.unit * 4f)) {
                    val r = size.width / 2
                    for (k in 0 until 5) { val a = k * 1.2566f; drawCircle(Color(0xFFF2B35A), r * 0.42f, Offset(r + r * 0.5f * kotlin.math.cos(a), r + r * 0.5f * kotlin.math.sin(a))) }
                    drawCircle(Color(0xFFB5651D), r * 0.3f, Offset(r, r))
                }
                TokenText(RecordText.day(ctx, s.date) + " · " + s.text, Tokens.TypeScale.footnote)
            }
        }
    }
}

private const val BLOOMED_SHOWN = 60

// ───────────────────────── 돌아온 날의 손님 ─────────────────────────

/** 다시 와 줘서 반가워요 (09): 빠진 날 대신 손님 이야기. 누르거나 조금 지나면 사라짐. */
@Composable
internal fun GreetingCard(guest: String, onDone: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    LaunchedEffect(guest) { delay(COMFORT_MS); onDone() }
    val story = remember(guest) { ctx.resources.getIdentifier("guest_${guest}_story", "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) }.orEmpty() }
    Column(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 1600).clickable(onClick = onDone)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
    ) {
        TokenText(stringResource(R.string.return_title), Tokens.TypeScale.headline.serif(), align = TextAlign.Center)
        if (story.isNotEmpty()) TokenText(story, Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
    }
}

/** 손님 그림 (손그림 느낌의 단순한 꼴, 오늘 하루 정원에). x = 발 자리, gy = 땅. */
@Composable
internal fun GuestFigure(guest: String, x: Dp, gy: Dp) {
    val u = Theme.unit
    val w = u * 11f
    val ink = Theme.gc.ink
    Canvas(Modifier.offset(x - w / 2, gy - w).size(w)) {
        val k = size.width / 10f
        fun eye(cx: Float, cy: Float) { drawCircle(Color.White, 0.55f * k, Offset(cx, cy)); drawCircle(ink, 0.3f * k, Offset(cx + 0.1f * k, cy)) }
        when (guest) {
            "tit" -> {   // 곤줄박이: 주황 배, 검은 머리
                drawOval(Color(0xFFB5651D), Offset(2.4f * k, 5.2f * k), Size(5f * k, 3.6f * k))
                drawCircle(Color(0xFF2E2A22), 1.6f * k, Offset(7f * k, 5f * k)); eye(7.5f * k, 4.8f * k)
                drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(8.5f * k, 5f * k); lineTo(9.6f * k, 5.3f * k); lineTo(8.5f * k, 5.6f * k); close() }, Color(0xFFE89A32))
                drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(2.6f * k, 6.6f * k); lineTo(0.6f * k, 5.8f * k); lineTo(1.2f * k, 7.4f * k); close() }, Color(0xFF6B6456))
                drawLine(ink, Offset(4.6f * k, 8.7f * k), Offset(4.4f * k, 9.8f * k), 0.3f * k); drawLine(ink, Offset(5.6f * k, 8.7f * k), Offset(5.8f * k, 9.8f * k), 0.3f * k)
            }
            "squirrel" -> {   // 다람쥐: 큰 꼬리
                drawOval(Color(0xFFA0683A), Offset(0.4f * k, 2f * k), Size(3.4f * k, 6.6f * k))
                drawOval(Color(0xFFB97A45), Offset(3f * k, 5f * k), Size(4f * k, 4.6f * k))
                drawCircle(Color(0xFFB97A45), 1.7f * k, Offset(6.8f * k, 4.6f * k)); drawCircle(Color(0xFFB97A45), 0.6f * k, Offset(6.4f * k, 2.9f * k)); eye(7.4f * k, 4.3f * k)
            }
            "hedgehog" -> {   // 고슴도치: 가시 등
                drawOval(Color(0xFF6B5236), Offset(1f * k, 5f * k), Size(7f * k, 4.6f * k))
                for (i in 0 until 7) { val a = 3.4f + i * 0.33f; drawLine(Color(0xFF4A3826), Offset(4.5f * k, 7.3f * k), Offset(4.5f * k + 4f * k * kotlin.math.cos(a), 7.3f * k + 3.4f * k * kotlin.math.sin(a)), 0.4f * k) }
                drawOval(Color(0xFFE3C9A0), Offset(6.4f * k, 6.4f * k), Size(2.8f * k, 2.6f * k)); eye(7.8f * k, 7.2f * k); drawCircle(ink, 0.3f * k, Offset(9.1f * k, 7.8f * k))
            }
            "rabbit" -> {   // 토끼: 긴 귀
                drawOval(Color(0xFFF4EEE2), Offset(1.6f * k, 5.4f * k), Size(5.4f * k, 4.2f * k))
                drawCircle(Color(0xFFF4EEE2), 1.8f * k, Offset(6.8f * k, 5.4f * k))
                drawOval(Color(0xFFF4EEE2), Offset(5.6f * k, 0.6f * k), Size(1.2f * k, 3.8f * k)); drawOval(Color(0xFFF4EEE2), Offset(7f * k, 0.8f * k), Size(1.2f * k, 3.6f * k))
                drawOval(ink.copy(alpha = 0.3f), Offset(1.6f * k, 5.4f * k), Size(5.4f * k, 4.2f * k), style = Stroke(0.25f * k)); eye(7.4f * k, 5.2f * k)
            }
            else -> {   // owl 부엉이
                drawOval(Color(0xFF8C6A44), Offset(2.4f * k, 2.4f * k), Size(5.2f * k, 7.2f * k))
                drawOval(Color(0xFFE3C9A0), Offset(3.4f * k, 5.4f * k), Size(3.2f * k, 3.6f * k))
                drawCircle(Color.White, 1.1f * k, Offset(4f * k, 4f * k)); drawCircle(Color.White, 1.1f * k, Offset(6f * k, 4f * k))
                drawCircle(ink, 0.5f * k, Offset(4f * k, 4f * k)); drawCircle(ink, 0.5f * k, Offset(6f * k, 4f * k))
                drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(4.6f * k, 4.8f * k); lineTo(5.4f * k, 4.8f * k); lineTo(5f * k, 5.6f * k); close() }, Color(0xFFE89A32))
            }
        }
    }
}
