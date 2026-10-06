package io.github.graviton94.carpediem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.garden.GardenButton

/**
 * 새로워진 점: 업데이트 뒤 처음 열 때 그 버전 하나 (only), 설정에서 열면 지난 버전까지 모두.
 * 보통 앱의 업데이트 소식처럼: 제목 · 버전 · 점 목록 · 확인.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(only: String?, onClose: () -> Unit) {
    val p = Theme.palette
    val list = if (only != null) listOfNotNull(Changelog.of(only)) else Changelog.entries
    if (list.isEmpty()) { androidx.compose.runtime.LaunchedEffect(Unit) { onClose() }; return }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Theme.gc.paper, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp8),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4),
        ) {
            TokenText(stringResource(R.string.news_title), Tokens.TypeScale.title3)
            list.forEach { (version, items) ->
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    TokenText(stringResource(R.string.news_version, version), Tokens.TypeScale.footnote, color = p.secondary)
                    items.forEach { id ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                            TokenText("·", Tokens.TypeScale.callout)
                            TokenText(stringResource(id), Tokens.TypeScale.callout, Modifier.weight(1f))
                        }
                    }
                }
            }
            GardenButton(stringResource(R.string.news_ok), onClose, filled = true, seed = 1301)
        }
    }
}
