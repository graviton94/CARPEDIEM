package io.github.graviton94.carpediem.ui

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import kotlinx.coroutines.launch

/**
 * 바퀴 한 줄 (알람 맞추기처럼): 세 칸이 보이고 가운데가 고른 칸. 굴리면 한 칸씩 착 멈춤.
 * 화면 읽기: 지금 칸을 읽고, 위 · 아래로 한 칸씩 (+1 · -1).
 */
@Composable
internal fun WheelColumn(count: Int, index: Int, label: (Int) -> String, a11y: (Int) -> String, modifier: Modifier = Modifier, onIndex: (Int) -> Unit) {
    val p = Theme.palette
    val itemH = Tokens.Layout.tapTarget
    // 앞뒤에 빈 칸 하나씩: 맨 위 칸이 j 면 가운데 칸은 j
    val list = rememberLazyListState(initialFirstVisibleItemIndex = index.coerceIn(0, count - 1))
    val fling = rememberSnapFlingBehavior(list)
    val itemPx = with(LocalDensity.current) { itemH.toPx() }
    val idx by remember { derivedStateOf { (list.firstVisibleItemIndex + if (list.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0).coerceIn(0, count - 1) } }
    val pick by rememberUpdatedState(onIndex)
    LaunchedEffect(idx) { pick(idx) }
    val scope = rememberCoroutineScope()
    val desc = a11y(idx)
    Box(modifier.height(itemH * 3).semantics(mergeDescendants = true) {
        contentDescription = desc
        customActions = listOf(
            CustomAccessibilityAction("+1") { scope.launch { list.animateScrollToItem((idx + 1).coerceAtMost(count - 1)) }; true },
            CustomAccessibilityAction("-1") { scope.launch { list.animateScrollToItem((idx - 1).coerceAtLeast(0)) }; true })
    }) {
        LazyColumn(Modifier.fillMaxWidth().height(itemH * 3), state = list, flingBehavior = fling, horizontalAlignment = Alignment.CenterHorizontally) {
            item { Spacer(Modifier.height(itemH)) }
            items(count) { i ->
                val on = i == idx
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    TokenText(label(i), if (on) Tokens.TypeScale.title2.serif() else Tokens.TypeScale.callout.serif(),
                        color = if (on) p.foreground else p.secondary.copy(alpha = 0.6f), weight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
            item { Spacer(Modifier.height(itemH)) }
        }
    }
}

/** 가운데 칸 위아래 가는 선 (고르는 자리). 바퀴들 뒤에 깔아 둠. */
@Composable
internal fun WheelLens(modifier: Modifier) {
    val c = Theme.palette.foreground.copy(alpha = 0.5f)
    val sw = with(LocalDensity.current) { (Theme.unit * 0.4f).toPx() }
    Box(modifier.height(Tokens.Layout.tapTarget).drawBehind {
        drawLine(c, Offset(0f, 0f), Offset(size.width, 0f), sw)
        drawLine(c, Offset(0f, size.height), Offset(size.width, size.height), sw)
    })
}

/**
 * 알림 시각 고르기 (1.1.5): 폰 시계 다이얼 대신 정원 창 안의 바퀴. 오전/오후 · 시 · 분 (5분 단위), 24시간제 폰은 0–23시.
 * 고르는 대로 아래 한 줄이 바뀜 (guide). [취소] [저장] 은 다른 창과 같은 버튼.
 */
@Composable
internal fun TimeWheelAlert(title: String, minute: Int, guide: (Int) -> String, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val ctx = LocalContext.current
    val p = Theme.palette
    val h24 = remember { android.text.format.DateFormat.is24HourFormat(ctx) }
    val locale = ctx.resources.configuration.locales[0]
    val ampm = remember(locale) { java.text.DateFormatSymbols.getInstance(locale).amPmStrings }
    // 오전/오후를 시 앞에 쓰는 말 (한국어 · 일본어 · 중국어)
    val apFirst = locale.language in AP_FIRST
    val start = remember { (minute + MINUTE_STEP / 2) / MINUTE_STEP * MINUTE_STEP % DAY_MIN }
    var h by remember { mutableIntStateOf(start / 60) }
    var m by remember { mutableIntStateOf(start % 60 / MINUTE_STEP) }
    val value = h * 60 + m * MINUTE_STEP
    val hourA11y: (Int) -> String = { ctx.getString(R.string.time_hourA11y, "$it") }
    GardenAlert(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    WheelLens(Modifier.fillMaxWidth(0.85f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        val ap: @Composable () -> Unit = { WheelColumn(2, h / 12, { ampm[it] }, { ampm[it] }, Modifier.width(AP_W)) { h = h % 12 + it * 12 } }
                        if (!h24 && apFirst) ap()
                        if (h24) WheelColumn(24, h, { "$it" }, hourA11y, Modifier.width(NUM_W)) { h = it }
                        else WheelColumn(12, (h % 12 + 11) % 12, { "${it + 1}" }, { hourA11y(it + 1) }, Modifier.width(NUM_W)) { h = h / 12 * 12 + (it + 1) % 12 }
                        TokenText(":", Tokens.TypeScale.title2.serif(), color = p.foreground)
                        WheelColumn(60 / MINUTE_STEP, m, { "%02d".format(it * MINUTE_STEP) }, { ctx.getString(R.string.time_minuteA11y, "${it * MINUTE_STEP}") }, Modifier.width(NUM_W)) { m = it }
                        if (!h24 && !apFirst) ap()
                    }
                }
                TokenText(guide(value), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
            }
        },
        confirmButton = { AlertButton(stringResource(R.string.settings_save), { onSave(value) }) },
        dismissButton = { AlertButton(stringResource(R.string.cancel), onDismiss, quiet = true) },
    )
}

private const val MINUTE_STEP = 5
private const val DAY_MIN = 24 * 60
private val AP_FIRST = setOf("ko", "ja", "zh")
private val NUM_W = 64.dp
private val AP_W = 72.dp
