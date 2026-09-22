package com.marwadiuniversity.trustlens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.marwadiuniversity.trustlens.navigation.NavGraph
import com.marwadiuniversity.trustlens.ui.theme.TrustLensTheme
import com.marwadiuniversity.trustlens.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrustLensTheme(darkTheme = viewModel.isDarkTheme) {
                NavGraph(viewModel = viewModel)
            }
        }
    }
}
