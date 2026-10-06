package io.github.graviton94.carpediem.ui

import io.github.graviton94.carpediem.ui.garden.keepAboveKeyboard
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import io.github.graviton94.carpediem.ui.garden.LetGoModal
import io.github.graviton94.carpediem.ui.garden.LetGoSection
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.graviton94.carpediem.R
import io.github.graviton94.carpediem.core.GridScale
import io.github.graviton94.carpediem.core.LifePeriod
import io.github.graviton94.carpediem.core.LifeProfile
import io.github.graviton94.carpediem.core.LifeSnapshot
import io.github.graviton94.carpediem.core.LifeUnit
import io.github.graviton94.carpediem.core.Quote
import io.github.graviton94.carpediem.core.Sex
import io.github.graviton94.carpediem.data.Design
import io.github.graviton94.carpediem.data.QuoteLanguage
import io.github.graviton94.carpediem.ui.garden.GardenButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import io.github.graviton94.carpediem.design.Theme
import io.github.graviton94.carpediem.design.Tokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
private fun Page(content: @Composable () -> Unit) {
    val m = Theme.deviceClass.pageMargin
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = m).padding(bottom = Tokens.Space.sp10),
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3),
    ) { content() }
}

/** 날짜 고르기에서 고를 수 있는 날: from (없으면 끝없이) ~ until (기본 오늘). 그 밖의 날은 흐리게. */
@ExperimentalMaterial3Api
fun pastDates(from: LocalDate? = null, until: LocalDate = LocalDate.now()): androidx.compose.material3.SelectableDates = object : androidx.compose.material3.SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return !d.isAfter(until) && (from == null || !d.isBefore(from))
    }
    override fun isSelectableYear(year: Int): Boolean = year <= until.year && (from == null || year >= from.year)
}

// ───────────────────────── 온보딩 ─────────────────────────

@Composable
fun OnboardingScreen(state: AppState, onCountry: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val draft = state.draft ?: state.defaultProfile().also { state.draft = it }
    // 생일은 꼭 직접 고르게 (기본값 그대로 시작하면 모든 숫자가 틀림)
    val picked = state.birthPicked
    fun start() { if (picked) { state.begin(draft); state.draft = null } else state.say(ctx.getString(R.string.onboard_needBirth)) }
    SkyBackground {
        Page {
            // 정원: 가운데 정렬, 한 줄 소개는 작게. 유리 버전은 그대로.
            if (Theme.garden) Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp10, bottom = Tokens.Space.sp5), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                TokenText("Carpe Diem", Tokens.TypeScale.display(Theme.deviceClass), align = TextAlign.Center)
                TokenText(stringResource(R.string.tagline), Tokens.TypeScale.callout.serif(), color = p.secondary, align = TextAlign.Center)
            }
            else Column(Modifier.padding(top = Tokens.Space.sp10, start = Tokens.Space.sp2, bottom = Tokens.Space.sp5), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                TokenText("Carpe Diem", Tokens.TypeScale.display(Theme.deviceClass))
                TokenText(stringResource(R.string.tagline), Tokens.TypeScale.title3, color = p.secondary)
            }
            ProfileFields(state, draft, { state.draft = it }, onCountry, birthUnset = !picked, onBirthPicked = { state.birthPicked = true })
            if (Theme.garden) GardenButton(stringResource(R.string.begin), { start() }, filled = picked, seed = 740, modifier = Modifier.padding(top = Tokens.Space.sp3))
            else Box(
                Modifier.fillMaxWidth().padding(top = Tokens.Space.sp3).heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp2)
                    .clip(RoundedCornerShape(Tokens.Radius.pill)).background(if (picked) p.olive else p.dim).clickable { start() },
                contentAlignment = Alignment.Center,
            ) { TokenText(stringResource(R.string.begin), Tokens.TypeScale.headline, color = p.onOlive) }
            TokenText(stringResource(R.string.privacy), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        }
    }
}

