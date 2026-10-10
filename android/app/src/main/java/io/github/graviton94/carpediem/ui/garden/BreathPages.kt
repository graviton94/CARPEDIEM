package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import io.github.graviton94.carpediem.ui.GardenAlert
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
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
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.sin
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Row

internal fun rhythm(k: BreathKind): Breath.Rhythm {
    val b = G.Breath
    return when (k) {
        BreathKind.CALM -> Breath.Rhythm(b.calmIn.toDouble(), 0.0, b.calmOut.toDouble(), 0.0)
        BreathKind.BOX -> Breath.Rhythm(b.boxIn.toDouble(), b.boxHold.toDouble(), b.boxOut.toDouble(), b.boxRest.toDouble())
        BreathKind.SLEEP -> Breath.Rhythm(b.sleepIn.toDouble(), b.sleepHold.toDouble(), b.sleepOut.toDouble(), 0.0)
        // 한숨 호흡 (cyclic sighing): 코로 두 번 나눠 들이쉬고 (2 + 1) 길게 내쉼 (6)
        BreathKind.THANKS -> Breath.Rhythm(b.thanksIn.toDouble(), 0.0, b.thanksOut.toDouble(), 0.0, topS = b.thanksTop.toDouble())
    }
}
/** 고르면 정해지는 숨 (1.1.4): 물결 5분 · 산책 4분 · 등불 여덟 번 · 꽃밭 5분. 시간은 고르지 않음. */
internal fun planFor(k: BreathKind): List<Breath.Phase> {
    val b = G.Breath
    return when (k) {
        BreathKind.CALM -> Breath.plan(rhythm(k), b.calmMinutes.toInt())
        BreathKind.BOX -> Breath.plan(rhythm(k), b.boxMinutes.toInt())
        BreathKind.SLEEP -> Breath.cycles(rhythm(k), b.sleepCycles.toInt())
        BreathKind.THANKS -> Breath.plan(rhythm(k), b.thanksMinutes.toInt())
    }
}
/** 그 숨의 길이 (분, 반올림). */
internal fun planMinutes(k: BreathKind): Int = planFor(k).last().let { ((it.startMs + it.lengthMs + 30_000) / 60_000).toInt().coerceAtLeast(1) }
internal fun partTitle(p: io.github.graviton94.carpediem.core.DayPart) = when (p) {
    io.github.graviton94.carpediem.core.DayPart.MORNING -> R.string.breath_part_morning; io.github.graviton94.carpediem.core.DayPart.DAY -> R.string.breath_part_day
    io.github.graviton94.carpediem.core.DayPart.EVENING -> R.string.breath_part_evening; io.github.graviton94.carpediem.core.DayPart.NIGHT -> R.string.breath_part_night
}
internal fun kindName(k: BreathKind) = when (k) { BreathKind.CALM -> R.string.breath_kind_calm; BreathKind.BOX -> R.string.breath_kind_box; BreathKind.SLEEP -> R.string.breath_kind_sleep; BreathKind.THANKS -> R.string.breath_kind_thanks }
private fun kindDesc(k: BreathKind) = when (k) { BreathKind.CALM -> R.string.breath_kindDesc_calm; BreathKind.BOX -> R.string.breath_kindDesc_box; BreathKind.SLEEP -> R.string.breath_kindDesc_sleep; BreathKind.THANKS -> R.string.breath_kindDesc_thanks }
private fun soundName(s: Sound) = when (s) { Sound.NONE -> R.string.sound_none; Sound.WAVES -> R.string.sound_waves; Sound.WIND -> R.string.sound_wind; Sound.RAIN -> R.string.sound_rain; Sound.TONE -> R.string.sound_tone; Sound.SEASON -> R.string.sound_season }
private fun stepName(s: BreathStep) = when (s) { BreathStep.IN -> R.string.breath_in; BreathStep.HOLD -> R.string.breath_hold; BreathStep.OUT -> R.string.breath_out; BreathStep.REST -> R.string.breath_rest }

/** 밤(nightFrom ~ 새벽)에는 잠드는 명상을 먼저. */
/** 손끝 숨에서 화면이 어두워지기까지 (ms). */
/** 말로 안내하는 숨 (처음 세 숨, 그다음은 종소리와 하루의 움직임만) · 마지막 말이 스르르 사라지는 시간. */
private const val CUE_BREATHS = 3
/** 숨을 마치고 리뷰 창을 청하기까지 (끝 한마디를 먼저 읽게). */
private const val REVIEW_AFTER_BREATH_MS = 4000L
private const val CUE_OUT_MS = 1500

