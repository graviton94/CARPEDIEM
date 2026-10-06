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

/** 버전마다 새로워진 점 (최신이 위). 새 버전을 낼 때 여기 한 줄과 strings 의 news.* 를 더함. */
object Changelog {
    val entries: List<Pair<String, List<Int>>> = listOf(
        "1.1.3" to listOf(R.string.news_113_update, R.string.news_113_notes),
        "1.1.2" to listOf(R.string.news_112_edit, R.string.news_112_welcome, R.string.news_112_keep, R.string.news_112_slip, R.string.news_112_fix),
    )
    fun of(version: String) = entries.firstOrNull { it.first == version }
}

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