// ───────────────────────── 나의 정보 입력 ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFields(state: AppState, draft: LifeProfile, onChange: (LifeProfile) -> Unit, onCountry: () -> Unit, birthUnset: Boolean = false, onBirthPicked: () -> Unit = {}) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val table = state.store.table
    var picking by remember { mutableStateOf(false) }
    var sexMenu by remember { mutableStateOf(false) }
    val dateText = if (birthUnset) stringResource(R.string.onboard_pickBirth) else draft.birthDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    FormSection(header = stringResource(R.string.you)) {
        FormRow(stringResource(R.string.birthday), onClick = { picking = true }) {
            Box(Modifier.clip(RoundedCornerShape(Tokens.Radius.sm)).background(p.dim).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1)) {
                TokenText(dateText, Tokens.TypeScale.callout, color = if (birthUnset) p.olive else p.foreground, weight = if (birthUnset) FontWeight.SemiBold else null)
            }
        }
        RowDivider()
        FormRow(stringResource(R.string.country), onClick = onCountry) {
            TokenText(Labels.country(ctx, table.country(draft.countryCode), draft.countryCode), Tokens.TypeScale.body, color = p.secondary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.secondary)
        }
        RowDivider()
        FormRow(stringResource(R.string.sex), onClick = { sexMenu = true }) {
            Box {
                TokenText(Labels.sex(ctx, draft.sex), Tokens.TypeScale.body, color = p.olive)
                DropdownMenu(expanded = sexMenu, onDismissRequest = { sexMenu = false }) {
                    Sex.entries.forEach { s -> DropdownMenuItem(text = { Text(Labels.sex(ctx, s)) }, onClick = { onChange(draft.copy(sex = s)); sexMenu = false }) }
                }
            }
        }
    }
    FormSection(footer = stringResource(R.string.lifeExpectancy_footer)) {
        FormRow(stringResource(R.string.lifeExpectancy)) {
            ChipPicker(listOf(false, true), draft.customExpectancy != null, { if (it) stringResource(R.string.custom) else stringResource(R.string.auto) }) { custom ->
                onChange(draft.copy(customExpectancy = if (custom) draft.expectancy(table) else null))
            }
        }
        RowDivider()
        val value = draft.expectancy(table)
        if (draft.customExpectancy != null) {
            FormRow(stringResource(R.string.expectancy_value, Labels.years(value))) {
                val step = Tokens.Expectancy.step.toDouble()
                listOf(-step, step).forEach { step ->
                    TextButton(onClick = { onChange(draft.copy(customExpectancy = (value + step).coerceIn(Tokens.Expectancy.min.toDouble(), Tokens.Expectancy.max.toDouble()))) }) {
                        TokenText(if (step < 0) "−" else "+", Tokens.TypeScale.title2, color = p.olive)
                    }
                }
            }
        } else {
            FormRow("${Labels.country(ctx, table.country(draft.countryCode), draft.countryCode)} · ${Labels.sex(ctx, draft.sex)}") {
                TokenText(stringResource(R.string.expectancy_value, Labels.years(value)), Tokens.TypeScale.headline, color = p.secondary)
            }
        }
    }

    if (picking) {
        val dp = rememberDatePickerState(initialSelectedDateMillis = draft.birthDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), selectableDates = pastDates())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (!d.isAfter(LocalDate.now())) { onChange(draft.copy(birthDate = d)); onBirthPicked() }
                    }
                    picking = false
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = dp) }
    }
}

// ───────────────────────── 홈 ─────────────────────────

@Composable
fun HomeScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, onSettings: () -> Unit, onSupport: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    SkyBackground {
        Page {
            Row(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(start = Tokens.Space.sp1)) {
                    TokenText("Carpe Diem", Tokens.TypeScale.largeTitle)
                    TokenText(stringResource(R.string.tagline), Tokens.TypeScale.subhead, color = p.secondary)
                }
                IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.settings) }) {
                    Icon(Icons.Filled.Settings, null, tint = p.foreground)
                }
            }
            // 남은 시간
            GlassCard {
                TokenText(stringResource(R.string.timeLeft), Tokens.TypeScale.subhead, color = p.secondary)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    TokenText(Labels.number(s.remaining(state.unit)), Tokens.TypeScale.display(Theme.deviceClass), maxLines = 1)
                    TokenText(Labels.unit(ctx, state.unit), Tokens.TypeScale.title2, Modifier.padding(bottom = Tokens.Space.sp2), color = p.secondary)
                }
                Segments(LifeUnit.entries, state.unit, { Labels.unit(ctx, it) }) { state.changeUnit(it) }
            }
            // 오늘의 문장
            state.quote?.let { QuoteCard(it, state.quoteLanguage) { state.nextQuote() } }
            // 지나온 길
            GlassCard {
                Row(verticalAlignment = Alignment.Bottom) {
                    TokenText(stringResource(R.string.path), Tokens.TypeScale.title3)
                    Spacer(Modifier.weight(1f))
                    TokenText(Labels.percent(s.progress), Tokens.TypeScale.title3, color = p.olive)
                }
                ProgressBar(s.progress.toFloat(), Tokens.Stroke.barThick, glowing = true)
                Row {
                    TokenText(stringResource(R.string.path_age, "${s.age}", Labels.season(ctx, s.season)), Tokens.TypeScale.caption1, color = p.secondary)
                    Spacer(Modifier.weight(1f))
                    TokenText(stringResource(R.string.path_expected, Labels.years(s.expectancy)), Tokens.TypeScale.caption1, color = p.secondary)
                }
            }
            // 흐르는 시간
            GlassCard {
                TokenText(stringResource(R.string.flow), Tokens.TypeScale.title3)
                LifePeriod.entries.forEach { period ->
                    val pp = s.period(period)
                    Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2), modifier = Modifier.padding(top = Tokens.Space.sp1)) {
                        Row {
                            TokenText(Labels.period(ctx, period), Tokens.TypeScale.subhead)
                            Spacer(Modifier.weight(1f))
                            TokenText("${Labels.percent(pp.progress, 0)} · ${Labels.remaining(ctx, pp)}", Tokens.TypeScale.caption1, color = p.secondary)
                        }
                        ProgressBar(pp.progress.toFloat())
                    }
                }
            }
            // 인생 달력
            GlassCard {
                var menu by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TokenText(stringResource(R.string.calendar), Tokens.TypeScale.title3)
                    Spacer(Modifier.weight(1f))
                    Box {
                        Row(Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { menu = true }, verticalAlignment = Alignment.CenterVertically) {
                            TokenText(Labels.grid(ctx, state.grid), Tokens.TypeScale.subhead, color = p.olive, weight = FontWeight.SemiBold)
                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = p.olive)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            GridScale.entries.forEach { g -> DropdownMenuItem(text = { Text(Labels.grid(ctx, g)) }, onClick = { state.changeGrid(g); menu = false }) }
                        }
                    }
                }
                val cols = when (state.grid) { GridScale.WEEKS -> Tokens.Grid.weeksColumns; GridScale.MONTHS -> Tokens.Grid.monthsColumns; GridScale.YEARS -> Tokens.Grid.yearsColumns }
                LifeGrid(s.total(state.grid.unit), s.lived(state.grid.unit), cols)
                TokenText(stringResource(R.string.calendar_legend, Labels.season(ctx, s.season)), Tokens.TypeScale.caption1, color = p.secondary)
            }
            // 오늘의 한 줄 (정원과 같은 기능, 유리 모양)
            GlassCard { LetGoSection(state, now.toLocalDate()) }
            // 응원하기
            GlassCard(onClick = onSupport) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                        TokenText(stringResource(R.string.support), Tokens.TypeScale.title3)
                        TokenText(stringResource(R.string.support_once), Tokens.TypeScale.caption1, color = p.secondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.secondary)
                }
            }
        }
        // 한 줄을 보낸 뒤: 깃털이 내려오며 한마디 창
        LetGoModal(state, Modifier.fillMaxSize().safeDrawingPadding())
    }
}

