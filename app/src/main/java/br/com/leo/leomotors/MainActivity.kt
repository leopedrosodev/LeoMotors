package br.com.leo.leomotors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.leo.leomotors.core.di.AppContainer
import br.com.leo.leomotors.presentation.app.LeoMotorsRoot
import br.com.leo.leomotors.reminder.ReminderScheduler

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.initialize(this)

        setContent {
            LeoMotorsRoot(appContainer = appContainer)
        }
    }
}
