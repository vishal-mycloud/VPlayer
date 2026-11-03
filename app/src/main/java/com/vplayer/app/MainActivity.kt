package com.vplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vplayer.app.ui.VPlayerRoot
import com.vplayer.app.ui.theme.VPlayerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VPlayerTheme {
                VPlayerRoot(viewModel)
            }
        }
    }
}
