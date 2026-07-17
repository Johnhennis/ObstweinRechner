package com.example.fruchtweinrechner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import com.example.fruchtweinrechner.ui.navigation.AppNavigation
import com.example.fruchtweinrechner.ui.settings.SettingsViewModel
import com.example.fruchtweinrechner.ui.theme.FruchtweinRechnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as FruchtweinApplication
        val factory = AppViewModelFactory.from(application)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val fontScale by settingsViewModel.fontScale.collectAsState()
            val baseDensity = LocalDensity.current

            FruchtweinRechnerTheme {
                CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(factory = factory)
                    }
                }
            }
        }
    }
}
