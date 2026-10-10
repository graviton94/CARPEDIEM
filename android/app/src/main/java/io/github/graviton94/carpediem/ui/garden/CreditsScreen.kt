package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.CreditItem
import io.github.graviton94.carpediem.core.CreditKind
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
import java.time.LocalDateTime
import kotlin.math.sin

private val CREDIT_INK = Color(0xFFFBEFD9)
private val CREDIT_WARM = Color(0xFFF2C27A)
private val CREDIT_BG = Color(0xFF16140F)

/**
 * 한 해의 엔딩 크레딧 (08): 인트로 (숫자가 차오름) · 네 계절 (계절 이름과 그 계절의 일이 한 장씩 빠르게) · 아웃트로 (올라가는 엔딩 크레딧), 60 ~ 80초.
 * 내가 남긴 한 줄만이 아니라 가족의 생일 · 함께한 날 · 특별한 날 · 만난 순간 · 손님 · 핀 씨앗 · 열린 항아리까지.
 * 누르면 멈춤 · 다시, ‘건너뛰기’는 아웃트로로, 뒤로 가기는 나가기. 끝나면 ‘정원으로’.
 */
@Composable
fun CreditsScreen(state: AppState, profile: LifeProfile, year: Int, onDone: () -> Unit) {
    val events = remember(year) {
        Credits.events(year, state.lines, profile.birthDate, state.people, state.specialDays, state.chancesMet.toList(), state.seeds, state.capsules, state.store.startDate)
    }
    val plan = remember(events) { Credits.plan(events, year) }
    val total = Credits.total(plan)
    var elapsed by remember { mutableLongStateOf(0L) }
    var paused by remember { mutableStateOf(false) }
    val done by remember { derivedStateOf { elapsed >= total } }
    KeepScreenOn(!done)
    BackHandler(onBack = onDone)
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (elapsed < total) { val t = withFrameMillis { it }; if (t - last < 16) continue; elapsed = (elapsed + t - last).coerceAtMost(total); last = t }
    }
    val scene by remember(plan) { derivedStateOf { Credits.at(plan, elapsed) ?: plan.last() } }
    val season = hemi(scene.season ?: Season.WINTER, state.profile?.countryCode)   // 인트로는 1월 (겨울) 에서 시작해 12월 겨울로 끝남 (남반구는 여름)
    // 앱을 떠나면 잠시 멈춤 (소리도 함께 멈추고, 돌아오면 눌러서 이어 봄)
    val creditsOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(creditsOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP && !done) paused = true }
        creditsOwner.lifecycle.addObserver(obs); onDispose { creditsOwner.lifecycle.removeObserver(obs) }
    }
    if (state.sound != Sound.NONE) {
        val player = remember(season) { Soundscape.Player(Sound.SEASON, season) }
        DisposableEffect(player, paused) { if (!paused) player.start(); onDispose { player.stop() } }
    }
    Box(Modifier.fillMaxSize().background(CREDIT_BG).pointerInput(done) { detectTapGestures { if (!done) paused = !paused } }) {
        // 배경: 그 장 계절의 정원을 어둑하게 (해 · 달 · 정원 글자 없이). 장이 바뀌면 천천히 바뀜
        Crossfade(season, animationSpec = tween(1600), label = "creditGround") { se -> CreditGround(se) }
        val local = elapsed - scene.startMs
        val fade = if (done) 1f else ((minOf(local, scene.startMs + scene.lengthMs - elapsed)) / 600f).coerceIn(0f, 1f)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade }) {
            when (scene.part) {
                CreditPart.INTRO -> Intro(state, year, local, scene.lengthMs)
                CreditPart.SEASON -> SeasonScene(state, year, scene, elapsed)
                CreditPart.OUTRO -> Outro(state, profile, year, if (done) 1f else (local.toFloat() / scene.lengthMs).coerceIn(0f, 1f))
            }
        }
        // 맨 위 가는 줄: 지나온 만큼 (영화처럼)
        Box(Modifier.align(Alignment.TopCenter).statusBarsPadding().fillMaxWidth().padding(horizontal = Tokens.Space.sp6).height(Theme.unit * 0.8f).background(CREDIT_INK.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth((elapsed.toFloat() / total).coerceIn(0f, 1f)).height(Theme.unit * 0.8f).background(CREDIT_WARM.copy(alpha = 0.7f)))
        }
        if (!done) TokenText(stringResource(R.string.credits_skip), Tokens.TypeScale.footnote,
            Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = Tokens.Space.sp2).heightIn(min = Tokens.Layout.tapTarget)
                .clickable { elapsed = plan.last().startMs; paused = false }.padding(Tokens.Space.sp4), color = CREDIT_INK.copy(alpha = 0.55f))
        if (paused && !done) TokenText(stringResource(R.string.credits_paused), Tokens.TypeScale.footnote, Modifier.align(Alignment.Center), color = CREDIT_INK.copy(alpha = 0.8f))
        if (done) Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp8)) {
            GardenButton(stringResource(R.string.breath_home), onDone, filled = true, seed = 1700)
        }
    }
}

