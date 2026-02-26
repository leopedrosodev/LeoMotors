package br.com.tec.tecmotors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.tec.tecmotors.core.di.AppContainer
import br.com.tec.tecmotors.presentation.app.TecMotorsRoot
import br.com.tec.tecmotors.reminder.ReminderScheduler

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.initialize(this)

        setContent {
            TecMotorsRoot(appContainer = appContainer)
        }
    }
}
