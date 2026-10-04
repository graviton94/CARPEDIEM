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
 * 하늘만 보이는 데서 시작해 카메라가 천천히 내려와 땅과 졸고 있는 하루에 닿고, 그다음 ‘하루의 정원’ · 오늘 · 인사가 떠오른다.
 * 누르면 (내려오는 중이면 끝 장면으로 먼저) 화면 전체가 옅어지며 아래에 그려 둔 정원이 나타난다.
 * 뒤로 가기는 바로 정원으로, 움직임을 끈 기기는 내려오기 없이 끝 장면부터.
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
    val pan = remember { Animatable(if (still) 1f else 0f) }      // 0 = 하늘 꼭대기, 1 = 땅과 하루
    val words = remember { Animatable(if (still) 1f else 0f) }    // 이름 · 인사
    val tap = remember { Animatable(if (still) 1f else 0f) }      // 눌러서 정원으로
    val fade = remember { Animatable(1f) }                         // 첫 화면 전체 (0 이면 정원)
    val lid = remember { Animatable(1f) }
    var entering by remember { mutableStateOf(false) }
    val breathe = rememberInfiniteTransition(label = "titleBreath")
    val b by breathe.animateFloat(0f, 1f, infiniteRepeatable(tween(TITLE_BREATH_MS, easing = LinearEasing), RepeatMode.Restart), label = "b")
    LaunchedEffect(Unit) {
        if (!still) pan.animateTo(1f, tween(TITLE_PAN_MS, easing = FastOutSlowInEasing))
        words.animateTo(1f, tween(TITLE_WORDS_MS))
        tap.animateTo(1f, tween(TITLE_WORDS_MS))
    }
    fun enter() {
        if (entering) return
        // 내려오는 중이면 먼저 끝 장면으로
        if (pan.value < 1f || words.value < 1f) { scope.launch { pan.snapTo(1f); words.snapTo(1f); tap.snapTo(1f) }; return }
        entering = true
        if (still) { onDone(); return }
        scope.launch {
            lid.animateTo(0f, tween(240))
            fade.animateTo(0f, tween(TITLE_FADE_MS, easing = LinearEasing))
            onDone()
        }
    }
    BackHandler { onDone() }
    val enterLabel = stringResource(R.string.title_enter)
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().semantics { contentDescription = enterLabel }.pointerInput(Unit) { detectTapGestures { enter() } }
        .graphicsLayer { alpha = fade.value }) {
        val u = Theme.unit
        val screenH = maxHeight
        val screenW = maxWidth
        val worldH = screenH * TITLE_WORLD
        val travel = with(density) { (worldH - screenH).toPx() }
        // 하늘 꼭대기부터 땅까지 이어진 한 장 (카메라가 위에서 아래로)
        // 부모(화면 높이)보다 큰 한 장이라 높이 제한을 풀어 둔다 (안 그러면 화면 높이로 잘려 아래 정원이 비친다)
        Box(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).height(worldH).graphicsLayer { translationY = -travel * pan.value }) {
            Box(Modifier.fillMaxSize().background(Theme.gc.base))
            if (night) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF070A16), Color(0xFF0E1222), Color(0xFF1B2134), Color(0xFF2B3046)))))
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val r = java.util.Random(7)
                    repeat(160) { val x = r.nextFloat() * size.width; val y = r.nextFloat() * size.height * 0.8f; val a = 0.25f + r.nextFloat() * 0.6f
                        drawCircle(Color(0xFFFFF4D6).copy(alpha = a * (0.7f + 0.3f * sin(b * 6.2832f + it))), 1.2f + r.nextFloat() * 1.8f, Offset(x, y)) }
                }
                Image(GardenArt.moonFull(ctx), null, Modifier.align(Alignment.TopEnd).padding(top = screenH * 0.55f, end = u * 50f).size(u * 34f))
            } else {
                Image(GardenArt.sky(ctx, season), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.BottomCenter)
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x00FFFFFF)))))
                // 내려오며 스치는 구름 몇 장
                listOf(0.18f to 0.12f, 0.42f to 0.6f, 0.7f to 0.25f).forEachIndexed { i, (yf, xf) ->
                    Image(GardenArt.image(ctx, "cloud_${season.name.lowercase()}_${i % 2}.webp"), null, Modifier.offset(x = screenW * xf, y = worldH * yf).size(u * (70f + 20f * i), u * (28f + 8f * i)), contentScale = ContentScale.Fit)
                }
            }
            // 땅 (월드 아래쪽) 과 졸고 있는 하루
            val haruY = worldH - screenH * (1f - TITLE_HARU_AT)
            val stripTop = haruY - u * G.Layout.stripLineY
            Image(GardenArt.strip(ctx, season), null, Modifier.fillMaxWidth().offset(y = stripTop).height(maxOf(u * G.Layout.stripHeight, worldH - stripTop)),
                contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
                colorFilter = if (night) androidx.compose.ui.graphics.ColorFilter.tint(Color(0xB30E1222), BlendMode.SrcAtop) else null)
            val scale = u * (TITLE_HARU_WIDTH / G.Layout.haruArtWidth)
            val boxH = scale * (G.Layout.haruGround - art.meta.bbox.top + G.Layout.sparkle)
            Box(Modifier.align(Alignment.TopCenter).offset(y = haruY - boxH).graphicsLayer {
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                val s = if (still) 0f else sin(b * 6.2832f) * 0.5f + 0.5f
                scaleY = 1f + 0.018f * s; scaleX = 1f + 0.008f * s
            }) { BigStoneOnly(art, scale, lid.value) }
        }
        // 위: 이름 · 오늘 · 인사 (내려온 뒤 떠오름)
        val ink = if (night) Color(0xFFEEEBDD) else Theme.gc.ink
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = u * 70f).padding(horizontal = Theme.deviceClass.pageMargin)
            .graphicsLayer { alpha = words.value; translationY = (1f - words.value) * 24f },
            horizontalAlignment = Alignment.CenterHorizontally) {
            TokenText("Carpe Diem", Tokens.TypeScale.footnote.serif(), color = ink.copy(alpha = 0.65f))
            Spacer(Modifier.height(Tokens.Space.sp2))
            TokenText(stringResource(R.string.title_name), Tokens.TypeScale.largeTitle.serif(), color = ink, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp4))
            TokenText(now.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), Tokens.TypeScale.footnote, color = ink.copy(alpha = 0.7f))
            Spacer(Modifier.height(Tokens.Space.sp1))
            val hello = remember(now.hour) { if (state.profile == null) ctx.getString(R.string.title_helloFirst) else Labels.timed(ctx, "title_hello", Labels.part(now)) ?: ctx.getString(R.string.title_hello) }
            TokenText(hello, Tokens.TypeScale.callout.serif(), color = ink.copy(alpha = 0.85f), align = TextAlign.Center)
        }
        // 아래: 눌러서 정원으로 (숨처럼 옅어졌다 짙어짐)
        val glow = if (still) 0.85f else 0.5f + 0.4f * (sin(b * 6.2832f) * 0.5f + 0.5f)
        TokenText(enterLabel, Tokens.TypeScale.headline.serif(),
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = u * 64f).graphicsLayer { alpha = tap.value * (if (entering) 0f else glow) }
                .background(Theme.gc.paper.copy(alpha = if (night) 0.12f else 0.55f), androidx.compose.foundation.shape.RoundedCornerShape(50)).padding(horizontal = Tokens.Space.sp5, vertical = Tokens.Space.sp2),
            color = ink)
    }
}

private const val TITLE_BREATH_MS = 5200
private const val TITLE_PAN_MS = 2800
private const val TITLE_WORDS_MS = 700
private const val TITLE_FADE_MS = 700
/** 하늘부터 땅까지 한 장의 높이 (화면의 몇 배). */
private const val TITLE_WORLD = 2.4f
private const val TITLE_HARU_AT = 0.66f
private const val TITLE_HARU_WIDTH = 64f
