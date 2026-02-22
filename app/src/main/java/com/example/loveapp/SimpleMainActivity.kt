package com.example.loveapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.theme.LoveAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SimpleMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LoveApp", "SimpleMainActivity.onCreate() START")
        try {
            super.onCreate(savedInstanceState)
            Log.d("LoveApp", "SimpleMainActivity.onCreate() SUPER done - Hilt injected")
            
            enableEdgeToEdge()
            setContent {
                LoveAppTheme {
                    SimpleTestScreen()
                }
            }
            Log.d("LoveApp", "SimpleMainActivity.onCreate() SUCCESS")
        } catch (e: Exception) {
            Log.e("LoveApp", "SimpleMainActivity.onCreate() FAILED", e)
            e.printStackTrace()
            throw e
        }
    }
}

@Composable
fun SimpleTestScreen(viewModel: SimpleTestViewModel = hiltViewModel()) {
    Log.d("LoveApp", "SimpleTestScreen composing - ViewModel injected")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LoveApp Test Screen with Hilt ViewModel")
        Text("If you see this, Hilt is working!")
        Button(onClick = { }) {
            Text("Test Button")
        }
    }
}