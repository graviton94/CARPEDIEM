package io.github.graviton94.carpediem.ui.garden

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Constellations
import io.github.graviton94.carpediem.core.Memories
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.core.Recap
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.data.Photos
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.share.RecapCards
import io.github.graviton94.carpediem.share.RecapVideo
import io.github.graviton94.carpediem.share.ShareCards
import io.github.graviton94.carpediem.ui.AlertButton
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.GardenAlert
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 달 · 해 돌아보기 보내기 (1.1.12): 추억 › 마음의 기록에서 달 · 해를 보다가 ‘그림으로 보내기’.
 * 고르기 시트 (그림 한 장 · 짧은 영상) → 그림: 미리보기 (다른 한 줄로 · 보내기) / 영상: 장면 확인 (다른 날로 · 빼기 · 날 더하기) → 만드는 중 → 보내기.
 * 무거운 마음의 줄은 저절로 고르지 않고, 사람이 직접 고르면 넣음 (core Recap).
 */
internal class RecapPlan(
    val year: Int, val month: Int?, val period: String, val title: String, val sub: String,
    val days: List<Pair<LocalDate, io.github.graviton94.carpediem.core.DayLine?>>, val all: List<Recap.Pick>, val photoDays: Set<LocalDate>,
) {
    val isMonth get() = month != null
    val cardPicks get() = if (isMonth) Recap.month(all) else Recap.yearLines(all)
    val videoPicks get() = if (isMonth) Recap.month(all) else Recap.year(all)
    val cardMax get() = if (isMonth) Recap.MONTH_MAX else Recap.YEAR_LINES
    val videoMax get() = if (isMonth) Recap.MONTH_MAX else Recap.YEAR_MAX
    val name get() = if (month != null) "month-$year-$month" else "year-$year"
}

/** 그 달 · 해의 재료 (사진이 있는지 파일을 보므로 화면 스레드 밖에서). */
internal fun recapPlan(ctx: android.content.Context, state: AppState, year: Int, month: Int?, today: LocalDate): RecapPlan {
    val from = LocalDate.of(year, month ?: 1, 1)
    val to = if (month != null) from.plusMonths(1).minusDays(1) else LocalDate.of(year, 12, 31)
    val birthdays = state.people.mapNotNull { p -> p.birth?.let { ctx.getString(R.string.recap_birthday, p.name) to it } } +
        listOfNotNull(state.profile?.birthDate?.let { ctx.getString(R.string.recap_myBirthday) to it })
    val specials = state.store.specialDays
    val inRange = state.lines.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
    val photoDays = inRange.filter { it.date != today && Photos.has(ctx, it.date) }.map { it.date }.toSet()
    val all = Recap.days(state.lines, from, to, today, { Recap.specialOf(it, specials, birthdays) }, { it in photoDays })
    val lines = ctx.getString(R.string.year_lines, "${inRange.size}")
    val photos = ctx.getString(R.string.recap_photos, "${photoDays.size}")
    val mood = Recap.mood(inRange)
    return if (month != null) {
        val period = RecordText.month(ctx, month).let { if (year == today.year) it else "${ctx.getString(R.string.recap_year, "$year")} $it" }
        val title = ctx.getString(R.string.record_monthTitle, RecordText.month(ctx, month)).let { if (year == today.year) it else "$year · $it" }
        val sub = listOf(RecordText.constellation(ctx, state, month), lines, photos.takeIf { photoDays.isNotEmpty() }).filter { !it.isNullOrBlank() }.joinToString(" · ")
        RecapPlan(year, month, period, title, sub, Constellations.monthDays(state.lines, year, month), all, photoDays)
    } else {
        val sub = listOfNotNull(lines, photos.takeIf { photoDays.isNotEmpty() }, mood?.let { ctx.getString(R.string.recap_mood, Labels.feeling(ctx, it)) }).joinToString(" · ")
        RecapPlan(year, null, ctx.getString(R.string.recap_year, "$year"), ctx.getString(R.string.year_title, "$year"), sub, Lines.yearDays(state.lines, year), all, photoDays)
    }
}

