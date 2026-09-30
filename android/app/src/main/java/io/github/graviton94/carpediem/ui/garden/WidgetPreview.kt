package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.LifePeriod
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import io.github.graviton94.carpediem.widget.GardenWidgetArt
import java.time.LocalDateTime

/** 디버그 빌드 전용 (cd.screen=widgets): 정원 위젯 바탕 그림과 글자를 앱 안에서 미리 본다. 화면 캡처 CI 가 쓴다. */
@Composable
fun WidgetPreviewScreen(state: AppState, now: LocalDateTime) {
    val ctx = LocalContext.current
    val s = state.profile?.let { LifeSnapshot(it.birthDate, it.expectancy(state.store.table), now) }
    val W = Tokens.Garden.Widget
    Column(Modifier.fillMaxSize().paperBackground().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
            Tile(GardenWidgetArt.Kind.DAYS, W.small.dp, W.small.dp, s, now) {
                s?.let { TokenText(Labels.number(it.remaining(LifeUnit.DAYS)), Tokens.TypeScale.largeTitle); TokenText(stringResource(R.string.widget_daysLeft), Tokens.TypeScale.caption1, color = Tokens.Garden.Colors.inkSoft) }
            }
            Tile(GardenWidgetArt.Kind.TODAY, W.small.dp, W.small.dp, s, now) {
                val t = (s ?: LifeSnapshot(now.toLocalDate(), 1.0, now)).period(LifePeriod.DAY)
                TokenText(ctx.getString(R.string.widget_todayLeft, "${t.hoursLeft}"), Tokens.TypeScale.title2.serif(), weight = FontWeight.Bold); TokenText(stringResource(R.string.widget_todaySub), Tokens.TypeScale.caption1, color = Tokens.Garden.Colors.inkSoft)
            }
        }
        Tile(GardenWidgetArt.Kind.CALENDAR, W.wide.dp, W.small.dp, s, now) {
            s?.let { TokenText(stringResource(R.string.calendar), Tokens.TypeScale.footnote.serif(), weight = FontWeight.Bold); TokenText("${it.remaining(LifeUnit.YEARS)}", Tokens.TypeScale.largeTitle); TokenText(stringResource(R.string.widget_yearsLeft), Tokens.TypeScale.caption1, color = Tokens.Garden.Colors.inkSoft) }
        }
        Tile(GardenWidgetArt.Kind.LARGE, W.wide.dp, W.wide.dp, s, now) {
            s?.let { TokenText(Labels.number(it.remaining(LifeUnit.DAYS)), Tokens.TypeScale.largeTitle); TokenText(stringResource(R.string.widget_daysLeft), Tokens.TypeScale.caption1, color = Tokens.Garden.Colors.inkSoft) }
        }
    }
}

@Composable
private fun Tile(kind: GardenWidgetArt.Kind, w: Dp, h: Dp, s: LifeSnapshot?, now: LocalDateTime, text: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val d = LocalDensity.current
    val bmp = remember(kind, s?.progress, now) { with(d) { GardenWidgetArt.render(ctx, kind, w.roundToPx(), h.roundToPx(), s, now) }.asImageBitmap() }
    Box(Modifier.size(w, h)) {
        Image(bmp, null, Modifier.size(w, h))
        Column(Modifier.padding(Tokens.Layout.widgetPadding)) { text() }
    }
}
