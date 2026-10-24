package io.github.graviton94.carpediem.ui.garden

import io.github.graviton94.carpediem.ui.AlertButton
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
import kotlinx.coroutines.sync.withLock
import androidx.compose.foundation.layout.size
import io.github.graviton94.carpediem.design.Theme

/** 그해에 받은 계절 조각 (앨범과 같은 것). */
internal fun yearCards(state: AppState, year: Int): List<SeasonCard> = state.seasonCards.mapNotNull { SeasonCard.parse(it) }.filter { it.year == year }

/** 그해에 처음 만난 순간의 키 (만난 날 순서). */
internal fun yearMet(state: AppState, year: Int): List<String> = state.chancesMet.mapNotNull { r ->
    r.split(':', limit = 2).takeIf { it.size == 2 }?.let { (k, d) -> runCatching { k to java.time.LocalDate.parse(d) }.getOrNull() }
}.filter { it.second.year == year }.sortedBy { it.second }.map { it.first }.distinct()

/** 지난 해의 정원 (1.1.12): 한 줄이 있는 지난 해마다 (12월 21일부터는 올해도), 누르면 해 카드 · 해 영상. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GardenYearAlbum(state: AppState, profile: LifeProfile, now: LocalDateTime, onOpen: (Int) -> Unit) {
    val today = now.toLocalDate()
    val years = remember(state.lines, today) { state.gardenYears().filter { it < today.year || (today.monthValue == 12 && today.dayOfMonth >= 21) } }
    if (years.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        TokenText(stringResource(R.string.gardenYear_album), Tokens.TypeScale.headline, Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            years.forEachIndexed { i, y -> GardenChip("$y", false, seed = 1400 + i) { onOpen(y) } }
        }
    }
}
