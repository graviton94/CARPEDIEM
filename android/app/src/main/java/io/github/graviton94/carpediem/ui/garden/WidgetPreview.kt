package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.widget.WidgetShots
import java.time.LocalDateTime

/** 디버그 빌드 전용 (cd.screen=widgets): 둘 수 있는 정원 위젯 셋을 실제 위젯 그대로 앱 안에서 미리 본다. 화면 캡처 CI 가 쓴다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetPreviewScreen(state: AppState, now: LocalDateTime) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().paperBackground().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(Tokens.Space.sp4)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4)) {
            WidgetShots.shots.forEach { s ->
                val bmp by produceState<ImageBitmap?>(null, s.name, now.hour, state.profile) {
                    value = runCatching { WidgetShots.render(ctx, s).asImageBitmap() }.getOrNull()
                }
                bmp?.let { Image(it, null, Modifier.size(s.w.dp, s.h.dp)) } ?: Spacer(Modifier.size(s.w.dp, s.h.dp))
            }
        }
    }
}
