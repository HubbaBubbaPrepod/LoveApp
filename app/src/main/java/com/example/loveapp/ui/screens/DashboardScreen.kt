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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsScore
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
    onNavigateToSettings: () -> Unit
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
    val r = rememberResponsiveConfig()

    // Tiles are static — remember them so IOSTile can be skipped on recomposition
    val tiles = remember(
        onNavigateToNotes, onNavigateToWishes, onNavigateToMood,
        onNavigateToActivity, onNavigateToMenstrual, onNavigateToCalendars,
        onNavigateToRelationship, onNavigateToSettings
    ) {
        listOf(
            MenutileData(lNotes,      Icons.Default.Description,     iOSPrimaryPink,      onNavigateToNotes),
            MenutileData(lWishes,     Icons.Default.CardGiftcard,     iOSSecondaryPeach,   onNavigateToWishes),
            MenutileData(lMood,       Icons.Default.SentimentSatisfied, iOSActivityBlue,   onNavigateToMood),
            MenutileData(lActivities, Icons.Default.SportsScore,      iOSActivityOrange,   onNavigateToActivity),
            MenutileData(lCycle,      Icons.Default.AutoGraph,        iOSActivityPurple,   onNavigateToMenstrual),
            MenutileData(lCalendars,  Icons.Default.CalendarMonth,    iOSTertiaryCoral,    onNavigateToCalendars),
            MenutileData(lRelation,   Icons.Default.People,           iOSActivityGreen,    onNavigateToRelationship),
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
