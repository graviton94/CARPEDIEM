package io.github.graviton94.carpediem.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.TypeToken
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 글자 토큰을 적용한 텍스트. */
@Composable
fun TokenText(
    text: String,
    token: TypeToken,
    modifier: Modifier = Modifier,
    color: Color = Theme.palette.foreground,
    weight: FontWeight? = null,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val style = token.style(text).let { if (weight != null) it.copy(fontWeight = weight) else it }
    Text(text, modifier, color = color, style = style, textAlign = align, maxLines = maxLines)
}

/** 화면 위쪽에서 햇빛처럼 번지는 배경. */
@Composable
fun SkyBackground(content: @Composable BoxScope.() -> Unit) {
    val p = Theme.palette
    val glow = Tokens.Effect.glowStrength * if (p.dark) 0.5f else 1.2f
    Box(
        Modifier.fillMaxSize().background(p.base).drawBehind {
            val r = max(size.width, size.height) * 0.6f
            drawRect(Brush.radialGradient(listOf(p.light.copy(alpha = glow.coerceAtMost(1f)), Color.Transparent), Offset(size.width * 0.22f, -size.height * 0.05f), r))
            drawRect(Brush.radialGradient(listOf(p.olive.copy(alpha = 0.18f), Color.Transparent), Offset(size.width * 0.3f, size.height * 1.05f), r))
        },
        content = content,
    )
}

/** 반투명 유리 카드. (Android 는 뒤를 흐리게 하지 않고 반투명 면 + 윤곽으로 표현) */
@Composable
fun GlassCard(modifier: Modifier = Modifier, padding: Dp = Tokens.Layout.cardPadding, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = Theme.palette
    val shape = RoundedCornerShape(Tokens.Radius.lg)
    Column(
        modifier.fillMaxWidth().clip(shape).background(p.glass).border(Tokens.Stroke.line, p.glassEdge, shape)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        content = content,
    )
}

@Composable
fun ProgressBar(value: Float, height: Dp = Tokens.Stroke.bar, glowing: Boolean = false) {
    val p = Theme.palette
    val v = value.coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(Tokens.Radius.pill)).background(p.dim)) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(v).clip(RoundedCornerShape(Tokens.Radius.pill))
                .background(if (glowing) Brush.horizontalGradient(listOf(p.olive, p.olive, p.light)) else Brush.horizontalGradient(listOf(p.olive, p.olive)))
        )
    }
}

/** 인생 달력: 지나온 칸은 계절 색, 지금 칸은 빛, 남은 칸은 흐리게. */
@Composable
fun LifeGrid(total: Int, filled: Int, columns: Int, modifier: Modifier = Modifier) {
    val p = Theme.palette
    val rows = max(1, (max(total, 1) + columns - 1) / columns)
    Canvas(modifier.fillMaxWidth().aspectRatio(columns.toFloat() / rows)) {
        if (total <= 0) return@Canvas
        val cell = min(size.width / columns, size.height / rows)
        val dot = cell * Tokens.Grid.dotRatio
        val originX = (size.width - cell * columns) / 2
        for (i in 0 until total) {
            val c = Offset(originX + (i % columns) * cell + cell / 2, (i / columns) * cell + cell / 2)
            when {
                i == filled -> {
                    drawCircle(Brush.radialGradient(listOf(p.now.copy(alpha = 0.55f), Color.Transparent), c, cell * 1.6f), cell * 1.6f, c)
                    drawCircle(p.now, cell * Tokens.Grid.nowRatio / 2, c)
                }
                i < filled -> drawCircle(p.seasons[min(3, i * 4 / total)], dot / 2, c)
                else -> drawCircle(p.future, dot / 2, c)
            }
        }
    }
}

/** 엔소 · 궤도: 가늘고 온전한 원(영원) 위에 굵고 열린 원(삶)과 한 점(오늘). */
@Composable
fun EnsoMark(size: Dp, modifier: Modifier = Modifier, color: Color = Theme.palette.olive, dot: Color = Theme.palette.now) {
    Canvas(modifier.width(size).height(size)) {
        val s = min(this.size.width, this.size.height)
        val r = s * 0.38f
        drawCircle(color.copy(alpha = 0.45f), r, style = Stroke(s * 0.035f))
        drawArc(color, -50f, 288f, false, topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(s * 0.11f, cap = StrokeCap.Round))
        val a = Math.toRadians(-86.0)
        drawCircle(dot, s * 0.05f, Offset(center.x + (cos(a) * r).toFloat(), center.y + (sin(a) * r).toFloat()))
    }
}

/** 알약 모양 선택지. */
@Composable
fun <T> ChipPicker(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val p = Theme.palette
    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.clip(RoundedCornerShape(Tokens.Radius.pill)).background(if (on) p.olive else p.dim)
                    .clickable(role = Role.RadioButton) { onSelect(o) }
                    .padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1)
            ) {
                TokenText(label(o), Tokens.TypeScale.subhead, color = if (on) p.onOlive else p.foreground, weight = if (on) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

/** 남은 시간 단위 선택 (일 · 주 · 개월 · 년). */
@Composable
fun <T> Segments(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    val p = Theme.palette
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.Radius.pill)).background(p.dim).padding(Tokens.Space.sp1 / 2)) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier.weight(1f).heightIn(min = Tokens.Layout.tapTarget - Tokens.Space.sp2).clip(RoundedCornerShape(Tokens.Radius.pill))
                    .background(if (on) p.olive else Color.Transparent).clickable(role = Role.Tab) { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                TokenText(label(o), Tokens.TypeScale.subhead, color = if (on) p.onOlive else p.foreground, weight = if (on) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

/** 유리 카드 안의 한 줄 (줄 전체가 눌림). */
@Composable
fun FormRow(title: String, onClick: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TokenText(title, Tokens.TypeScale.body)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun RowDivider() = Box(Modifier.fillMaxWidth().height(Tokens.Stroke.hair).background(Theme.palette.dim))

@Composable
fun FormSection(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = Theme.palette
    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        header?.let { TokenText(it, Tokens.TypeScale.footnote, Modifier.padding(horizontal = Tokens.Space.sp4), color = p.secondary) }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.Radius.md)).background(p.glass)
                .border(Tokens.Stroke.line, p.glassEdge, RoundedCornerShape(Tokens.Radius.md)).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp1),
            content = content,
        )
        footer?.let { TokenText(it, Tokens.TypeScale.footnote, Modifier.padding(horizontal = Tokens.Space.sp4), color = p.secondary) }
    }
}
