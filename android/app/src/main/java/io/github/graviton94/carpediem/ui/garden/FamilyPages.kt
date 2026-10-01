package io.github.graviton94.carpediem.ui.garden

import io.github.graviton94.carpediem.ui.GardenAlert
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.Family
import io.github.graviton94.carpediem.core.GridScale
import io.github.graviton94.carpediem.core.Kind
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.Person
import io.github.graviton94.carpediem.core.Season
import io.github.graviton94.carpediem.core.Sex
import io.github.graviton94.carpediem.core.Species
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import io.github.graviton94.carpediem.design.Tokens.Garden as G
import io.github.graviton94.carpediem.ui.AppState
import io.github.graviton94.carpediem.ui.Labels
import io.github.graviton94.carpediem.ui.SkyBackground
import io.github.graviton94.carpediem.ui.TokenText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private fun dateText(d: LocalDate) = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
private fun speciesName(s: Species) = when (s) { Species.DOG -> R.string.species_dog; Species.CAT -> R.string.species_cat; Species.OTHER -> R.string.species_other }

/** 돌 하나를 크게 (만남 · 돌의 페이지): 땅 한 줄 위에. */
@Composable
internal fun BigStone(art: HaruArt, pet: Boolean, hat: Boolean, onOpen: (() -> Unit)? = null, sparkle: Boolean = false, size: Float = 1f) {
    val u = Theme.unit
    val scale = u * (G.Layout.meetHaruWidth / G.Layout.haruArtWidth) * (if (pet) G.Family.petScale else 1f) * size
    val boxH = scale * (G.Layout.haruGround - art.meta.bbox.top + if (hat) Tokens.Garden.Party.hatHeight else G.Layout.sparkle * 2)
    BoxWithConstraints(Modifier.fillMaxWidth().height(boxH + u * G.Layout.labelGap)) {
        val w = maxWidth
        val cx = w / 2 - scale * (art.meta.bbox.center.x - art.meta.box / 2)
        val at = Modifier.offset(cx - scale * (art.meta.box / 2), boxH - scale * G.Layout.haruGround)
        HaruFigure(art, scale, at, hat = hat, onOpen = onOpen)
        if (sparkle) Box(at) { Sparkles(art, scale) }
        CrayonRule(Modifier.offset(y = boxH - u * 2).padding(horizontal = w * 0.2f), seed = 861)
    }
}

// ───────────────────────── 돌의 페이지 ─────────────────────────

/**
 * 두 번 누르면 열리는 그 돌의 페이지. 숫자는 ‘남은 날’이 아니라 ‘함께한 날’.
 * id = null 이면 내 하루. 그 사람의 인생 달력은 남은 칸을 기본으로 숨긴다.
 */
