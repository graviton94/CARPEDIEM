package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import io.github.graviton94.carpediem.design.Theme

/**
 * 아래에서 올라오는 창 (정원의 모든 시트, 1.1.6).
 * 내용을 한 장의 레이어로 묶어, 올라오고 내려가는 동안은 그려 둔 것을 옮기기만 한다.
 * 크레용 상자 · 버튼은 종이 결을 입히려고 상자마다 saveLayer 를 쓰는데, 묶지 않으면 창이 움직이는 매 프레임 그것을 다시 그려 버벅인다.
 * 내용이 바뀔 때 (누름 · 스크롤 · 글자 입력) 만 레이어를 새로 그린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, containerColor = Theme.gc.paper, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }, content = content)
    }
}