@Composable
private fun QuoteCard(q: Quote, language: QuoteLanguage, onNext: () -> Unit) {
    val p = Theme.palette
    val lang = io.github.graviton94.carpediem.data.Words.lang(androidx.compose.ui.platform.LocalContext.current)
    val main = io.github.graviton94.carpediem.data.Words.main(q, language, lang)
    GlassCard(onClick = onNext) {
        TokenText(stringResource(R.string.words).uppercase(), Tokens.TypeScale.caption1, color = p.olive, weight = FontWeight.Bold)
        TokenText(main, Tokens.TypeScale.headline.serif())
        io.github.graviton94.carpediem.data.Words.second(q, language, lang)?.let { TokenText(it, Tokens.TypeScale.footnote.serif(), color = p.secondary) }
        TokenText(stringResource(R.string.words_next), Tokens.TypeScale.caption2, color = p.secondary, weight = FontWeight.Normal)
    }
}

// ───────────────────────── 설정 ─────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(state: AppState, profile: LifeProfile, onClose: () -> Unit, onCountry: () -> Unit, onCollection: () -> Unit, onSupport: () -> Unit, onStone: (String?) -> Unit = {}, onAddPerson: () -> Unit = {}) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val draft = state.draft ?: profile.also { state.draft = it }
    var confirmErase by remember { mutableStateOf(false) }
    // 생일 · 나라 · 성별 · 기대수명은 ‘완료’로 저장. 바꾼 채 뒤로 가면 저장할지 묻는다 (말없이 버리지 않게)
    var askSave by remember { mutableStateOf(false) }
    BackHandler { if (draft != profile) askSave = true else { state.draft = null; onClose() } }
    // 알림 허락이 막혀 있을 때: 폰 설정으로 안내
    var blocked by remember { mutableStateOf(false) }
    var lastBackup by remember { mutableStateOf(state.store.lastBackup) }
    val settingsScope = rememberCoroutineScope()
    // 백업 파일 저장 (설정의 백업 · 모두 지우기 전에)
    val saveFile = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) settingsScope.launch {
            // 사진까지 담으면 커서 화면 밖에서 만들고 씀
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { ctx.contentResolver.openOutputStream(uri)?.use { state.store.backup(it) } != null }.getOrDefault(false) }
            if (ok) { state.store.lastBackup = java.time.LocalDate.now().toEpochDay(); lastBackup = state.store.lastBackup }
            state.say(ctx.getString(if (ok) R.string.backup_saved else R.string.backup_saveFail))
        }
    }
    // 백업 · 불러오기는 폰의 화면 잠금을 한 번 확인한 뒤에만
    val deviceCheck = rememberDeviceCheck()
    fun backupNow() = deviceCheck { saveFile.launch("haru-garden-" + java.time.LocalDate.now() + ".json") }
    SkyBackground {
        Page {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (draft != profile) askSave = true else { state.draft = null; onClose() } }, modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.back) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = p.olive)
                }
                Spacer(Modifier.weight(1f))
                TokenText(stringResource(R.string.settings), Tokens.TypeScale.headline)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { state.save(draft); state.draft = null; onClose() }) { TokenText(stringResource(R.string.done), Tokens.TypeScale.headline, color = p.olive) }
            }
            ProfileFields(state, draft, { state.draft = it }, onCountry)
            // 여섯 묶음: 나 → 알림 → 기록 · 백업 → 정원 → 위젯 → 도움 · 응원 (지우기는 기록 · 백업 맨 아래)
            val sw = SwitchDefaults.colors(checkedTrackColor = p.olive)
            val chevron: @Composable RowScope.() -> Unit = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.secondary) }
            // ── 알림: 아침 · 밤 · 가족의 날을 한 묶음으로 ──
            val permission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok -> state.changeNotify(ok); if (!ok) blocked = true }
            val eveningPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok -> state.changeEvening(ok); if (!ok) blocked = true }
            val tomorrowPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok -> state.changeTomorrow(ok); if (!ok) blocked = true }
            fun needsAsk() = android.os.Build.VERSION.SDK_INT >= 33 && !io.github.graviton94.carpediem.notify.Daily.allowed(ctx)
            fun toggleNotify(on: Boolean) { if (on && needsAsk()) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) else state.changeNotify(on) }
            fun toggleEvening(on: Boolean) { if (on && needsAsk()) eveningPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) else state.changeEvening(on) }
            fun toggleTomorrow(on: Boolean) { if (on && needsAsk()) tomorrowPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS) else state.changeTomorrow(on) }
            FormSection(header = stringResource(R.string.settings_notify), footer = stringResource(R.string.settings_notifyFooter)) {
                FormRow(stringResource(R.string.notify_row), onClick = { toggleNotify(!state.notify) }) { Switch(state.notify, { toggleNotify(it) }, colors = sw) }
                if (state.notify) {
                    RowDivider()
                    FormRow(stringResource(R.string.notify_time), onClick = { pickTime(ctx, state.morningMinute) { state.changeMorningMinute(it) } }) {
                        TokenText(clockText(ctx, state.morningMinute), Tokens.TypeScale.body, color = p.olive)
                    }
                    if (Theme.garden) {
                        RowDivider()
                        FormRow(stringResource(R.string.notify_morningBreath), onClick = { state.changeMorningBreath(!state.morningBreath) }) { Switch(state.morningBreath, { state.changeMorningBreath(it) }, colors = sw) }
                    }
                }
                RowDivider()
                FormRow(stringResource(R.string.notify_eveningRow), onClick = { toggleEvening(!state.eveningNotify) }) { Switch(state.eveningNotify, { toggleEvening(it) }, colors = sw) }
                if (state.eveningNotify) {
                    RowDivider()
                    FormRow(stringResource(R.string.notify_time), onClick = { pickTime(ctx, state.eveningMinute) { state.changeEveningMinute(it) } }) {
                        TokenText(clockText(ctx, state.eveningMinute), Tokens.TypeScale.body, color = p.olive)
                    }
                }
                if (Theme.garden) {
                    RowDivider()
                    FormRow(stringResource(R.string.notify_tomorrowRow), onClick = { toggleTomorrow(!state.tomorrowNotify) }) { Switch(state.tomorrowNotify, { toggleTomorrow(it) }, colors = sw) }
                }
            }
            // ── 기록 · 백업: 남기기 · 내보내기 · 파일로 저장 / 들여오기 · 지우기 ──
            var confirmClear by remember { mutableStateOf(false) }
            var confirmKeepOff by remember { mutableStateOf(false) }
            var pickHelp by remember { mutableStateOf(false) }
            var restoreFrom by remember { mutableStateOf<android.net.Uri?>(null) }
            // 들여오기: 저장했던 파일이 보통 있는 ‘다운로드’ 에서 열기
            val openFile = androidx.activity.compose.rememberLauncherForActivityResult(object : androidx.activity.result.contract.ActivityResultContracts.OpenDocument() {
                override fun createIntent(context: android.content.Context, input: Array<String>) = super.createIntent(context, input).apply {
                    putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"))
                }
            }) { uri -> restoreFrom = uri }
            val backupView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
            androidx.compose.runtime.LaunchedEffect(state.settingsFocus) { if (state.settingsFocus == "backup") { kotlinx.coroutines.delay(300); backupView.bringIntoView(); state.settingsFocus = null } }
            FormSection(header = stringResource(R.string.settings_records), footer = stringResource(R.string.settings_recordsFooter), modifier = Modifier.bringIntoViewRequester(backupView)) {
                FormRow(stringResource(R.string.lines_keep), onClick = { if (state.keepLines) confirmKeepOff = true else state.changeKeepLines(true) }) { Switch(state.keepLines, { if (it) state.changeKeepLines(true) else confirmKeepOff = true }, colors = sw) }
                RowDivider()
                FormRow(stringResource(R.string.backup_export), onClick = { backupNow() }) {
                    // 마지막으로 저장한 날 (조용한 안심 한 줄)
                    if (lastBackup >= 0) TokenText(stringResource(R.string.backup_last, java.time.LocalDate.ofEpochDay(lastBackup).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))), Tokens.TypeScale.footnote, color = p.secondary)
                }
                RowDivider()
                FormRow(stringResource(R.string.backup_import), onClick = { pickHelp = true }, trailing = chevron)
                RowDivider()
                FormRow(stringResource(R.string.lines_export), onClick = {
                    val text = state.exportLines()
                    if (text.isBlank()) state.say(ctx.getString(R.string.lines_exportEmpty))
                    else ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(android.content.Intent.EXTRA_SUBJECT, ctx.getString(R.string.lines_exportTitle)).putExtra(android.content.Intent.EXTRA_TEXT, text), null))
                }) {
                    TokenText(stringResource(R.string.lines_count, "${state.lines.count { it.text.isNotBlank() }}"), Tokens.TypeScale.subhead, color = p.secondary)
                }
                RowDivider()
                FormRow(stringResource(R.string.lines_clear), onClick = { confirmClear = true }) {}
                RowDivider()
                FormRow(stringResource(R.string.erase), onClick = { confirmErase = true }) {}
                if (state.devMode) {
                    RowDivider()
                    FormRow(stringResource(R.string.recall_addSample), onClick = { state.addSampleYearAgo() }) {}
                    RowDivider()
                    FormRow(stringResource(R.string.recall_addRandom), onClick = { state.addSampleRandom() }) {}
                    RowDivider()
                    FormRow(stringResource(R.string.dev_letter), onClick = { state.addSampleLetter() }) {}
                    RowDivider()
                    FormRow(stringResource(R.string.dev_year), onClick = { state.addSampleYear() }) {}
                    RowDivider()
                    // 아침 · 생일 (내일) · 하루 정리 알림을 지금 한 번씩 (켜 두지 않았어도, 알림 권한만 있으면)
                    FormRow(stringResource(R.string.dev_notify), onClick = {
                        io.github.graviton94.carpediem.notify.Daily.post(ctx, force = true)
                        io.github.graviton94.carpediem.notify.Tomorrow.post(ctx, 1, sample = true)
                        io.github.graviton94.carpediem.notify.Evening.post(ctx, force = true)
                    }) {}
                }
            }
            // 기록 남기기를 끄면 지금까지의 글 · 사진도 지워지므로 한 번 묻기
            if (confirmKeepOff) GardenAlert(
                onDismissRequest = { confirmKeepOff = false },
                title = { Text(stringResource(R.string.lines_keepOffConfirm)) },
                text = { Text(stringResource(R.string.backup_before)) },
                confirmButton = { TextButton(onClick = { confirmKeepOff = false; state.changeKeepLines(false) }) { Text(stringResource(R.string.lines_keepOffAction), color = p.danger) } },
                dismissButton = { TextButton(onClick = { confirmKeepOff = false; backupNow() }) { Text(stringResource(R.string.backup_first)) } },
            )
            if (confirmClear) GardenAlert(
                onDismissRequest = { confirmClear = false },
                title = { Text(stringResource(R.string.lines_clearConfirm)) },
                text = { Text(stringResource(R.string.backup_before)) },
                confirmButton = { TextButton(onClick = { confirmClear = false; state.clearLines() }) { Text(stringResource(R.string.lines_clearAction), color = p.danger) } },
                dismissButton = { TextButton(onClick = { confirmClear = false; backupNow() }) { Text(stringResource(R.string.backup_first)) } },
            )
            // 들여오기 전에: 어떤 파일을 골라야 하는지 한 번 알려 줌 (저장할 때의 이름 ‘haru-garden-날짜.json’)
            if (pickHelp) GardenAlert(
                onDismissRequest = { pickHelp = false },
                title = { Text(stringResource(R.string.backup_pickTitle)) },
                text = { Text(stringResource(R.string.backup_pickHelp)) },
                confirmButton = { TextButton(onClick = { pickHelp = false; deviceCheck { openFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) } }) { Text(stringResource(R.string.backup_pickGo)) } },
                dismissButton = { TextButton(onClick = { pickHelp = false }) { Text(stringResource(R.string.cancel)) } },
            )
            restoreFrom?.let { uri ->
                val name = remember(uri) { fileName(ctx, uri) }
                GardenAlert(
                    onDismissRequest = { restoreFrom = null },
                    title = { Text(stringResource(R.string.backup_importConfirm)) },
                    text = { Text(listOfNotNull(name?.let { stringResource(R.string.backup_picked, it) }, stringResource(R.string.backup_notOurs).takeIf { name != null && !name.startsWith("haru") }).joinToString("\n\n")) },
                    confirmButton = { TextButton(onClick = {
                        restoreFrom = null
                        settingsScope.launch {
                            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)?.use { state.store.restore(it) } }.getOrNull() == true }
                            if (ok) {
                                state.say(ctx.getString(R.string.backup_done))
                                // 새로 들여온 기록으로 처음부터 (알림 · 위젯도 새로)
                                io.github.graviton94.carpediem.widget.Widgets.refresh(ctx)
                                // 한마디를 잠깐 보인 뒤에
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ (ctx as? android.app.Activity)?.takeIf { !it.isFinishing && !it.isDestroyed }?.recreate() }, (Tokens.Garden.Motion.noteMs / 2).toLong())
                            } else state.say(ctx.getString(R.string.backup_fail))
                        }
                    }) { Text(stringResource(R.string.backup_importAction), color = p.danger) } },
                    dismissButton = { TextButton(onClick = { restoreFrom = null }) { Text(stringResource(R.string.cancel)) } },
                )
            }
            // ── 정원: 디자인 · 내 하루 · 가족 · 정원이 건네는 것 (질문 · 씨앗 · 돌봄) · 문장 언어 ──
            FormSection(header = stringResource(R.string.settings_garden)) {
                FormRow(stringResource(R.string.design)) {
                    ChipPicker(listOf(Design.GARDEN, Design.GLASS), state.design, { Labels.design(ctx, it) }) { state.changeDesign(it) }
                }
                if (state.design == Design.GARDEN) {
                    RowDivider()
                    // 내 하루: ‘2026년 9월 30일에 만난 회색 화강암’ (누르면 돌의 페이지)
                    FormRow(stringResource(R.string.garden_haru), onClick = { onStone(null) }) {
                        TokenText(stringResource(R.string.garden_metOn, state.store.startDate.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),
                            Labels.stone(ctx, io.github.graviton94.carpediem.core.HaruShape.traits(state.store.haruSeed).stone.id)), Tokens.TypeScale.footnote, color = p.secondary, maxLines = 2)
                    }
                    // 가족의 돌: 이름마다 한 줄, 끝에 ‘더하기’
                    state.people.forEach { person -> RowDivider(); FormRow(person.name, onClick = { onStone(person.id) }, trailing = chevron) }
                    if (state.people.size < Tokens.Garden.Family.max.toInt() - 1) { RowDivider(); FormRow(stringResource(R.string.family_add), onClick = onAddPerson, trailing = chevron) }
                    RowDivider()
                    FormRow(stringResource(R.string.collection), onClick = onCollection, trailing = chevron)
                    RowDivider()
                    // 하루의 첫 화면
                    FormRow(stringResource(R.string.title_setting), onClick = { state.changeTitleOn(!state.titleOn) }) { Switch(state.titleOn, { state.changeTitleOn(it) }, colors = sw) }
                    RowDivider()
                    // 오늘의 질문 받기: 끄면 질문 날에도 늘 오늘의 문장
                    FormRow(stringResource(R.string.question_on), onClick = { state.changeQuestionsOn(!state.questionsOn) }) { Switch(state.questionsOn, { state.changeQuestionsOn(it) }, colors = sw) }
                    RowDivider()
                    // 아침 씨앗 (04)
                    FormRow(stringResource(R.string.seed_setting), onClick = { state.changeSeedsOn(!state.seedsOn) }) { Switch(state.seedsOn, { state.changeSeedsOn(it) }, colors = sw) }
                    RowDivider()
                    FormRow(stringResource(R.string.care_setting), onClick = { state.changeCare(!state.careOn) }) { Switch(state.careOn, { state.changeCare(it) }, colors = sw) }
                    if (state.devMode) {
                        RowDivider()
                        FormRow(stringResource(R.string.garden_preview), onClick = { state.changePreviewAll(!state.previewAll) }) { Switch(state.previewAll, { state.changePreviewAll(it) }, colors = sw) }
                    }
                }
                // 문장 언어: 고를 것이 있을 때만 (영어 폰은 영어 하나라 칸째 없음)
                if (io.github.graviton94.carpediem.data.Words.choosable(ctx)) {
                    RowDivider()
                    FormRow(stringResource(R.string.words_language)) {
                        ChipPicker(QuoteLanguage.entries, state.quoteLanguage, { Labels.quoteLanguage(ctx, it) }) { state.changeQuoteLanguage(it) }
                    }
                }
            }
            // ── 위젯: 둘 (남은 날 · 오늘의 한 줄 4×2, 마음의 기록). 런처가 바로 두기를 못 하면 두는 법 한 줄 ──
            if (state.design == Design.GARDEN) FormSection(header = stringResource(R.string.widgets)) {
                listOf(R.string.widgets_name_line to R.string.widget_line_desc, R.string.widgets_name_record to R.string.widget_record_desc).forEachIndexed { i, (name, desc) ->
                    if (i > 0) RowDivider()
                    Row(Modifier.padding(vertical = Tokens.Space.sp3), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
                            TokenText(stringResource(name), Tokens.TypeScale.subhead, weight = FontWeight.SemiBold)
                            TokenText(stringResource(desc), Tokens.TypeScale.footnote, color = p.secondary)
                        }
                        val kind = listOf("line", "record")[i]
                        TextButton(onClick = { if (!io.github.graviton94.carpediem.widget.Widgets.pin(ctx, kind)) state.say(ctx.getString(R.string.nudge_widgetHow)) }) {
                            TokenText(stringResource(R.string.widget_pin), Tokens.TypeScale.footnote, color = p.olive, weight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            // ── 도움 · 응원: 안내 다시 보기 · 의견 보내기 (메일에 기기 · 앱 정보만, 기록은 담지 않음) · 응원하기 ──
            FormSection(header = stringResource(R.string.settings_help), footer = stringResource(R.string.feedback_footer)) {
                if (state.design == Design.GARDEN) {
                    FormRow(stringResource(R.string.guide_again), onClick = { state.draft = null; state.restartGuide(); onClose() }, trailing = chevron)
                    RowDivider()
                }
                FormRow(stringResource(R.string.feedback_row), onClick = { if (!io.github.graviton94.carpediem.data.Feedback.send(ctx)) state.say(ctx.getString(R.string.feedback_copied)) }, trailing = chevron)
                RowDivider()
                FormRow(stringResource(R.string.support), onClick = onSupport, trailing = chevron)
            }
            SettingsFooter(onVersionTap = { if (io.github.graviton94.carpediem.BuildConfig.DEV_TOOLS && !state.devMode) { state.unlockDev(); state.say(ctx.getString(R.string.dev_unlocked)) } })
        }
    }
    if (confirmErase) {
        GardenAlert(
            onDismissRequest = { confirmErase = false },
            title = { Text(stringResource(R.string.erase_confirm)) },
            text = { Text(stringResource(R.string.backup_before)) },
            confirmButton = { TextButton(onClick = { confirmErase = false; state.eraseAll(); onClose()
                // 지운 뒤에는 화면을 새로 지어, 메모리에 남은 지난 값 (묻어 둔 편지 · 씨앗 등) 이 다시 저장되지 않게
                (ctx as? android.app.Activity)?.takeIf { !it.isFinishing && !it.isDestroyed }?.recreate() }) { Text(stringResource(R.string.erase_action), color = p.danger) } },
            dismissButton = { TextButton(onClick = { confirmErase = false; backupNow() }) { Text(stringResource(R.string.backup_first)) } },
        )
    }
    if (askSave) GardenAlert(
        onDismissRequest = { askSave = false },
        title = { Text(stringResource(R.string.settings_saveAsk)) },
        confirmButton = { TextButton(onClick = { askSave = false; state.save(draft); state.draft = null; onClose() }) { Text(stringResource(R.string.settings_save)) } },
        dismissButton = { TextButton(onClick = { askSave = false; state.draft = null; onClose() }) { Text(stringResource(R.string.settings_discard), color = p.secondary) } },
    )
    if (blocked) GardenAlert(
        onDismissRequest = { blocked = false },
        title = { Text(stringResource(R.string.notify_blockedTitle)) },
        text = { Text(stringResource(R.string.notify_blocked)) },
        confirmButton = { TextButton(onClick = {
            blocked = false
            runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)) }
        }) { Text(stringResource(R.string.notify_openSettings)) } },
        dismissButton = { TextButton(onClick = { blocked = false }) { Text(stringResource(R.string.garden_close), color = p.secondary) } },
    )
}