internal fun isNight(now: LocalDateTime) = now.hour >= G.Breath.nightFrom.toInt() || now.hour < G.Motion.sunrise.toInt()
/** 하루 닫기의 그날: 해 뜨기 전 (자정 넘어) 은 아직 어젯밤. */
internal fun closeDayOf(now: LocalDateTime): LocalDate = now.toLocalDate().let { if (now.hour < G.Motion.sunrise.toInt()) it.minusDays(1) else it }

// ───────────────────────── 숨 고르기 창 ─────────────────────────

/** 아래에서 올라오는 한 장: 숨 · 시간 · 소리를 고르고 시작. 마지막에 고른 것을 기억. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BreathSheet(state: AppState, now: LocalDateTime, onStart: (BreathKind, Int, Sound) -> Unit, onDismiss: () -> Unit) {
    val p = Theme.palette
    var kind by remember { mutableStateOf(if (isNight(now)) BreathKind.SLEEP else state.breathKind) }
    var sound by remember { mutableStateOf(state.sound) }
    var touchOn by remember { mutableStateOf(state.breathTouch) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Theme.gc.paper, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            TokenText(stringResource(R.string.breath), Tokens.TypeScale.title3.serif())
            // 때에 맞는 숨의 이름 (하루를 여는 · 잠시 쉬어가는 · 내려놓는 · 마무리하는)
            TokenText(stringResource(partTitle(io.github.graviton94.carpediem.ui.Labels.part(now))), Tokens.TypeScale.footnote.serif(), color = p.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                BreathKind.entries.forEachIndexed { i, k -> GardenChip(stringResource(kindName(k)), kind == k, 1000 + i) { kind = k } }
            }
            // 숨마다 목적 · 호흡법 · 정해진 길이 (시간은 고르지 않음)
            TokenText(stringResource(kindDesc(kind)), Tokens.TypeScale.footnote, color = p.secondary)
            // 하루와 함께하는 방법: 눈으로 (화면) · 손끝으로 (떨림, 화면은 어둡게)
            val ctx = androidx.compose.ui.platform.LocalContext.current
            if (remember { canTouch(ctx) }) {
                TokenText(stringResource(R.string.breath_way), Tokens.TypeScale.caption1, color = p.secondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    GardenChip(stringResource(R.string.breath_wayEyes), !touchOn, 1030) { touchOn = false }
                    GardenChip(stringResource(R.string.breath_wayTouch), touchOn, 1031) { touchOn = true }
                }
                if (touchOn) TokenText(stringResource(R.string.breath_wayTouchHelp), Tokens.TypeScale.footnote, color = p.secondary)
            }
            TokenText(stringResource(R.string.breath_sound), Tokens.TypeScale.caption1, color = p.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                Sound.entries.forEachIndexed { i, s -> GardenChip(stringResource(soundName(s)), sound == s, 1008 + i) { sound = s } }
            }
            Spacer(Modifier.height(Tokens.Space.sp2))
            GardenButton(stringResource(R.string.breath_start), { val minutes = planMinutes(kind); state.chooseBreath(if (isNight(now) && kind == BreathKind.SLEEP) state.breathKind else kind, minutes, sound); state.changeBreathTouch(touchOn); onStart(kind, minutes, sound) }, filled = true, seed = 1015)
        }
    }
}

// ───────────────────────── 숨 쉬는 화면 ─────────────────────────

/** 화면이 켜져 있게 (숨 · 멍하니 보는 정원 동안만). */
@Composable
internal fun KeepScreenOn(on: Boolean) {
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
    val plan = remember(kind) { planFor(kind) }
    val total = plan.last().let { it.startMs + it.lengthMs }
    var elapsed by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val animate = remember { !reducedMotion(ctx) }
    val player = remember(sound) { Soundscape.Player(sound, io.github.graviton94.carpediem.core.Memories.seasonOf(now.toLocalDate())) }
    val part = io.github.graviton94.carpediem.ui.Labels.part(now)
    // 밤의 잠드는 명상: 끝나면 화면이 스르르 어두워지고 그대로 머묾 (앱이 꺼진 듯 사라지지 않게, 화면은 폰이 스스로 끔). 누르면 정원으로
    val sleepAfter = kind == BreathKind.SLEEP && isNight(now)
    val blackout = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (done && sleepAfter) {
            delay(G.Breath.sleepFadeAfter.toLong()); blackout.animateTo(1f, tween(G.Breath.sleepFadeMs.toInt()))
        }
    }
    KeepScreenOn(!done)
    val touchOn = state.breathTouch
    // 소리는 숨이 흐를 때만: 멈추면 스르르 꺼지고, 이어 하면 다시
    DisposableEffect(player, paused, done) { if (!paused && !done) player.start() else player.stop(); onDispose { player.stop() } }
    // 앱을 떠나면 (홈 · 화면 끔) 멈춤으로: 소리가 혼자 계속 흐르지 않게 (손끝 숨은 주머니 속에서도 이어지므로 그대로)
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner, touchOn) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP && !touchOn) paused = true }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // 숨 시작 전: “맑은 종에 들이쉬고, 낮은 종에 내쉬어요” 와 두 종을 한 번씩 (소리가 있을 때만)
    val S = Tokens.Garden.Sound
    var intro by remember { mutableStateOf(sound != Sound.NONE) }
    LaunchedEffect(Unit) {
        if (!intro) return@LaunchedEffect
        delay(300); Soundscape.bowl(S.bowlInHz.toDouble())
        delay((S.introMs * 0.42f).toLong()); Soundscape.bowl(S.bowlOutHz.toDouble())
        delay((S.introMs * 0.58f).toLong() - 300); intro = false
    }
    // 하루의 숨결을 손끝으로 (05): 숨을 떨림으로. 화면은 곧 아주 어두워지고, 한 번 누르면 잠깐 밝아짐 (멈춤은 길게 누르기)
    val touch = rememberTouchBreath(state.breathTouch)
    // 화면은 숨이 끝날 때까지 그대로 켜 둠 (어둡게 하지 않음: 손끝 숨에서도 눈을 뜨면 하루가 보이게)
    // 시계: 멈춘 동안은 흐르지 않음. 손끝 숨은 벽시계로 (화면이 잠깐 꺼졌다 와도 떨림과 같은 자리)
    LaunchedEffect(paused, done, intro) {
        if (paused || done || intro) { touch?.stop(); return@LaunchedEffect }
        touch?.play(plan, elapsed)
        val wall0 = wallNow() - elapsed
        var last = withFrameMillis { it }
        while (!done) {
            val t = withFrameMillis { it }
            if (t - last < 33) continue   // 1초에 30번이면 충분
            if (touch != null) elapsed = wallNow() - wall0 else elapsed += t - last
            last = t
            player.breath = fullAt(plan, elapsed)
            if (elapsed >= total) { done = true; touch?.stop(); state.recordBreath(kind); if (sound != Sound.NONE) Soundscape.bowl(S.bowlOutHz.toDouble(), 2); player.stop() }
        }
    }
    // 숨을 끝까지 쉬었으면 (잠드는 밤 숨은 빼고): 조금 머문 뒤 Play 리뷰 창을 청할 수도 (넉 달에 한 번까지)
    LaunchedEffect(done) { if (done && !sleepAfter && elapsed >= total) { delay(REVIEW_AFTER_BREATH_MS); state.maybeAskReview() } }
    // 매 프레임 바뀌는 값 (elapsed) 은 그리는 단계에서만 읽음: 화면은 단계가 바뀔 때만 다시 짜임
    val step by remember(plan) { androidx.compose.runtime.derivedStateOf { Breath.at(plan, elapsed)?.first?.step } }
    // 말 안내는 처음 세 숨까지 (네 번째 숨이 시작되면 말 없이)
    val cueUntil = remember(plan) { plan.filter { it.step == BreathStep.IN && it.lo == 0f }.getOrNull(CUE_BREATHS)?.startMs ?: Long.MAX_VALUE }
    val cueOn by remember { androidx.compose.runtime.derivedStateOf { elapsed < cueUntil } }
    // 한숨 호흡의 두 번째 들이쉼 (“한 번 더 들이쉬어요”)
    val topUp by remember(plan) { androidx.compose.runtime.derivedStateOf { (Breath.at(plan, elapsed)?.first?.lo ?: 0f) > 0f } }
    // 단계가 바뀌면 들이쉼에만 아주 짧게 한 번 (내쉼은 고요히)
    LaunchedEffect(step, intro) {
        if (intro) return@LaunchedEffect
        // 숨마다 명상 종: 들이쉴 땐 맑은 종, 내쉴 땐 낮은 종 (머무는 숨에는 없음)
        if (sound != Sound.NONE) when (step) { BreathStep.IN -> Soundscape.bowl(S.bowlInHz.toDouble()); BreathStep.OUT -> Soundscape.bowl(S.bowlOutHz.toDouble()); else -> {} }
        if (touch != null) return@LaunchedEffect   // 손끝 숨은 숨결 떨림이 대신
        if (step == BreathStep.IN) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
        // 내쉼에는 떨지 않음 (들이쉼에 아주 짧게 한 번만, 눈으로 할 때)
    }
    // 눈: 처음 3초는 뜨고, 그다음 지긋이 감고, 끝나면 천천히 뜸
    val lid = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (!done) { delay((b.eyesAfter * 1000).toLong()); lid.animateTo(1f, tween(1200)) } else lid.animateTo(0f, tween(1500))
    }
    BackHandler { if (done) onDone() else paused = true }

    BoxWithConstraints(Modifier.fillMaxSize().paperBackground().pointerInput(touch != null, done) {
        // 손끝 숨: 주머니 속 누름에 멈추지 않게 한 번 누르면 잠깐 밝아지기만, 길게 누르면 멈춤
        detectTapGestures(onTap = { if (!done && touch == null) paused = true }, onLongPress = { if (!done) paused = true })
    }) {
        val u = Theme.unit
        val screenW = maxWidth
        // 숨의 말은 화면 한가운데, 하루는 작게 화면 높이 haruAt 즈음에 (말이 먼저 눈에 들어오게)
        val groundY = maxHeight * b.haruAt
        val art = HaruArt.of(state.store.haruSeed, false)
        val scale = u * (b.haruWidth / G.Layout.haruArtWidth)
        val k = with(androidx.compose.ui.platform.LocalDensity.current) { scale.toPx() }
        // 숨의 갈래마다의 그림: 마음 물결 · 마음 산책 · 마음 등불 · 마음 꽃밭
        BreathScene(kind, art, scale, groundY, animate, plan, { elapsed }, { fullAt(plan, elapsed) })
        // 위에 아주 옅게 때의 이름 (첫 1분만)
        Box(Modifier.fillMaxWidth().statusBarsPadding().padding(top = Tokens.Space.sp8).height(Tokens.Space.sp8), contentAlignment = Alignment.Center) {
            if (!done && touch != null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TokenText(stringResource(R.string.breath_touchTitle), Tokens.TypeScale.footnote.serif(), color = p.secondary.copy(alpha = 0.8f))
                TokenText(stringResource(R.string.breath_touchSub), Tokens.TypeScale.caption2, color = p.secondary.copy(alpha = 0.6f))
            } else if (!done && cueOn) TokenText(stringResource(partTitle(part)), Tokens.TypeScale.footnote.serif(), color = p.secondary.copy(alpha = 0.7f))
        }
        // 하루와 땅선 (마음 물결은 웅덩이가 땅선)
        val boxH = scale * (G.Layout.haruGround - art.meta.bbox.top + G.Layout.sparkle)
        Column(Modifier.align(Alignment.TopCenter).offset(y = groundY - boxH).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.graphicsLayer {
                if (animate) {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    val full = fullAt(plan, elapsed)
                    val s = 1f + b.swell * full; scaleX = s; scaleY = s
                    translationY = -b.rise * k * full
                }
            }) { BigStoneOnly(art, scale, lid.value) }
            if (kind != BreathKind.CALM) CrayonRule(Modifier.padding(horizontal = screenW * b.ruleInset), seed = 1020)
        }
        // 가운데: 숨의 말이 한 글자씩 (오늘의 문장 크기, 첫 1분만). 끝나면 한 줄 + 정원으로
        Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin), horizontalAlignment = Alignment.CenterHorizontally) {
            val cue = if (done) null else step?.takeIf { cueOn }
            // 두 줄이 되어도 잘리지 않게 (높이는 최소만 정함)
            Box(Modifier.heightIn(min = Tokens.Space.sp10 * 2), contentAlignment = Alignment.Center) {
                val cueText = when {
                    intro -> stringResource(R.string.breath_bells)
                    cue != null -> stringResource(if (topUp && cue == BreathStep.IN) R.string.breath_inTop else cueName(kind, cue))
                    else -> null
                }
                // 세 숨이 지나면 마지막 말이 스르르 사라짐 (뚝 끊기지 않게)
                val lastCue = remember { arrayOfNulls<String>(1) }
                if (cueText != null) lastCue[0] = cueText
                androidx.compose.animation.AnimatedVisibility(visible = cueText != null, enter = androidx.compose.animation.EnterTransition.None,
                    exit = androidx.compose.animation.fadeOut(tween(CUE_OUT_MS))) { lastCue[0]?.let { BreathCue(it, p.secondary) } }
            }
            if (kind == BreathKind.BOX && cue != null && animate) WalkCount(plan, { elapsed }, Modifier.size(u * 44f, u * 4f))
            if (done) {
                // 아침 · 저녁 · 밤은 때의 말, 낮은 숨마다의 말
                val msg = remember { if (touch != null) ctx.getString(R.string.breath_touchDone) else io.github.graviton94.carpediem.ui.Labels.timed(ctx, "breath_done", part) ?: ctx.getString(ctx.resources.getIdentifier("breath_done_${kind.name.lowercase()}_${(1..3).random()}", "string", ctx.packageName)) }
                TokenText(msg, Tokens.TypeScale.headline.serif(), Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, align = TextAlign.Center)
                Spacer(Modifier.height(Tokens.Space.sp6))
                // 고마움 명상 뒤: 떠오른 고마움을 오늘의 한 줄로 (오늘 아직 보내지 않았을 때)
                if (kind == BreathKind.THANKS && state.keepLines) ThanksAfter(state, now.toLocalDate())
                GardenButton(stringResource(R.string.breath_home), onDone, filled = true, seed = 1021)
            } else TokenText(stringResource(R.string.breath_startA11y, stringResource(kindName(kind)), "${planMinutes(kind)}"), Tokens.TypeScale.caption2,
                Modifier.semantics { liveRegion = LiveRegionMode.Polite }.graphicsLayer { alpha = 0f })
        }
        // 아주 옅은 가는 선 하나가 차오름 (남은 시간은 보이지 않음)
        val lineC = Theme.gc.ink.copy(alpha = 0.28f)
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp6)
            .fillMaxWidth().height(u * b.line).background(Theme.gc.ink.copy(alpha = 0.08f)).drawBehind {
                drawRect(lineC, size = androidx.compose.ui.geometry.Size(size.width * (elapsed.toFloat() / total).coerceIn(0f, 1f), size.height))
            })
        if (blackout.value > 0f) Box(Modifier.fillMaxSize().graphicsLayer { alpha = blackout.value }.background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onDone() } }, contentAlignment = Alignment.BottomCenter) {
            // 어둠 속 아주 옅은 한 줄: 누르면 정원으로
            if (blackout.value >= 1f) TokenText(stringResource(R.string.breath_sleepBack), Tokens.TypeScale.footnote,
                Modifier.navigationBarsPadding().padding(bottom = Tokens.Space.sp10), color = Color.White.copy(alpha = 0.35f))
        }
        if (paused && !done) PauseCard(onKeep = { paused = false }, onStop = { paused = false; done = true; player.stop(); onDone() })
    }
}

