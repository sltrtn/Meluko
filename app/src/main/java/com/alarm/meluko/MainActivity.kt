package com.alarm.meluko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.alarm.meluko.ui.HomeScreen
import com.alarm.meluko.ui.LoginScreen
import com.alarm.meluko.ui.theme.MelukoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelukoTheme {
                var currentScreen by remember { mutableStateOf("login") }

                when (currentScreen) {
                    "login" -> LoginScreen(onLoginSuccess = {
                        currentScreen = "home"
                    })
                    "home" -> HomeScreen()
                }
            }
        }
    }
}
