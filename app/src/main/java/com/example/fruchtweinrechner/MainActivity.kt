package com.example.fruchtweinrechner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.fruchtweinrechner.ui.AppViewModelFactory
import com.example.fruchtweinrechner.ui.navigation.AppNavigation
import com.example.fruchtweinrechner.ui.theme.FruchtweinRechnerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as FruchtweinApplication
        val factory = AppViewModelFactory.from(application)

        setContent {
            FruchtweinRechnerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(factory = factory)
                }
            }
        }
    }
}
