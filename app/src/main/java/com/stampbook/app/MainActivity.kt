package com.stampbook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stampbook.app.nav.StampbookApp
import com.stampbook.app.ui.theme.StampbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StampbookTheme {
                StampbookApp()
            }
        }
    }
}
