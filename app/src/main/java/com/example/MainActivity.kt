package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FileHelper
import com.example.viewmodel.ViewerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ViewerViewModel by viewModels {
        ViewerViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming intent if the user opened an XML or HTML file from another app
        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MyApplicationTheme(themeMode = uiState.themeMode) {
                MainViewerScreen(
                    viewModel = viewModel,
                    onFinishApp = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val dataUri = intent.data

        if ((Intent.ACTION_VIEW == action || Intent.ACTION_EDIT == action) && dataUri != null) {
            // Take persistable permission if possible
            FileHelper.tryTakePersistableUriPermission(this, dataUri)
            val autoEdit = (Intent.ACTION_EDIT == action)
            viewModel.openFileFromUri(this, dataUri, autoSwitchToEditor = autoEdit)
        }
    }
}
