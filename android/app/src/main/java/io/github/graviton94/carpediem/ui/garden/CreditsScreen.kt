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
private val CREDIT_WARM = Color(0xFFF5B45C)

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
    val season = scene.season ?: if (scene.part == CreditPart.INTRO) Season.SPRING else Season.WINTER
    if (state.sound != Sound.NONE) {
        val player = remember(season) { Soundscape.Player(Sound.SEASON, season) }
        DisposableEffect(player, paused) { if (!paused) player.start(); onDispose { player.stop() } }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(done) { detectTapGestures { if (!done) paused = !paused } }) {
        Crossfade(sceneTime(year, scene), animationSpec = tween(1200), label = "creditGarden") { t ->
            GardenHome(state, profile, t, onSettings = {}, onCollection = {}, onSupport = {}, onStone = {}, onAddPerson = {}, bare = true)
        }
        val shade = if (scene.part == CreditPart.SEASON) 0.5f else 0.75f
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = shade + 0.2f), Color.Black.copy(alpha = shade), Color.Black.copy(alpha = shade * 0.6f), Color.Black.copy(alpha = shade + 0.1f)))))
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

/** 인트로: 해 이름과 한 해의 숫자 셋이 0부터 차오름. */
@Composable
private fun Intro(state: AppState, year: Int, local: Long, length: Long) {
    val lines = remember(year) { state.lines.count { it.date.year == year } }
    val breaths = remember(year) { state.breaths.count { it.first.year == year } }
    val thanks = remember(year) { state.lines.count { it.date.year == year && it.feeling == Feeling.THANKS } }
    val p = ((local - 1500f) / (length - 3000f)).coerceIn(0f, 1f)
    val e = 1f - (1f - p) * (1f - p)
    Column(Modifier.fillMaxSize().padding(horizontal = Theme.deviceClass.pageMargin), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        TokenText(stringResource(R.string.credits_garden), Tokens.TypeScale.footnote.serif(), color = CREDIT_INK.copy(alpha = 0.7f))
        Spacer(Modifier.height(Tokens.Space.sp3))
        TokenText("$year", Tokens.TypeScale.largeTitle.serif(), color = CREDIT_INK)
        Spacer(Modifier.height(Tokens.Space.sp3))
        TokenText(stringResource(R.string.credits_intro), Tokens.TypeScale.callout.serif(), color = CREDIT_INK.copy(alpha = 0.85f), align = TextAlign.Center)
        Spacer(Modifier.height(Tokens.Space.sp8))
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp6)) {
            Counter((lines * e).toInt(), stringResource(R.string.credits_countLines))
            Counter((thanks * e).toInt(), stringResource(R.string.credits_countThanks))
            Counter((breaths * e).toInt(), stringResource(R.string.credits_countBreaths))
        }
    }
}

@Composable
private fun Counter(n: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TokenText(Labels.number(n), Tokens.TypeScale.title2.serif(), color = CREDIT_WARM)
        TokenText(label, Tokens.TypeScale.caption1, color = CREDIT_INK.copy(alpha = 0.7f))
    }
}