/**
 * 배경: 그 장 계절의 정원 (하늘 · 땅) 을 어둑하게. 1–2월 겨울 → 봄 → 여름 → 가을 → 12월 겨울로 크레딧과 함께 바뀜.
 * 글자가 읽히게 위아래를 어둡게 덮는다 (해 · 달 · 돌 · 글자는 없이).
 */
@Composable
private fun CreditGround(season: Season) {
    val ctx = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Image(GardenArt.sky(ctx, season), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        val h = maxHeight * 0.34f
        Image(GardenArt.strip(ctx, season), null, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(h), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CREDIT_BG.copy(alpha = 0.78f), CREDIT_BG.copy(alpha = 0.6f), CREDIT_BG.copy(alpha = 0.5f), CREDIT_BG.copy(alpha = 0.7f)))))
    }
}

/** 인트로: 작은 제목 · 해 숫자 · 한마디, 조금 뒤 한 해의 숫자 한 줄. */
@Composable
private fun Intro(state: AppState, year: Int, local: Long, length: Long) {
    val lines = remember(year) { state.lines.count { it.date.year == year } }
    val breaths = remember(year) { state.breaths.count { it.first.year == year } }
    val thanks = remember(year) { state.lines.count { it.date.year == year && it.feeling == Feeling.THANKS } }
    val sum = ((local - 2200f) / 1400f).coerceIn(0f, 1f)
    Column(Modifier.fillMaxSize().padding(horizontal = Theme.deviceClass.pageMargin), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        TokenText(stringResource(R.string.credits_garden), Tokens.TypeScale.footnote.serif(), color = CREDIT_INK.copy(alpha = 0.6f))
        Spacer(Modifier.height(Tokens.Space.sp3))
        TokenText("$year", Tokens.TypeScale.largeTitle.serif(), color = CREDIT_INK)
        Spacer(Modifier.height(Tokens.Space.sp4))
        TokenText(stringResource(R.string.credits_intro), Tokens.TypeScale.callout.serif(), color = CREDIT_INK.copy(alpha = 0.8f), align = TextAlign.Center)
        Spacer(Modifier.height(Tokens.Space.sp8))
        val counts = listOfNotNull(
            lines.takeIf { it > 0 }?.let { stringResource(R.string.credits_sLines, Labels.number(it)) },
            thanks.takeIf { it > 0 }?.let { stringResource(R.string.credits_sThanks, Labels.number(it)) },
            breaths.takeIf { it > 0 }?.let { stringResource(R.string.credits_sBreaths, Labels.number(it)) },
        )
        if (counts.isNotEmpty()) TokenText(counts.joinToString("  ·  "), Tokens.TypeScale.footnote, Modifier.graphicsLayer { alpha = sum }, color = CREDIT_WARM.copy(alpha = 0.85f))
    }
}

/** 장의 달 (‘1월 – 2월’ · ‘12월’), 폰 언어로. */
private fun monthsLabel(months: IntRange): String {
    val loc = java.util.Locale.getDefault()
    fun m(i: Int) = java.time.Month.of(i).getDisplayName(java.time.format.TextStyle.FULL, loc)
    return if (months.first == months.last) m(months.first) else m(months.first) + " – " + m(months.last)
}

