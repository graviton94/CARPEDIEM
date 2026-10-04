package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 앱 첫 화면 (타이틀): 아이콘으로 켤 때마다 · 30분 넘게 떠났다 돌아올 때.
 * 정원과 같은 하늘에서 시작해, 빈 땅이 아래에서 천천히 올라와 정원의 땅 자리에 멈춘다 (돌 · 꾸밈 · 글자 없는 빈 정원).
 * 그다음 ‘하루의 정원’ · 오늘 · 인사가 천천히 떠오르고, 누르면 글자가 먼저 옅어진 뒤 화면 전체가 천천히 옅어져
 * 아래에 그려 둔 정원 (같은 하늘 · 같은 땅) 에 돌들과 글자가 생겨나는 것처럼 이어진다.
 * 올라오는 중에 누르면 끝 장면으로, 뒤로 가기는 바로 정원으로, 움직임을 끈 기기는 끝 장면부터.
 */
@Composable
fun TitleScreen(state: AppState, now: LocalDateTime, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val still = remember { reducedMotion(ctx) }
    val season = io.github.graviton94.carpediem.core.GardenDecor.realSeason(now.toLocalDate(), state.profile?.countryCode ?: "KR")
    val night = SkyTime.isDark(now)
    val sky = remember { Animatable(if (still) 1f else 0f) }       // 하늘이 밝아짐
    val rise = remember { Animatable(if (still) 1f else 0f) }      // 0 = 땅이 화면 아래, 1 = 정원의 땅 자리
    val words = remember { Animatable(if (still) 1f else 0f) }     // 이름 · 오늘 · 인사
    val tap = remember { Animatable(if (still) 1f else 0f) }       // 눌러서 정원으로
    val fade = remember { Animatable(1f) }                          // 첫 화면 전체 (0 이면 정원)
    var entering by remember { mutableStateOf(false) }
    val breathe = rememberInfiniteTransition(label = "titleBreath")
    val b by breathe.animateFloat(0f, 1f, infiniteRepeatable(tween(TITLE_BREATH_MS, easing = LinearEasing), RepeatMode.Restart), label = "b")
    LaunchedEffect(Unit) {
        if (!still) {
            sky.animateTo(1f, tween(TITLE_SKY_MS, easing = LinearEasing))
            rise.animateTo(1f, tween(TITLE_RISE_MS, easing = CALM))
        }
        words.animateTo(1f, tween(TITLE_WORDS_MS, easing = LinearEasing))
        delay(TITLE_TAP_DELAY_MS)
        tap.animateTo(1f, tween(TITLE_WORDS_MS, easing = LinearEasing))
    }
    fun enter() {
        if (entering) return
        // 아직 오르는 중이면 먼저 끝 장면으로
        if (sky.value < 1f || rise.value < 1f || words.value < 1f) { scope.launch { sky.snapTo(1f); rise.snapTo(1f); words.snapTo(1f); tap.snapTo(1f) }; return }
        entering = true
        if (still) { onDone(); return }
        scope.launch {
            launch { tap.animateTo(0f, tween(TITLE_WORDS_OUT_MS)) }
            words.animateTo(0f, tween(TITLE_WORDS_OUT_MS, easing = LinearEasing))
            fade.animateTo(0f, tween(TITLE_FADE_MS, easing = LinearEasing))
            onDone()
        }
    }
    BackHandler { onDone() }
    val enterLabel = stringResource(R.string.title_enter)
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().paperBackground().semantics { contentDescription = enterLabel }
        .pointerInput(Unit) { detectTapGestures { enter() } }.graphicsLayer { alpha = fade.value }) {
        val u = Theme.unit
        val screenH = maxHeight
        // 정원과 같은 자리 (정원이 적어 둔 땅 높이, 아직 없으면 정원의 기본 비율)
        val gy = state.gardenGround ?: (screenH * G.Layout.groundRatio)
        val groundY = gy + (screenH - gy + u * G.Layout.stripLineY) * (1f - rise.value)
        // 하늘: 정원과 같은 그림 · 같은 시간의 빛. 처음엔 종이 바탕에서 천천히 밝아짐
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = sky.value }) {
            Image(GardenArt.sky(ctx, season), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillWidth, alignment = Alignment.TopCenter)
            // 띠 아래로 종이 바탕이 비치지 않게 화면 끝까지 (정원에서는 그 자리를 버튼 · 탭이 덮음)
            val stripTop = groundY - u * G.Layout.stripLineY
            Image(GardenArt.strip(ctx, season), null, Modifier.offset(y = stripTop).fillMaxWidth().height(maxOf(u * G.Layout.stripHeight, screenH - stripTop)), contentScale = ContentScale.FillBounds)
            SkyTimeLayer(now, groundY, screenH * 0.14f, maxOf(screenH * 0.2f, groundY - u * 40f), Modifier.fillMaxSize())
        }
        // 위: 이름 · 오늘 · 인사 (땅이 자리 잡은 뒤 천천히)
        val ink = if (night) Color(0xFFEEEBDD) else Theme.gc.ink
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = screenH * 0.16f).padding(horizontal = Theme.deviceClass.pageMargin)
            .graphicsLayer { alpha = words.value; translationY = (1f - words.value) * 14f },
            horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText("Carpe Diem", Tokens.TypeScale.footnote.serif(), color = ink.copy(alpha = 0.6f))
            Spacer(Modifier.height(Tokens.Space.sp2))
            TokenText(stringResource(R.string.title_name), Tokens.TypeScale.largeTitle.serif(), color = ink, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp4))
            TokenText(now.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), Tokens.TypeScale.footnote, color = ink.copy(alpha = 0.65f))
            Spacer(Modifier.height(Tokens.Space.sp1))
            val hello = remember(now.hour) { if (state.profile == null) ctx.getString(R.string.title_helloFirst) else Labels.timed(ctx, "title_hello", Labels.part(now)) ?: ctx.getString(R.string.title_hello) }
            TokenText(hello, Tokens.TypeScale.callout.serif(), color = ink.copy(alpha = 0.8f), align = TextAlign.Center)
        }
        // 아래: 눌러서 정원으로 (숨처럼 아주 천천히 옅어졌다 짙어짐, 상자 없이)
        val glow = if (still) 0.8f else 0.45f + 0.35f * (sin(b * 6.2832f) * 0.5f + 0.5f)
        TokenText(enterLabel, Tokens.TypeScale.callout.serif(),
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = u * 56f).graphicsLayer { alpha = tap.value * glow },
            color = ink)
    }
}

/** 천천히 출발해 아주 천천히 멈춤. */
private val CALM = CubicBezierEasing(0.33f, 0f, 0.15f, 1f)
private const val TITLE_BREATH_MS = 6400
private const val TITLE_SKY_MS = 1200
private const val TITLE_RISE_MS = 4200
private const val TITLE_WORDS_MS = 1600
private const val TITLE_TAP_DELAY_MS = 500L
private const val TITLE_WORDS_OUT_MS = 600
private const val TITLE_FADE_MS = 1400