@Composable
fun StoneScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, id: String?, onBack: () -> Unit, onEdit: (String) -> Unit,
                onBreath: (io.github.graviton94.carpediem.core.BreathKind, Int, io.github.graviton94.carpediem.core.Sound) -> Unit = { _, _, _ -> }) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val today = now.toLocalDate()
    val person = id?.let { i -> state.people.firstOrNull { it.id == i } }
    if (id != null && person == null) { androidx.compose.runtime.LaunchedEffect(Unit) { onBack() }; return }
    val me = person == null
    val sprout = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now).season == Season.SPRING
    val art = HaruArt.of(person?.seed ?: state.store.haruSeed, sprout)
    val name = person?.name ?: stringResource(R.string.garden_haru)
    val birth = if (me) profile.birthDate else person!!.birth
    // 만난 날 = 함께한 첫날 (카드의 ‘…에 만난’ 과 ‘함께한 지’ 가 같은 날에서): 나는 하루를 만난 날, 가족은 정한 날 또는 함께 살아온 첫날
    val metOn = if (person == null) state.store.startDate else Family.togetherSince(profile.birthDate, person)
    val soon = Family.birthdaySoon(birth, now, Tokens.Notify.birthdayFrom.toInt())
    val birthday = soon != null
    var breathSheet by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    SkyBackground {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp10),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4),
        ) {
            PageBar(name, onBack)
            BigStone(art, person?.kind == Kind.PET, birthday, size = G.Family.pageStone)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                TokenText(name, Tokens.TypeScale.title3.serif(), align = TextAlign.Center)
                TokenText(stringResource(R.string.garden_metOn, dateText(metOn), Labels.stone(ctx, art.meta.stone)), Tokens.TypeScale.footnote, color = p.secondary, align = TextAlign.Center)
                if (birth != null) {
                    val age = LifeSnapshot(birth, if (me) profile.expectancy(state.store.table) else state.store.expectancy(person!!), now).age
                    TokenText(stringResource(if (person?.kind == Kind.PET) R.string.stone_petAge else R.string.stone_age, "$age"), Tokens.TypeScale.footnote, color = p.secondary)
                }
            }
            // 가족의 생일 (전날 저녁부터 그날까지): 나와 그 사람의 돌이 나란히 앉은 카드 한 장
            if (birthday) TokenText(if (me) stringResource(if (soon == 0) R.string.bday_mineToday else R.string.bday_mineTomorrow) else stringResource(if (soon == 0) R.string.bday_today else R.string.bday_tomorrow, name),
                Tokens.TypeScale.callout.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
            if (birthday && !me) GardenButton(stringResource(R.string.bday_card), {
                io.github.graviton94.carpediem.share.ShareCards.send(ctx, io.github.graviton94.carpediem.share.ShareCards.birthday(ctx, name, person!!.seed, person.kind == Kind.PET, state.store.haruSeed, SkyTime.isDark(now)), "birthday-${person.id}-$today")
            }, filled = true, seed = 873)
            // 함께한 날 · 다음 생일
            Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                val since = metOn
                Info(stringResource(if (me) R.string.stone_sinceMet else R.string.stone_together), stringResource(R.string.stone_days, Labels.number(Family.daysUntil(today, since).toInt().coerceAtLeast(0))), 870, Modifier.weight(1f))
                if (birth != null) {
                    val next = Family.nextBirthday(birth, today); val left = Family.daysUntil(next, today).toInt()
                    Info(stringResource(R.string.stone_nextBirthday), when (left) { 0 -> stringResource(R.string.stone_birthdayToday); 1 -> stringResource(R.string.stone_birthdayTomorrow); else -> stringResource(R.string.stone_days, Labels.number(left)) }, 872, Modifier.weight(1f))
                }
            }
            // 그 사람의 인생 달력 (조약돌): 함께한 해는 호박빛 테두리, 남은 칸은 켜야만
            if (birth != null) {
                var grid by remember { mutableStateOf(GridScale.YEARS) }
                val exp = if (me) profile.expectancy(state.store.table) else state.store.expectancy(person!!)
                val snap = LifeSnapshot(birth, exp, now)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TokenText(stringResource(R.string.stone_calendar, name), Tokens.TypeScale.headline, Modifier.weight(1f))
                    listOf(GridScale.WEEKS, GridScale.MONTHS, GridScale.YEARS).forEachIndexed { i, g -> GardenChip(Labels.gridShort(ctx, g), g == grid, seed = 874 + i) { grid = g } }
                }
                val cols = when (grid) { GridScale.WEEKS -> Tokens.Grid.weeksColumns; GridScale.MONTHS -> Tokens.Grid.monthsColumns; GridScale.YEARS -> Tokens.Grid.yearsColumns }
                val shared = if (me) Int.MAX_VALUE else LifeSnapshot(birth, exp, Family.togetherSince(profile.birthDate, person!!).atStartOfDay()).lived(grid.unit)
                CrayonCalendar(snap.total(grid.unit), snap.lived(grid.unit), cols, androidx.compose.ui.Modifier.fillMaxWidth(), sharedFrom = shared, showAhead = me || person!!.showAhead)
                if (!me) {
                    TokenText(stringResource(R.string.stone_calendarLegend), Tokens.TypeScale.caption1, color = p.secondary)
                    Row(Modifier.fillMaxWidth().clickable { state.savePerson(person!!.copy(showAhead = !person.showAhead)) }, verticalAlignment = Alignment.CenterVertically) {
                        TokenText(stringResource(R.string.stone_showAhead), Tokens.TypeScale.subhead, Modifier.weight(1f))
                        Switch(person!!.showAhead, { state.savePerson(person.copy(showAhead = it)) }, colors = SwitchDefaults.colors(checkedTrackColor = p.olive))
                    }
                }
            }
            // 내 돌: 하루와 숨 쉬기
            if (me) GardenButton(stringResource(R.string.breath), { breathSheet = true }, filled = false, seed = 879)
            // 이 돌에게 보낸 마음
            if (!me) {
                val sent = state.lines.filter { it.to == id && it.text.isNotBlank() }
                if (sent.isNotEmpty()) {
                    var openLines by rememberSaveable { mutableStateOf(false) }
                    Column(Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 878).clickable { openLines = !openLines }.padding(Tokens.Space.sp4),
                        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                        Row { TokenText(stringResource(R.string.stone_lines), Tokens.TypeScale.subhead, Modifier.weight(1f), weight = FontWeight.SemiBold); TokenText(stringResource(R.string.stone_linesCount, "${sent.size}"), Tokens.TypeScale.subhead, color = p.secondary) }
                        if (!openLines) TokenText(stringResource(R.string.recall_open), Tokens.TypeScale.footnote, color = p.secondary)
                        else sent.asReversed().forEach { l ->
                            TokenText(l.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), Tokens.TypeScale.caption1, color = p.secondary)
                            TokenText(l.text, Tokens.TypeScale.callout.serif())
                        }
                    }
                }
                GardenButton(stringResource(R.string.stone_edit), { onEdit(id!!) }, filled = false, seed = 880)
            }
        }
    }
    if (breathSheet) BreathSheet(state, now, { k, m, snd -> breathSheet = false; onBreath(k, m, snd) }) { breathSheet = false }
}

