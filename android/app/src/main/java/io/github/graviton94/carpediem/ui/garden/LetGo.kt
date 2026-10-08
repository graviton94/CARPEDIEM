package io.github.graviton94.carpediem.ui.garden

import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import kotlinx.coroutines.launch
import io.github.graviton94.carpediem.ui.pop
import io.github.graviton94.carpediem.ui.modalBox
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.LinearEasing
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import java.time.format.FormatStyle
import java.time.format.DateTimeFormatter
import io.github.graviton94.carpediem.core.Lines
import io.github.graviton94.carpediem.core.DayLine
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Feeling
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Care
import io.github.graviton94.carpediem.ui.TokenText
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

internal fun feelingName(f: Feeling) = when (f) {
    Feeling.JOY -> R.string.feeling_joy; Feeling.HOPE -> R.string.feeling_hope; Feeling.CALM -> R.string.feeling_calm; Feeling.THANKS -> R.string.feeling_thanks
    Feeling.DISAPPOINT -> R.string.feeling_disappoint; Feeling.SAD -> R.string.feeling_sad; Feeling.WORRY -> R.string.feeling_worry
}


// ───── 정원(크레용) · 유리 두 디자인에서 함께 쓰는 작은 부품 ─────

/** 상자: 정원은 크레용 선, 유리는 옅은 유리판. strong = 조금 더 눈에 띄게 (돌아온 한 줄). */
@Composable
private fun Modifier.lineBox(seed: Int, strong: Boolean = false, pill: Boolean = false): Modifier {
    if (Theme.garden) return crayonBox(if (strong) Theme.gc.chip else Theme.gc.paper, if (pill) G.Radius.chip else G.Radius.box, G.Stroke.chip, seed)
    val p = Theme.palette
    val shape = RoundedCornerShape(if (pill) Tokens.Radius.pill else Tokens.Radius.md)
    return clip(shape).background(if (strong) p.olive.copy(alpha = 0.14f) else p.glass).border(Tokens.Stroke.line, p.glassEdge, shape)
}

