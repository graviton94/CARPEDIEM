package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.CreditPart
import io.github.graviton94.carpediem.core.CreditScene
import io.github.graviton94.carpediem.core.Credits
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.core.Sound
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.sound.Soundscape
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import java.time.LocalDateTime

/** 장면마다 비출 정원의 때: 인트로는 이른 봄 새벽, 계절은 그 계절 한낮, 아웃트로는 그해 마지막 밤. */
private fun sceneTime(year: Int, sc: CreditScene): LocalDateTime = when (sc.part) {
    CreditPart.INTRO -> LocalDateTime.of(year, 3, 1, 6, 30)
    CreditPart.OUTRO -> LocalDateTime.of(year, 12, 31, 22, 30)
    CreditPart.SEASON -> when (sc.season) {
        Season.SPRING -> LocalDateTime.of(year, 4, 15, 15, 0); Season.SUMMER -> LocalDateTime.of(year, 7, 20, 15, 0)
        Season.AUTUMN -> LocalDateTime.of(year, 10, 20, 15, 0); else -> LocalDateTime.of(year, 12, 20, 14, 0)
    }
}

private val CREDIT_INK = Color(0xFFFBEFD9)

/**
 * 한 해의 엔딩 크레딧 (08): 인트로 · 네 계절 · 아웃트로 (60 ~ 80초). 계절 정원이 천천히 바뀌고, 그 위로 그때의 한 줄 (과 사진) 이 올라간다.
 * 누르면 멈춤 · 다시, ‘건너뛰기’는 아웃트로로, 뒤로 가기는 나가기. 끝나면 ‘정원으로’.
 */
@Composable
fun CreditsScreen(state: AppState, profile: LifeProfile, year: Int, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val plan = remember(year) { Credits.plan(state.lines, year) }
    val total = Credits.total(plan)
    var elapsed by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    val done by remember { derivedStateOf { elapsed >= total } }
    KeepScreenOn(!done)
    BackHandler(onBack = onDone)
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (elapsed < total) { val t = withFrameMillis { it }; if (t - last < 33) continue; elapsed = (elapsed + t - last).coerceAtMost(total); last = t }
    }
    val scene by remember(plan) { derivedStateOf { Credits.at(plan, elapsed) ?: plan.last() } }
    // 계절의 소리 (소리를 켜 둔 사람만): 장면의 계절로
    val season = scene.season ?: if (scene.part == CreditPart.INTRO) Season.SPRING else Season.WINTER
    if (state.sound != Sound.NONE) {
        val player = remember(season) { Soundscape.Player(Sound.SEASON, season) }
        DisposableEffect(player, paused) { if (!paused) player.start(); onDispose { player.stop() } }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(done) { detectTapGestures { if (!done) paused = !paused } }) {
        Crossfade(sceneTime(year, scene), animationSpec = tween(1600), label = "creditGarden") { t ->
            GardenHome(state, profile, t, onSettings = {}, onCollection = {}, onSupport = {}, onStone = {}, onAddPerson = {}, bare = true)
        }
        // 글이 읽히게: 위는 진하게, 가운데는 옅게
        val shade = if (scene.part == CreditPart.SEASON) 0.55f else 0.78f
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = shade + 0.15f), Color.Black.copy(alpha = shade), Color.Black.copy(alpha = shade * 0.5f), Color.Black.copy(alpha = shade)))))
        val local = elapsed - scene.startMs
        // 장면이 바뀔 때 글이 옅어졌다 나타남 (끝난 뒤 아웃트로는 그대로 남김)
        val fade = if (done) 1f else ((minOf(local, scene.startMs + scene.lengthMs - elapsed)) / 900f).coerceIn(0f, 1f)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade }) {
            when (scene.part) {
                CreditPart.INTRO -> Intro(year)
                CreditPart.SEASON -> SeasonRoll(state, scene, local)
                CreditPart.OUTRO -> Outro(state, year)
            }
        }
        // 위 끝: 건너뛰기 · 나가기 (아주 작게)
        if (!done) TokenText(stringResource(R.string.credits_skip), Tokens.TypeScale.footnote,
            Modifier.align(Alignment.TopEnd).statusBarsPadding().heightIn(min = Tokens.Layout.tapTarget)
                .clickable { elapsed = plan.last().startMs; paused = false }.padding(Tokens.Space.sp4), color = CREDIT_INK.copy(alpha = 0.55f))
        if (paused && !done) TokenText(stringResource(R.string.credits_paused), Tokens.TypeScale.footnote, Modifier.align(Alignment.Center), color = CREDIT_INK.copy(alpha = 0.8f))
        if (done) Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp8)) {
            GardenButton(stringResource(R.string.breath_home), onDone, filled = true, seed = 1700)
        }
    }
}

