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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.loveapp.ui.screens.ArtGalleryScreen
import com.example.loveapp.ui.screens.CanvasEditorScreen
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
import com.example.loveapp.ui.screens.ChatScreen
import com.example.loveapp.ui.screens.DrawingScreen
import com.example.loveapp.ui.screens.MemorialDaysScreen
import com.example.loveapp.ui.screens.SparkScreen
import com.example.loveapp.ui.screens.TaskCenterScreen
import com.example.loveapp.ui.screens.SleepTrackerScreen
import com.example.loveapp.ui.screens.GalleryScreen
import com.example.loveapp.ui.screens.LoveTouchScreen
import com.example.loveapp.ui.screens.MissYouScreen
import com.example.loveapp.ui.screens.AppLockScreen
import com.example.loveapp.ui.screens.ChatSettingsScreen
import com.example.loveapp.ui.screens.DailyQAScreen
import com.example.loveapp.ui.screens.IntimacyScreen
import com.example.loveapp.ui.screens.MomentsScreen
import com.example.loveapp.ui.screens.PlacesScreen
import com.example.loveapp.ui.screens.PetScreen
import com.example.loveapp.ui.screens.GamesHubScreen
import com.example.loveapp.ui.screens.AchievementsScreen
import com.example.loveapp.ui.screens.LettersScreen
import com.example.loveapp.ui.screens.BucketListScreen
import com.example.loveapp.ui.screens.MoodInsightsScreen
import com.example.loveapp.ui.screens.PremiumScreen
import com.example.loveapp.ui.screens.StoryScreen
import com.example.loveapp.ui.screens.LocationMapScreen
import com.example.loveapp.ui.screens.GeofencesScreen
import com.example.loveapp.ui.screens.PhoneStatusScreen
import com.example.loveapp.ui.screens.ShopScreen
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
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToArt = { navController.navigate(Screen.ArtGallery.route) },
                onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                onNavigateToMemorial = { navController.navigate(Screen.MemorialDays.route) },
                onNavigateToSpark = { navController.navigate(Screen.SparkInfo.route) },
                onNavigateToTasks = { navController.navigate(Screen.TaskCenter.route) },
                onNavigateToSleep = { navController.navigate(Screen.SleepTracker.route) },
                onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                onNavigateToLoveTouch = { navController.navigate(Screen.LoveTouch.route) },
                onNavigateToMissYou = { navController.navigate(Screen.MissYou.route) },
                onNavigateToAppLock = { navController.navigate(Screen.AppLockSettings.route) },
                onNavigateToChatSettings = { navController.navigate(Screen.ChatSettings.route) },
                onNavigateToDailyQA = { navController.navigate(Screen.DailyQA.route) },
                onNavigateToIntimacy = { navController.navigate(Screen.Intimacy.route) },
                onNavigateToMoments = { navController.navigate(Screen.Moments.route) },
                onNavigateToPlaces = { navController.navigate(Screen.Places.route) },
                onNavigateToPet = { navController.navigate(Screen.Pet.route) },
                onNavigateToGames = { navController.navigate(Screen.Games.route) },
                onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                onNavigateToLetters = { navController.navigate(Screen.Letters.route) },
                onNavigateToBucketList = { navController.navigate(Screen.BucketList.route) },
                onNavigateToMoodInsights = { navController.navigate(Screen.MoodInsights.route) },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) },
                onNavigateToStory = { navController.navigate(Screen.Story.route) },
                onNavigateToLocationMap = { navController.navigate(Screen.LocationMap.route) },
                onNavigateToGeofences = { navController.navigate(Screen.Geofences.route) },
                onNavigateToPhoneStatus = { navController.navigate(Screen.PhoneStatus.route) },
                onNavigateToShop = { navController.navigate(Screen.Shop.route) }
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

        composable(Screen.ArtGallery.route) {
            ArtGalleryScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenCanvas = { canvas ->
                    navController.navigate(Screen.CanvasEditor.createRoute(canvas.id))
                }
            )
        }

        composable(
            route = Screen.CanvasEditor.route,
            arguments = listOf(navArgument("canvasId") { type = NavType.IntType })
        ) { backStackEntry ->
            val canvasId = backStackEntry.arguments?.getInt("canvasId") ?: -1
            CanvasEditorScreen(
                canvasId = canvasId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDrawing = { navController.navigate(Screen.Drawing.route) }
            )
        }

        composable(Screen.Drawing.route) {
            val chatEntry = remember { navController.getBackStackEntry(Screen.Chat.route) }
            val chatVm: com.example.loveapp.viewmodel.ChatViewModel =
                androidx.hilt.navigation.compose.hiltViewModel(chatEntry)
            DrawingScreen(
                onNavigateBack = { navController.popBackStack() },
                onSendDrawing = { bytes ->
                    chatVm.sendDrawingMessage(bytes)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MemorialDays.route) {
            MemorialDaysScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.SparkInfo.route) {
            SparkScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.TaskCenter.route) {
            TaskCenterScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.SleepTracker.route) {
            SleepTrackerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Gallery.route) {
            GalleryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.LoveTouch.route) {
            LoveTouchScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.MissYou.route) {
            MissYouScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.AppLockSettings.route) {
            AppLockScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.ChatSettings.route) {
            ChatSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.DailyQA.route) {
            DailyQAScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Intimacy.route) {
            IntimacyScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Moments.route) {
            MomentsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Places.route) {
            PlacesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Pet.route) {
            PetScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Games.route) {
            GamesHubScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Achievements.route) {
            AchievementsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Letters.route) {
            LettersScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.BucketList.route) {
            BucketListScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.MoodInsights.route) {
            MoodInsightsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Premium.route) {
            PremiumScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Story.route) {
            StoryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.LocationMap.route) {
            LocationMapScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Geofences.route) {
            GeofencesScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.PhoneStatus.route) {
            PhoneStatusScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Screen.Shop.route) {
            ShopScreen(onNavigateBack = { navController.popBackStack() })
        }
        }

        // ── Global preload overlay — shown briefly after login while Room is being populated ──
        val isPreloading by authViewModel.isPreloading.collectAsState()
        if (isPreloading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Загружаем данные...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}