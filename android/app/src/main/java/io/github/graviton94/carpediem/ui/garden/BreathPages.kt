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
    }
}
internal fun kindName(k: BreathKind) = when (k) { BreathKind.CALM -> R.string.breath_kind_calm; BreathKind.BOX -> R.string.breath_kind_box; BreathKind.SLEEP -> R.string.breath_kind_sleep }
private fun kindDesc(k: BreathKind) = when (k) { BreathKind.CALM -> R.string.breath_kindDesc_calm; BreathKind.BOX -> R.string.breath_kindDesc_box; BreathKind.SLEEP -> R.string.breath_kindDesc_sleep }
private fun soundName(s: Sound) = when (s) { Sound.NONE -> R.string.sound_none; Sound.WAVES -> R.string.sound_waves; Sound.WIND -> R.string.sound_wind; Sound.RAIN -> R.string.sound_rain; Sound.TONE -> R.string.sound_tone }
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
 * 하루와 함께 숨 쉬기. 하루가 들이쉴 때 조금 부풀어 떠오르고 내쉴 때 가라앉는다. 3초 뒤 지긋이 눈을 감는다.
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
    val player = remember(sound) { Soundscape.Player(sound) }
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
        val season = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now).season
        Image(GardenArt.sky(ctx, season), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
        SkyTimeLayer(now, maxHeight * G.Layout.groundRatio, 0.dp, 0.dp, Modifier.fillMaxSize())
        // 숨에 따라 하늘이 아주 조금 밝아졌다 짙어짐
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = b.skyLift * full }.background(Color.White))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            val art = HaruArt.of(state.store.haruSeed, false)
            val scale = u * (G.Layout.meetHaruWidth / G.Layout.haruArtWidth)
            val k = with(androidx.compose.ui.platform.LocalDensity.current) { scale.toPx() }
            Box(Modifier.graphicsLayer {
                if (animate) {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    val s = 1f + b.swell * full; scaleX = s; scaleY = s
                    translationY = -b.rise * k * full
                }
            }) { BigStoneOnly(art, scale, lid.value) }
            CrayonRule(Modifier.padding(horizontal = screenW * 0.2f), seed = 1020)
            Spacer(Modifier.height(Tokens.Space.sp8))
            // 글자: 첫 1분만. 끝나면 한 줄 + 정원으로
            val cue = if (done) null else step?.takeIf { elapsed < b.cueSeconds * 1000 }
            Box(Modifier.height(Tokens.Space.sp10), contentAlignment = Alignment.Center) {
                if (cue != null) TokenText(stringResource(stepName(cue)), Tokens.TypeScale.title2.serif(), color = p.secondary, align = TextAlign.Center)
            }
            if (done) {
                val msg = remember { ctx.resources.getIdentifier("breath_done_${kind.name.lowercase()}_${(1..3).random()}", "string", ctx.packageName) }
                TokenText(stringResource(msg), Tokens.TypeScale.headline.serif(), Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
                Spacer(Modifier.height(Tokens.Space.sp6))
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
 * 숫자도 글자도 없이 하늘과 땅, 나와 가족의 돌만. 구름이 아주 느리게 흐르고, 돌들은 가끔 깜빡인다 (누르면 쓰다듬기만).
 * 5분 뒤 스르르 어두워지고, 10분 뒤 화면 켜둠을 푼다. 소리는 마지막에 고른 바탕 소리 (끄고 켤 수 있음).
 */
@Composable
fun GazeScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, onBack: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val z = G.Gaze
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    var soundOn by remember { mutableStateOf(state.sound != Sound.NONE) }
    val player = remember(soundOn) { Soundscape.Player(if (soundOn) state.sound else Sound.NONE) }
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
    val drift by rememberInfiniteTransition(label = "clouds").animateFloat(0f, 1f, infiniteRepeatable(tween((z.cloudSeconds * 1000).toInt(), easing = LinearEasing), RepeatMode.Restart), label = "drift")

    BoxWithConstraints(Modifier.fillMaxSize().paperBackground()) {
        val u = Theme.unit
        val screenH = maxHeight
        Image(GardenArt.sky(ctx, s.season), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
        val gy = screenH * G.Layout.groundRatio
        SkyTimeLayer(now, gy, screenH * 0.08f, gy - u * 60, Modifier.fillMaxSize())
        // 구름: 앱이 그리는 둥근 조각 몇 개가 아주 느리게
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val uu = u.toPx()
            val r = Crayon.Rng(7)
            repeat(z.clouds.toInt()) { i ->
                val y = size.height * (0.12f + 0.32f * r.next()); val sc = 0.7f + 0.6f * r.next(); val off = r.next()
                val x = ((drift + off) % 1f) * (w + uu * 160) - uu * 80
                val c = Color.White.copy(alpha = 0.55f)
                listOf(Offset(0f, 0f) to 26f, Offset(22f, -8f) to 20f, Offset(-22f, -4f) to 18f, Offset(40f, 4f) to 14f).forEach { (o, rr) ->
                    drawOval(c, Offset(x + (o.x - rr) * uu * sc, y + (o.y - rr * 0.6f) * uu * sc), Size(rr * 2 * uu * sc, rr * 1.2f * uu * sc))
                }
            }
        }
        Image(GardenArt.strip(ctx, s.season), null, Modifier.offset(y = gy - u * G.Layout.stripLineY).fillMaxWidth().height(u * G.Layout.stripHeight), contentScale = ContentScale.FillBounds)
        // 돌들 (정원과 같은 자리 규칙, 이름표 없이)
        val haruScale = u * (G.Layout.haruWidth / G.Layout.haruArtWidth)
        val slots = gardenSlots(state, profile, s, now, haruScale)
        val targets = slots.map { sl -> sl.progress?.let { (G.Layout.pathStart + G.Layout.pathInset + (G.Layout.pathEnd - G.Layout.pathStart - 2 * G.Layout.pathInset) * it.toFloat().coerceIn(0f, 1f)).toDouble() * u.value } }
        val widths = slots.map { sl -> (sl.art.meta.bbox.width * sl.scale.value).toDouble() }
        val xs = Family.place(targets, widths, 0, (u * G.Layout.pathStart).value.toDouble(), (u * G.Layout.pathEnd).value.toDouble(), (G.Family.gap * u.value).toDouble(), (G.Family.minGap * u.value).toDouble()).map { it.toFloat().dp }
        slots.forEachIndexed { i, sl ->
            val cx = xs[i] - sl.scale * (sl.art.meta.bbox.center.x - sl.art.meta.box / 2)
            HaruFigure(sl.art, sl.scale, Modifier.offset(cx - sl.scale * (sl.art.meta.box / 2), gy - sl.scale * G.Layout.haruGround), hat = sl.birthday)
        }
        // 스르르 어두워짐
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = dim.value }.background(Color.Black))
        // 소리 끄고 켜기 (아주 작게), 나가는 법은 처음 3초만
        TokenText(stringResource(if (soundOn) R.string.gaze_soundOff else R.string.gaze_soundOn), Tokens.TypeScale.caption1,
            Modifier.align(Alignment.TopEnd).statusBarsPadding().clickable { soundOn = !soundOn; if (soundOn && state.sound == Sound.NONE) state.changeSound(Sound.WAVES) }.padding(Tokens.Space.sp4),
            color = p.secondary.copy(alpha = 0.7f), weight = FontWeight.Normal)
        if (hint) TokenText(stringResource(R.string.gaze_exit), Tokens.TypeScale.caption1, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Tokens.Space.sp6), color = p.secondary)
    }
}
