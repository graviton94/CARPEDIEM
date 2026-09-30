package io.github.graviton94.carpediem.ui

import io.github.graviton94.carpediem.ui.garden.LetGoModal
import io.github.graviton94.carpediem.ui.garden.LetGoSection
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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

// ───────────────────────── 온보딩 ─────────────────────────

@Composable
fun OnboardingScreen(state: AppState, onCountry: () -> Unit) {
    val p = Theme.palette
    val draft = state.draft ?: state.defaultProfile().also { state.draft = it }
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
            ProfileFields(state, draft, { state.draft = it }, onCountry)
            if (Theme.garden) GardenButton(stringResource(R.string.begin), { state.begin(draft); state.draft = null }, filled = true, seed = 740, modifier = Modifier.padding(top = Tokens.Space.sp3))
            else Box(
                Modifier.fillMaxWidth().padding(top = Tokens.Space.sp3).heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp2)
                    .clip(RoundedCornerShape(Tokens.Radius.pill)).background(p.olive).clickable { state.begin(draft); state.draft = null },
                contentAlignment = Alignment.Center,
            ) { TokenText(stringResource(R.string.begin), Tokens.TypeScale.headline, color = p.onOlive) }
            TokenText(stringResource(R.string.privacy), Tokens.TypeScale.footnote, Modifier.fillMaxWidth(), color = p.secondary, align = TextAlign.Center)
        }
    }
}

// ───────────────────────── 나의 정보 입력 ─────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFields(state: AppState, draft: LifeProfile, onChange: (LifeProfile) -> Unit, onCountry: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val table = state.store.table
    var picking by remember { mutableStateOf(false) }
    var sexMenu by remember { mutableStateOf(false) }
    val dateText = draft.birthDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    FormSection(header = stringResource(R.string.you)) {
        FormRow(stringResource(R.string.birthday), onClick = { picking = true }) {
            Box(Modifier.clip(RoundedCornerShape(Tokens.Radius.sm)).background(p.dim).padding(horizontal = Tokens.Space.sp3, vertical = Tokens.Space.sp1)) {
                TokenText(dateText, Tokens.TypeScale.callout)
            }
        }
        RowDivider()
        FormRow(stringResource(R.string.country), onClick = onCountry) {
            TokenText(Labels.country(ctx, table.country(draft.countryCode), draft.countryCode), Tokens.TypeScale.body, color = p.secondary)
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = p.secondary)
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
        val dp = rememberDatePickerState(initialSelectedDateMillis = draft.birthDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (!d.isAfter(LocalDate.now())) onChange(draft.copy(birthDate = d))
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
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = p.secondary)
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
    val main = if (language == QuoteLanguage.ENGLISH) q.english else q.korean
    GlassCard(onClick = onNext) {
        TokenText(stringResource(R.string.words).uppercase(), Tokens.TypeScale.caption1, color = p.olive, weight = FontWeight.Bold)
        TokenText(main, Tokens.TypeScale.headline.serif())
        if (language == QuoteLanguage.BOTH) TokenText(q.english, Tokens.TypeScale.footnote.serif(), color = p.secondary)
        TokenText(stringResource(R.string.words_next), Tokens.TypeScale.caption2, color = p.secondary, weight = FontWeight.Normal)
    }
}

// ───────────────────────── 설정 ─────────────────────────

