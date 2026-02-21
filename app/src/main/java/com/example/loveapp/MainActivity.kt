package com.example.loveapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.loveapp.navigation.Screen
import com.example.loveapp.ui.screens.ActivityFeedScreen
import com.example.loveapp.ui.screens.CustomCalendarsScreen
import com.example.loveapp.ui.screens.DashboardScreen
import com.example.loveapp.ui.screens.LoginScreen
import com.example.loveapp.ui.screens.MenstrualCalendarScreen
import com.example.loveapp.ui.screens.MoodTrackerScreen
import com.example.loveapp.ui.screens.NotesScreen
import com.example.loveapp.ui.screens.RelationshipDashboardScreen
import com.example.loveapp.ui.screens.SettingsScreen
import com.example.loveapp.ui.screens.SignupScreen
import com.example.loveapp.ui.screens.WishesScreen
import com.example.loveapp.ui.theme.LoveAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoveAppTheme {
                LoveAppNavigation()
            }
        }
    }
}

@Composable
fun LoveAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToNotes = { navController.navigate(Screen.Notes.route) },
                onNavigateToWishes = { navController.navigate(Screen.Wishes.route) },
                onNavigateToMood = { navController.navigate(Screen.MoodTracker.route) },
                onNavigateToActivity = { navController.navigate(Screen.ActivityFeed.route) },
                onNavigateToMenstrual = { navController.navigate(Screen.MenstrualCalendar.route) },
                onNavigateToCalendars = { navController.navigate(Screen.CustomCalendars.route) },
                onNavigateToRelationship = { navController.navigate(Screen.RelationshipDashboard.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Notes.route) {
            NotesScreen(navController)
        }

        composable(Screen.Wishes.route) {
            WishesScreen(navController)
        }

        composable(Screen.MoodTracker.route) {
            MoodTrackerScreen(navController)
        }

        composable(Screen.ActivityFeed.route) {
            ActivityFeedScreen(navController)
        }

        composable(Screen.MenstrualCalendar.route) {
            MenstrualCalendarScreen(navController)
        }

        composable(Screen.CustomCalendars.route) {
            CustomCalendarsScreen(navController)
        }

        composable(Screen.RelationshipDashboard.route) {
            RelationshipDashboardScreen(navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
    }
}