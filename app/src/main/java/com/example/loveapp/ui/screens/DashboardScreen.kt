package com.example.loveapp.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.loveapp.ui.theme.PastelBackground
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.ui.components.IOSTile
import com.example.loveapp.ui.theme.AccentBlue
import com.example.loveapp.ui.theme.AccentGreen
import com.example.loveapp.ui.theme.AccentOrange
import com.example.loveapp.ui.theme.AccentPurple
import com.example.loveapp.ui.theme.PrimaryPink
import com.example.loveapp.ui.theme.SecondaryPeach
import com.example.loveapp.R
import com.example.loveapp.ui.theme.TertiaryRose
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
        MenutileData(stringResource(R.string.notes), Icons.Default.List, PrimaryPink, onNavigateToNotes),
        MenutileData(stringResource(R.string.wishes), Icons.Default.Favorite, SecondaryPeach, onNavigateToWishes),
        MenutileData(stringResource(R.string.mood), Icons.Default.Favorite, AccentBlue, onNavigateToMood),
        MenutileData(stringResource(R.string.activities), Icons.Default.Info, AccentOrange, onNavigateToActivity),
        MenutileData(stringResource(R.string.cycle), Icons.Default.Favorite, AccentPurple, onNavigateToMenstrual),
        MenutileData(stringResource(R.string.calendars), Icons.Default.List, TertiaryRose, onNavigateToCalendars),
        MenutileData(stringResource(R.string.relationship), Icons.Default.Favorite, AccentGreen, onNavigateToRelationship),
        MenutileData(stringResource(R.string.settings), Icons.Default.Settings, MaterialTheme.colorScheme.outline, onNavigateToSettings),
    )

    Scaffold(
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tiles.size) { index ->
                    val tile = tiles[index]
                    IOSTile(
                        title = tile.title,
                        icon = tile.icon,
                        backgroundColor = tile.color,
                        onClick = tile.onNavigate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun Modifier.aspectRatio(ratio: Float): Modifier {
    return this.then(
        Modifier.padding(0.dp)
    )
}
