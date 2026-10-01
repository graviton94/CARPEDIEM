package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Breath
import io.github.graviton94.carpediem.core.BreathKind
import io.github.graviton94.carpediem.core.BreathStep
import io.github.graviton94.carpediem.core.Family
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.Sound
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.sound.Soundscape
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.TokenText
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.sin

internal fun rhythm(k: BreathKind): Breath.Rhythm {
    val b = G.Breath
    return when (k) {
        BreathKind.CALM -> Breath.Rhythm(b.calmIn.toDouble(), 0.0, b.calmOut.toDouble(), 0.0)
        BreathKind.BOX -> Breath.Rhythm(b.boxIn.toDouble(), b.boxHold.toDouble(), b.boxOut.toDouble(), b.boxRest.toDouble())
        BreathKind.SLEEP -> Breath.Rhythm(b.sleepIn.toDouble(), b.sleepHold.toDouble(), b.sleepOut.toDouble(), 0.0)
        BreathKind.THANKS -> Breath.Rhythm(b.calmIn.toDouble(), 0.0, b.calmOut.toDouble(), 0.0)
    }
}
internal fun partTitle(p: io.github.graviton94.carpediem.core.DayPart) = when (p) {
    io.github.graviton94.carpediem.core.DayPart.MORNING -> R.string.breath_part_morning; io.github.graviton94.carpediem.core.DayPart.DAY -> R.string.breath_part_day
    io.github.graviton94.carpediem.core.DayPart.EVENING -> R.string.breath_part_evening; io.github.graviton94.carpediem.core.DayPart.NIGHT -> R.string.breath_part_night
}
internal fun kindName(k: BreathKind) = when (k) { BreathKind.CALM -> R.string.breath_kind_calm; BreathKind.BOX -> R.string.breath_kind_box; BreathKind.SLEEP -> R.string.breath_kind_sleep; BreathKind.THANKS -> R.string.breath_kind_thanks }
private fun kindDesc(k: BreathKind) = when (k) { BreathKind.CALM -> R.string.breath_kindDesc_calm; BreathKind.BOX -> R.string.breath_kindDesc_box; BreathKind.SLEEP -> R.string.breath_kindDesc_sleep; BreathKind.THANKS -> R.string.breath_kindDesc_thanks }
private fun soundName(s: Sound) = when (s) { Sound.NONE -> R.string.sound_none; Sound.WAVES -> R.string.sound_waves; Sound.WIND -> R.string.sound_wind; Sound.RAIN -> R.string.sound_rain; Sound.TONE -> R.string.sound_tone; Sound.SEASON -> R.string.sound_season }
private fun stepName(s: BreathStep) = when (s) { BreathStep.IN -> R.string.breath_in; BreathStep.HOLD -> R.string.breath_hold; BreathStep.OUT -> R.string.breath_out; BreathStep.REST -> R.string.breath_rest }

/** 밤(nightFrom ~ 새벽)에는 잠드는 숨을 먼저. */
internal fun isNight(now: LocalDateTime) = now.hour >= G.Breath.nightFrom.toInt() || now.hour < G.Motion.sunrise.toInt()

// ───────────────────────── 숨 고르기 창 ─────────────────────────