/** 숨의 말: 단계가 바뀌면 한 번에 천천히 스며 나옴 (한 글자씩 치면 짧은 숨에선 휙휙 지나가서). 움직임을 끈 기기면 바로. */
@Composable
internal fun BreathCue(text: String, color: Color) {
    val ctx = LocalContext.current
    val still = remember { reducedMotion(ctx) }
    val a = remember(text) { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(text) { a.animateTo(1f, tween(CUE_FADE_MS)) }
    TypedText(text, text.length, Tokens.TypeScale.headline.serif(), color, Modifier.fillMaxWidth().graphicsLayer { alpha = a.value })
}
private const val CUE_FADE_MS = 1200

/** 단계의 말. 마음 산책은 걸음으로 (네 걸음 들이쉬고 · 네 걸음 내쉬고), 마음 꽃밭은 내쉴 때 “고마운 것 하나”. */
internal fun cueName(kind: BreathKind, s: BreathStep): Int = when {
    kind == BreathKind.BOX && s == BreathStep.IN -> R.string.breath_walk_in
    kind == BreathKind.BOX && s == BreathStep.OUT -> R.string.breath_walk_out
    kind == BreathKind.THANKS && s == BreathStep.OUT -> R.string.breath_out_thanks
    else -> stepName(s)
}

/** 지금 숨이 얼마나 찼는지 (0 … 1). 머무는 숨에서도 멈춰 있지 않고 아주 조금 부풀었다 가라앉음. */
internal fun fullAt(plan: List<Breath.Phase>, ms: Long): Float {
    val a = Breath.at(plan, ms) ?: return 0f
    val f = Breath.fullness(a.first, a.second)
    return if (a.first.step == BreathStep.HOLD) f + 0.03f * kotlin.math.sin(a.second * a.first.lengthMs / 2400f * 6.2832f) else f
}

/** 숨 쉬는 동안의 큰 하루 (땅선은 부르는 쪽이). lid = 눈꺼풀. */
@Composable
internal fun BigStoneOnly(art: HaruArt, scale: Dp, lid: Float) {
    val boxW = scale * art.meta.box
    Box(Modifier.size(boxW, scale * (G.Layout.haruGround - art.meta.bbox.top + G.Layout.sparkle))) {
        val cx = boxW / 2 - scale * (art.meta.bbox.center.x - art.meta.box / 2)
        HaruFigure(art, scale, Modifier.offset(cx - boxW / 2, scale * (art.meta.bbox.top - G.Layout.sparkle) * -1f + 0.dp), lid = lid)
    }
}

@Composable
internal fun PauseCard(onKeep: () -> Unit, onStop: () -> Unit) {
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
    val player = remember(soundOn) { Soundscape.Player(if (soundOn) state.sound else Sound.NONE, io.github.graviton94.carpediem.core.GardenDecor.realSeason(now.toLocalDate(), profile.countryCode), gaze = true,
        songNotes = io.github.graviton94.carpediem.sound.StoneSong.gardenNotes(state.store.haruSeed, state.people.map { it.seed })) }
    // 잠들기 (1.1.5): 고른 n분이 되면 소리를 반으로 줄이고 화면을 서서히 어둡게 끈 뒤, 2n분에 소리도 완전히 멈춤
    var sleepPicking by remember { mutableStateOf(state.debugSleepSheet) }
    var sleepMin by remember { androidx.compose.runtime.mutableIntStateOf(state.store.sleepMinutes) }
    var sleepFrom by remember { mutableStateOf<Long?>(null) }   // 시작한 때 (벽시계), null = 꺼짐
    var sleeping by remember { mutableStateOf(false) }           // n분이 지나 잦아드는 중
    var slept by remember { mutableStateOf(false) }              // 2n분이 지나 소리까지 멈춤
    var sleepLeft by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val sleepOnNow by androidx.compose.runtime.rememberUpdatedState(sleepFrom != null || slept)
    val sleptNow by androidx.compose.runtime.rememberUpdatedState(slept)
    // 돌멍하기 소리: 앱을 떠나면 스르르 꺼지고, 돌아오면 다시 (잠들기 중이면 화면이 꺼져도 그대로 이어짐)
    val gazeOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(player, gazeOwner) {
        player.start()
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) { if (!sleepOnNow) player.stop() }
            else if (e == androidx.lifecycle.Lifecycle.Event.ON_START) { if (!sleptNow) player.start() }
        }
        gazeOwner.lifecycle.addObserver(obs); onDispose { gazeOwner.lifecycle.removeObserver(obs); player.stop() }
    }
    LaunchedEffect(sleepFrom, player) {
        val from = sleepFrom ?: run { player.level = 1f; sleeping = false; return@LaunchedEffect }
        val n = sleepMin * 60_000L
        while (true) {
            val e = wallNow() - from
            sleepLeft = ((n - e + 59_999) / 60_000).toInt().coerceAtLeast(0)
            if (e < n) { player.level = 1f; sleeping = false }
            else if (e < 2 * n) { sleeping = true; player.level = SLEEP_HALF * (1f - (e - n).toFloat() / n) }
            else { player.level = 0f; player.stop(); slept = true; sleeping = false; sleepFrom = null; break }
            delay(1000)
        }
    }
    var screenOn by remember { mutableStateOf(true) }
    KeepScreenOn(screenOn)
    val dim = remember { Animatable(0f) }
    var hint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(6000); hint = false }
    // 조금 머문 날만 돌멍하기 한 날로 (열 날마다 이끼에 봉오리 하나)
    LaunchedEffect(Unit) { delay(Tokens.Garden.Decor.gazeCountMs.toLong()); state.recordGaze() }
    // 누를 때마다 처음부터: 밝게 → 조금 뒤 스르르 어두워짐 → 더 지나면 화면을 놓아 줌
    var idleKick by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(idleKick, sleeping, slept, sleepFrom != null) {
        // 잠들기: 잦아드는 중이면 2분에 걸쳐 까맣게 어두워진 뒤 화면을 놓아 줌 (폰이 스스로 잠듦), 다 잠들었으면 어둠 그대로
        if (slept) { dim.snapTo(1f); screenOn = false; return@LaunchedEffect }
        dim.snapTo(0f); screenOn = true
        if (sleeping) { dim.animateTo(1f, tween(SLEEP_DIM_MS, easing = LinearEasing)); screenOn = false; return@LaunchedEffect }
        if (sleepFrom != null) return@LaunchedEffect   // 정한 시간까지는 밝은 그대로
        delay((z.dimAfter * 1000).toLong()); dim.animateTo(z.dimAlpha, tween(z.dimMs.toInt(), easing = LinearEasing))
        delay(((z.releaseAfter - z.dimAfter) * 1000).toLong() - z.dimMs.toLong()); screenOn = false
    }
    // 어디를 누르든 (돌을 쓰다듬어도) 다시 밝아짐. 누름은 그대로 정원에 닿음
    // 돌이 아닌 빈 곳을 누르면 ‘정원으로 돌아갈까요?’
    var ask by remember { mutableStateOf(false) }
    BackHandler { if (ask) onBack() else ask = true }
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope { while (true) { val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial); if (e.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
            // 다 잠든 뒤 다시 누르면: 정원이 밝아지고 소리도 처음 크기로
            if (slept) { slept = false; player.level = 1f; player.start() }
            idleKick++ } } }
    }.pointerInput(Unit) { detectTapGestures { ask = true } }) {
        // 홈의 정원 그대로 (하늘 · 해와 달 · 땅 · 돌 · 놓인 것 · 밤빛), 글자만 없이
        GardenHome(state, profile, now, onSettings = {}, onCollection = {}, onSupport = {}, onStone = {}, onAddPerson = {}, bare = true)
        // 움직이는 정원: 구름 · 빛의 숨 · 내려오는 잎 · 새 · 반딧불, 봄 · 여름엔 연못, 가을 · 겨울엔 화톳불
        GazeLife(io.github.graviton94.carpediem.core.GardenDecor.realSeason(now.toLocalDate(), profile.countryCode), now, calm = io.github.graviton94.carpediem.core.BreathKind.CALM in state.breathTrace(now.toLocalDate())) { dim.value }
        // 스르르 어두워짐
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = dim.value }.background(Color.Black))
        // 소리 끄고 켜기 · 정원으로 (아주 작게, 위 양끝). 돌은 누르면 쓰다듬기, 나가기는 ‘정원으로’ · 빈 곳 누르기 · 뒤로 가기
        TokenText(stringResource(R.string.breath_home), Tokens.TypeScale.footnote,
            Modifier.align(Alignment.TopStart).statusBarsPadding().heightIn(min = Tokens.Layout.tapTarget).clickable { ask = true }.padding(Tokens.Space.sp4),
            color = p.secondary.copy(alpha = 0.8f), weight = FontWeight.Normal)
        Row(Modifier.align(Alignment.TopEnd).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
            // 잠들기: 꺼져 있으면 ‘잠들기’, 켜져 있으면 작은 달과 남은 분 (밤엔 조금 더 또렷이)
            val sleepLabel = when { sleeping -> stringResource(R.string.gaze_sleepFading); sleepFrom != null -> stringResource(R.string.gaze_sleepLeft, "$sleepLeft"); else -> stringResource(R.string.gaze_sleep) }
            TokenText(sleepLabel, Tokens.TypeScale.footnote,
                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { sleepPicking = true }.padding(vertical = Tokens.Space.sp4, horizontal = Tokens.Space.sp3),
                color = if (sleepFrom != null) Color(0xFFFFD796).copy(alpha = 0.85f) else p.secondary.copy(alpha = if (isNight(now)) 0.9f else 0.7f), weight = FontWeight.Normal)
            TokenText(stringResource(if (soundOn) R.string.gaze_soundOff else R.string.gaze_soundOn), Tokens.TypeScale.footnote,
                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { soundOn = !soundOn; if (soundOn && state.sound == Sound.NONE) state.changeSound(Sound.WAVES) }.padding(Tokens.Space.sp4),
                color = p.secondary.copy(alpha = 0.7f), weight = FontWeight.Normal)
        }
        if (sleepPicking) SleepSheet(sleepMin, on = sleepFrom != null,
            onStart = { m -> sleepMin = m; state.store.sleepMinutes = m; slept = false; sleepFrom = wallNow(); sleepPicking = false; idleKick++ },
            onOff = { sleepFrom = null; sleeping = false; sleepPicking = false; idleKick++ },
            onClose = { sleepPicking = false })
        if (hint) TokenText(stringResource(R.string.gaze_exit), Tokens.TypeScale.caption1, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Tokens.Space.sp6), color = p.secondary)
        if (ask) GardenAlert(
            onDismissRequest = { ask = false },
            containerColor = Theme.gc.paper,
            text = { TokenText(stringResource(R.string.gaze_ask), Tokens.TypeScale.callout.serif()) },
            confirmButton = { io.github.graviton94.carpediem.ui.AlertButton(stringResource(R.string.gaze_back), { ask = false; onBack() }) },
            dismissButton = { io.github.graviton94.carpediem.ui.AlertButton(stringResource(R.string.gaze_stay), { ask = false }, quiet = true) },
        )
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