/** 계절: 위에 계절 이름 · 달 · 그 계절의 숫자, 가운데 그 계절의 일이 한 장씩 (떠오르며 나타났다 옅어짐), 아래 작은 점들. */
@Composable
private fun SeasonScene(state: AppState, year: Int, sc: CreditScene, elapsed: Long) {
    val ctx = LocalContext.current
    val season = sc.season ?: return
    val months = stringResource(when (season) { Season.SPRING -> R.string.credits_spring; Season.SUMMER -> R.string.credits_summer; Season.AUTUMN -> R.string.credits_autumn; else -> R.string.credits_winter })
    val inSeason = remember(sc) { state.lines.filter { it.date.year == year && io.github.graviton94.carpediem.core.Memories.seasonOf(it.date) == season } }
    val breaths = remember(sc) { state.breaths.count { it.first.year == year && io.github.graviton94.carpediem.core.Memories.seasonOf(it.first) == season } }
    val now = (state.fixedNow ?: LocalDateTime.now()).toLocalDate()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = Tokens.Space.sp10).padding(horizontal = Theme.deviceClass.pageMargin), horizontalAlignment = Alignment.CenterHorizontally) {
        TokenText(Labels.season(ctx, season), Tokens.TypeScale.title2.serif(), color = CREDIT_INK)
        TokenText(months, Tokens.TypeScale.caption1, color = CREDIT_INK.copy(alpha = 0.6f))
        Spacer(Modifier.height(Tokens.Space.sp1))
        // 그 계절의 숫자 (0 인 것은 빼고)
        val counts = listOfNotNull(
            inSeason.size.takeIf { it > 0 }?.let { stringResource(R.string.credits_sLines, "$it") },
            inSeason.count { it.feeling == Feeling.THANKS }.takeIf { it > 0 }?.let { stringResource(R.string.credits_sThanks, "$it") },
            breaths.takeIf { it > 0 }?.let { stringResource(R.string.credits_sBreaths, "$it") },
        )
        if (counts.isNotEmpty()) TokenText(counts.joinToString(" · "), Tokens.TypeScale.footnote, color = CREDIT_WARM.copy(alpha = 0.9f))
        Spacer(Modifier.weight(0.6f))
        val cur = sc.itemAt(elapsed)
        Box(Modifier.fillMaxWidth().heightIn(min = Theme.unit * 200f), contentAlignment = Alignment.Center) {
            if (sc.items.isEmpty()) TokenText(stringResource(R.string.credits_quiet), Tokens.TypeScale.callout.serif(), color = CREDIT_INK.copy(alpha = 0.8f), align = TextAlign.Center)
            cur?.let { (item, f) ->
                // 0 → 0.18 떠오르며 나타남, 0.82 → 1 옅어짐
                val a = minOf(f / 0.18f, (1f - f) / 0.18f).coerceIn(0f, 1f)
                val rise = (1f - (f / 0.18f).coerceAtMost(1f))
                androidx.compose.runtime.key(item) {
                    ItemCard(state, item, now, Modifier.graphicsLayer { alpha = a; translationY = rise * 40f; scaleX = 0.96f + 0.04f * a; scaleY = 0.96f + 0.04f * a })
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // 이 계절의 몇 장 가운데 몇 번째
        Row(Modifier.padding(bottom = Tokens.Space.sp10), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            sc.items.forEach { it2 ->
                val on = cur?.first == it2
                Box(Modifier.size(Theme.unit * (if (on) 6f else 2.4f), Theme.unit * 2.4f).background(if (on) CREDIT_WARM else CREDIT_INK.copy(alpha = 0.35f), RoundedCornerShape(Theme.unit * 2f)))
            }
        }
    }
}

/** 크레딧 한 장: 작은 그림 · 날짜 · 그날의 일. */
@Composable
private fun ItemCard(state: AppState, item: CreditItem, today: java.time.LocalDate, modifier: Modifier) {
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
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        CreditIcon(item.kind, Theme.unit * 64f)
        TokenText(RecordText.day(ctx, item.date), Tokens.TypeScale.footnote, color = CREDIT_INK.copy(alpha = 0.7f))
        TokenText(text, Tokens.TypeScale.title2.serif(), Modifier.fillMaxWidth(),
            color = CREDIT_INK, align = TextAlign.Center, maxLines = 5)
        if (item.kind == CreditKind.LINE) WeatheredPhoto(state, item.date, today, Theme.unit * 120f, modifier = Modifier.padding(top = Tokens.Space.sp2))
    }
}

/** 장마다의 작은 그림 (따뜻한 빛 동그라미 위). */
@Composable
private fun CreditIcon(kind: CreditKind, size: Dp) {
    val ctx = LocalContext.current
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) { drawCircle(Brush.radialGradient(listOf(CREDIT_WARM.copy(alpha = 0.35f), Color.Transparent)), this.size.minDimension / 2) }
        when (kind) {
            CreditKind.LINE -> Image(GardenArt.obj(ctx, "feather"), null, Modifier.size(size * 0.6f))
            CreditKind.MY_BIRTHDAY, CreditKind.BIRTHDAY -> Canvas(Modifier.size(size * 0.62f)) { birthdayCake() }
            CreditKind.CAPSULE -> Jar(size * 0.66f, open = true)
            CreditKind.TOGETHER_DAYS, CreditKind.TOGETHER_YEARS -> Canvas(Modifier.size(size * 0.7f)) {
                val k = this.size.width / 10f
                drawOval(Color(0xFF8C8A74), Offset(0.8f * k, 4.4f * k), Size(4.8f * k, 3.6f * k))
                drawOval(Color(0xFF7A8F6A), Offset(4.4f * k, 4.0f * k), Size(4.8f * k, 4f * k))
                drawCircle(Color(0xFFF2B35A), 0.9f * k, Offset(5f * k, 2.4f * k))
            }
            CreditKind.MOMENT -> Image(GardenArt.obj(ctx, "dandelion"), null, Modifier.size(size * 0.6f))
            CreditKind.SPECIAL, CreditKind.SEED -> Canvas(Modifier.size(size * 0.6f)) {
                val r = this.size.width / 2
                for (i in 0 until 5) { val a = i * 1.2566f; drawCircle(Color(0xFFF2B35A), r * 0.4f, Offset(r + r * 0.5f * kotlin.math.cos(a), r + r * 0.5f * sin(a))) }
                drawCircle(Color(0xFFB5651D), r * 0.28f, Offset(r, r))
            }
            CreditKind.FIRST -> Image(GardenArt.obj(ctx, "moss"), null, Modifier.size(size * 0.6f))
        }
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
            .map { k -> ctx.resources.getIdentifier("chance_$k", "string", ctx.packageName).takeIf { it != 0 }?.let { ctx.getString(it) } ?: k }.distinct()
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