@Composable
private fun Intro(year: Int) {
    Column(Modifier.fillMaxSize().padding(horizontal = Theme.deviceClass.pageMargin), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        TokenText(stringResource(R.string.credits_garden), Tokens.TypeScale.footnote.serif(), color = CREDIT_INK.copy(alpha = 0.7f))
        Spacer(Modifier.height(Tokens.Space.sp4))
        TokenText("$year", Tokens.TypeScale.largeTitle.serif(), color = CREDIT_INK)
        Spacer(Modifier.height(Tokens.Space.sp4))
        TokenText(stringResource(R.string.credits_intro), Tokens.TypeScale.callout.serif(), color = CREDIT_INK.copy(alpha = 0.85f), align = TextAlign.Center)
    }
}

/** 계절의 한 줄들이 아래에서 위로 천천히 (장면 길이 동안 한 번). */
@Composable
private fun SeasonRoll(state: AppState, sc: CreditScene, local: Long) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val season = sc.season ?: return
    val months = stringResource(when (season) { Season.SPRING -> R.string.credits_spring; Season.SUMMER -> R.string.credits_summer; Season.AUTUMN -> R.string.credits_autumn; else -> R.string.credits_winter })
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val h = with(density) { maxHeight.toPx() }
        var listH by remember(sc) { mutableStateOf(0f) }
        Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp10), horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText(Labels.season(ctx, season), Tokens.TypeScale.title3.serif(), color = CREDIT_INK)
            TokenText(months, Tokens.TypeScale.caption1, color = CREDIT_INK.copy(alpha = 0.6f))
        }
        val p = (local.toFloat() / sc.lengthMs).coerceIn(0f, 1f)
        // 아래 끝에서 들어와 위 1/4 즈음까지 (한 줄이 적으면 짧게 지나감)
        val y = h * 0.92f - (h * 0.7f + listH) * p
        Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).graphicsLayer { translationY = y }.onSizeChanged { listH = it.height.toFloat() },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp5)) {
            if (sc.lines.isEmpty()) TokenText(stringResource(R.string.credits_quiet), Tokens.TypeScale.callout.serif(), color = CREDIT_INK.copy(alpha = 0.8f), align = TextAlign.Center)
            sc.lines.forEach { l ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TokenText(RecordText.day(ctx, l.date), Tokens.TypeScale.caption1, color = CREDIT_INK.copy(alpha = 0.6f))
                    TokenText(l.text, Tokens.TypeScale.callout.serif(), color = CREDIT_INK, align = TextAlign.Center)
                    WeatheredPhoto(state, l.date, (state.fixedNow ?: LocalDateTime.now()).toLocalDate(), Theme.unit * 110f, modifier = Modifier.padding(top = Tokens.Space.sp2))
                }
            }
        }
    }
}

@Composable
private fun Outro(state: AppState, year: Int) {
    val ctx = LocalContext.current
    val inYear = remember(year) { state.lines.filter { it.date.year == year } }
    val top = remember(year) { inYear.mapNotNull { it.feeling }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key }
    val thanks = inYear.count { it.feeling == Feeling.THANKS }
    @Composable fun role(label: Int, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText(stringResource(label), Tokens.TypeScale.caption2, color = CREDIT_INK.copy(alpha = 0.55f))
            TokenText(value, Tokens.TypeScale.callout.serif(), color = CREDIT_INK, align = TextAlign.Center)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = Theme.deviceClass.pageMargin), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4, Alignment.CenterVertically)) {
        role(R.string.credits_starring, stringResource(R.string.you))
        role(R.string.credits_stone, stringResource(R.string.credits_haru))
        if (state.people.isNotEmpty()) role(R.string.credits_people, state.people.joinToString(" · ") { it.name })
        role(R.string.credits_kept, stringResource(R.string.ring_count, "${inYear.size}", "$thanks"))
        top?.let { role(R.string.credits_mood, Labels.feeling(ctx, it)) }
        Spacer(Modifier.height(Tokens.Space.sp4))
        TokenText(stringResource(R.string.credits_next, "${year + 1}"), Tokens.TypeScale.headline.serif(), color = CREDIT_INK)
    }
}