/** 고마움 명상을 마친 뒤: 떠오른 것을 한 줄로 고마움 책에 (오늘의 한 줄이 된다, 하루 한 줄). */
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
            modifier = Modifier.fillMaxWidth().keepAboveKeyboard().crayonBox(null, G.Radius.box, G.Stroke.chip, 1025).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
            decorationBox = { inner -> Box { if (text.isEmpty()) TokenText(stringResource(R.string.breath_thanksHint), Tokens.TypeScale.callout, color = p.secondary); inner() } },
        )
        GardenButton(stringResource(R.string.breath_thanksKeep), {
            if (text.isNotBlank()) { state.letGo(text, io.github.graviton94.carpediem.core.Feeling.THANKS); state.toast = null; state.care = null; kept = true }
        }, filled = text.isNotBlank(), seed = 1026)
    }
}

/** 잠들기: 고른 시간의 소리 크기 (그다음 0 까지 천천히) · 화면이 까맣게 되기까지. */
private const val SLEEP_HALF = 0.5f
private const val SLEEP_DIM_MS = 120_000

/**
 * 잠들기 시트: 알람 맞추듯 위아래로 굴려 1분 단위로 고름 (1 ~ 180분). 고르는 대로 아래 안내가 바뀜.
 * ‘시작’ · (켜져 있으면) ‘끄기’. 버튼은 다른 창과 같은 모양.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SleepSheet(initial: Int, on: Boolean, onStart: (Int) -> Unit, onOff: () -> Unit, onClose: () -> Unit) {
    val p = Theme.palette
    var m by remember { androidx.compose.runtime.mutableIntStateOf(initial.coerceIn(SLEEP_RANGE)) }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onClose, containerColor = Theme.gc.paper,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp6),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            TokenText(stringResource(R.string.gaze_sleep), Tokens.TypeScale.title3.serif())
            MinuteWheel(m, SLEEP_RANGE, stringResource(R.string.sleep_minutes)) { m = it }
            TokenText(stringResource(R.string.sleep_guide, "$m", "${m * 2}"), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2, Alignment.End)) {
                if (on) io.github.graviton94.carpediem.ui.AlertButton(stringResource(R.string.sleep_off), onOff, quiet = true)
                io.github.graviton94.carpediem.ui.AlertButton(stringResource(R.string.sleep_start), { onStart(m) })
            }
        }
    }
}
private val SLEEP_RANGE = 1..180

/**
 * 숫자 바퀴 (알람 시간 맞추기처럼): 세 칸이 보이고 가운데가 고른 값. 굴리면 한 칸씩 착 멈춤.
 * 화면 읽기: ‘잠들기까지 n분’, 위 · 아래로 한 칸씩 (늘리기 · 줄이기).
 */