/** 아래에서 올라오는 한 장: 숨 · 시간 · 소리를 고르고 시작. 마지막에 고른 것을 기억. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BreathSheet(state: AppState, now: LocalDateTime, onStart: (BreathKind, Int, Sound) -> Unit, onDismiss: () -> Unit) {
    val p = Theme.palette
    var kind by remember { mutableStateOf(if (isNight(now)) BreathKind.SLEEP else state.breathKind) }
    var minutes by remember { mutableStateOf(state.breathMinutes) }
    var sound by remember { mutableStateOf(state.sound) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Theme.gc.paper) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            TokenText(stringResource(R.string.breath), Tokens.TypeScale.title3.serif())
            // 때에 맞는 숨의 이름 (하루를 여는 · 잠시 멈추는 · 내려놓는 · 마무리하는)
            TokenText(stringResource(partTitle(io.github.graviton94.carpediem.ui.Labels.part(now))), Tokens.TypeScale.footnote.serif(), color = p.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                BreathKind.entries.forEachIndexed { i, k -> GardenChip(stringResource(kindName(k)), kind == k, 1000 + i) { kind = k } }
            }
            TokenText(stringResource(kindDesc(kind)), Tokens.TypeScale.footnote, color = p.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                listOf(1, 3, 5).forEachIndexed { i, m -> GardenChip(stringResource(R.string.breath_minutes, "$m"), minutes == m, 1004 + i) { minutes = m } }
            }
            TokenText(stringResource(R.string.breath_sound), Tokens.TypeScale.caption1, color = p.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                Sound.entries.forEachIndexed { i, s -> GardenChip(stringResource(soundName(s)), sound == s, 1008 + i) { sound = s } }
            }
            Spacer(Modifier.height(Tokens.Space.sp2))
            GardenButton(stringResource(R.string.breath_start), { state.chooseBreath(if (isNight(now) && kind == BreathKind.SLEEP) state.breathKind else kind, minutes, sound); onStart(kind, minutes, sound) }, filled = true, seed = 1015)
        }
    }
}

// ───────────────────────── 숨 쉬는 화면 ─────────────────────────

/** 화면이 켜져 있게 (숨 · 멍하니 보는 정원 동안만). */
@Composable
private fun KeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(on) { view.keepScreenOn = on; onDispose { view.keepScreenOn = false } }
}

/**
 * 하루와 함께 숨 쉬기. 바탕 한 빛 · 작은 하루 · 선 하나 (비움). 하루가 들이쉴 때 조금 부풀어 떠오르고 내쉴 때 가라앉는다. 3초 뒤 지긋이 눈을 감는다.
 * 글자는 첫 1분만, 진동은 단계가 바뀔 때 아주 짧게, 소리는 고른 바탕 소리 + 시작 · 끝 종소리.
 * 누르거나 뒤로 가기 = ‘여기서 멈출까요?’. 끝까지 쉬면 날짜를 남긴다.
 */
