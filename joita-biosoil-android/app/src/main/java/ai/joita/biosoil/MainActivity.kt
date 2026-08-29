package ai.joita.biosoil

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import ai.joita.biosoil.collective.CollectiveApp
import ai.joita.biosoil.ui.JoitaTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JoitaTheme {
                CollectiveApp()
            }
        }
    }
}