/** 고른 날 이름: (특별한 날 · ) 9월 14일 · 고마움. */
internal fun recapDayLabel(ctx: android.content.Context, p: Recap.Pick): String =
    listOfNotNull(p.special, RecordText.day(ctx, p.date), p.line.feeling?.let { Labels.feeling(ctx, it) }).joinToString(" · ")

/** 카드 · 영상 한 벌의 재료: 고른 날의 사진까지 (화면 스레드 밖에서). */
internal fun recapArt(ctx: android.content.Context, state: AppState, plan: RecapPlan, picks: List<Recap.Pick>, night: Boolean, today: LocalDate): RecapCards.Art {
    val photos = picks.filter { it.photo }.mapNotNull { p -> runCatching { Photos.weathered(ctx, p.date, today) }.getOrNull()?.let { p.date to it } }.toMap()
    val inRange = plan.days.mapNotNull { it.second }
    val mood = Recap.mood(inRange)
    val specials = plan.all.filter { it.special != null }.groupBy { it.date.monthValue }.mapValues { it.value.first().special!! }
    return RecapCards.Art(ctx, plan.year, plan.month, plan.period, plan.title, plan.sub, plan.days, state.store.constellations, state.store.haruSeed, state.store.haruSeed,
        night, today, mood, mood?.let { Labels.feeling(ctx, it) }, picks, photos, specials) { recapDayLabel(ctx, it) }
}

private enum class RecapStep { CHOOSE, CARD, PICK_CARD, PICK_VIDEO, MAKING }

/** 달 · 해 보내기 전체 (고르기 시트부터). month = null 이면 한 해. */
@Composable
internal fun RecapShare(state: AppState, year: Int, month: Int?, today: LocalDate, night: Boolean, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val plan by produceState<RecapPlan?>(null, year, month, state.lines) { value = withContext(Dispatchers.IO) { recapPlan(ctx, state, year, month, today) } }
    val pl = plan ?: return
    var step by remember { mutableStateOf(RecapStep.CHOOSE) }
    var cardPicks by remember(pl) { mutableStateOf(pl.cardPicks) }
    var videoPicks by remember(pl) { mutableStateOf(pl.videoPicks) }
    when (step) {
        RecapStep.CHOOSE -> GardenAlert(
            onDismissRequest = onDismiss,
            title = { androidx.compose.material3.Text(stringResource(R.string.recap_sendTitle, pl.title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                    RecapOption(stringResource(R.string.recap_card), stringResource(R.string.recap_cardSub), 1301) { step = RecapStep.CARD }
                    RecapOption(stringResource(R.string.recap_video, "${RecapVideo.seconds(pl.isMonth, videoPicks.size)}"),
                        stringResource(if (pl.isMonth) R.string.recap_videoMonthSub else R.string.recap_videoYearSub), 1303) { step = RecapStep.PICK_VIDEO }
                }
            },
            confirmButton = { AlertButton(stringResource(R.string.cancel), onDismiss, quiet = true) },
        )
        RecapStep.CARD -> RecapCardPreview(state, pl, cardPicks, night, today, onOther = { step = RecapStep.PICK_CARD }, onDismiss = onDismiss)
        RecapStep.PICK_CARD -> RecapPicker(pl, cardPicks, pl.cardMax, video = false, today = today, onChange = { cardPicks = it }, onBack = { step = RecapStep.CARD }, onGo = { step = RecapStep.CARD })
        RecapStep.PICK_VIDEO -> RecapPicker(pl, videoPicks, pl.videoMax, video = true, today = today, onChange = { videoPicks = it }, onBack = { step = RecapStep.CHOOSE }, onGo = { step = RecapStep.MAKING })
        RecapStep.MAKING -> RecapMaking(state, pl, videoPicks, night, today, onDone = onDismiss, onStop = { step = RecapStep.PICK_VIDEO })
    }
}

/** 고르기 시트의 한 칸: 굵은 이름과 아래 작은 설명. */
@Composable
private fun RecapOption(title: String, sub: String, seed: Int, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, seed).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
        .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
        TokenText(title, Tokens.TypeScale.headline)
        TokenText(sub, Tokens.TypeScale.footnote, color = Theme.palette.secondary)
    }
}

