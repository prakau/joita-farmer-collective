package ai.joita.biosoil.ui

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import ai.joita.biosoil.BuildConfig
import ai.joita.biosoil.R
import ai.joita.biosoil.model.FieldDraft
import ai.joita.biosoil.model.FieldProfile
import ai.joita.biosoil.model.ReadingSource
import ai.joita.biosoil.model.SoilStatus
import ai.joita.biosoil.model.SoilTestRecord
import ai.joita.biosoil.report.ReportService
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeScreen(
    tests: List<SoilTestRecord>,
    padding: PaddingValues,
    onStartTest: () -> Unit,
    onAddField: () -> Unit,
    onOpenResult: (SoilTestRecord) -> Unit,
) {
    val context = LocalContext.current
    val online = remember { context.isOnline() }
    ScreenContainer(stringResource(R.string.home_greeting), padding) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(if (online) stringResource(R.string.online) else stringResource(R.string.offline), Icons.Rounded.CloudOff)
                    StatusChip(stringResource(R.string.location_not_captured), Icons.Rounded.LocationOff)
                    StatusChip(stringResource(R.string.sensor_not_connected), Icons.Rounded.SensorsOff)
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = JoitaGreenDark),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.start_soil_test), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.start_soil_test_body), color = Color.White)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = onStartTest,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        ) { Text(stringResource(R.string.start_soil_test)) }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = LeafLight)) {
                    Text(stringResource(R.string.offline_saved), modifier = Modifier.padding(16.dp), color = JoitaGreenDark)
                }
            }
            item {
                OutlinedButton(onClick = onAddField, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.add_field))
                }
            }
            item {
                Text(stringResource(R.string.recent_result), style = MaterialTheme.typography.titleLarge)
            }
            if (tests.isEmpty()) {
                item { EmptyCard(stringResource(R.string.no_recent_result), stringResource(R.string.no_tests_body)) }
            } else {
                item { TestCard(tests.first(), onClick = { onOpenResult(tests.first()) }) }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.pending_sync), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.local_only), color = InkMuted)
                    }
                }
            }
        }
    }
}

@Composable
internal fun FieldsScreen(
    fields: List<FieldProfile>,
    padding: PaddingValues,
    onAdd: (FieldDraft) -> Unit,
    onStartTest: () -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    ScreenContainer(stringResource(R.string.fields_title), padding) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.add_field))
                }
            }
            if (fields.isEmpty()) {
                item { EmptyCard(stringResource(R.string.no_fields), stringResource(R.string.no_fields_body)) }
            } else {
                items(fields, key = { it.id }) { field ->
                    Card {
                        Column(Modifier.padding(16.dp)) {
                            Text(field.fieldName, style = MaterialTheme.typography.titleLarge)
                            Text(field.farmerName, fontWeight = FontWeight.SemiBold)
                            Text(listOf(field.village, field.district, field.state).filter { it.isNotBlank() }.joinToString(", "), color = InkMuted)
                            Text(listOfNotNull(field.crop.takeIf(String::isNotBlank), field.plotNumber.takeIf(String::isNotBlank), field.areaAcres?.let { "$it ${stringResource(R.string.acre)}" }).joinToString(" • "), color = InkMuted)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onStartTest) { Text(stringResource(R.string.start_soil_test)) }
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        AddFieldDialog(
            onDismiss = { showAdd = false },
            onSave = {
                onAdd(it)
                showAdd = false
            },
        )
    }
}

@Composable
internal fun TestsScreen(
    tests: List<SoilTestRecord>,
    padding: PaddingValues,
    onStartTest: () -> Unit,
    onOpenResult: (SoilTestRecord) -> Unit,
) {
    ScreenContainer(stringResource(R.string.tests_title), padding) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Button(onClick = onStartTest, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.start_soil_test))
                }
            }
            if (tests.isEmpty()) {
                item { EmptyCard(stringResource(R.string.no_tests), stringResource(R.string.no_tests_body)) }
            } else {
                items(tests, key = { it.id }) { test -> TestCard(test, onClick = { onOpenResult(test) }) }
            }
        }
    }
}

