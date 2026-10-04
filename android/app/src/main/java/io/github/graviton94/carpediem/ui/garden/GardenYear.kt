package io.github.graviton94.carpediem.ui.garden

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.SeasonCard
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.share.ShareCards
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.GardenAlert
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDateTime
import androidx.compose.foundation.layout.size
import io.github.graviton94.carpediem.design.Theme

/** 그해에 받은 계절 조각 (앨범과 같은 것). */
internal fun yearCards(state: AppState, year: Int): List<SeasonCard> = state.seasonCards.mapNotNull { SeasonCard.parse(it) }.filter { it.year == year }

/** 그해에 처음 만난 순간의 키 (만난 날 순서). */
internal fun yearMet(state: AppState, year: Int): List<String> = state.chancesMet.mapNotNull { r ->
    r.split(':', limit = 2).takeIf { it.size == 2 }?.let { (k, d) -> runCatching { k to java.time.LocalDate.parse(d) }.getOrNull() }
}.filter { it.second.year == year }.sortedBy { it.second }.map { it.first }.distinct()

/**
 * 정원의 한 해 (S2): 한지 한 장에 그해의 계절 조각 · 말뚝 · 연 리본 · 만난 순간 · 작은 하루. 숫자는 쓰지 않고 그림으로만.
 * ‘그림으로 보내기’ 로 저장 · 공유 (지금 있는 보내기 그대로).
 */
@Composable
internal fun GardenYearSheet(state: AppState, profile: LifeProfile, now: LocalDateTime, year: Int, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val title = stringResource(R.string.gardenYear_title, "$year")
    var bmp by remember(year) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(year) {
        val decor = state.decor(profile, LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now), now.toLocalDate())
        val cards = yearCards(state, year); val met = yearMet(state, year); val seed = state.store.haruSeed
        bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            // 지난 해는 그때의 말뚝 · 연을 알 수 없어 조각 · 순간만 (올해만 지금 걸린 것 · 리본까지)
            val thisYear = year == now.year
            ShareCards.gardenYear(ctx, title, cards, if (thisYear) decor.hang else io.github.graviton94.carpediem.core.Hang.NONE, if (thisYear) decor.ribbons else emptyList(), met, decor.season, seed)
        }
    }
    GardenAlert(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { bmp?.let { ShareCards.send(ctx, it, "garden-year-$year") } }, enabled = bmp != null) { Text(stringResource(R.string.share_image)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        text = {
            val ratio = Tokens.Garden.Share.lineW / Tokens.Garden.Share.lineH
            // 그리는 동안은 빈 상자 대신 가운데에 작은 기다림 표시
            bmp?.let { Image(it.asImageBitmap(), title, Modifier.fillMaxWidth().aspectRatio(ratio)) } ?: Box(Modifier.fillMaxWidth().aspectRatio(ratio), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(Theme.unit * 28), color = Theme.palette.olive, strokeWidth = Theme.unit * 2)
            }
        },
    )
}

/** 앨범: 계절 조각을 받은 해마다 ‘한 해를 한 장으로’. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GardenYearAlbum(state: AppState, onOpen: (Int) -> Unit) {
    val years = remember(state.seasonCards) { state.seasonCards.mapNotNull { SeasonCard.parse(it)?.year }.distinct().sortedDescending() }
    if (years.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        TokenText(stringResource(R.string.gardenYear_album), Tokens.TypeScale.headline, Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            years.forEachIndexed { i, y -> GardenChip("$y", false, seed = 1400 + i) { onOpen(y) } }
        }
    }
}