/** 그림 한 장 미리보기: 다른 한 줄로 · 보내기. */
@Composable
private fun RecapCardPreview(state: AppState, pl: RecapPlan, picks: List<Recap.Pick>, night: Boolean, today: LocalDate, onOther: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(null, picks) {
        value = withContext(Dispatchers.Default) { runCatching { recapArt(ctx, state, pl, picks, night, today).let { if (pl.isMonth) RecapCards.month(it) else RecapCards.year(it) } }.getOrNull() }
    }
    GardenAlert(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(pl.title + " · " + stringResource(R.string.recap_card)) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val m = Modifier.heightIn(max = 460.dp).aspectRatio(RecapCards.W / RecapCards.H.toFloat())
                bmp?.let { Image(it.asImageBitmap(), pl.title, m) } ?: Box(m, contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(Theme.unit * 28), color = Theme.palette.olive, strokeWidth = Theme.unit * 2)
                }
            }
        },
        dismissButton = { if (pl.all.isNotEmpty()) AlertButton(stringResource(R.string.recap_otherLine), onOther, quiet = true) },
        confirmButton = { AlertButton(stringResource(R.string.recap_send), { bmp?.let { ShareCards.send(ctx, it, pl.name) }; onDismiss() }, enabled = bmp != null) },
    )
}

/**
 * 장면 확인 (영상) · 한 줄 고르기 (그림): 고른 날마다 다른 날로 · 빼기, 모자라면 날 더하기.
 * 영상이면 위아래에 고정 장면 (질문 · 피는 정원 / 카드 · 인사) 과 길이, 아래 ‘약 n초 · 만들기’.
 */
@Composable
private fun RecapPicker(pl: RecapPlan, picks: List<Recap.Pick>, max: Int, video: Boolean, today: LocalDate, onChange: (List<Recap.Pick>) -> Unit, onBack: () -> Unit, onGo: () -> Unit) {
    val p = Theme.palette
    var choosing by remember { mutableStateOf<Recap.Pick?>(null) }
    var adding by remember { mutableStateOf(false) }
    Dialog(onBack, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(Theme.gc.paper).statusBarsPadding().navigationBarsPadding().padding(horizontal = Theme.deviceClass.pageMargin)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = Tokens.Space.sp4), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                TokenText(if (video) stringResource(R.string.recap_makeTitle, pl.period) else stringResource(R.string.recap_chooseDay), Tokens.TypeScale.title3.serif())
                TokenText(if (video && !pl.isMonth) stringResource(R.string.recap_makeYearSub, "$max") else stringResource(R.string.recap_makeSub), Tokens.TypeScale.footnote, color = p.secondary)
                val introSec = RecapVideo.scenes(pl.isMonth, emptyList())
                if (video) {
                    FixedRow(stringResource(R.string.recap_intro, pl.period).replace('\n', ' '), stringResource(R.string.recap_sec, "${introSec[0].sec.toInt()}"))
                    FixedRow(if (pl.isMonth) stringResource(R.string.recap_bloomRow, "${pl.days.count { it.second != null }}") else stringResource(R.string.recap_monthsRow),
                        stringResource(R.string.recap_sec, "${introSec[1].sec.toInt()}"))
                }
                TokenText(stringResource(R.string.recap_daysHead, "$max"), Tokens.TypeScale.subhead, color = p.secondary, weight = FontWeight.SemiBold)
                var lastSeason: Season? = null
                picks.forEach { pick ->
                    val s = Memories.seasonOf(pick.date)
                    if (!pl.isMonth && s != lastSeason) { TokenText(seasonName(s), Tokens.TypeScale.footnote, color = p.secondary); lastSeason = s }
                    PickRow(pl, pick, today, onOther = { choosing = pick }, onDrop = { onChange(picks - pick) })
                }
                if (picks.size < max && pl.all.any { a -> picks.none { it.date == a.date } })
                    Box(Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).crayonBox(null, G.Radius.box, G.Stroke.chip, 1311).clickable { adding = true }, contentAlignment = Alignment.Center) {
                        TokenText(stringResource(R.string.recap_addDay), Tokens.TypeScale.subhead, color = p.secondary)
                    }
                if (video) FixedRow(stringResource(R.string.recap_endRow), stringResource(R.string.recap_sec, "${(introSec[2].sec + introSec[3].sec).let { if (it % 1f == 0f) "${it.toInt()}" else "$it" }}"))
            }
            Row(Modifier.fillMaxWidth().padding(vertical = Tokens.Space.sp3), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically) {
                AlertButton(stringResource(R.string.cancel), onBack, quiet = true)
                GardenButton(if (video) stringResource(R.string.recap_make, "${RecapVideo.seconds(pl.isMonth, picks.size)}") else stringResource(R.string.done), onGo, filled = true, seed = 1313, modifier = Modifier.weight(1f))
            }
        }
    }
    // 날 고르기는 장면 확인 위에 (나중에 연 창이 위)
    if (choosing != null || adding) RecapDayChooser(pl, picks, onPick = { np ->
        val old = choosing
        onChange((if (old != null) picks.map { if (it == old) np else it } else picks + np).distinctBy { it.date }.sortedBy { it.date })
        choosing = null; adding = false
    }, onDismiss = { choosing = null; adding = false })
}

