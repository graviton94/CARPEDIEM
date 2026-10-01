package io.github.graviton94.carpediem.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.crayonBox
import kotlinx.coroutines.delay

/**
 * 앱 안의 모든 ‘떠오르는 것’ (묻는 창 · 한 줄 창 · 알림 한마디) 이 같은 나타남을 쓴다:
 * 옅게 → 또렷이, 조금 작게 → 제 크기. 시간과 곡선은 토큰 garden.motion.modalFadeMs 하나.
 */
@Composable
fun rememberPop(key: Any?, delayMs: Long = 0): Animatable<Float, AnimationVector1D> {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { if (delayMs > 0) delay(delayMs); a.animateTo(1f, tween(Tokens.Garden.Motion.modalFadeMs.toInt(), easing = LinearOutSlowInEasing)) }
    return a
}

fun Modifier.pop(a: Animatable<Float, AnimationVector1D>): Modifier = graphicsLayer { alpha = a.value; val k = 0.94f + 0.06f * a.value; scaleX = k; scaleY = k }

/** 떠오르는 창의 바탕: 정원은 종이 위 크레용 선, 유리는 불투명한 판 (뒤가 비치면 글이 흐려서). */
@Composable
fun Modifier.modalBox(seed: Int = 997): Modifier {
    if (Theme.garden) return crayonBox(Theme.gc.paper, Tokens.Garden.Radius.box, Tokens.Garden.Stroke.box, seed)
    val p = Theme.palette
    val shape = RoundedCornerShape(Tokens.Radius.lg)
    return clip(shape).background(p.base).border(Tokens.Stroke.line, p.glassEdge, shape)
}

/** 묻는 창 (Material AlertDialog 대신, 같은 자리 · 같은 이름의 인자). 바깥을 누르면 닫힘. */
@Composable
fun GardenAlert(
    onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null, title: (@Composable () -> Unit)? = null, text: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") containerColor: Color = Color.Unspecified,
) {
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false)) {
        // 창 뒤의 어둠도 우리가 같은 속도로 (기본 어둠은 끔)
        (LocalView.current.parent as? DialogWindowProvider)?.window?.let { w -> SideEffect { w.setDimAmount(0f) } }
        val a = rememberPop(Unit)
        val scrim = if (Theme.garden) Theme.gc.scrim else Color.Black.copy(alpha = 0.32f)
        val none = remember { MutableInteractionSource() }
        Box(
            Modifier.fillMaxSize().drawBehind { drawRect(scrim.copy(alpha = scrim.alpha * a.value)) }.clickable(interactionSource = none, indication = null, onClick = onDismissRequest),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier.padding(horizontal = Theme.deviceClass.pageMargin).widthIn(max = 440.dp).fillMaxWidth().pop(a).modalBox()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}.padding(Tokens.Space.sp5),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
            ) {
                CompositionLocalProvider(LocalContentColor provides Theme.palette.foreground) {
                    title?.let { ProvideTextStyle(Tokens.TypeScale.headline.style("가").copy(textAlign = TextAlign.Start)) { it() } }
                    text?.let { ProvideTextStyle(Tokens.TypeScale.callout.style("가")) { it() } }
                    Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        dismissButton?.invoke(); confirmButton()
                    }
                }
            }
        }
    }
}

/** 알림 한마디 (시스템 Toast 대신): 아래에 잠깐 떠올랐다가 사라짐. */
@Composable
fun NoteHost(note: Pair<String, Long>?, onDone: () -> Unit) {
    val (text, id) = note ?: return
    val a = rememberPop(id)
    LaunchedEffect(id) { delay(Tokens.Garden.Motion.noteMs.toLong()); onDone() }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = Tokens.Space.sp10 * 2), contentAlignment = Alignment.BottomCenter) {
        TokenText(text, Tokens.TypeScale.subhead, Modifier.padding(horizontal = Theme.deviceClass.pageMargin).pop(a).modalBox(998)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3), align = TextAlign.Center)
    }
}
