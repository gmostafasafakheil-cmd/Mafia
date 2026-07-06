package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.ui.MafiaScreen
import com.example.ui.MafiaViewModel
import com.example.ui.MafiaViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize Room Database and Repository
        val database = GameDatabase.getDatabase(applicationContext)
        val repository = GameRepository(database.matchDao(), applicationContext)
        
        setContent {
            MyApplicationTheme {
                // Instantiate our MafiaViewModel with custom Factory
                val viewModel: MafiaViewModel = viewModel(
                    factory = MafiaViewModelFactory(repository)
                )
                
                MafiaScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