@Composable
private fun seasonName(s: Season) = stringResource(when (s) { Season.SPRING -> R.string.season_spring; Season.SUMMER -> R.string.season_summer; Season.AUTUMN -> R.string.season_autumn; Season.WINTER -> R.string.season_winter })

/** 바꿀 수 없는 장면 한 줄 (옅은 테두리, 오른쪽에 길이). */
@Composable
private fun FixedRow(text: String, sec: String) {
    Row(Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip * 0.6f, 1307).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3), verticalAlignment = Alignment.CenterVertically) {
        TokenText(text, Tokens.TypeScale.callout.serif(), Modifier.weight(1f), color = Theme.palette.secondary, maxLines = 2)
        TokenText(sec, Tokens.TypeScale.footnote, color = Theme.palette.secondary)
    }
}

/** 고른 날 한 칸: 사진 (없으면 ‘한 줄’ 자리), 날 이름, 한 줄, 다른 날로 · 빼기. */
@Composable
private fun PickRow(pl: RecapPlan, pick: Recap.Pick, today: LocalDate, onOther: () -> Unit, onDrop: () -> Unit) {
    val ctx = LocalContext.current
    val p = Theme.palette
    val thumb by produceState<Bitmap?>(null, pick.date) { if (pick.photo) value = withContext(Dispatchers.IO) { runCatching { Photos.weathered(ctx, pick.date, today) }.getOrNull() } }
    Row(Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, 1309 + pick.date.dayOfYear).padding(Tokens.Space.sp3), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        Box(Modifier.size(Theme.unit * 64).clip(androidx.compose.foundation.shape.RoundedCornerShape(Theme.unit * 6)).background(Theme.gc.chip), contentAlignment = Alignment.Center) {
            thumb?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                ?: TokenText("“ ”", Tokens.TypeScale.footnote, color = p.secondary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
            TokenText(recapDayLabel(ctx, pick), Tokens.TypeScale.caption1, color = if (pick.special != null) p.olive else p.secondary, weight = FontWeight.SemiBold)
            TokenText("“${RecapCards.short(pick.line.text, 60)}”", Tokens.TypeScale.callout.serif(), maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                if (pl.all.size > 1) GardenChip(stringResource(R.string.recap_otherDay), false, 1315, onOther)
                GardenChip(stringResource(R.string.recap_drop), false, 1317, onDrop)
            }
        }
    }
}

