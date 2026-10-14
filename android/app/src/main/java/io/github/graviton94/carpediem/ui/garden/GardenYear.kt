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

/**
 * 정원의 한 해 (S2): 한지 한 장에 그해의 계절 조각 · 말뚝 · 연 리본 · 만난 순간 · 작은 하루. 숫자는 쓰지 않고 그림으로만.
 * ‘그림으로 보내기’ 로 저장 · 공유 (지금 있는 보내기 그대로).
 */
/**
 * 한 해 한 장 그림 (느린 폰에서 열 때 기다리지 않게): 앨범 · 권유가 보이면 미리 그려 두고, 같은 재료 (해 · 조각 · 정원 · 말) 면 다시 그리지 않음.
 * 그림은 화면 스레드 밖에서. 몇 장만 둠.
 */
internal object YearPoster {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()
    private val lock = kotlinx.coroutines.sync.Mutex()

    private class Plan(val key: String, val draw: () -> Bitmap)

    private fun plan(ctx: android.content.Context, state: AppState, profile: LifeProfile, now: LocalDateTime, year: Int): Plan {
        val decor = state.decor(profile, LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now), now.toLocalDate())
        val cards = yearCards(state, year); val seed = state.store.haruSeed
        // 지난 해는 그때의 말뚝 · 연을 알 수 없어 조각 · 순간만 (올해만 지금 걸린 것 · 리본까지)
        val thisYear = year == now.year
        val hang = if (thisYear) decor.hang else io.github.graviton94.carpediem.core.Hang.NONE
        val ribbons = if (thisYear) decor.ribbons else emptyList()
        val kite = thisYear && decor.kite
        val key = listOf(year, cards.joinToString { it.key + it.season }, hang, ribbons, decor.season, seed, decor.tree, decor.stage, kite, ctx.resources.configuration.locales[0]).joinToString("|")
        return Plan(key) { ShareCards.gardenYear(ctx, year, cards, hang, ribbons, decor.season, seed, tree = decor.tree, stage = decor.stage, kite = kite) }
    }

    /** 이미 그려 둔 것이 있으면 바로. */
    fun ready(ctx: android.content.Context, state: AppState, profile: LifeProfile, now: LocalDateTime, year: Int): Bitmap? = cache[plan(ctx, state, profile, now, year).key]

    suspend fun get(ctx: android.content.Context, state: AppState, profile: LifeProfile, now: LocalDateTime, year: Int): Bitmap {
        val pl = plan(ctx, state, profile, now, year)
        cache[pl.key]?.let { return it }
        // 한 번에 하나만 그림 (미리 그리기와 여는 것이 겹쳐도 두 번 그리지 않게)
        return lock.withLock {
            cache[pl.key] ?: kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { pl.draw() }.also { if (cache.size >= 2) cache.clear(); cache[pl.key] = it }
        }
    }
}

/** 보이는 동안 그 해의 그림을 미리 그려 둠. */
@Composable
internal fun WarmYearPoster(state: AppState, profile: LifeProfile, now: LocalDateTime, year: Int) {
    val ctx = LocalContext.current
    // 화면이 막 열릴 때의 그림과 겹치지 않게 조금 뒤에
    LaunchedEffect(year, state.seasonCards) { kotlinx.coroutines.delay(WARM_AFTER_MS); runCatching { YearPoster.get(ctx, state, profile, now, year) } }
}

private const val WARM_AFTER_MS = 1200L

@Composable
internal fun GardenYearSheet(state: AppState, profile: LifeProfile, now: LocalDateTime, year: Int, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val title = stringResource(R.string.gardenYear_title, "$year")
    var bmp by remember(year) { mutableStateOf(YearPoster.ready(ctx, state, profile, now, year)) }
    LaunchedEffect(year) { if (bmp == null) bmp = YearPoster.get(ctx, state, profile, now, year) }
    GardenAlert(
        onDismissRequest = onDismiss,
        confirmButton = { io.github.graviton94.carpediem.ui.AlertButton(stringResource(R.string.share_image), { bmp?.let { ShareCards.send(ctx, it, "garden-year-$year") } }, enabled = bmp != null) },
        dismissButton = { AlertButton(stringResource(R.string.done), onDismiss, quiet = true) },
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
internal fun GardenYearAlbum(state: AppState, profile: LifeProfile, now: LocalDateTime, onOpen: (Int) -> Unit) {
    val years = remember(state.seasonCards) { state.seasonCards.mapNotNull { SeasonCard.parse(it)?.year }.distinct().sortedDescending() }
    if (years.isEmpty()) return
    // 가장 최근 해는 미리 그려 둠 (누르면 바로 보이게)
    WarmYearPoster(state, profile, now, years.first())
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
        TokenText(stringResource(R.string.gardenYear_album), Tokens.TypeScale.headline, Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            years.forEachIndexed { i, y -> GardenChip("$y", false, seed = 1400 + i) { onOpen(y) } }
        }
    }
}