/** 하루의 몇째 분 → 폰의 시각 표기 (오전 7:00 · 7:00 AM · 19:00). */
private fun clockText(ctx: android.content.Context, minute: Int): String =
    java.time.LocalTime.of(minute / 60, minute % 60).format(java.time.format.DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(ctx.resources.configuration.locales[0]))

/** 시각 고르기 (폰의 시계 모양 그대로). */
private fun pickTime(ctx: android.content.Context, minute: Int, onPick: (Int) -> Unit) {
    android.app.TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minute / 60, minute % 60, android.text.format.DateFormat.is24HourFormat(ctx)).show()
}

// ───────────────────────── 나라 선택 ─────────────────────────

@Composable
fun CountryScreen(state: AppState, selected: String, sex: Sex, onPick: (String) -> Unit, onBack: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val locale = ctx.resources.configuration.locales[0]
    var query by remember { mutableStateOf("") }
    val all = remember(locale) {
        val t = state.store.table
        val world = t.countries.filter { it.code == "WLD" }
        world + t.countries.filter { it.code != "WLD" }.sortedWith(compareBy(java.text.Collator.getInstance(locale)) { it.displayName(locale) })
    }
    val shown = if (query.isBlank()) all else all.filter { c -> listOf(Labels.country(ctx, c, c.code), c.sourceName, c.code).any { it.contains(query.trim(), ignoreCase = true) } }
    BackHandler(onBack = onBack)
    SkyBackground {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Theme.deviceClass.pageMargin)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = ctx.getString(R.string.back) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = p.olive)
                }
                TokenText(stringResource(R.string.country), Tokens.TypeScale.headline)
            }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().keepAboveKeyboard(), placeholder = { Text(stringResource(R.string.country_search)) }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) })
            Spacer(Modifier.height(Tokens.Space.sp3))
            LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(shown, key = { it.code }) { c ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = Tokens.Layout.tapTarget).clickable { onPick(c.code); onBack() }.padding(horizontal = Tokens.Space.sp2),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TokenText(Labels.country(ctx, c, c.code), Tokens.TypeScale.body, Modifier.weight(1f))
                        TokenText(stringResource(R.string.expectancy_value, Labels.years(c.expectancy(sex))), Tokens.TypeScale.subhead, color = p.secondary)
                        Box(Modifier.size(Tokens.Layout.tapTarget / 2)) { if (c.code == selected) Icon(Icons.Filled.Check, null, tint = p.olive) }
                    }
                    RowDivider()
                }
                item { TokenText(stringResource(R.string.country_source), Tokens.TypeScale.footnote, Modifier.padding(vertical = Tokens.Space.sp4), color = p.secondary) }
            }
        }
    }
}