@Composable
private fun Info(label: String, value: String, seed: Int, modifier: Modifier) {
    val p = Theme.palette
    Column(modifier.crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, seed).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
        TokenText(label, Tokens.TypeScale.caption1, color = p.secondary)
        TokenText(value, Tokens.TypeScale.headline, maxLines = 1)
    }
}

// ───────────────────────── 돌 더하기 · 고치기 ─────────────────────────

/**
 * 가족의 돌을 정원에 부르는 네 장 (누구 → 이름 → 생일 → 만남). editId 가 있으면 한 장에서 고치기 · 내려놓기.
 * onDone(id): 저장한 돌의 id (내려놓았으면 null).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPersonScreen(state: AppState, profile: LifeProfile, editId: String?, onDone: (String?) -> Unit, onBack: () -> Unit,
                    memory: Boolean = false, onAddMemory: () -> Unit = {}, onMovedToMemory: () -> Unit = {}) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val editing = editId?.let { i -> state.people.firstOrNull { it.id == i } }
    var step by rememberSaveable { mutableStateOf(0) }
    var kind by rememberSaveable { mutableStateOf(editing?.kind ?: Kind.PERSON) }
    var species by rememberSaveable { mutableStateOf(editing?.species ?: Species.DOG) }
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var birth by rememberSaveable { mutableStateOf(editing?.birth?.toEpochDay()) }
    var sex by rememberSaveable { mutableStateOf(editing?.sex ?: Sex.OTHER) }
    var together by rememberSaveable { mutableStateOf(editing?.together?.toEpochDay()) }
    var pickingTogether by remember { mutableStateOf(false) }
    var seed by rememberSaveable { mutableStateOf(editing?.seed ?: state.newSeed()) }
    var rerolls by rememberSaveable { mutableStateOf(editing?.rerolls ?: 0) }
    var picking by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    // 기억의 돌: 곁을 떠난 날 (선택)
    var until by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickingUntil by remember { mutableStateOf(false) }
    var confirmMemory by remember { mutableStateOf(false) }
    val back = { if (editing == null && step > 0) step-- else onBack() }
    BackHandler { back() }
    fun person() = Person(editing?.id ?: state.newPersonId(), Family.cleanName(name), kind, if (kind == Kind.PET) species else null, birth?.let { LocalDate.ofEpochDay(it) },
        sex, editing?.country ?: profile.countryCode, seed, rerolls, editing?.metOn ?: (state.fixedNow ?: LocalDateTime.now()).toLocalDate(), editing?.showAhead ?: false,
        together?.let { LocalDate.ofEpochDay(it) })

    @Composable fun KindPicker() {
        TokenText(stringResource(R.string.add_kind), Tokens.TypeScale.title3.serif())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            GardenChip(stringResource(R.string.kind_person), kind == Kind.PERSON, 890) { kind = Kind.PERSON }
            GardenChip(stringResource(R.string.kind_pet), kind == Kind.PET, 891) { kind = Kind.PET }
        }
        if (kind == Kind.PET) FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            Species.entries.forEachIndexed { i, s -> GardenChip(stringResource(speciesName(s)), species == s, 892 + i) { species = s } }
        }
    }
    @Composable fun NameField() {
        TokenText(stringResource(R.string.add_name), Tokens.TypeScale.title3.serif())
        BasicTextField(
            value = name, onValueChange = { v -> name = v.replace('\n', ' ').take(Family.NAME_MAX * 2).let { if (it.codePointCount(0, it.length) <= Family.NAME_MAX) it else name } },
            singleLine = true, textStyle = Tokens.TypeScale.headline.style().copy(color = p.foreground), cursorBrush = SolidColor(p.foreground),
            modifier = Modifier.fillMaxWidth().crayonBox(Theme.gc.paper, G.Radius.box, G.Stroke.chip, 896).padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
            decorationBox = { inner -> Box { if (name.isEmpty()) TokenText(stringResource(R.string.add_nameHint), Tokens.TypeScale.headline, color = p.secondary); inner() } },
        )
    }
    @Composable fun MemoryDates() {
        TokenText(stringResource(R.string.add_together), Tokens.TypeScale.title3.serif())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            GardenChip(together?.let { dateText(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.add_birthPick), together != null, 911) { pickingTogether = true }
            GardenChip(stringResource(R.string.add_birthUnknown), together == null, 912) { together = null }
        }
        TokenText(stringResource(R.string.memory_addUntil), Tokens.TypeScale.title3.serif())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            GardenChip(until?.let { dateText(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.add_birthPick), until != null, 913) { pickingUntil = true }
            GardenChip(stringResource(R.string.add_birthUnknown), until == null, 914) { until = null }
        }
    }
    @Composable fun BirthField() {
        TokenText(stringResource(if (kind == Kind.PET) R.string.add_birthPet else R.string.add_birth), Tokens.TypeScale.title3.serif())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            GardenChip(birth?.let { dateText(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.add_birthPick), birth != null, 897) { picking = true }
            GardenChip(stringResource(R.string.add_birthUnknown), birth == null, 898) { birth = null }
        }
        // 함께한 첫날 (선택): 정하지 않으면 저절로
        TokenText(stringResource(R.string.add_together), Tokens.TypeScale.subhead, color = p.secondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
            GardenChip(together?.let { dateText(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.add_birthPick), together != null, 902) { pickingTogether = true }
            GardenChip(stringResource(R.string.add_togetherAuto), together == null, 903) { together = null }
        }
        TokenText(stringResource(R.string.add_togetherHelp), Tokens.TypeScale.caption1, color = p.secondary)
        if (kind == Kind.PERSON) {
            TokenText(stringResource(R.string.sex), Tokens.TypeScale.subhead, color = p.secondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                Sex.entries.forEachIndexed { i, s -> GardenChip(Labels.sex(ctx, s), sex == s, 899 + i) { sex = s } }
            }
        }
    }

    SkyBackground {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = Theme.deviceClass.pageMargin).padding(bottom = Tokens.Space.sp10),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp4),
        ) {
            PageBar(stringResource(if (memory) R.string.memory_addMore else if (editing != null) R.string.stone_edit else R.string.family_add), { back() })
            if (memory) when (step) {
                // 기억의 돌 더하기: 누구 · 이름 · 날 (모두 선택) · 눈 감은 돌
                0 -> { KindPicker(); GardenButton(stringResource(R.string.add_next), { step = 1 }, filled = true, seed = 915) }
                1 -> { NameField(); GardenButton(stringResource(R.string.add_next), { if (name.isNotBlank()) step = 2 }, filled = name.isNotBlank(), seed = 916) }
                2 -> { MemoryDates(); GardenButton(stringResource(R.string.add_next), { step = 3 }, filled = true, seed = 917) }
                else -> {
                    val art = HaruArt.of(seed, false)
                    TokenText(stringResource(R.string.memory_meet, Family.cleanName(name)), Tokens.TypeScale.title2.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                    val u = Theme.unit
                    val scale = u * (G.Layout.meetHaruWidth * 0.7f * (if (kind == Kind.PET) G.Family.petScale else 1f) / G.Layout.haruArtWidth)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { HaruFigure(art, scale, Modifier, lid = 1f, tiltOn = false) }
                    if (rerolls < Family.REROLLS) TokenText(stringResource(R.string.add_reroll, "${Family.REROLLS - rerolls}"), Tokens.TypeScale.footnote,
                        Modifier.fillMaxWidth().clickable { seed = state.newSeed(); rerolls++ }.padding(vertical = Tokens.Space.sp2), color = p.olive, align = TextAlign.Center, weight = FontWeight.SemiBold)
                    GardenButton(stringResource(R.string.done), {
                        val m = Person(state.newPersonId(), Family.cleanName(name), kind, if (kind == Kind.PET) species else null, null, Sex.OTHER, profile.countryCode, seed, rerolls,
                            (state.fixedNow ?: LocalDateTime.now()).toLocalDate(), false, together?.let { LocalDate.ofEpochDay(it) }, until?.let { LocalDate.ofEpochDay(it) }, true)
                        state.saveMemory(m); onDone(m.id)
                    }, filled = true, seed = 918)
                }
            } else if (editing != null) {
                KindPicker(); NameField(); BirthField()
                Spacer(Modifier.height(Tokens.Space.sp2))
                GardenButton(stringResource(R.string.stone_save), { if (name.isNotBlank()) { state.savePerson(person()); onDone(editing.id) } }, filled = name.isNotBlank(), seed = 905)
                // 이 돌 정리하기: 무엇이 되는지 이름과 설명 한 줄로 (둘 다 한 번 더 묻는다)
                Spacer(Modifier.height(Tokens.Space.sp4))
                TokenText(stringResource(R.string.stone_tidy), Tokens.TypeScale.caption1, color = p.secondary)
                ActionNote(stringResource(R.string.memory_toMemory), stringResource(R.string.memory_toMemoryHelp), 919) { confirmMemory = true }
                ActionNote(stringResource(R.string.stone_remove), stringResource(R.string.stone_removeHelp), 906) { confirmRemove = true }
            } else when (step) {
                0 -> {
                    KindPicker(); GardenButton(stringResource(R.string.add_next), { step = 1 }, filled = true, seed = 907)
                    // 곁을 떠난 가족 · 반려동물은 여기서 더하지 않고, 정원의 돌을 다듬기에서 기억의 자리로 옮긴다
                }
                1 -> { NameField(); GardenButton(stringResource(R.string.add_next), { if (name.isNotBlank()) step = 2 }, filled = name.isNotBlank(), seed = 908) }
                2 -> { BirthField(); GardenButton(stringResource(R.string.add_next), { step = 3 }, filled = true, seed = 909) }
                else -> {
                    // 만남: 첫 만남에만 반짝이
                    val art = HaruArt.of(seed, false)
                    TokenText(stringResource(R.string.add_meet, Family.cleanName(name)), Tokens.TypeScale.title2.serif(), Modifier.fillMaxWidth(), align = TextAlign.Center)
                    TokenText(Labels.stone(ctx, art.meta.stone), Tokens.TypeScale.subhead, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
                    BigStone(art, kind == Kind.PET, false, sparkle = true)
                    if (rerolls < Family.REROLLS) TokenText(stringResource(R.string.add_reroll, "${Family.REROLLS - rerolls}"), Tokens.TypeScale.footnote,
                        Modifier.fillMaxWidth().clickable { seed = state.newSeed(); rerolls++ }.padding(vertical = Tokens.Space.sp2), color = p.olive, align = TextAlign.Center, weight = FontWeight.SemiBold)
                    GardenButton(stringResource(R.string.garden_meet_go), { val np = person(); state.savePerson(np); onDone(np.id) }, filled = true, seed = 910)
                }
            }
        }
    }

    if (pickingTogether) {
        val dp = rememberDatePickerState(initialSelectedDateMillis = (together?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingTogether = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms -> val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate(); if (!d.isAfter(LocalDate.now())) together = d.toEpochDay() }
                    pickingTogether = false
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = { TextButton(onClick = { pickingTogether = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = dp) }
    }
    if (picking) {
        val init = (birth?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now().minusYears(if (kind == Kind.PET) 3 else 30)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val dp = rememberDatePickerState(initialSelectedDateMillis = init)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms -> val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate(); if (!d.isAfter(LocalDate.now())) birth = d.toEpochDay() }
                    picking = false
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = dp) }
    }
    if (pickingUntil) {
        val dp = rememberDatePickerState(initialSelectedDateMillis = (until?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingUntil = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms -> val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate(); if (!d.isAfter(LocalDate.now())) until = d.toEpochDay() }
                    pickingUntil = false
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = { TextButton(onClick = { pickingUntil = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = dp) }
    }
    if (confirmMemory && editing != null) {
        GardenAlert(
            onDismissRequest = { confirmMemory = false },
            text = { Text(stringResource(R.string.memory_toMemoryConfirm, editing.name)) },
            confirmButton = { TextButton(onClick = { confirmMemory = false; if (state.toMemory(editing.id)) onMovedToMemory() else state.say(ctx.getString(R.string.memory_full)) }) { Text(stringResource(R.string.memory_toMemoryAction)) } },
            dismissButton = { TextButton(onClick = { confirmMemory = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmRemove && editing != null) {
        GardenAlert(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.stone_removeConfirm, editing.name)) },
            confirmButton = { TextButton(onClick = { confirmRemove = false; state.removePerson(editing.id); onDone(null) }) { Text(stringResource(R.string.stone_removeAction), color = p.danger) } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** 버튼 하나와 그 아래 설명 한 줄 (무엇이 되는지 바로 알게). */
@Composable
internal fun ActionNote(title: String, help: String, seed: Int, onClick: () -> Unit) {
    val p = Theme.palette
    Column(
        Modifier.fillMaxWidth().crayonBox(null, G.Radius.box, G.Stroke.chip, seed).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = Tokens.Space.sp4, vertical = Tokens.Space.sp3),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1),
    ) {
        TokenText(title, Tokens.TypeScale.subhead, weight = FontWeight.SemiBold)
        TokenText(help, Tokens.TypeScale.footnote, color = p.secondary)
    }
}