@Composable
fun BreathScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, kind: BreathKind, minutes: Int, sound: Sound, onDone: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val view = LocalView.current
    val b = G.Breath
    val plan = remember(kind, minutes) { Breath.plan(rhythm(kind), minutes) }
    val total = plan.last().let { it.startMs + it.lengthMs }
    var elapsed by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val animate = remember { !reducedMotion(ctx) }
    val player = remember(sound) { Soundscape.Player(sound, io.github.graviton94.carpediem.core.Memories.seasonOf(now.toLocalDate())) }
    val part = io.github.graviton94.carpediem.ui.Labels.part(now)
    // 밤의 잠드는 숨: 끝나면 화면이 스르르 어두워지고 앱이 물러남 (화면은 폰이 스스로 끔)
    val sleepAfter = kind == BreathKind.SLEEP && isNight(now)
    val blackout = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (done && sleepAfter) {
            delay(G.Breath.sleepFadeAfter.toLong()); blackout.animateTo(1f, tween(G.Breath.sleepFadeMs.toInt()))
            (ctx as? android.app.Activity)?.moveTaskToBack(true); onDone()
        }
    }
    KeepScreenOn(!done)
    DisposableEffect(player) { player.start(); onDispose { player.stop() } }
    LaunchedEffect(Unit) { if (sound != Sound.NONE) Soundscape.chime(1) }
    // 시계: 멈춘 동안은 흐르지 않음
    LaunchedEffect(paused, done) {
        if (paused || done) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (!done) {
            val t = withFrameMillis { it }
            elapsed += t - last; last = t
            if (elapsed >= total) { done = true; state.recordBreath(kind); if (sound != Sound.NONE) Soundscape.chime(2); player.stop() }
        }
    }
    val at = Breath.at(plan, elapsed)
    val full = at?.let { Breath.fullness(it.first.step, it.second) } ?: 0f
    player.breath = full
    // 단계가 바뀌면 아주 짧게 (들이쉼 한 번, 내쉼 두 번)
    val step = at?.first?.step
    LaunchedEffect(step) {
        if (step == BreathStep.IN) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        if (step == BreathStep.OUT) { view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); delay(120); view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK) }
    }
    // 눈: 처음 3초는 뜨고, 그다음 지긋이 감고, 끝나면 천천히 뜸
    val lid = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (!done) { delay((b.eyesAfter * 1000).toLong()); lid.animateTo(1f, tween(1200)) } else lid.animateTo(0f, tween(1500))
    }
    BackHandler { if (done) onDone() else paused = true }

    BoxWithConstraints(Modifier.fillMaxSize().paperBackground().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!done) paused = true }) {
        val u = Theme.unit
        val screenW = maxWidth
        // 비움: 하늘 그림 · 땅 그림 없이 바탕 한 빛 위에 작은 하루와 선 하나
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin),
            horizontalAlignment = Alignment.CenterHorizontally) {
            // 위에 아주 옅게 숨의 이름 (첫 1분만)
            Box(Modifier.padding(top = Tokens.Space.sp8).height(Tokens.Space.sp8), contentAlignment = Alignment.Center) {
                if (!done && elapsed < b.cueSeconds * 1000) TokenText(stringResource(partTitle(part)), Tokens.TypeScale.footnote.serif(), color = p.secondary.copy(alpha = 0.7f))
            }
            Spacer(Modifier.weight(1f))
            val art = HaruArt.of(state.store.haruSeed, false)
            val scale = u * (b.haruWidth / G.Layout.haruArtWidth)
            val k = with(androidx.compose.ui.platform.LocalDensity.current) { scale.toPx() }
            Box(Modifier.graphicsLayer {
                if (animate) {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    val s = 1f + b.swell * full; scaleX = s; scaleY = s
                    translationY = -b.rise * k * full
                }
            }) { BigStoneOnly(art, scale, lid.value) }
            CrayonRule(Modifier.padding(horizontal = screenW * b.ruleInset), seed = 1020)
            Spacer(Modifier.height(Tokens.Space.sp8))
            // 글자: 첫 1분만. 끝나면 한 줄 + 정원으로
            val cue = if (done) null else step?.takeIf { elapsed < b.cueSeconds * 1000 }
            Box(Modifier.height(Tokens.Space.sp10), contentAlignment = Alignment.Center) {
                // 고마움 숨: 내쉴 때 “고마운 것 하나”
                if (cue != null) TokenText(stringResource(if (kind == BreathKind.THANKS && cue == BreathStep.OUT) R.string.breath_out_thanks else stepName(cue)), Tokens.TypeScale.title2.serif(), color = p.secondary, align = TextAlign.Center)
            }
            if (done) {
                // 아침 · 저녁 · 밤은 때의 말, 낮은 숨마다의 말
                val msg = remember { io.github.graviton94.carpediem.ui.Labels.timed(ctx, "breath_done", part) ?: ctx.getString(ctx.resources.getIdentifier("breath_done_${kind.name.lowercase()}_${(1..3).random()}", "string", ctx.packageName)) }
                TokenText(msg, Tokens.TypeScale.headline.serif(), Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
                Spacer(Modifier.height(Tokens.Space.sp6))
                // 고마움 숨 뒤: 떠오른 고마움을 오늘의 한 줄로 (오늘 아직 보내지 않았을 때)
                if (kind == BreathKind.THANKS && state.keepLines) ThanksAfter(state, now.toLocalDate())
                GardenButton(stringResource(R.string.breath_home), onDone, filled = true, seed = 1021)
            } else TokenText(stringResource(R.string.breath_startA11y, stringResource(kindName(kind)), "$minutes"), Tokens.TypeScale.caption2,
                Modifier.semantics { liveRegion = LiveRegionMode.Polite }.graphicsLayer { alpha = 0f })
            Spacer(Modifier.weight(1f))
            // 아주 옅은 가는 선 하나가 차오름 (남은 시간은 보이지 않음)
            Box(Modifier.fillMaxWidth().height(u * b.line).background(Theme.gc.ink.copy(alpha = 0.08f))) {
                Box(Modifier.fillMaxWidth((elapsed.toFloat() / total).coerceIn(0f, 1f)).height(u * b.line).background(Theme.gc.ink.copy(alpha = 0.28f)))
            }
            Spacer(Modifier.height(Tokens.Space.sp6))
        }
        if (blackout.value > 0f) Box(Modifier.fillMaxSize().graphicsLayer { alpha = blackout.value }.background(Color.Black))
        if (paused && !done) PauseCard(onKeep = { paused = false }, onStop = { paused = false; done = true; player.stop(); onDone() })
    }
}