@Composable
private fun Chip(text: String, selected: Boolean, seed: Int, onClick: () -> Unit) {
    if (Theme.garden) { GardenChip(text, selected, seed, onClick); return }
    val p = Theme.palette
    Box(Modifier.heightIn(min = Tokens.Layout.tapTarget), contentAlignment = Alignment.Center) {
        Box(Modifier.clip(RoundedCornerShape(Tokens.Radius.pill)).background(if (selected) p.olive else p.dim).clickable(onClick = onClick)
            .padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1)) {
            TokenText(text, Tokens.TypeScale.subhead, color = if (selected) p.onOlive else p.foreground, weight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun Action(text: String, filled: Boolean, seed: Int, onClick: () -> Unit) {
    if (Theme.garden) { GardenButton(text, onClick, filled = filled, seed = seed); return }
    val p = Theme.palette
    Box(Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp2).clip(RoundedCornerShape(Tokens.Radius.pill))
        .background(if (filled) p.olive else p.dim).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        TokenText(text, Tokens.TypeScale.headline, color = if (filled) p.onOlive else p.foreground)
    }
}

/** 깃털 그림 (정원만). 유리 디자인은 그림 없이 글만. */
@Composable
private fun Feather(size: Dp) { if (Theme.garden) Image(GardenArt.obj(LocalContext.current, "feather"), null, Modifier.size(size)) }

/** 오늘의 한 줄에서 글꼴: 정원은 명조(세리프), 유리는 기본 글꼴로 조금 더 모던하게. */
private fun lineType(t: io.github.graviton94.carpediem.design.TypeToken, garden: Boolean) = if (garden) t.serif() else t

/**
 * 오늘의 한 줄: 기쁨도 슬픔도 한 줄에 실어 떠나보낸다. 하루에 한 번.
 * 보내면 글이 깃털에 실려 하늘로 올라가며 옅어지고, 그 뒤로는 오늘 쓴 글을 다시 보여 주지 않는다 (기기 안에만 남음).
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LetGoSection(state: AppState, today: LocalDate, modifier: Modifier = Modifier, guide: GuideTargets? = null) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val px = with(LocalDensity.current) { u.toPx() }
    val focus = LocalFocusManager.current
    val max = G.LetGo.maxChars.toInt()
    // 쓰던 글 · 고른 마음은 state 에 (알림 · 위젯으로 정원이 다시 그려져도 남게)
    var text by state.draftText
    var feeling by state.draftFeeling
    // 쓰는 동안 잠깐 멈출 때마다 폰에 적어 둠 (보내면 빈 글로)
    LaunchedEffect(Unit) { snapshotFlow { text }.collectLatest { t -> kotlinx.coroutines.delay(400); state.store.saveDraft((state.fixedNow ?: java.time.LocalDateTime.now()).toLocalDate(), t) } }
    // 누구에게 (선택): 생일인 사람이 있으면 먼저 골라 둠
    val birthdayId = state.people.firstOrNull { io.github.graviton94.carpediem.core.Family.isBirthday(it.birth, today) }?.id
    var to by rememberSaveable(birthdayId) { mutableStateOf(birthdayId) }
    var flying by remember { mutableStateOf<String?>(null) }
    val fly = remember { Animatable(0f) }
    LaunchedEffect(flying) {
        if (flying == null) return@LaunchedEffect
        fly.snapTo(0f); fly.animateTo(1f, tween(G.Motion.letGoMs.toInt(), easing = LinearOutSlowInEasing)); flying = null
    }
    // 다른 날의 한 줄: 고른 지난 날 (null = 오늘). 그날엔 질문 · 돌아온 한 줄 없이 쓰기만
    val day = state.writeDay
    var picking by remember { mutableStateOf(false) }
    val sent = if (day == null) state.sentOn(today) else false
    // 오늘 보낸 한 줄 고치기 (그날 안에만) · 지우기
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // 고치는 중에 오늘의 사진을 빼기로 했는지 (저장할 때 반영, 그만두면 그대로)
    var dropPhoto by rememberSaveable { mutableStateOf(false) }
    fun stopEditing() {
        editing = false; text = ""; feeling = null; dropPhoto = false
        if (state.draftPhoto) { io.github.graviton94.carpediem.data.Photos.dropPending(ctx); state.draftPhoto = false; state.photoKick++ }
    }
    // 고치는 중에 날이 바뀌었거나 다른 날로 가면 고치기를 그만두고 칸을 비움 (어제 글이 오늘 칸에 남지 않게)
    if (editing && (!sent || day != null)) stopEditing()
    // 이미 보낸 날엔 ‘바로 쓰기’ 표시를 지움 (다음에 칸이 열릴 때 갑자기 키보드가 뜨지 않게)
    if (sent && !editing && state.focusWrite) state.focusWrite = false
    // 쓰는 칸에 커서가 있을 때 뒤로 가기: 먼저 키보드만 내림
    var boxFocused by remember { mutableStateOf(false) }
    BackHandler(enabled = boxFocused) { focus.clearFocus() }
    val formView = remember { BringIntoViewRequester() }
    val focusBox = remember { androidx.compose.ui.focus.FocusRequester() }
    val scope = rememberCoroutineScope()
    // 한 줄에 사진 한 장 (11): 쓰는 중에 골라 두면 보낼 때 그날의 사진으로 · 그날 안에는 고치기에서 바꾸거나 뺄 수 있음
    val photoFail = stringResource(R.string.photo_fail)
    val pickDraft = rememberPhotoPicker { uri -> scope.launch {
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { io.github.graviton94.carpediem.data.Photos.importPending(ctx, uri) }
        if (ok) { state.draftPhoto = true; state.photoKick++ } else state.say(photoFail)
    } }
    fun send() {
        // 비어 있으면 그냥 지나치지 않고: 한 줄을 먼저 적어 달라고 말하고 쓰는 칸으로
        if (text.isBlank()) { state.say(ctx.getString(R.string.letgo_empty)); runCatching { focusBox.requestFocus() }; return }
        // 남기지 못했으면 (자정이 지나 고칠 수 없거나 그날이 이미 찼으면) 쓰던 글은 그대로
        if (editing) {
            if (state.editToday(text, feeling, today)) {
                // 사진: 새로 골랐으면 바꾸고, 빼기로 했으면 뺌
                if (state.draftPhoto) state.commitPhoto(today)
                else if (dropPhoto) { io.github.graviton94.carpediem.data.Photos.remove(ctx, today); state.photoKick++ }
                editing = false; text = ""; feeling = null; dropPhoto = false; focus.clearFocus()
            }
            return
        }
        val who = to?.takeIf { id -> state.people.any { it.id == id } }
        // 날은 한 번만 정해 글과 사진에 함께 (자정 무렵 보내도 둘이 다른 날로 갈라지지 않게)
        val on = day ?: (state.fixedNow ?: java.time.LocalDateTime.now()).toLocalDate()
        if (day != null) { if (!state.letGoOn(day, text, feeling, who)) return } else state.letGo(text, feeling, who, on)
        flying = text.trim()
        state.commitPhoto(on)
        text = ""; feeling = null; focus.clearFocus()
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
        TokenText(if (day == null) stringResource(R.string.letgo_title) else stringResource(R.string.letgo_dayTitle, RecordText.day(ctx, day)), Tokens.TypeScale.title3)
        // 오늘 | 다른 날 (기본은 늘 오늘)
        if (Theme.garden && state.profile != null) Row(Modifier.guideTarget(guide, "write.days"), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Chip(stringResource(R.string.letgo_today), day == null, seed = 950) { state.writeDay = null }
            Chip(day?.let { RecordText.day(ctx, it) } ?: stringResource(R.string.letgo_other), day != null, seed = 951) { picking = true }
        }
        // 때에 맞는 소개 (아침 · 저녁 · 밤), 낮은 원래 말
        val sub = remember(today) { io.github.graviton94.carpediem.ui.Labels.timed(ctx, "letgo_sub", io.github.graviton94.carpediem.ui.Labels.part(state.fixedNow ?: java.time.LocalDateTime.now())) }
        TokenText(sub ?: stringResource(R.string.letgo_sub), lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary)
        // 몇 해 전 오늘 보낸 한 줄: 먼저 조용히 알리고, 누르면 펼친다
        val recalls = remember(state.lines, today, day) { if (day != null) emptyList() else Lines.yearsAgo(state.lines, today) }
        recalls.forEach { (years, l) -> RecallCard(state, today, stringResource(R.string.recall_title, "$years"), l, 990 + years) }
        // 정한 주기 없이 문득 찾아온 지난 한 줄
        state.randomLine?.takeIf { r -> day == null && recalls.none { it.second.date == r.date } }?.let { r ->
            RecallCard(state, today, stringResource(R.string.recall_random, r.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))), r, 996)
        }
        val line = flying
        when {
            // 떠나보내는 중: 깃털과 함께 위로, 옆으로 살짝 흔들리며 옅어진다
            line != null -> Box(Modifier.fillMaxWidth().heightIn(min = u * G.LetGo.rise * 0.6f), contentAlignment = Alignment.BottomCenter) {
                Row(
                    Modifier.graphicsLayer {
                        val f = fly.value
                        translationY = -G.LetGo.rise * px * f
                        translationX = G.LetGo.drift * px * sin(f * PI * 1.5).toFloat()
                        rotationZ = -6f * f; alpha = (1f - f) * (1f - f)
                    }.lineBox(960, pill = true).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp2),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
                ) {
                    Feather(u * G.LetGo.feather)
                    TokenText(line, lineType(Tokens.TypeScale.callout, Theme.garden), maxLines = 2)
                }
            }
            // 오늘은 이미 보냄 (고치는 중이 아니면)
            sent && !editing -> {
                val shown = remember { Animatable(0f) }
                LaunchedEffect(Unit) { shown.animateTo(1f, tween(G.Motion.pageMs.toInt())) }
                // 이미 남긴 날에도 (?) 안내가 비출 자리가 있게: 쓰는 칸 대신 남긴 한 줄 묶음
                Column(Modifier.fillMaxWidth().guideTarget(guide, "write.box").graphicsLayer { alpha = shown.value }, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                        Feather(u * G.LetGo.feather)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                            TokenText(stringResource(R.string.letgo_done), Tokens.TypeScale.subhead)
                        }
                    }
                    // 남긴 뒤의 글은 짧게: 몇 번 남겼는지 (세는 숫자) · ‘아래 기록에 더해졌어요’ 같은 설명은 두지 않음
                    // 그날 안에는 고치거나 지울 수 있음 (조용히, 작게)
                    val mine = state.lines.lastOrNull { it.date == today }
                    // 오늘 남긴 한 줄 그대로 (무엇을 남겼는지 보이게)
                    if (mine != null && state.keepLines && mine.text.isNotBlank())
                        TokenText("“${mine.text}”", lineType(Tokens.TypeScale.callout, Theme.garden), Modifier.fillMaxWidth().lineBox(962).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3))
                    if (mine != null && state.keepLines && mine.text.isNotBlank()) TokenText(stringResource(R.string.edit_until), Tokens.TypeScale.caption1, color = p.secondary)
                    if (mine != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.keepLines && mine.text.isNotBlank()) {
                            TokenText(stringResource(R.string.edit_action), Tokens.TypeScale.footnote,
                                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { text = mine.text; feeling = mine.feeling; editing = true }.padding(end = Tokens.Space.sp3, top = Tokens.Space.sp3, bottom = Tokens.Space.sp3),
                                color = p.olive, weight = FontWeight.SemiBold)
                            TokenText("·", Tokens.TypeScale.footnote, color = p.secondary)
                        }
                        TokenText(stringResource(R.string.edit_delete), Tokens.TypeScale.footnote,
                            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { confirmDelete = true }.padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp3), color = p.secondary)
                    }
                    // 오늘의 사진 (11): 붙였으면 한지 액자로 (보낸 뒤에 붙이라고 권하지는 않음, 쓰는 칸 옆 ‘사진 한 장’으로)
                    if (Theme.garden && state.keepLines && mine != null && mine.text.isNotBlank()) {
                        val hasPhoto = remember(state.photoKick, today) { io.github.graviton94.carpediem.data.Photos.has(ctx, today) }
                        if (hasPhoto) WeatheredPhoto(state, today, today, u * 170f, modifier = Modifier.padding(vertical = Tokens.Space.sp2))

                    }
                    // 아침에 심은 씨앗 (04): 저녁 · 밤이면 ‘싹이 텄나요?’
                    if (Theme.garden && state.profile != null) SeedAsk(state, today, state.fixedNow ?: java.time.LocalDateTime.now())
                }
            }
            else -> Column(Modifier.fillMaxWidth().bringIntoViewRequester(formView), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                // 돌아온 한 줄에 이어 쓰는 중이면 (12): 입력칸 위에 그날의 한 줄
                state.recallReply?.takeIf { day == null && state.answering == null }?.let { r ->
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        TokenText(stringResource(R.string.recall_continueLabel, r.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))), Tokens.TypeScale.caption1, color = p.secondary)
                        TokenText("“${r.text.replace('\n', ' ')}”", lineType(Tokens.TypeScale.subhead, Theme.garden), maxLines = 3)
                    }
                }
                // 질문에 답하는 중이면 입력칸 위에 질문 한 줄
                state.answering?.takeIf { day == null }?.let { q ->
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        TokenText(stringResource(R.string.question_label), Tokens.TypeScale.caption1, color = p.secondary)
                        TokenText(io.github.graviton94.carpediem.data.Words.main(q, state.quoteLanguage, io.github.graviton94.carpediem.data.Words.lang(ctx)), lineType(Tokens.TypeScale.headline, Theme.garden))
                    }
                }
                TokenText(stringResource(R.string.letgo_feeling), Tokens.TypeScale.footnote, color = p.secondary)
                FlowRow(Modifier.guideTarget(guide, "write.feeling"), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    Feeling.entries.forEachIndexed { i, f ->
                        Chip(stringResource(feelingName(f)), feeling == f, seed = 970 + i) { feeling = if (feeling == f) null else f }
                    }
                }
                if (Theme.garden && state.people.isNotEmpty() && !editing) {
                    TokenText(stringResource(R.string.letgo_to), Tokens.TypeScale.footnote, color = p.secondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        state.people.forEachIndexed { i, person -> Chip(person.name, to == person.id, seed = 980 + i) { to = if (to == person.id) null else person.id } }
                    }
                }
                // 쓰는 중에는 기본 글꼴 (글자를 칠 때마다 글꼴이 바뀌지 않게)
                val style = Tokens.TypeScale.callout.style().copy(color = p.foreground)
                // 이름은 ‘한 줄’이지만 Enter 로 줄을 나눌 수 있음 (최대 Lines.MAX_LINES 줄). 보내기는 아래 버튼으로
                BasicTextField(
                    value = text,
                    onValueChange = { v -> if (v.codePointCount(0, v.length) <= max && v.count { it == '\n' } < Lines.MAX_LINES) text = v },
                    singleLine = false, minLines = 2, maxLines = Lines.MAX_LINES, textStyle = style, cursorBrush = SolidColor(p.foreground),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    // 키보드가 올라온 뒤 입력칸 · 보내기 버튼이 보이게 끌어올린다
                    modifier = Modifier.fillMaxWidth().guideTarget(guide, "write.box").focusRequester(focusBox).onFocusEvent { f -> boxFocused = f.isFocused; if (f.isFocused) scope.launch { delay(G.Motion.keyboardMs.toLong()); formView.bringIntoView() } }
                        .lineBox(964).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (text.isEmpty()) TokenText(stringResource(if (day == null) R.string.letgo_hint else R.string.letgo_dayHint), Tokens.TypeScale.callout, color = p.secondary)
                            inner()
                        }
                    },
                )
                // 위젯 · 둘러보기에서 ‘한 줄 쓰러’ 왔으면 쓰는 칸에 바로 (페이지 둘러보기가 끝난 뒤)
                LaunchedEffect(state.focusWrite, state.touring) {
                    if (state.focusWrite && !state.touring) { delay(G.Motion.pageMs.toLong()); runCatching { focusBox.requestFocus() }; state.focusWrite = false }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // 사진 한 장 (11): 기록을 남길 때만 (끄면 글처럼 사진도 남기지 않음)
                    if (Theme.garden && state.keepLines) {
                        // 고치는 중: 오늘 붙인 사진이 있으면 바꾸거나 뺄 수 있음 (저장할 때 반영)
                        val hasToday = remember(state.photoKick, today) { io.github.graviton94.carpediem.data.Photos.has(ctx, today) }
                        val kept = editing && !dropPhoto && hasToday
                        if (kept && !state.draftPhoto) {
                            WeatheredPhoto(state, today, today, u * 56f, dated = false)
                            TokenText(stringResource(R.string.photo_change), Tokens.TypeScale.footnote,
                                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { pickDraft() }.padding(Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold)
                            TokenText(stringResource(R.string.photo_remove), Tokens.TypeScale.footnote,
                                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { dropPhoto = true }.padding(vertical = Tokens.Space.sp3), color = p.secondary)
                        } else if (state.draftPhoto) {
                            WeatheredPhoto(state, day ?: today, today, u * 56f, pending = true, dated = false)
                            TokenText(stringResource(R.string.photo_remove), Tokens.TypeScale.footnote,
                                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { io.github.graviton94.carpediem.data.Photos.dropPending(ctx); state.draftPhoto = false }.padding(Tokens.Space.sp3), color = p.secondary)
                        } else GardenChip(stringResource(R.string.photo_add), false, 969) { pickDraft() }
                    }
                    Spacer(Modifier.weight(1f))
                    // 글자 수는 끝에 가까워질 때만 (늘 숫자가 보이면 재촉하는 것 같아서)
                    val count = text.codePointCount(0, text.length)
                    if (count >= max * 4 / 5) TokenText("$count / $max", Tokens.TypeScale.caption1, color = p.secondary, align = TextAlign.End)
                }
                Action(stringResource(when { editing -> R.string.edit_save; day == null -> R.string.letgo_send; else -> R.string.letgo_daySend }), filled = text.isNotBlank(), seed = 968) { send() }
                if (editing) TokenText(stringResource(R.string.cancel), Tokens.TypeScale.footnote,
                    Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable { stopEditing(); focus.clearFocus() }.padding(vertical = Tokens.Space.sp3), color = p.secondary, align = TextAlign.Center)
                if (day != null) TokenText(stringResource(R.string.letgo_backToday), Tokens.TypeScale.footnote,
                    Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable { state.writeDay = null }.padding(vertical = Tokens.Space.sp3), color = p.secondary, align = TextAlign.Center)
            }
        }
        val yesterday = today.minusDays(1)
        if (day == null && line == null && state.store.startDate.isBefore(today) && state.canWriteOn(yesterday, today) && state.lines.none { it.date == yesterday })
            TokenText(stringResource(R.string.letgo_yesterday), Tokens.TypeScale.footnote,
                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { state.writeDay = yesterday }.padding(vertical = Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold)
        // 방금 지운 한 줄 되돌리기 (앱을 켜 둔 동안)
        if (day == null && state.lastDeleted?.date == today && !sent)
            TokenText(stringResource(R.string.edit_undo), Tokens.TypeScale.footnote,
                Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { state.undoDelete() }.padding(vertical = Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold)
        // 남긴 줄이 어디에 남는지: 정원은 아래 마음의 기록, 유리 디자인은 이 폰에, 기록 남기기를 끄면 날짜만
        TokenText(stringResource(when { !state.keepLines -> R.string.letgo_privacyOff; !Theme.garden -> R.string.letgo_privacyPlain; else -> R.string.letgo_privacy }), Tokens.TypeScale.footnote, color = p.secondary)
    }
    if (confirmDelete) io.github.graviton94.carpediem.ui.GardenAlert(
        onDismissRequest = { confirmDelete = false },
        title = { androidx.compose.material3.Text(stringResource(R.string.edit_deleteAsk)) },
        text = { androidx.compose.material3.Text(stringResource(R.string.edit_deleteHelp)) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false; state.deleteLine(today) }) { androidx.compose.material3.Text(stringResource(R.string.edit_delete), color = p.danger) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { androidx.compose.material3.Text(stringResource(R.string.cancel)) } },
    )
    // 날짜 고르기: 생일부터 어제까지, 이미 한 줄이 있는 날은 고를 수 없음 (하루에 한 줄)
    if (picking) {
        val zone = java.time.ZoneOffset.UTC
        val birth = state.profile?.birthDate ?: today
        val taken = remember(state.lines) { state.lines.map { it.date }.toSet() }
        val dp = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = (day ?: today.minusDays(1)).atStartOfDay().toInstant(zone).toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(zone).toLocalDate()
                    return d.isBefore(today) && !d.isBefore(birth) && d !in taken
                }
                override fun isSelectableYear(year: Int): Boolean = year in birth.year..today.year
            })
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms -> val d = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate(); if (state.canWriteOn(d, today)) state.writeDay = d }
                    picking = false
                }) { androidx.compose.material3.Text(stringResource(R.string.done)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { picking = false }) { androidx.compose.material3.Text(stringResource(R.string.cancel)) } },
        ) { androidx.compose.material3.DatePicker(state = dp) }
    }
}

/** 돌아온 한 줄 (몇 해 전 오늘 · 문득). 처음엔 접혀 있고, 펼쳐 읽은 뒤 ‘다시 보내기’로 오늘은 접어 둔다. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RecallCard(state: AppState, today: LocalDate, title: String, line: DayLine, seed: Int) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    var opened by rememberSaveable(line.date) { mutableStateOf(false) }
    var gone by rememberSaveable(line.date) { mutableStateOf(false) }
    if (gone) return
    Column(
        Modifier.fillMaxWidth().lineBox(seed, strong = true).clickable { opened = true }
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Feather(u * G.LetGo.feather * 0.7f)
            TokenText(title, Tokens.TypeScale.subhead, Modifier.weight(1f), weight = FontWeight.SemiBold)
        }
        if (!opened) TokenText(stringResource(R.string.recall_open), Tokens.TypeScale.footnote, color = p.secondary)
        else {
            val meta = listOfNotNull(line.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), line.feeling?.let { stringResource(feelingName(it)) }).joinToString(" · ")
            TokenText(meta, Tokens.TypeScale.caption1, color = p.secondary)
            TokenText(line.text, lineType(Tokens.TypeScale.headline, Theme.garden))
            // 그날의 사진 (11): 그 사이 시간만큼 바랜 모습
            if (Theme.garden) WeatheredPhoto(state, line.date, today, u * 160f, modifier = Modifier.padding(vertical = Tokens.Space.sp2))
            // 돌아온 한 줄로 할 수 있는 것 (12): 카드로 간직 · 오늘 한 줄에 이어 쓰기 (오늘 아직 쓰지 않았을 때)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                if (Theme.garden) GardenChip(stringResource(R.string.recall_keepCard), false, seed + 40) {
                    io.github.graviton94.carpediem.share.ShareCards.send(ctx, "line-${line.date}") { io.github.graviton94.carpediem.share.ShareCards.line(ctx, line, io.github.graviton94.carpediem.share.ShareCards.feelingName(ctx, line.feeling), state.store.haruSeed) }
                }
                if (!state.sentOn(today)) GardenChip(stringResource(R.string.recall_continue), false, seed + 41) { state.recallReply = line; state.focusWrite = true }
            }
            TokenText(stringResource(R.string.recall_close), Tokens.TypeScale.footnote, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { gone = true }.padding(vertical = Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold)
        }
    }
}


/**
 * 한 줄을 보낸 뒤: 마음에 맞춘 한마디가 담긴 창이 조용히 떠오른다. ‘확인’으로 닫는다.
 * 창 밖을 눌러도 닫히지 않는다 (한마디를 읽을 틈). 뒤로 가기는 닫기.
 */
@Composable
fun LetGoModal(state: AppState, modifier: Modifier = Modifier, onCare: (Care) -> Unit = {}) {
    val msg = state.toast ?: return
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    // 창은 앱의 다른 떠오르는 것과 같은 나타남
    val card = io.github.graviton94.carpediem.ui.rememberPop(msg)
    BackHandler { state.toast = null; state.care = null }
    val feather = GardenArt.obj(ctx, "feather")
    val scrim = Theme.gc.scrim
    BoxWithConstraints(
        modifier.fillMaxSize()
            .drawBehind { drawRect(scrim.copy(alpha = scrim.alpha * card.value)) }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        // 깃털 비는 쓰지 않음 (보내면 창만 조용히 떠오름)
        Column(
            Modifier.align(Alignment.Center).padding(horizontal = Theme.deviceClass.pageMargin)
                .pop(card).modalBox().padding(Tokens.Space.sp6).semantics { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
        ) {
            Image(feather, null, Modifier.size(u * G.LetGo.feather * 1.3f))
            TokenText(state.toastTitle ?: stringResource(R.string.letgo_modalTitle), lineType(Tokens.TypeScale.title3, Theme.garden), align = TextAlign.Center)
            TokenText(msg, lineType(Tokens.TypeScale.callout, Theme.garden), color = p.secondary, align = TextAlign.Center)
            Spacer(Modifier.height(Tokens.Space.sp2))
            Action(stringResource(R.string.letgo_ok), filled = true, seed = 999) { state.toast = null; state.care = null }
            // 그림으로 보내기: 오늘 보낸 한 줄을 카드 한 장으로 (글을 남겼을 때만)
            val todayLine = state.lines.lastOrNull { it.date == (state.fixedNow ?: java.time.LocalDateTime.now()).toLocalDate() }
            if (Theme.garden && todayLine != null && todayLine.text.isNotBlank()) TokenText(stringResource(R.string.share_image), Tokens.TypeScale.footnote,
                Modifier.clickable {
                    io.github.graviton94.carpediem.share.ShareCards.send(ctx, "line-${todayLine.date}") { io.github.graviton94.carpediem.share.ShareCards.line(ctx, todayLine,
                        io.github.graviton94.carpediem.share.ShareCards.feelingName(ctx, todayLine.feeling), state.store.haruSeed) }
                }.heightIn(min = Tokens.Layout.tapTarget).padding(vertical = Tokens.Space.sp3), color = p.secondary, align = TextAlign.Center)
            // 돌봄 권하기: 확인 아래 작은 한 줄 (지나쳐도 되는 곳에)
            state.care?.let { c -> CareLine(state, c) { state.toast = null; state.care = null; onCare(c) } }
        }
    }
}

/** 한마디 창 맨 아래 권유 한 줄. 누구에게는 가족 이름 칩, 나머지는 누르면 바로 그 일로. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CareLine(state: AppState, c: Care, onGo: () -> Unit) {
    val p = Theme.palette
    if (c == Care.SEND_TO) {
        TokenText(stringResource(R.string.care_sendTo), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            state.people.forEachIndexed { i, person -> Chip(person.name, false, seed = 1200 + i) { state.sendTodayTo(person.id); state.toast = null; state.care = null } }
        }
        return
    }
    val text = stringResource(when (c) { Care.CALM_BREATH -> R.string.care_calm; Care.BOX_BREATH -> R.string.care_box; Care.SLEEP_BREATH -> R.string.care_sleep; Care.MORNING_BREATH -> R.string.care_morning; else -> R.string.care_look })
    TokenText(text, Tokens.TypeScale.footnote, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable(onClick = onGo).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold, align = TextAlign.Center)
}

/**
 * 지난 날의 한 줄 고치기 (마음의 기록에서 그날을 고른 뒤): 글 · 마음 · 사진 (붙이기 · 바꾸기 · 빼기).
 * 저장할 때만 반영하고, 그만두면 그대로.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun PastLineSheet(state: AppState, line: DayLine, today: LocalDate, onClose: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val u = Theme.unit
    val scope = rememberCoroutineScope()
    var text by rememberSaveable(line.date) { mutableStateOf(line.text) }
    var feeling by rememberSaveable(line.date) { mutableStateOf(line.feeling) }
    var dropPhoto by rememberSaveable(line.date) { mutableStateOf(false) }
    var newPhoto by rememberSaveable(line.date) { mutableStateOf(false) }
    val max = Tokens.Garden.LetGo.maxChars.toInt()
    val photoFail = stringResource(R.string.photo_fail)
    val pick = rememberPhotoPicker { uri -> scope.launch {
        val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { io.github.graviton94.carpediem.data.Photos.importPending(ctx, uri, edit = true) }
        if (ok) { newPhoto = true; dropPhoto = false; state.photoKick++ } else state.say(photoFail)
    } }
    fun close() { if (newPhoto) io.github.graviton94.carpediem.data.Photos.dropPending(ctx, edit = true); state.photoKick++; onClose() }
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = { close() }, containerColor = Theme.gc.paper,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp8),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
            TokenText(stringResource(R.string.edit_pastTitle, RecordText.day(ctx, line.date)), Tokens.TypeScale.title3)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                Feeling.entries.forEachIndexed { i, f -> Chip(stringResource(feelingName(f)), feeling == f, seed = 1270 + i) { feeling = if (feeling == f) null else f } }
            }
            BasicTextField(
                value = text,
                onValueChange = { v -> if (v.codePointCount(0, v.length) <= max && v.count { it == '\n' } < Lines.MAX_LINES) text = v },
                singleLine = false, minLines = 2, maxLines = Lines.MAX_LINES, textStyle = Tokens.TypeScale.callout.style().copy(color = p.foreground), cursorBrush = SolidColor(p.foreground),
                modifier = Modifier.fillMaxWidth().keepAboveKeyboard().lineBox(1280).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
            )
            // 사진: 새로 고른 것 · 지금 붙은 것 (바꾸기 · 빼기) · 없으면 ‘사진 한 장’
            val has = remember(state.photoKick, line.date) { io.github.graviton94.carpediem.data.Photos.has(ctx, line.date) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    newPhoto -> {
                        WeatheredPhoto(state, line.date, today, u * 56f, pending = true, edit = true, dated = false)
                        TokenText(stringResource(R.string.photo_remove), Tokens.TypeScale.footnote,
                            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { io.github.graviton94.carpediem.data.Photos.dropPending(ctx, edit = true); newPhoto = false; state.photoKick++ }.padding(Tokens.Space.sp3), color = p.secondary)
                    }
                    has && !dropPhoto -> {
                        WeatheredPhoto(state, line.date, today, u * 56f, dated = false)
                        TokenText(stringResource(R.string.photo_change), Tokens.TypeScale.footnote,
                            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { pick() }.padding(Tokens.Space.sp3), color = p.olive, weight = FontWeight.SemiBold)
                        TokenText(stringResource(R.string.photo_remove), Tokens.TypeScale.footnote,
                            Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { dropPhoto = true }.padding(vertical = Tokens.Space.sp3), color = p.secondary)
                    }
                    else -> GardenChip(stringResource(R.string.photo_add), false, 1285) { pick() }
                }
            }
            Action(stringResource(R.string.edit_save), filled = text.isNotBlank(), seed = 1286) {
                if (text.isBlank()) { state.say(ctx.getString(R.string.letgo_empty)); return@Action }
                if (state.editLine(line.date, text, feeling)) {
                    if (newPhoto) { io.github.graviton94.carpediem.data.Photos.commitPending(ctx, line.date, edit = true); newPhoto = false }
                    else if (dropPhoto) io.github.graviton94.carpediem.data.Photos.remove(ctx, line.date)
                    state.photoKick++
                    onClose()
                }
            }
            TokenText(stringResource(R.string.cancel), Tokens.TypeScale.footnote,
                Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable { close() }.padding(vertical = Tokens.Space.sp3), color = p.secondary, align = TextAlign.Center)
        }
    }
}