/** 날 고르기: 그 달 · 해의 지난 날 (무거운 마음도, 사람이 고르는 것이니), 이미 고른 날은 빼고. */
@Composable
private fun RecapDayChooser(pl: RecapPlan, picks: List<Recap.Pick>, onPick: (Recap.Pick) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val rest = remember(pl, picks) { pl.all.filter { a -> picks.none { it.date == a.date } }.sortedByDescending { it.date } }
    GardenAlert(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(stringResource(R.string.recap_chooseDay)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(rest, key = { it.date.toEpochDay() }) { a ->
                    Column(Modifier.fillMaxWidth().clickable { onPick(a) }.padding(vertical = Tokens.Space.sp2)) {
                        TokenText(recapDayLabel(ctx, a), Tokens.TypeScale.caption1, color = Theme.palette.secondary)
                        TokenText(RecapCards.short(a.line.text, 60), Tokens.TypeScale.callout.serif(), maxLines = 2)
                    }
                }
            }
        },
        confirmButton = { AlertButton(stringResource(R.string.cancel), onDismiss, quiet = true) },
    )
}

/** 만드는 중: 하루 · ‘하루가 9월의 정원을 엮고 있어요’ · 진행 막대 · 그만두기. 다 되면 보내기 창. */
@Composable
private fun RecapMaking(state: AppState, pl: RecapPlan, picks: List<Recap.Pick>, night: Boolean, today: LocalDate, onDone: () -> Unit, onStop: () -> Unit) {
    val ctx = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    val failed = stringResource(R.string.recap_failed)
    LaunchedEffect(Unit) {
        val r = withContext(Dispatchers.Default) {
            runCatching {
                val art = recapArt(ctx, state, pl, picks, night, today)
                // 마지막 카드는 해 카드의 한 줄 셋 · 달 카드의 사진을 그대로 (영상에 고른 날로)
                val card = if (pl.isMonth) RecapCards.month(art) else RecapCards.year(recapArt(ctx, state, pl, Recap.yearLines(picks), night, today))
                RecapVideo.make(art, card, RecapVideo.scenes(pl.isMonth, picks), pl.name) { f -> progress = f }
            }
        }
        r.onSuccess { ShareCards.sendVideo(ctx, it); onDone() }
            .onFailure { e -> if (e is kotlinx.coroutines.CancellationException) throw e; android.util.Log.e("Recap", "video", e); state.say(failed); onStop() }
    }
    Dialog(onStop, DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Column(Modifier.fillMaxSize().background(Theme.gc.paper).statusBarsPadding().navigationBarsPadding().padding(Theme.deviceClass.pageMargin),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Spacer(Modifier.weight(1f))
            val art = remember { HaruArt.of(state.store.haruSeed, false) }
            Canvas(Modifier.size(Theme.unit * 120)) {
                val k = size.width * 0.8f / art.meta.bbox.width
                translate(size.width / 2f - art.meta.bbox.center.x * k, size.height * 0.9f - art.meta.ground * k) { drawHaru(art, k, lid = 1f, look = Offset.Zero) }
            }
            Spacer(Modifier.height(Tokens.Space.sp4))
            TokenText(stringResource(R.string.recap_making, pl.title), Tokens.TypeScale.headline.serif(), align = TextAlign.Center)
            TokenText(stringResource(R.string.recap_makingSub), Tokens.TypeScale.footnote, color = Theme.palette.secondary, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp4))
            Box(Modifier.width(Theme.unit * 240).height(Theme.unit * 6).clip(androidx.compose.foundation.shape.RoundedCornerShape(Theme.unit * 3)).background(Theme.gc.chip)) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0.02f, 1f)).height(Theme.unit * 6).background(Theme.gc.ink))
            }
            Spacer(Modifier.weight(1f))
            AlertButton(stringResource(R.string.recap_stop), onStop, quiet = true)
        }
    }
}