@Composable
internal fun MinuteWheel(value: Int, range: IntRange, unit: String, onValue: (Int) -> Unit) {
    val p = Theme.palette
    val itemH = Tokens.Layout.tapTarget
    // 앞뒤에 빈 칸 하나씩: 맨 위 칸이 j 면 가운데 값은 range.first + j
    val list = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = value - range.first)
    val fling = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(list)
    val itemPx = with(androidx.compose.ui.platform.LocalDensity.current) { itemH.toPx() }
    val lensW = with(androidx.compose.ui.platform.LocalDensity.current) { (Theme.unit * 0.4f).toPx() }
    val idx by remember { androidx.compose.runtime.derivedStateOf { (list.firstVisibleItemIndex + if (list.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0).coerceIn(0, range.count() - 1) } }
    LaunchedEffect(idx) { onValue(range.first + idx) }
    val scope = rememberCoroutineScope()
    val a11y = stringResource(R.string.sleep_a11y, "${range.first + idx}")
    Box(Modifier.fillMaxWidth().height(itemH * 3).semantics(mergeDescendants = true) {
        contentDescription = a11y
        customActions = listOf(
            androidx.compose.ui.semantics.CustomAccessibilityAction("+1") { scope.launch { list.animateScrollToItem((idx + 1).coerceAtMost(range.count() - 1)) }; true },
            androidx.compose.ui.semantics.CustomAccessibilityAction("-1") { scope.launch { list.animateScrollToItem((idx - 1).coerceAtLeast(0)) }; true })
    }, contentAlignment = Alignment.Center) {
        // 가운데 칸 위아래 가는 선 (고르는 자리)
        Box(Modifier.fillMaxWidth(0.5f).height(itemH).drawBehind {
            val sw = lensW; val c = p.foreground.copy(alpha = 0.5f)
            drawLine(c, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), sw)
            drawLine(c, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), sw)
        })
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().height(itemH * 3), state = list, flingBehavior = fling) {
            item { Spacer(Modifier.height(itemH)) }
            items(range.count()) { i ->
                val on = i == idx
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    TokenText("${range.first + i}", if (on) Tokens.TypeScale.title2.serif() else Tokens.TypeScale.callout.serif(),
                        color = if (on) p.foreground else p.secondary.copy(alpha = 0.6f), weight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            item { Spacer(Modifier.height(itemH)) }
        }
        TokenText(unit, Tokens.TypeScale.callout.serif(), Modifier.align(Alignment.CenterEnd).padding(end = Theme.unit * 18f), color = p.foreground)
    }
}