@Composable
fun SettingsScreen(state: AppState, profile: LifeProfile, onClose: () -> Unit, onCountry: () -> Unit, onCollection: () -> Unit, onSupport: () -> Unit, onStone: (String?) -> Unit = {}, onAddPerson: () -> Unit = {}) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val draft = state.draft ?: profile.also { state.draft = it }
    var confirmErase by remember { mutableStateOf(false) }
    BackHandler { state.draft = null; onClose() }
    SkyBackground {
        Page {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { state.draft = null; onClose() }) { TokenText(stringResource(R.string.cancel), Tokens.TypeScale.body, color = p.olive) }
                Spacer(Modifier.weight(1f))
                TokenText(stringResource(R.string.settings), Tokens.TypeScale.headline)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { state.save(draft); state.draft = null; onClose() }) { TokenText(stringResource(R.string.done), Tokens.TypeScale.headline, color = p.olive) }
            }
            ProfileFields(state, draft, { state.draft = it }, onCountry)
            FormSection(header = stringResource(R.string.design)) {
                FormRow(stringResource(R.string.design)) {
                    ChipPicker(Design.entries, state.design, { Labels.design(ctx, it) }) { state.changeDesign(it) }
                }
                if (state.design == Design.GARDEN) {
                    RowDivider()
                    // 내 하루: ‘2026년 9월 30일에 만난 회색 화강암’ (누르면 돌의 페이지)
                    FormRow(stringResource(R.string.garden_haru), onClick = { onStone(null) }) {
                        TokenText(stringResource(R.string.garden_metOn, state.store.startDate.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),
                            Labels.stone(ctx, io.github.graviton94.carpediem.core.HaruShape.traits(state.store.haruSeed).stone.id)), Tokens.TypeScale.footnote, color = p.secondary, maxLines = 2)
                    }
                    RowDivider()
                    FormRow(stringResource(R.string.collection), onClick = onCollection) {
                        Icon(Icons.Filled.KeyboardArrowRight, null, tint = p.secondary)
                    }
                    if (state.devMode) {
                        RowDivider()
                        FormRow(stringResource(R.string.garden_preview), onClick = { state.changePreviewAll(!state.previewAll) }) {
                            Switch(state.previewAll, { state.changePreviewAll(it) }, colors = SwitchDefaults.colors(checkedTrackColor = p.olive))
                        }
                    }
                }
            }
            // 가족의 정원 (정원 디자인에서만. 유리 디자인은 안내 한 줄)
            if (state.design == Design.GARDEN) FormSection(header = stringResource(R.string.family)) {
                state.people.forEachIndexed { i, person ->
                    if (i > 0) RowDivider()
                    FormRow(person.name, onClick = { onStone(person.id) }) { Icon(Icons.Filled.KeyboardArrowRight, null, tint = p.secondary) }
                }
                if (state.people.size < Tokens.Garden.Family.max.toInt() - 1) {
                    if (state.people.isNotEmpty()) RowDivider()
                    FormRow(stringResource(R.string.family_add), onClick = onAddPerson) { Icon(Icons.Filled.KeyboardArrowRight, null, tint = p.secondary) }
                }
            } else TokenText(stringResource(R.string.family_glassNote), Tokens.TypeScale.footnote, Modifier.padding(horizontal = Tokens.Space.sp4), color = p.secondary)
            FormSection(header = stringResource(R.string.defaults), footer = stringResource(R.string.defaults_footer)) {
                Column(Modifier.padding(vertical = Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    TokenText(stringResource(R.string.defaults_unit), Tokens.TypeScale.body)
                    ChipPicker(LifeUnit.entries, state.defaultUnit, { Labels.unit(ctx, it) }) { state.changeDefaultUnit(it) }
                }
                RowDivider()
                Column(Modifier.padding(vertical = Tokens.Space.sp2), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp2)) {
                    TokenText(stringResource(R.string.defaults_grid), Tokens.TypeScale.body)
                    ChipPicker(GridScale.entries, state.defaultGrid, { Labels.gridShort(ctx, it) }) { state.changeDefaultGrid(it) }
                }
            }
            FormSection(header = stringResource(R.string.words)) {
                FormRow(stringResource(R.string.words_language)) {
                    ChipPicker(QuoteLanguage.entries, state.quoteLanguage, { Labels.quoteLanguage(ctx, it) }) { state.changeQuoteLanguage(it) }
                }
            }
            val permission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok -> state.changeNotify(ok) }
            fun toggleNotify(on: Boolean) {
                if (on && android.os.Build.VERSION.SDK_INT >= 33 && !io.github.graviton94.carpediem.notify.Daily.allowed(ctx)) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else state.changeNotify(on)
            }
            FormSection(footer = stringResource(R.string.notify_footer)) {
                FormRow(stringResource(R.string.notify_row), onClick = { toggleNotify(!state.notify) }) {
                    Switch(state.notify, { toggleNotify(it) }, colors = SwitchDefaults.colors(checkedTrackColor = p.olive))
                }
            }
            run {
                var confirmClear by remember { mutableStateOf(false) }
                FormSection(header = stringResource(R.string.lines), footer = stringResource(R.string.lines_keepFooter) + if (Theme.garden) "\n" + stringResource(R.string.care_footer) else "") {
                    FormRow(stringResource(R.string.lines_keep), onClick = { state.changeKeepLines(!state.keepLines) }) {
                        Switch(state.keepLines, { state.changeKeepLines(it) }, colors = SwitchDefaults.colors(checkedTrackColor = p.olive))
                    }
                    if (Theme.garden) {
                        RowDivider()
                        FormRow(stringResource(R.string.care_setting), onClick = { state.changeCare(!state.careOn) }) {
                            Switch(state.careOn, { state.changeCare(it) }, colors = SwitchDefaults.colors(checkedTrackColor = p.olive))
                        }
                    }
                    RowDivider()
                    FormRow(stringResource(R.string.lines_export), onClick = {
                        val text = state.exportLines()
                        if (text.isBlank()) android.widget.Toast.makeText(ctx, ctx.getString(R.string.lines_exportEmpty), android.widget.Toast.LENGTH_SHORT).show()
                        else ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(android.content.Intent.EXTRA_SUBJECT, ctx.getString(R.string.lines_exportTitle)).putExtra(android.content.Intent.EXTRA_TEXT, text), null))
                    }) {
                        TokenText(stringResource(R.string.lines_count, "${state.lines.count { it.text.isNotBlank() }}"), Tokens.TypeScale.subhead, color = p.secondary)
                    }
                    RowDivider()
                    FormRow(stringResource(R.string.lines_clear), onClick = { confirmClear = true }) {}
                    if (state.devMode) {
                        RowDivider()
                        FormRow(stringResource(R.string.recall_addSample), onClick = { state.addSampleYearAgo() }) {}
                        RowDivider()
                        FormRow(stringResource(R.string.recall_addRandom), onClick = { state.addSampleRandom() }) {}
                        RowDivider()
                        FormRow(stringResource(R.string.dev_letter), onClick = { state.addSampleLetter() }) {}
                    }
                }
                if (confirmClear) AlertDialog(
                    onDismissRequest = { confirmClear = false },
                    title = { Text(stringResource(R.string.lines_clearConfirm)) },
                    confirmButton = { TextButton(onClick = { confirmClear = false; state.clearLines() }) { Text(stringResource(R.string.lines_clearAction), color = p.danger) } },
                    dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
                )
            }
            FormSection(header = stringResource(R.string.widgets)) {
                listOf(Icons.Filled.Home to R.string.widgets_android1, Icons.Filled.Search to R.string.widgets_android2, Icons.Filled.Edit to R.string.widgets_android3).forEachIndexed { i, (icon, text) ->
                    if (i > 0) RowDivider()
                    Row(Modifier.padding(vertical = Tokens.Space.sp3), horizontalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                        Icon(icon, null, tint = p.olive, modifier = Modifier.size(Tokens.Stroke.icon))
                        TokenText(stringResource(text), Tokens.TypeScale.subhead)
                    }
                }
            }
            FormSection {
                FormRow(stringResource(R.string.support), onClick = onSupport) {
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = p.secondary)
                }
            }
            // 도움이 필요할 때: 앱이 먼저 판단해 띄우지 않고, 늘 여기 조용히
            FormSection(header = stringResource(R.string.help), footer = stringResource(R.string.help_body)) {
                fun dial(n: String) = runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$n"))) }
                FormRow(stringResource(R.string.help_suicide), onClick = { dial("109") }) {}
                RowDivider()
                FormRow(stringResource(R.string.help_crisis), onClick = { dial("15770199") }) {}
                RowDivider()
                FormRow(stringResource(R.string.help_global), onClick = { runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://findahelpline.com"))) } }) {}
            }
            FormSection {
                FormRow(stringResource(R.string.erase), onClick = { confirmErase = true }) {}
            }
            // 맨 아래: 웹사이트 바닥글처럼 소개 · 문의 · 고지사항, 그 아래 버전 (여러 번 누르면 개발자 모드)
            SettingsFooter(onVersionTap = { if (!state.devMode) { state.unlockDev(); android.widget.Toast.makeText(ctx, ctx.getString(R.string.dev_unlocked), android.widget.Toast.LENGTH_SHORT).show() } })
        }
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text(stringResource(R.string.erase_confirm)) },
            confirmButton = { TextButton(onClick = { confirmErase = false; state.eraseAll(); onClose() }) { Text(stringResource(R.string.erase_action), color = p.danger) } },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
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
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.country_search)) }, singleLine = true,
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
                TokenText(stringResource(label), Tokens.TypeScale.footnote, Modifier.clickable { page = pg }.padding(horizontal = Tokens.Space.sp2, vertical = Tokens.Space.sp2), color = p.secondary)
            }
        }
        TokenText(stringResource(R.string.privacy) + " · " + stringResource(R.string.app_name) + " " + ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName, Tokens.TypeScale.caption2,
            Modifier.clickable { taps++; if (taps >= Tokens.Garden.Layout.devTaps.toInt()) onVersionTap() }.padding(Tokens.Space.sp1), color = p.secondary, align = TextAlign.Center)
    }
    page?.let { pg ->
        val email = stringResource(R.string.contact_email)
        AlertDialog(
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
                    page = null; runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:$email"))) }
                }) { Text(stringResource(R.string.contact_send)) }
                else TextButton(onClick = { page = null }) { Text(stringResource(R.string.garden_close)) }
            },
            dismissButton = if (pg == FooterPage.CONTACT && email.isNotBlank()) ({ TextButton(onClick = { page = null }) { Text(stringResource(R.string.garden_close)) } }) else null,
        )
    }
}
