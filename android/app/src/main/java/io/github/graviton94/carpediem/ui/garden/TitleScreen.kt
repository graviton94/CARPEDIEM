package io.github.graviton94.carpediem.ui.garden

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.statusBarsPadding
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
 * 하루의 첫 화면: 그날 처음 열 때 한 번. 지금 때의 하늘 아래 하루가 눈을 감고 졸고 있고, 위에 ‘하루의 정원’과 오늘의 인사.
 * 어디든 누르면 하루가 눈을 뜨며 살짝 뛰어오르고, 하루 자리에서 동그랗게 정원이 열린다 (아래에 이미 그려 둔 정원이 드러남).
 * 뒤로 가기 · 움직임을 끈 기기에서는 바로 정원으로.
 */
@Composable
fun TitleScreen(state: AppState, now: LocalDateTime, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val still = remember { reducedMotion(ctx) }
    val season = io.github.graviton94.carpediem.core.GardenDecor.realSeason(now.toLocalDate(), state.profile?.countryCode ?: "KR")
    val night = SkyTime.isDark(now)
    val art = remember { HaruArt.of(state.store.haruSeed, false) }
    val lid = remember { Animatable(1f) }
    val hop = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }   // 0 = 첫 화면 그대로, 1 = 정원이 다 열림
    val shown = remember { Animatable(0f) }
    var entering by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(900)) }
    val breathe = rememberInfiniteTransition(label = "titleBreath")
    val b by breathe.animateFloat(0f, 1f, infiniteRepeatable(tween(TITLE_BREATH_MS, easing = LinearEasing), RepeatMode.Restart), label = "b")
    fun enter() {
        if (entering) return
        entering = true
        if (still) { onDone(); return }
        scope.launch {
            lid.animateTo(0f, tween(260))
            launch { hop.animateTo(1f, tween(420)) }
            delay(280)
            reveal.animateTo(1f, tween(TITLE_OPEN_MS, easing = FastOutSlowInEasing))
            onDone()
        }
    }
    BackHandler { onDone() }
    val enterLabel = stringResource(R.string.title_enter)
    BoxWithConstraints(Modifier.fillMaxSize().semantics { contentDescription = enterLabel }.pointerInput(Unit) { detectTapGestures { enter() } }) {
        val u = Theme.unit
        val w = with(density) { maxWidth.toPx() }; val h = with(density) { maxHeight.toPx() }
        val haruY = maxHeight * TITLE_HARU_AT
        val scale = u * (TITLE_HARU_WIDTH / G.Layout.haruArtWidth)
        val k = with(density) { scale.toPx() }
        val center = Offset(w / 2, with(density) { haruY.toPx() } - k * 10f)
        val maxR = hypot(maxOf(center.x, w - center.x), maxOf(center.y, h - center.y))
        // 정원이 열리는 자리: 하루에서 동그랗게 (아래 정원이 비쳐 보임)
        Box(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; alpha = shown.value }.drawWithContent {
            drawContent()
            if (reveal.value > 0f) drawCircle(Color.Black, maxR * reveal.value, center, blendMode = BlendMode.Clear)
        }) {
            Image(GardenArt.sky(ctx, season), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(if (night) Color(0xCC0E1018) else Color(0x33FFFFFF)))
            // 땅 한 줄과 졸고 있는 하루
            Image(GardenArt.strip(ctx, season), null, Modifier.fillMaxWidth().offset(y = haruY - u * G.Layout.stripLineY).height(u * G.Layout.stripHeight), contentScale = ContentScale.FillBounds)
            val boxH = scale * (G.Layout.haruGround - art.meta.bbox.top + G.Layout.sparkle)
            Box(Modifier.align(Alignment.TopCenter).offset(y = haruY - boxH).graphicsLayer {
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                val s = if (still) 0f else sin(b * 6.2832f) * 0.5f + 0.5f
                scaleY = 1f + 0.018f * s; scaleX = 1f + 0.008f * s
                translationY = -k * 9f * sin(hop.value * Math.PI).toFloat()
            }) { BigStoneOnly(art, scale, lid.value) }
            // 위: 이름 · 오늘 · 인사
            val ink = if (night) Color(0xFFEEEBDD) else Theme.gc.ink
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = u * 70f).padding(horizontal = Theme.deviceClass.pageMargin).graphicsLayer { alpha = 1f - reveal.value * 2f },
                horizontalAlignment = Alignment.CenterHorizontally) {
                TokenText("Carpe Diem", Tokens.TypeScale.footnote.serif(), color = ink.copy(alpha = 0.65f))
                Spacer(Modifier.height(Tokens.Space.sp2))
                TokenText(stringResource(R.string.title_name), Tokens.TypeScale.largeTitle.serif(), color = ink, align = TextAlign.Center)
                Spacer(Modifier.height(Tokens.Space.sp4))
                TokenText(now.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), Tokens.TypeScale.footnote, color = ink.copy(alpha = 0.7f))
                Spacer(Modifier.height(Tokens.Space.sp1))
                val hello = remember(now.hour) { Labels.timed(ctx, "title_hello", Labels.part(now)) ?: ctx.getString(R.string.title_hello) }
                TokenText(hello, Tokens.TypeScale.callout.serif(), color = ink.copy(alpha = 0.85f), align = TextAlign.Center)
            }
            // 아래: 눌러서 정원으로 (숨처럼 옅어졌다 짙어짐)
            val glow = if (still) 0.8f else 0.45f + 0.4f * (sin(b * 6.2832f) * 0.5f + 0.5f)
            TokenText(enterLabel, Tokens.TypeScale.subhead.serif(),
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = u * 64f).graphicsLayer { alpha = if (entering) 0f else glow },
                color = ink)
        }
    }
}

private const val TITLE_BREATH_MS = 5200
private const val TITLE_OPEN_MS = 900
private const val TITLE_HARU_AT = 0.66f
private const val TITLE_HARU_WIDTH = 64f
