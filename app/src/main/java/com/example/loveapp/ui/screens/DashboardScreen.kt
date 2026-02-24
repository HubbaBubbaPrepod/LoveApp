package com.example.loveapp.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTile
import com.example.loveapp.ui.theme.iOSActivityBlue
import com.example.loveapp.ui.theme.iOSActivityGreen
import com.example.loveapp.ui.theme.iOSActivityOrange
import com.example.loveapp.ui.theme.iOSActivityPurple
import com.example.loveapp.ui.theme.iOSPrimaryPink
import com.example.loveapp.ui.theme.iOSSecondaryPeach
import com.example.loveapp.ui.theme.iOSTertiaryCoral
import com.example.loveapp.R
import com.example.loveapp.viewmodel.AuthViewModel

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
    viewModel: AuthViewModel = hiltViewModel()
) {
    val tiles = listOf(
        MenutileData(stringResource(R.string.notes), Icons.Default.List, iOSPrimaryPink, onNavigateToNotes),
        MenutileData(stringResource(R.string.wishes), Icons.Default.Favorite, iOSSecondaryPeach, onNavigateToWishes),
        MenutileData(stringResource(R.string.mood), Icons.Default.Favorite, iOSActivityBlue, onNavigateToMood),
        MenutileData(stringResource(R.string.activities), Icons.Default.Info, iOSActivityOrange, onNavigateToActivity),
        MenutileData(stringResource(R.string.cycle), Icons.Default.Favorite, iOSActivityPurple, onNavigateToMenstrual),
        MenutileData(stringResource(R.string.calendars), Icons.Default.List, iOSTertiaryCoral, onNavigateToCalendars),
        MenutileData(stringResource(R.string.relationship), Icons.Default.Favorite, iOSActivityGreen, onNavigateToRelationship),
        MenutileData(stringResource(R.string.settings), Icons.Default.Settings, MaterialTheme.colorScheme.outline, onNavigateToSettings),
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Love App",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your personal companion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = 14.sp,
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
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(tiles.size) { index ->
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
