package com.alarm.meluko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.alarm.meluko.navigation.AppNavGraph
import com.alarm.meluko.ui.theme.MelukoTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

/**
 * @AndroidEntryPoint — tells Hilt to inject dependencies into this Activity.
 * Without this annotation, hiltViewModel() inside AppNavGraph would crash
 * because there's no Hilt component attached to the Activity.
 *
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelukoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // AppNavGraph owns routing — MainActivity knows nothing about screens
                    AppNavGraph()
                }
            }
        }
    }
}
