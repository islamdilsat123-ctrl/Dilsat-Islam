package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.GolpoKoriTheme
import com.example.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val model: ChatViewModel = viewModel()
      GolpoKoriTheme(darkTheme = model.currentThemeIsDark) {
        MainAppScreen(viewModel = model)
      }
    }
  }
}