/** 숨 쉬는 동안의 큰 하루 (땅선은 부르는 쪽이). lid = 눈꺼풀. */
@Composable
private fun BigStoneOnly(art: HaruArt, scale: Dp, lid: Float) {
    val boxW = scale * art.meta.box
    Box(Modifier.size(boxW, scale * (G.Layout.haruGround - art.meta.bbox.top + G.Layout.sparkle))) {
        val cx = boxW / 2 - scale * (art.meta.bbox.center.x - art.meta.box / 2)
        HaruFigure(art, scale, Modifier.offset(cx - boxW / 2, scale * (art.meta.bbox.top - G.Layout.sparkle) * -1f + 0.dp), lid = lid)
    }
}

@Composable
private fun PauseCard(onKeep: () -> Unit, onStop: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Theme.gc.scrim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onKeep() }, contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = Theme.deviceClass.pageMargin).crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.box, 1022).padding(Tokens.Space.sp6),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            TokenText(stringResource(R.string.breath_pause), Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
            GardenButton(stringResource(R.string.breath_keep), onKeep, filled = true, seed = 1023)
            GardenButton(stringResource(R.string.breath_stop), onStop, filled = false, seed = 1024)
        }
    }
}

// ───────────────────────── 멍하니 보는 정원 ─────────────────────────

/**
 * 홈의 정원에서 글자만 뺀 화면: 하늘 · 해와 달 · 땅 · 나와 가족의 돌 · 놓인 것. 돌들은 가끔 깜빡인다 (누르면 쓰다듬기만).
 * 5분 뒤 스르르 어두워지고, 10분 뒤 화면 켜둠을 푼다. 소리는 마지막에 고른 바탕 소리 (끄고 켤 수 있음).
 */
@Composable
fun GazeScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, onBack: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val z = G.Gaze
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    var soundOn by remember { mutableStateOf(state.sound != Sound.NONE) }
    val player = remember(soundOn) { Soundscape.Player(if (soundOn) state.sound else Sound.NONE, io.github.graviton94.carpediem.core.Memories.seasonOf(now.toLocalDate())) }
    DisposableEffect(player) { player.start(); onDispose { player.stop() } }
    var screenOn by remember { mutableStateOf(true) }
    KeepScreenOn(screenOn)
    val dim = remember { Animatable(0f) }
    var hint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(3000); hint = false }
    LaunchedEffect(Unit) {
        delay((z.dimAfter * 1000).toLong()); dim.animateTo(z.dimAlpha, tween(z.dimMs.toInt(), easing = LinearEasing))
        delay(((z.releaseAfter - z.dimAfter) * 1000).toLong() - z.dimMs.toLong()); screenOn = false
    }
    BackHandler(onBack = onBack)

    Box(Modifier.fillMaxSize()) {
        // 홈의 정원 그대로 (하늘 · 해와 달 · 땅 · 돌 · 놓인 것 · 밤빛), 글자만 없이
        GardenHome(state, profile, now, onSettings = {}, onCollection = {}, onSupport = {}, onStone = {}, onAddPerson = {}, bare = true)
        // 스르르 어두워짐
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = dim.value }.background(Color.Black))
        // 소리 끄고 켜기 (아주 작게), 나가는 법은 처음 3초만
        TokenText(stringResource(if (soundOn) R.string.gaze_soundOff else R.string.gaze_soundOn), Tokens.TypeScale.caption1,
            Modifier.align(Alignment.TopEnd).statusBarsPadding().clickable { soundOn = !soundOn; if (soundOn && state.sound == Sound.NONE) state.changeSound(Sound.WAVES) }.padding(Tokens.Space.sp4),
            color = p.secondary.copy(alpha = 0.7f), weight = FontWeight.Normal)
        if (hint) TokenText(stringResource(R.string.gaze_exit), Tokens.TypeScale.caption1, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Tokens.Space.sp6), color = p.secondary)
    }
}

