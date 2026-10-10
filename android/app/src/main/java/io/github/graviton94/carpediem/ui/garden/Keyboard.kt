package io.github.graviton94.carpediem.ui.garden

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

/**
 * 입력칸이 키보드에 가리지 않게: 칸을 누른 뒤 키보드가 다 올라오면 (높이가 바뀔 때마다) 그 칸 전체가 보이도록 스크롤을 끌어올린다.
 * 칸을 누르는 순간에는 아직 키보드가 없어 스크롤이 움직이지 않으므로, 키보드 높이를 지켜보다 멈춘 뒤에 한 번 더.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.keepAboveKeyboard(): Modifier {
    val req = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(focused, imeBottom) { if (focused && imeBottom > 0) { delay(120); req.bringIntoView() } }
    return this.bringIntoViewRequester(req).onFocusChanged { focused = it.isFocused || it.hasFocus }
}
