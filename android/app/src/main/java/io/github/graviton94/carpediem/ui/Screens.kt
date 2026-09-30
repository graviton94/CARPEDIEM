package io.github.graviton94.carpediem.ui

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
import io.github.graviton94.carpediem.data.QuoteLanguage
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
            Column(Modifier.padding(top = Tokens.Space.sp10, start = Tokens.Space.sp2, bottom = Tokens.Space.sp5), verticalArrangement = Arrangement.spacedBy(Tokens.Space.sp3)) {
                TokenText("Carpe Diem", Tokens.TypeScale.display(Theme.deviceClass))
                TokenText(stringResource(R.string.tagline), Tokens.TypeScale.title3, color = p.secondary)
            }
            ProfileFields(state, draft, { state.draft = it }, onCountry)
            Box(
                Modifier.fillMaxWidth().padding(top = Tokens.Space.sp3).heightIn(min = Tokens.Layout.tapTarget + Tokens.Space.sp2)
                    .clip(RoundedCornerShape(Tokens.Radius.pill)).background(p.olive).clickable { state.save(draft); state.draft = null },
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
                listOf(-0.5, 0.5).forEach { step ->
                    TextButton(onClick = { onChange(draft.copy(customExpectancy = (value + step).coerceIn(30.0, 120.0))) }) {
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
fun HomeScreen(state: AppState, profile: LifeProfile, now: LocalDateTime, onSettings: () -> Unit) {
    val p = Theme.palette
    val ctx = LocalContext.current
    val s = LifeSnapshot(profile.birthDate, profile.expectancy(state.store.table), now)
    SkyBackground {
        Page {
            Row(Modifier.fillMaxWidth().padding(top = Tokens.Space.sp2), verticalAlignment = Alignment.CenterVertically) {
                TokenText("Carpe Diem", Tokens.TypeScale.largeTitle, Modifier.padding(start = Tokens.Space.sp1))
                Spacer(Modifier.weight(1f))
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
        }
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
fun SettingsScreen(state: AppState, profile: LifeProfile, onClose: () -> Unit, onCountry: () -> Unit) {
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
            FormSection(header = stringResource(R.string.words)) {
                FormRow(stringResource(R.string.words_language)) {
                    ChipPicker(QuoteLanguage.entries, state.quoteLanguage, { Labels.quoteLanguage(ctx, it) }) { state.changeQuoteLanguage(it) }
                }
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
            FormSection(footer = stringResource(R.string.privacy) + "\nCarpe Diem " + ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName) {
                FormRow(stringResource(R.string.erase), onClick = { confirmErase = true }) {}
            }
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
