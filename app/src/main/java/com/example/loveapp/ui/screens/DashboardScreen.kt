package com.example.loveapp.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveapp.ui.components.IOSTile
import com.example.loveapp.ui.theme.iOSActivityBlue
import com.example.loveapp.ui.theme.iOSActivityGreen
import com.example.loveapp.ui.theme.iOSActivityOrange
import com.example.loveapp.ui.theme.iOSActivityPurple
import com.example.loveapp.ui.theme.iOSPrimaryPink
import com.example.loveapp.ui.theme.iOSSecondaryPeach
import com.example.loveapp.ui.theme.iOSTertiaryCoral
import com.example.loveapp.utils.rememberResponsiveConfig
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.MonetizationOn
import com.example.loveapp.R

data class MenutileData(
    val title: String,
    val icon: ImageVector,
    val color: androidx.compose.ui.graphics.Color,
    val onNavigate: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToWishes: () -> Unit,
    onNavigateToMood: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToMenstrual: () -> Unit,
    onNavigateToCalendars: () -> Unit,
    onNavigateToRelationship: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToArt: () -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToMemorial: () -> Unit = {},
    onNavigateToSpark: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToSleep: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToLoveTouch: () -> Unit = {},
    onNavigateToMissYou: () -> Unit = {},
    onNavigateToAppLock: () -> Unit = {},
    onNavigateToChatSettings: () -> Unit = {},
    onNavigateToDailyQA: () -> Unit = {},
    onNavigateToIntimacy: () -> Unit = {},
    onNavigateToMoments: () -> Unit = {},
    onNavigateToPlaces: () -> Unit = {},
    onNavigateToPet: () -> Unit = {},
    onNavigateToGames: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToLetters: () -> Unit = {},
    onNavigateToBucketList: () -> Unit = {},
    onNavigateToMoodInsights: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToStory: () -> Unit = {},
    onNavigateToLocationMap: () -> Unit = {},
    onNavigateToGeofences: () -> Unit = {},
    onNavigateToPhoneStatus: () -> Unit = {},
    onNavigateToShop: () -> Unit = {}
) {
    // Pre-compute string resources (stable across recompositions)
    val lNotes     = stringResource(R.string.notes)
    val lWishes    = stringResource(R.string.wishes)
    val lMood      = stringResource(R.string.mood)
    val lActivities = stringResource(R.string.activities)
    val lCycle     = stringResource(R.string.cycle)
    val lCalendars = stringResource(R.string.calendars)
    val lRelation  = stringResource(R.string.relationship)
    val lSettings  = stringResource(R.string.settings)
    val lArt       = stringResource(R.string.art)
    val lChat      = stringResource(R.string.chat)
    val lMemorial  = stringResource(R.string.memorial_days)
    val lSpark     = stringResource(R.string.spark)
    val lTasks     = stringResource(R.string.tasks)
    val lSleep     = stringResource(R.string.sleep)
    val lGallery   = stringResource(R.string.gallery)
    val lLoveTouch = stringResource(R.string.love_touch)
    val lMissYou   = stringResource(R.string.miss_you)
    val lAppLock   = stringResource(R.string.app_lock)
    val lChatSettings = stringResource(R.string.chat_settings)
    val lDailyQA   = stringResource(R.string.daily_qa)
    val lIntimacy  = stringResource(R.string.intimacy)
    val lMoments   = stringResource(R.string.moments)
    val lPlaces    = stringResource(R.string.places)
    val lPet       = stringResource(R.string.pet)
    val lGames     = stringResource(R.string.games)
    val lAchievements = "Достижения"
    val lLetters   = "Письма"
    val lBucketList = "Мечты"
    val lMoodInsights = "Аналитика"
    val lPremium = "Премиум"
    val lStory = "Наша история"
    val lLocationMap = "Геолокация"
    val lGeofences = "Геозоны"
    val lPhoneStatus = "Статус телефона"
    val lShop = "Магазин"
    val r = rememberResponsiveConfig()

    // Tiles are static — remember them so IOSTile can be skipped on recomposition
    val tiles = remember(
        onNavigateToNotes, onNavigateToWishes, onNavigateToMood,
        onNavigateToActivity, onNavigateToMenstrual, onNavigateToCalendars,
        onNavigateToRelationship, onNavigateToSettings, onNavigateToArt,
        onNavigateToChat, onNavigateToMemorial, onNavigateToSpark,
        onNavigateToTasks, onNavigateToSleep,
        onNavigateToGallery, onNavigateToLoveTouch, onNavigateToMissYou, onNavigateToAppLock,
        onNavigateToChatSettings, onNavigateToDailyQA, onNavigateToIntimacy,
        onNavigateToMoments, onNavigateToPlaces,
        onNavigateToPet, onNavigateToGames,
        onNavigateToAchievements, onNavigateToLetters, onNavigateToBucketList,
        onNavigateToMoodInsights,
        onNavigateToPremium, onNavigateToStory,
        onNavigateToLocationMap,
        onNavigateToGeofences,
        onNavigateToPhoneStatus,
        onNavigateToShop
    ) {
        listOf(
            MenutileData(lChat,       Icons.Default.Chat,              Color(0xFF5E5CE6),   onNavigateToChat),
            MenutileData(lSpark,      Icons.Default.LocalFireDepartment, Color(0xFFFF6D00),  onNavigateToSpark),
            MenutileData(lNotes,      Icons.Default.Description,     iOSPrimaryPink,      onNavigateToNotes),
            MenutileData(lWishes,     Icons.Default.CardGiftcard,     iOSSecondaryPeach,   onNavigateToWishes),
            MenutileData(lMood,       Icons.Default.SentimentSatisfied, iOSActivityBlue,   onNavigateToMood),
            MenutileData(lMoodInsights, Icons.Default.AutoGraph,       Color(0xFF00BCD4),  onNavigateToMoodInsights),
            MenutileData(lActivities, Icons.Default.SportsScore,      iOSActivityOrange,   onNavigateToActivity),
            MenutileData(lTasks,      Icons.Default.Checklist,        Color(0xFF4CAF50),   onNavigateToTasks),
            MenutileData(lMemorial,   Icons.Default.Star,             Color(0xFFFF6B9D),   onNavigateToMemorial),
            MenutileData(lSleep,      Icons.Default.Bedtime,          Color(0xFF9C5CE6),   onNavigateToSleep),
            MenutileData(lCycle,      Icons.Default.AutoGraph,        iOSActivityPurple,   onNavigateToMenstrual),
            MenutileData(lCalendars,  Icons.Default.CalendarMonth,    iOSTertiaryCoral,    onNavigateToCalendars),
            MenutileData(lRelation,   Icons.Default.People,           iOSActivityGreen,    onNavigateToRelationship),
            MenutileData(lGallery,    Icons.Default.PhotoLibrary,     Color(0xFF26A69A),   onNavigateToGallery),
            MenutileData(lLoveTouch,  Icons.Default.TouchApp,         Color(0xFFE91E63),   onNavigateToLoveTouch),
            MenutileData(lMissYou,    Icons.Default.FavoriteBorder,   Color(0xFFFF6B9D),   onNavigateToMissYou),
            MenutileData(lChatSettings, Icons.Default.Palette,        Color(0xFF7C4DFF),   onNavigateToChatSettings),
            MenutileData(lDailyQA,   Icons.Default.QuestionMark,     Color(0xFFFF6D00),   onNavigateToDailyQA),
            MenutileData(lIntimacy,  Icons.Default.Favorite,         Color(0xFFE91E63),   onNavigateToIntimacy),
            MenutileData(lMoments,   Icons.Default.PhotoLibrary,     Color(0xFFFF7043),   onNavigateToMoments),
            MenutileData(lPlaces,    Icons.Default.LocationOn,       Color(0xFF26A69A),   onNavigateToPlaces),
            MenutileData(lLocationMap, Icons.Default.Explore,        Color(0xFF0288D1),   onNavigateToLocationMap),
            MenutileData(lGeofences,   Icons.Default.ShareLocation,  Color(0xFF00897B),   onNavigateToGeofences),
            MenutileData(lPhoneStatus, Icons.Default.PhoneAndroid,   Color(0xFF5C6BC0),   onNavigateToPhoneStatus),
            MenutileData(lShop,        Icons.Default.MonetizationOn,  Color(0xFFFFD700),   onNavigateToShop),
            MenutileData(lPet,       Icons.Default.Pets,             Color(0xFFFF6B9D),   onNavigateToPet),
            MenutileData(lGames,     Icons.Default.SportsEsports,   Color(0xFF7C4DFF),   onNavigateToGames),
            MenutileData(lAchievements, Icons.Default.EmojiEvents,   Color(0xFFFFB300),   onNavigateToAchievements),
            MenutileData(lLetters,    Icons.Default.Email,           Color(0xFFE91E63),   onNavigateToLetters),
            MenutileData(lBucketList, Icons.Default.PlaylistAddCheck, Color(0xFF7C4DFF),  onNavigateToBucketList),
            MenutileData(lStory,      Icons.Default.Bookmark,        Color(0xFFFF6B9D),   onNavigateToStory),
            MenutileData(lArt,        Icons.Default.Brush,            Color(0xFF9C27B0),   onNavigateToArt),
            MenutileData(lPremium,    Icons.Default.Star,            Color(0xFFFFD700),   onNavigateToPremium),
            MenutileData(lAppLock,    Icons.Default.Lock,             Color(0xFF455A64),   onNavigateToAppLock),
            MenutileData(lSettings,   Icons.Default.Settings,         Color(0xFF607D8B),   onNavigateToSettings),
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(horizontal = r.hPadding, vertical = r.vSpacingMedium)
            ) {
                Text(
                    text = "Love App",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = r.titleFontSize,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your personal companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = r.bodyFontSize,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(r.gridColumns),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(r.vSpacingMedium),
                horizontalArrangement = Arrangement.spacedBy(r.vSpacingMedium),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(r.hPadding)
            ) {
                items(tiles.size, key = { it }) { index ->
                    val tile = tiles[index]
                    IOSTile(
                        title = tile.title,
                        icon = tile.icon,
                        backgroundColor = tile.color,
                        onClick = tile.onNavigate
                    )
                }
            }
        }
    }
}