@Composable
internal fun MoreScreen(
    padding: PaddingValues,
    onDataCleared: () -> Unit,
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    ScreenContainer(stringResource(R.string.nav_more), padding) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionCard(Icons.Rounded.CloudOff, stringResource(R.string.sync_and_storage), stringResource(R.string.local_only)) }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = JoitaGreen)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.guidance), style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.guide_sampling))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.guide_otg))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.guide_parameters))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.advisory_disclaimer), fontWeight = FontWeight.SemiBold, color = SoilBrown)
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = JoitaGreen)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }) {
                                Text(stringResource(R.string.english))
                            }
                            OutlinedButton(onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("hi")) }) {
                                Text(stringResource(R.string.hindi))
                            }
                        }
                    }
                }
            }
            item {
                Card {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Image(
                            painter = painterResource(R.drawable.joita_bioseed_logo),
                            contentDescription = stringResource(R.string.publisher),
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.about), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.about_body), color = InkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text(stringResource(R.string.publisher), color = JoitaGreenDark, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.version_format, BuildConfig.VERSION_NAME), color = InkMuted)
                        Text(BuildConfig.APPLICATION_ID, color = InkMuted)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.privacy), color = InkMuted)
                    }
                }
            }
            item {
                if (BuildConfig.SUPPORT_PHONE.isBlank()) {
                    SectionCard(Icons.Rounded.Phone, stringResource(R.string.call_support), stringResource(R.string.support_not_configured))
                } else {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${BuildConfig.SUPPORT_PHONE}"))) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.call_support)) }
                }
            }
            item {
                OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.clear_local_data))
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_data_title)) },
            text = { Text(stringResource(R.string.clear_data_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDataCleared()
                    confirmClear = false
                }) { Text(stringResource(R.string.clear_local_data)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.not_now)) } },
        )
    }
}

@Composable
private fun AddFieldDialog(onDismiss: () -> Unit, onSave: (FieldDraft) -> Unit) {
    var farmer by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var field by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var plotNumber by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    val valid = listOf(farmer, field, village, district, state, crop).all { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_field)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RequiredField(farmer, { farmer = it }, R.string.farmer_name, showErrors)
                OutlinedTextField(phone, { phone = it }, label = { Text(stringResource(R.string.mobile_optional)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                RequiredField(field, { field = it }, R.string.field_name, showErrors)
                RequiredField(village, { village = it }, R.string.village, showErrors)
                RequiredField(district, { district = it }, R.string.district, showErrors)
                RequiredField(state, { state = it }, R.string.state, showErrors)
                OutlinedTextField(pincode, { pincode = it.take(6) }, label = { Text(stringResource(R.string.pincode)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(plotNumber, { plotNumber = it }, label = { Text(stringResource(R.string.plot_number)) }, modifier = Modifier.fillMaxWidth())
                RequiredField(crop, { crop = it }, R.string.main_crop, showErrors)
                OutlinedTextField(area, { area = it }, label = { Text(stringResource(R.string.area_acres_optional)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                showErrors = true
                if (valid) {
                    onSave(
                        FieldDraft(
                            farmerName = farmer,
                            village = village,
                            district = district,
                            state = state,
                            fieldName = field,
                            crop = crop,
                            areaAcres = area.toDoubleOrNull(),
                            phone = phone,
                            pincode = pincode,
                            plotNumber = plotNumber,
                        ),
                    )
                }
            }) { Text(stringResource(R.string.save_field)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.keep_editing)) } },
    )
}

@Composable
private fun RequiredField(value: String, onValue: (String) -> Unit, label: Int, showErrors: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text("${stringResource(label)} *") },
        isError = showErrors && value.isBlank(),
        supportingText = if (showErrors && value.isBlank()) ({ Text(stringResource(R.string.required_field)) }) else null,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatusChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    AssistChip(
        onClick = {},
        label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
internal fun SourceBadge(source: ReadingSource) {
    val color = when (source) {
        ReadingSource.USB -> LeafLight
        ReadingSource.MANUAL -> Color(0xFFDDEEFF)
        ReadingSource.SAMPLE -> Color(0xFFFFE2A8)
    }
    val label = when (source) {
        ReadingSource.USB -> stringResource(R.string.source_usb)
        ReadingSource.MANUAL -> stringResource(R.string.source_manual)
        ReadingSource.SAMPLE -> stringResource(R.string.source_sample)
    }
    Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(color, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun TestCard(test: SoilTestRecord, onClick: () -> Unit) {
    Card(onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(test.fieldLabel, style = MaterialTheme.typography.titleLarge)
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(test.createdAtEpochMs)), color = InkMuted)
                Spacer(Modifier.height(8.dp))
                SourceBadge(test.source)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${test.score}/100", style = MaterialTheme.typography.headlineMedium, color = statusColor(test.status))
                Text(statusText(test.status), fontWeight = FontWeight.SemiBold, color = statusColor(test.status))
            }
        }
    }
}

@Composable
private fun statusText(status: SoilStatus) = stringResource(
    when (status) {
        SoilStatus.GOOD -> R.string.status_good
        SoilStatus.NEEDS_ATTENTION -> R.string.status_attention
        SoilStatus.URGENT -> R.string.status_urgent
    },
)

private fun statusColor(status: SoilStatus) = when (status) {
    SoilStatus.GOOD -> JoitaGreenDark
    SoilStatus.NEEDS_ATTENTION -> SoilBrown
    SoilStatus.URGENT -> Color(0xFFBA1A1A)
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Card {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = JoitaGreen, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = InkMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SectionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Card {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = JoitaGreen)
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, color = InkMuted)
            }
        }
    }
}

private fun Context.isOnline(): Boolean {
    val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivity.activeNetwork ?: return false
    val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