private enum class FooterPage { ABOUT, CONTACT, NOTICES }

/** 고른 파일의 이름 (들여오기 전에 맞는 파일인지 보이게). */
internal fun fileName(ctx: android.content.Context, uri: android.net.Uri): String? = runCatching {
    ctx.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}.getOrNull()

/** 설정 맨 아래 바닥글: 소개 · 문의 · 고지사항 (누르면 작은 창), 기록은 기기에만 · 버전. */
@Composable
private fun SettingsFooter(onVersionTap: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    var page by remember { mutableStateOf<FooterPage?>(null) }
    var taps by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp4), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf(FooterPage.ABOUT to R.string.footer_about, FooterPage.CONTACT to R.string.footer_contact, FooterPage.NOTICES to R.string.footer_notices).forEachIndexed { i, (pg, label) ->
                if (i > 0) TokenText("·", Tokens.TypeScale.footnote, color = p.secondary)
                TokenText(stringResource(label), Tokens.TypeScale.footnote, Modifier.heightIn(min = Tokens.Layout.tapTarget).clickable { page = pg }.padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp3), color = p.secondary)
            }
        }
        TokenText(stringResource(R.string.privacy) + " · " + stringResource(R.string.app_name) + " " + ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName, Tokens.TypeScale.caption2,
            Modifier.clickable { taps++; if (taps >= Tokens.Garden.Layout.devTaps.toInt()) onVersionTap() }.padding(Tokens.Space.sp1), color = p.secondary, align = TextAlign.Center)
    }
    page?.let { pg ->
        val email = stringResource(R.string.contact_email)
        GardenAlert(
            onDismissRequest = { page = null },
            title = { Text(stringResource(when (pg) { FooterPage.ABOUT -> R.string.app_name; FooterPage.CONTACT -> R.string.footer_contact; FooterPage.NOTICES -> R.string.footer_notices })) },
            text = {
                Text(when (pg) {
                    FooterPage.ABOUT -> stringResource(R.string.about_body)
                    FooterPage.CONTACT -> stringResource(R.string.contact_body) + "\n\n" + email.ifBlank { stringResource(R.string.contact_soon) }
                    FooterPage.NOTICES -> stringResource(R.string.notices_privacy) + "\n\n" + stringResource(R.string.licenses_body) + "\n\n" + stringResource(R.string.country_source)
                })
            },
            confirmButton = {
                if (pg == FooterPage.CONTACT && email.isNotBlank()) TextButton(onClick = {
                    page = null; io.github.graviton94.carpediem.data.Feedback.send(ctx)
                }) { Text(stringResource(R.string.contact_send)) }
                else TextButton(onClick = { page = null }) { Text(stringResource(R.string.garden_close)) }
            },
            dismissButton = if (pg == FooterPage.CONTACT && email.isNotBlank()) ({ TextButton(onClick = { page = null }) { Text(stringResource(R.string.garden_close)) } }) else null,
        )
    }
}
