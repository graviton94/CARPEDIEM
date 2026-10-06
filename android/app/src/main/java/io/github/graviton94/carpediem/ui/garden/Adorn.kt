package io.github.graviton94.carpediem.ui.garden

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Adornments
import io.github.graviton94.carpediem.core.SeasonCard
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText

/** 꾸밈 하나의 그림 (계절 조각과 같은 한지 조각, 같은 상자). */
internal fun adornArt(ctx: Context, item: String): ImageBitmap? = when {
    item.startsWith("card:") -> SeasonCard.parse(item.removePrefix("card:"))?.let { GardenArt.card(ctx, it.key) }
    item == "support:support_tea" -> GardenArt.card(ctx, "tea")
    item == "support:support_coffee" -> GardenArt.card(ctx, "coffee")
    item == "support:support_cake" -> GardenArt.card(ctx, "cake")
    item.startsWith("pebble:") -> GardenArt.card(ctx, "pebble")
    else -> null
}

/** 꾸밈 이름 (예: ‘가을 은행잎’ · ‘커피’ · ‘조약돌’). */
internal fun adornName(ctx: Context, item: String): String = when {
    item.startsWith("card:") -> SeasonCard.parse(item.removePrefix("card:"))?.let { c -> ctx.getString(R.string.adorn_card, Labels.season(ctx, c.season), cardName(ctx, c.key)) } ?: ""
    item == "support:support_tea" -> ctx.getString(R.string.support_mark1)
    item == "support:support_coffee" -> ctx.getString(R.string.support_mark2)
    item == "support:support_cake" -> ctx.getString(R.string.support_mark3)
    item.startsWith("pebble:") -> ctx.getString(R.string.adorn_pebble)
    else -> ""
}

/** 그 꾸밈을 어디서 얻었는지 한 줄 (예: ‘2026 가을 · 은행나무’ · ‘10월 6일 응원’ · ‘하루가 준 날 · 10월 4일’). */
internal fun adornFrom(ctx: Context, state: AppState, item: String): String = when {
    item.startsWith("card:") -> SeasonCard.parse(item.removePrefix("card:"))?.let { c -> "${c.year} ${Labels.season(ctx, c.season)}" } ?: ""
    item.startsWith("support:") -> state.supportMarks[item.removePrefix("support:")]?.let { ctx.getString(R.string.adorn_fromSupport, RecordText.day(ctx, it)) } ?: ""
    item.startsWith("pebble:") -> item.removePrefix("pebble:").toLongOrNull()?.let { ctx.getString(R.string.adorn_fromPebble, RecordText.day(ctx, java.time.LocalDate.ofEpochDay(it))) } ?: ""
    else -> ""
}

/**
 * 돌 페이지의 ‘곁에 둔 것’: 지금 꾸밈 (그림 · 이름) 과 고르기. 아무것도 없으면 무엇을 둘 수 있는지 한 줄.
 */
@Composable
internal fun AdornRow(state: AppState, stone: String, stoneName: String, onPick: () -> Unit) {
    val ctx = LocalContext.current
    val p = Theme.palette
    val item = state.adornmentsShown()[stone]
    val owned = state.ownedAdornments()
    Row(
        Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 1401).clickable(enabled = owned.isNotEmpty(), role = Role.Button, onClick = onPick)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) {
        Box(Modifier.size(Theme.unit * 48f), contentAlignment = Alignment.Center) {
            item?.let { adornArt(ctx, it) }?.let { Image(it, null, Modifier.size(Theme.unit * 48f)) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            TokenText(stringResource(R.string.adorn_title, stoneName), Tokens.TypeScale.subhead, weight = FontWeight.SemiBold)
            TokenText(when {
                item != null -> adornName(ctx, item)
                owned.isEmpty() -> stringResource(R.string.adorn_none)
                else -> stringResource(R.string.adorn_empty)
            }, Tokens.TypeScale.footnote, color = p.secondary)
        }
        if (owned.isNotEmpty()) TokenText(stringResource(if (item != null) R.string.adorn_change else R.string.adorn_pick), Tokens.TypeScale.footnote, color = p.olive, weight = FontWeight.SemiBold)
    }
}

/**
 * 꾸밈 고르기: 가진 것 (모은 계절 조각 · 응원 · 조약돌) 이 세 칸씩. 다른 돌 곁에 있는 것은 그 이름이 적혀 있고, 고르면 이리로 옮겨 옴.
 * 돌마다 하나라서 고르면 그 돌에 있던 것은 손으로 돌아감.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdornSheet(state: AppState, stone: String, stoneName: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val p = Theme.palette
    val shown = state.adornmentsShown()
    val nameOf = { id: String -> if (id == Adornments.ME) ctx.getString(R.string.garden_haru) else state.people.firstOrNull { it.id == id }?.name.orEmpty() }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Theme.gc.paper, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).navigationBarsPadding().padding(bottom = Tokens.Space.sp8),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        ) {
            TokenText(stringResource(R.string.adorn_sheetTitle, stoneName), Tokens.TypeScale.title3.serif())
            TokenText(stringResource(R.string.adorn_sheetHelp), Tokens.TypeScale.footnote, color = p.secondary)
            state.ownedAdornments().chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                    row.forEach { item ->
                        val at = Adornments.stoneOf(shown, item)
                        val here = at == stone
                        Column(
                            Modifier.weight(1f).crayonBox(if (here) Theme.gc.chip else null, G.Radius.box, if (here) G.Stroke.box else G.Stroke.chip, 1410 + item.hashCode() % 50)
                                .clickable(role = Role.Button) {
                                    state.adorn(stone, item)
                                    state.say(ctx.getString(R.string.adorn_done, stoneName, adornName(ctx, item)))
                                    onClose()
                                }
                                .padding(Tokens.Space.sp2),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
                        ) {
                            adornArt(ctx, item)?.let { Image(it, null, Modifier.fillMaxWidth(0.8f).aspectRatio(G.Decor.cardBoxW / G.Decor.cardBoxH)) }
                            TokenText(adornName(ctx, item), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), weight = FontWeight.SemiBold, align = TextAlign.Center, maxLines = 1)
                            TokenText(if (at != null && !here) stringResource(R.string.adorn_atOther, nameOf(at)) else adornFrom(ctx, state, item),
                                Tokens.TypeScale.caption2, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center, maxLines = 1)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (shown[stone] != null) TokenText(stringResource(R.string.adorn_remove), Tokens.TypeScale.footnote,
                Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable { state.unadorn(stone); onClose() }.padding(vertical = Tokens.Space.sp3), color = p.secondary, align = TextAlign.Center)
        }
    }
}