/**
 * 한 장 (계절): 처음 2초는 가운데에 계절 이름 · 달, 그다음 이름은 위로 작게 물러나고 그 장의 일이 한 줄씩 가운데에 (그림 · 상자 없이 글자만).
 * 한 줄을 남긴 날이면 그날의 사진이 아래에 작게.
 */
/** 장은 달 순서 (1–2월 · 3–5월 …) 그대로, 남반구 나라면 그 달의 실제 계절로 (1월 = 여름). */
private fun hemi(s: Season, country: String?): Season =
    if (country?.uppercase() in io.github.graviton94.carpediem.core.GardenDecor.SOUTH) Season.entries[(s.ordinal + 2) % 4] else s

@Composable
private fun SeasonScene(state: AppState, year: Int, sc: CreditScene, elapsed: Long) {
    val ctx = LocalContext.current
    val season = hemi(sc.season ?: return, state.profile?.countryCode)
    val now = (state.fixedNow ?: LocalDateTime.now()).toLocalDate()
    val local = elapsed - sc.startMs
    val settle = ((local - Credits.HEADER_MS + 500f) / 700f).coerceIn(0f, 1f)   // 이름이 위로 물러나는 정도
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin)) {
        val headY = maxHeight * (0.40f - 0.28f * settle)
        Column(Modifier.fillMaxWidth().offset(y = headY).graphicsLayer { val k = 1f - 0.22f * settle; scaleX = k; scaleY = k },
            horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText(Labels.season(ctx, season), Tokens.TypeScale.title2.serif(), color = CREDIT_INK)
            Spacer(Modifier.height(Tokens.Space.sp1))
            TokenText(monthsLabel(sc.months), Tokens.TypeScale.footnote, color = CREDIT_INK.copy(alpha = 0.55f))
        }
        val cur = sc.itemAt(elapsed)
        if (sc.items.isEmpty() && settle >= 1f) TokenText(stringResource(R.string.credits_quiet), Tokens.TypeScale.callout.serif(),
            Modifier.fillMaxWidth().offset(y = maxHeight * 0.42f), color = CREDIT_INK.copy(alpha = 0.7f), align = TextAlign.Center)
        cur?.let { (item, f) ->
            // 앞 0.2 동안 천천히 나타나고, 끝 0.18 동안 옅어짐
            val a = minOf(f / 0.2f, (1f - f) / 0.18f).coerceIn(0f, 1f)
            androidx.compose.runtime.key(item) {
                CreditLine(state, item, now, Modifier.fillMaxWidth().offset(y = maxHeight * 0.34f).graphicsLayer { alpha = a; translationY = (1f - a) * 18f })
            }
        }
    }
}

