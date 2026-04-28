package com.example.pickuphoos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.pickuphoos.navigation.PickupHoosNavGraph
import com.example.pickuphoos.navigation.Routes
import com.example.pickuphoos.ui.screens.LoginScreen
import com.example.pickuphoos.ui.theme.PickupHoosTheme
import com.example.pickuphoos.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    @ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PickupHoosTheme {
                val navController = rememberNavController()
                PickupHoosNavGraph(navController = navController)
            }
        }
    }
}