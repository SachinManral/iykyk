package com.iykyk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iykyk.app.presentation.navigation.IykykNavGraph
import com.iykyk.app.presentation.theme.IykykTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IykykTheme {
                IykykNavGraph()
            }
        }
    }
}
