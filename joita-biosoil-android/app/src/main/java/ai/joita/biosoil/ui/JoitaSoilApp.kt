package ai.joita.biosoil.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import ai.joita.biosoil.R
import ai.joita.biosoil.data.SoilRepository
import ai.joita.biosoil.model.SoilTestRecord

private enum class TopDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Rounded.Home),
    FIELDS(R.string.nav_fields, Icons.Rounded.Landscape),
    TESTS(R.string.nav_tests, Icons.Rounded.Science),
    MORE(R.string.nav_more, Icons.Rounded.MoreHoriz),
}

private sealed interface OverlayScreen {
    data object TestFlow : OverlayScreen
    data class Result(val test: SoilTestRecord) : OverlayScreen
}

@Composable
fun JoitaSoilApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("joita_preferences", Context.MODE_PRIVATE) }
    var languageChosen by remember { mutableStateOf(preferences.getBoolean("language_chosen", false)) }
    if (!languageChosen) {
        LanguageWelcome { tag ->
            preferences.edit().putBoolean("language_chosen", true).apply()
            languageChosen = true
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
        return
    }

    val repository = remember { SoilRepository(context) }
    var destination by remember { mutableStateOf(TopDestination.HOME) }
    var fields by remember { mutableStateOf(repository.fields()) }
    var tests by remember { mutableStateOf(repository.tests()) }
    var overlay by remember { mutableStateOf<OverlayScreen?>(null) }

    fun refresh() {
        fields = repository.fields()
        tests = repository.tests()
    }

    when (val current = overlay) {
        OverlayScreen.TestFlow -> TestWizard(
            fields = fields,
            onBack = { overlay = null },
            onNeedField = {
                overlay = null
                destination = TopDestination.FIELDS
            },
            onSaved = { test ->
                repository.saveTest(test)
                refresh()
                overlay = OverlayScreen.Result(test)
            },
        )
        is OverlayScreen.Result -> ResultScreen(
            test = current.test,
            onBack = { overlay = null },
        )
        null -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.height(72.dp),
                ) {
                    TopDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.label)) },
                            label = { Text(stringResource(item.label), textAlign = TextAlign.Center) },
                        )
                    }
                }
            },
        ) { padding ->
            when (destination) {
                TopDestination.HOME -> HomeScreen(
                    tests = tests,
                    padding = padding,
                    onStartTest = { overlay = OverlayScreen.TestFlow },
                    onAddField = { destination = TopDestination.FIELDS },
                    onOpenResult = { overlay = OverlayScreen.Result(it) },
                )
                TopDestination.FIELDS -> FieldsScreen(
                    fields = fields,
                    padding = padding,
                    onAdd = { draft ->
                        repository.addField(draft)
                        refresh()
                    },
                    onStartTest = { overlay = OverlayScreen.TestFlow },
                )
                TopDestination.TESTS -> TestsScreen(
                    tests = tests,
                    padding = padding,
                    onStartTest = { overlay = OverlayScreen.TestFlow },
                    onOpenResult = { overlay = OverlayScreen.Result(it) },
                )
                TopDestination.MORE -> MoreScreen(
                    padding = padding,
                    onDataCleared = {
                        repository.clearAll()
                        refresh()
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageWelcome(onLanguage: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.joita_bioseed_logo),
                    contentDescription = stringResource(R.string.publisher),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))
                Text(stringResource(R.string.choose_language), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onLanguage("en") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(stringResource(R.string.english)) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onLanguage("hi") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(stringResource(R.string.hindi)) }
            }
        }
    }
}

@Composable
internal fun ScreenContainer(
    title: String,
    padding: PaddingValues,
    content: @Composable (PaddingValues) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
        content(PaddingValues(horizontal = 16.dp))
    }
}
