package io.github.graviton94.carpediem.ui.garden

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import io.github.graviton94.carpediem.data.Photos
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.TokenText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** 한지 액자에 걸린 시간이 묻은 사진 (11). 날마다 기울기가 조금씩 다르고, 아래에 날짜 (와 한마디). 사진이 없으면 아무것도 그리지 않음. */
@Composable
internal fun WeatheredPhoto(state: AppState, day: LocalDate, today: LocalDate, width: Dp, pending: Boolean = false, edit: Boolean = false, caption: String? = null, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var img by remember(day, pending) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(day, today, pending, state.photoKick) {
        img = withContext(Dispatchers.Default) { runCatching { Photos.weathered(ctx, day, today, pending, edit)?.asImageBitmap() }.getOrNull() }
    }
    val bmp = img ?: return
    val tilt = remember(day) { (Math.floorMod(day.toEpochDay() * 7919, 70L) - 35) / 10f }   // -3.5° … 3.4°
    Box(modifier.width(width).graphicsLayer { rotationZ = tilt }) {
        Image(bmp, caption ?: RecordText.day(ctx, day), Modifier.fillMaxWidth().aspectRatio(bmp.width.toFloat() / bmp.height))
        TokenText(listOfNotNull(RecordText.day(ctx, day), caption).joinToString(" · "), Tokens.TypeScale.caption1.serif(),
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = width * 0.08f).padding(bottom = width * 0.06f),
            color = Theme.gc.inkSoft, align = TextAlign.Center, maxLines = 1)
    }
}

/** 사진 고르기 (시스템 사진 선택기, 저장소 권한 없이). 고르면 onPicked. */
@Composable
internal fun rememberPhotoPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