// ───────────────────────── 잠깐 바라보기 ─────────────────────────

/**
 * 실망을 보낸 뒤 권하는 30초: 바탕 한 빛 위 손그림 동그라미 하나가 천천히 차오른다. 글자는 한 줄뿐.
 * 끝나면 “잘 쉬었어요.” 와 정원으로. 누르거나 뒤로 가면 바로 돌아간다.
 */
@Composable
fun LookScreen(onDone: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val c = G.Care
    val fill = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    KeepScreenOn(!done)
    LaunchedEffect(Unit) { fill.animateTo(1f, tween((c.lookSeconds * 1000).toInt(), easing = LinearEasing)); done = true }
    BackHandler(onBack = onDone)
    val ink = Theme.gc.ink; val soft = G.Mood.Colors.calm
    val u = with(androidx.compose.ui.platform.LocalDensity.current) { Theme.unit.toPx() }
    val lineMask = Crayon.tooth(GardenArt.toothLine(ctx), u)
    Column(Modifier.fillMaxSize().paperBackground().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
        .statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.size(Theme.unit * c.lookSize)) {
            val r = size.minDimension / 2 * 0.92f
            val ctr = Offset(size.width / 2, size.height / 2)
            // 안에서부터 조용히 차오르는 빛
            drawCircle(soft.copy(alpha = 0.55f), r * fill.value, ctr)
            with(Crayon) { textured(lineMask) { stroke(handCircle(ctr, r, 17), G.Mood.line * 2 * u, ink, 17, passes = 1) } }
        }
        Spacer(Modifier.height(Tokens.Space.sp8))
        val doneText = remember { io.github.graviton94.carpediem.ui.Labels.timed(ctx, "look_done", io.github.graviton94.carpediem.ui.Labels.part(java.time.LocalDateTime.now())) }
        TokenText(if (done) doneText ?: stringResource(R.string.look_done) else stringResource(R.string.look_cue), Tokens.TypeScale.title3.serif(), color = p.secondary, align = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Spacer(Modifier.height(Tokens.Space.sp6))
        Box(Modifier.height(Tokens.Layout.tapTarget + Tokens.Space.sp2)) { if (done) GardenButton(stringResource(R.string.breath_home), onDone, filled = true, seed = 1210) }
        Spacer(Modifier.weight(1f))
    }
}

/** 고마움 숨을 마친 뒤: 떠오른 것을 한 줄로 고마움 책에 (오늘의 한 줄이 된다, 하루 한 줄). */
@Composable
private fun ThanksAfter(state: AppState, today: java.time.LocalDate) {
    val p = Theme.palette
    var text by remember { mutableStateOf("") }
    var kept by remember { mutableStateOf(false) }
    if (state.sentOn(today) && !kept) return
    Column(Modifier.fillMaxWidth().padding(bottom = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), horizontalAlignment = Alignment.CenterHorizontally) {
        if (kept) { TokenText(stringResource(R.string.breath_thanksKept), Tokens.TypeScale.subhead.serif(), align = TextAlign.Center); return@Column }
        TokenText(stringResource(R.string.breath_thanksAsk), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
        androidx.compose.foundation.text.BasicTextField(
            value = text, onValueChange = { v -> val one = v.replace('\n', ' '); if (one.codePointCount(0, one.length) <= G.LetGo.maxChars.toInt()) text = one },
            singleLine = true, textStyle = Tokens.TypeScale.callout.style().copy(color = p.foreground), cursorBrush = androidx.compose.ui.graphics.SolidColor(p.foreground),
            modifier = Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1025).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
            decorationBox = { inner -> Box { if (text.isEmpty()) TokenText(stringResource(R.string.breath_thanksHint), Tokens.TypeScale.callout, color = p.secondary); inner() } },
        )
        GardenButton(stringResource(R.string.breath_thanksKeep), {
            if (text.isNotBlank()) { state.letGo(text, io.github.graviton94.carpediem.core.Feeling.THANKS); state.toast = null; state.care = null; kept = true }
        }, filled = text.isNotBlank(), seed = 1026)
    }
}