/** 크레딧 한 줄: 날짜 (작게, 따뜻한 색) · 그날의 일 (명조). */
@Composable
private fun CreditLine(state: AppState, item: CreditItem, today: java.time.LocalDate, modifier: Modifier) {
    val ctx = LocalContext.current
    val text = when (item.kind) {
        CreditKind.LINE -> item.line?.text.orEmpty()
        CreditKind.MY_BIRTHDAY -> stringResource(R.string.credits_myBirthday, item.b)
        CreditKind.BIRTHDAY -> stringResource(R.string.credits_birthday, item.a)
        CreditKind.TOGETHER_DAYS -> stringResource(R.string.credits_togetherDays, item.a, Labels.number(item.b.toIntOrNull() ?: 0))
        CreditKind.TOGETHER_YEARS -> stringResource(R.string.credits_togetherYears, item.a, item.b)
        CreditKind.SPECIAL -> stringResource(R.string.credits_special, item.a, item.b)
        CreditKind.MOMENT -> stringResource(R.string.credits_moment, ctx.resources.getIdentifier("chance_${item.a}", "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) } ?: item.a)
        CreditKind.SEED -> stringResource(R.string.credits_seed, item.a)
        CreditKind.CAPSULE -> stringResource(R.string.credits_capsule, item.a)
        CreditKind.FIRST -> stringResource(R.string.credits_first)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(RecordText.day(ctx, item.date), Tokens.TypeScale.footnote, color = CREDIT_WARM.copy(alpha = 0.85f))
        TokenText(text, Tokens.TypeScale.title3.serif(), Modifier.fillMaxWidth(), color = CREDIT_INK, align = TextAlign.Center, maxLines = 5)
        if (item.kind == CreditKind.LINE) WeatheredPhoto(state, item.date, today, Theme.unit * 110f, modifier = Modifier.padding(top = Tokens.Space.sp2))
    }
}

/** 아웃트로: 진짜 엔딩 크레딧처럼 아래에서 위로 올라가다 마지막 인사가 가운데에서 멈춤. p = 0 … 1. */
@Composable
private fun Outro(state: AppState, profile: LifeProfile, year: Int, p: Float) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val inYear = remember(year) { state.lines.filter { it.date.year == year } }
    val top = remember(year) { inYear.mapNotNull { it.feeling }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key }
    val thanks = inYear.count { it.feeling == Feeling.THANKS }
    val breaths = remember(year) { state.breaths.count { it.first.year == year } }
    val guests = remember(year) {
        state.chancesMet.mapNotNull { r -> r.split(':', limit = 2).takeIf { it.size == 2 && it[1].startsWith("$year") }?.get(0) }
            .mapNotNull { k -> ctx.resources.getIdentifier("chance_$k", "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) } }.distinct()   // 이름 없는 것은 빼고
    }
    val bloomed = state.seeds.count { it.date.year == year && it.state == io.github.graviton94.carpediem.core.SeedState.BLOOMED }
    val end = java.time.LocalDate.of(year, 12, 31)
    @Composable fun role(label: Int, vararg values: String) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText(stringResource(label), Tokens.TypeScale.caption2, color = CREDIT_INK.copy(alpha = 0.55f))
            values.forEach { v -> TokenText(v, Tokens.TypeScale.callout.serif(), color = CREDIT_INK, align = TextAlign.Center) }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = with(density) { maxHeight.toPx() }
        var listH by remember { mutableStateOf(0f) }
        // 처음엔 화면 아래, 끝에는 마지막 인사가 화면 가운데쯤
        val e = 1f - (1f - p) * (1f - p)
        val y = h + ((h * 0.62f - listH) - h) * e
        Column(Modifier.fillMaxWidth().padding(horizontal = Theme.deviceClass.pageMargin).graphicsLayer { translationY = y }.onSizeChanged { listH = it.height.toFloat() },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp5)) {
            role(R.string.credits_starring, stringResource(R.string.you))
            role(R.string.credits_stone, stringResource(R.string.credits_haru))
            if (state.people.isNotEmpty()) role(R.string.credits_people, *state.people.map { pp ->
                io.github.graviton94.carpediem.core.Memories.from(pp)?.let { t -> val d = java.time.temporal.ChronoUnit.DAYS.between(t, end); if (d > 0) ctx.getString(R.string.credits_personDays, pp.name, Labels.number(d.toInt())) else pp.name } ?: pp.name
            }.toTypedArray())
            if (guests.isNotEmpty()) role(R.string.credits_guests, guests.joinToString(" · "))
            role(R.string.credits_kept, listOfNotNull(
                inYear.size.takeIf { it > 0 }?.let { stringResource(R.string.credits_sLines, Labels.number(it)) },
                thanks.takeIf { it > 0 }?.let { stringResource(R.string.credits_sThanks, Labels.number(it)) },
                breaths.takeIf { it > 0 }?.let { stringResource(R.string.credits_sBreaths, Labels.number(it)) },
            ).joinToString(" · ").ifEmpty { stringResource(R.string.credits_quiet) })
            if (bloomed > 0) role(R.string.seed_garden, stringResource(R.string.credits_bloomed, "$bloomed"))
            top?.let { role(R.string.credits_mood, Labels.feeling(ctx, it)) }
            role(R.string.credits_made, stringResource(R.string.credits_madeBy))
            Spacer(Modifier.height(Tokens.Space.sp6))
            TokenText(stringResource(R.string.credits_next, "${year + 1}"), Tokens.TypeScale.title3.serif(), color = CREDIT_WARM)
        }
    }
}
