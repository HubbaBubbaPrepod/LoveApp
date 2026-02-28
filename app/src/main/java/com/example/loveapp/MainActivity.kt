package com.example.loveapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.viewmodel.AuthViewModel
import com.example.loveapp.viewmodel.SettingsViewModel
import com.example.loveapp.navigation.Screen
import com.example.loveapp.ui.screens.ActivityFeedScreen
import com.example.loveapp.ui.screens.CustomCalendarsScreen
import com.example.loveapp.ui.screens.DashboardScreen
import com.example.loveapp.ui.screens.LoginScreen
import com.example.loveapp.ui.screens.MenstrualCalendarScreen
import com.example.loveapp.ui.screens.MoodTrackerScreen
import com.example.loveapp.ui.screens.NoteDetailScreen
import com.example.loveapp.ui.screens.NotesScreen
import com.example.loveapp.ui.screens.PairingScreen
import com.example.loveapp.ui.screens.PrivacyPolicyScreen
import com.example.loveapp.ui.screens.RelationshipDashboardScreen
import com.example.loveapp.ui.screens.TermsOfUseScreen
import com.example.loveapp.ui.screens.SettingsScreen
import com.example.loveapp.ui.screens.SetupProfileScreen
import com.example.loveapp.ui.screens.SignupScreen
import com.example.loveapp.ui.screens.WishDetailScreen
import com.example.loveapp.ui.screens.WishesScreen
import com.example.loveapp.ui.theme.LoveAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var widgetDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            Log.d("LoveApp", "MainActivity.onCreate started")

            val splashScreen = installSplashScreen()
            // Анимация как у веб-прелоадера: задержка, затем плавное исчезновение (fade-out 1s ease-in-out)
            splashScreen.setOnExitAnimationListener { provider: SplashScreenViewProvider ->
                val view = provider.view
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    val fadeOut = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
                    fadeOut.setDuration(1000L)
                    fadeOut.setInterpolator(AccelerateDecelerateInterpolator())
                    fadeOut.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            provider.remove()
                        }
                    })
                    fadeOut.start()
                }, 800L)
            }

            widgetDestination = intent.getStringExtra("destination")
            enableEdgeToEdge()
            setContent {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
                LoveAppTheme(darkTheme = isDarkMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true }
                    ) {
                        LoveAppNavigation(widgetDestination = widgetDestination)
                    }
                }
            }
            Log.d("LoveApp", "MainActivity.onCreate completed")
        } catch (e: Exception) {
            Log.e("LoveApp", "Error in MainActivity.onCreate", e)
            throw e
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        widgetDestination = intent.getStringExtra("destination")
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LoveAppNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
    widgetDestination: String? = null
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = null)

    // ── Android 13+ notification permission ───────────────────────────────
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = rememberPermissionState(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        LaunchedEffect(notifPermission.status.isGranted) {
            if (!notifPermission.status.isGranted) {
                notifPermission.launchPermissionRequest()
            }
        }
    }
    // ─────────────────────────────────────────────────────────────────────

    val startDestination = when (isLoggedIn) {
        true -> Screen.Dashboard.route
        false -> Screen.Login.route
        null -> null
    }

    if (startDestination == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    key(startDestination) {
        val navController = rememberNavController()
        // Navigate to the screen requested by a home-screen widget tap
        LaunchedEffect(widgetDestination) {
            if (widgetDestination != null) {
                navController.navigate(widgetDestination!!) { launchSingleTop = true }
            }
        }
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTermsOfUse = { navController.navigate(Screen.TermsOfUse.route) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToProfileSetup = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTermsOfUse = { navController.navigate(Screen.TermsOfUse.route) }
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
            NotesScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToNote = { noteId ->
                    navController.navigate(Screen.NoteDetail.createRoute(noteId))
                }
            )
        }

        composable(
            route = Screen.NoteDetail.route,
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
            NoteDetailScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Wishes.route) {
            WishesScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWish = { wishId ->
                    navController.navigate(Screen.WishDetail.createRoute(wishId))
                }
            )
        }

        composable(
            route = Screen.WishDetail.route,
            arguments = listOf(navArgument("wishId") { type = NavType.IntType })
        ) { backStackEntry ->
            val wishId = backStackEntry.arguments?.getInt("wishId") ?: -1
            WishDetailScreen(
                wishId = wishId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MoodTracker.route) {
            MoodTrackerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.ActivityFeed.route) {
            ActivityFeedScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.MenstrualCalendar.route) {
            MenstrualCalendarScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.CustomCalendars.route) {
            CustomCalendarsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.RelationshipDashboard.route) {
            RelationshipDashboardScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPairing = { navController.navigate(Screen.Pairing.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                onNavigateToTermsOfUse = { navController.navigate(Screen.TermsOfUse.route) },
                authViewModel = authViewModel
            )
        }

        composable(Screen.Pairing.route) {
            PairingScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.TermsOfUse.route) {
            TermsOfUseScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.ProfileSetup.route) {
            SetupProfileScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }
        }
    }
}