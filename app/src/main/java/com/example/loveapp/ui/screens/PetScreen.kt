package com.example.loveapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.*
import com.example.loveapp.ui.components.IOSTopAppBar
import com.example.loveapp.viewmodel.PetViewModel

private val Pink = Color(0xFFFF6B9D)
private val PinkLight = Color(0xFFFFF0F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetScreen(
    onNavigateBack: () -> Unit,
    viewModel: PetViewModel = hiltViewModel()
) {
    val pet by viewModel.pet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isActing by viewModel.isActing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val levelReward by viewModel.levelReward.collectAsState()
    val hatchResult by viewModel.hatchResult.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("🐾 Питомец", "🥚 Яйца", "🏠 Комната", "🗺️ Поход", "⭐ Желания", "🏆 Паспорт")

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(actionResult) {
        actionResult?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(
        topBar = { IOSTopAppBar(title = "Питомец", onBackClick = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Pink,
                edgePadding = 8.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp, maxLines = 1) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && pet == null) {
                    CircularProgressIndicator(color = Pink, modifier = Modifier.align(Alignment.Center))
                } else if (pet != null) {
                    when (selectedTab) {
                        0 -> PetMainTab(pet!!, isActing, viewModel, onRename = { showRenameDialog = true })
                        1 -> EggsTab(viewModel)
                        2 -> RoomTab(viewModel, pet!!)
                        3 -> AdventuresTab(viewModel, pet!!)
                        4 -> WishesTab(viewModel)
                        5 -> PassportTab(viewModel)
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🐾", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Привяжите партнёра\nчтобы получить питомца!", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    // Dialogs
    if (showRenameDialog) {
        RenameDialog(
            currentName = pet?.name ?: "",
            onDismiss = { showRenameDialog = false },
            onRename = { viewModel.renamePet(it); showRenameDialog = false }
        )
    }
    levelReward?.let { reward ->
        AlertDialog(
            onDismissRequest = { viewModel.clearLevelReward() },
            shape = RoundedCornerShape(20.dp),
            title = { Text("🎉 Новый уровень!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text(reward.description, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = { viewModel.clearLevelReward() }, colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(12.dp)) {
                    Text("Ура!")
                }
            }
        )
    }
    hatchResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearHatchResult() },
            shape = RoundedCornerShape(20.dp),
            title = { Text(if (result.isDuplicate) "Дубликат!" else "🥚 Вылупился!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Питомец: ${result.petTypeCode}", fontSize = 18.sp)
                    Text("Редкость: ${result.rarity}", color = rarityColor(result.rarity))
                    if (result.isDuplicate) Text("+${result.coinsGained} монет 🪙", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearHatchResult() }, colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(12.dp)) {
                    Text("Ок")
                }
            }
        )
    }
}

// ==================== TAB 0: PET MAIN ====================

@Composable
private fun PetMainTab(pet: PetResponse, isActing: Boolean, viewModel: PetViewModel, onRename: () -> Unit) {
    val checkinStatus by viewModel.checkinStatus.collectAsState()
    val spinResult by viewModel.spinResult.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadCheckinStatus() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PetCard(pet, onRename) }
        item { StatsCard(pet) }
        item { XpProgressCard(pet) }
        item { CoinEnergyRow(pet) }
        item { DailyActions(isActing, checkinStatus, viewModel) }
        item { CareButtons(isActing, onFeed = { viewModel.feed() }, onPlay = { viewModel.play() }, onClean = { viewModel.clean() }) }
    }
}

@Composable
private fun PetCard(pet: PetResponse, onRename: () -> Unit) {
    val petEmoji = petTypeEmoji(pet.petType)
    val moodEmoji = when (pet.mood) {
        "happy" -> "😊"
        "normal" -> "😐"
        "sad" -> "😢"
        "sick" -> "🤒"
        else -> "😐"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(PinkLight, Color.White))).padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(petEmoji, fontSize = 80.sp)
                Spacer(Modifier.height(4.dp))
                Text(moodEmoji, fontSize = 32.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pet.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, "Переименовать", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = Pink.copy(alpha = 0.15f)) {
                    Text(
                        "Уровень ${pet.level}/${pet.maxLevel}  ·  🪙 ${pet.coins}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Pink
                    )
                }
            }
        }
    }
}

@Composable
private fun XpProgressCard(pet: PetResponse) {
    val progress by animateFloatAsState(targetValue = pet.xpProgress, animationSpec = tween(800), label = "xp")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Опыт", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    if (pet.isMaxLevel) "МАКС!" else "${pet.xpInCurrentLevel}/${pet.xpNeeded} XP",
                    fontSize = 14.sp, color = Pink
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = Color(0xFFFFD700),
                trackColor = Color(0xFFFFD700).copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
private fun CoinEnergyRow(pet: PetResponse) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoChip(Modifier.weight(1f), "🪙 Монеты", "${pet.coins}")
        InfoChip(Modifier.weight(1f), "⚡ Энергия", "${pet.energy}/100")
        InfoChip(Modifier.weight(1f), "🔥 Серия", "${pet.checkinStreak}")
    }
}

@Composable
private fun InfoChip(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun DailyActions(isActing: Boolean, checkinStatus: CheckinStatus?, viewModel: PetViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ежедневные действия", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.doCheckin() },
                    enabled = !isActing && checkinStatus?.checkedInToday != true,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.4f))
                ) {
                    Text(if (checkinStatus?.checkedInToday == true) "✅ Отмечено" else "📅 Отметиться", fontSize = 14.sp, color = Color.White)
                }
                Button(
                    onClick = { viewModel.doSpin() },
                    enabled = !isActing,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), disabledContainerColor = Color(0xFFFF9800).copy(alpha = 0.4f))
                ) {
                    Text("🎰 Крутить", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatsCard(pet: PetResponse) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Характеристики", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            StatBar("🍖 Сытость", pet.hunger, Pink)
            StatBar("😊 Счастье", pet.happiness, Color(0xFFFF8E8E))
            StatBar("🛁 Чистота", pet.cleanliness, Color(0xFF64B5F6))
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, color: Color) {
    val animated by animateFloatAsState(targetValue = value / 100f, animationSpec = tween(800), label = "stat")
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp)
            Text("$value%", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animated }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color, trackColor = color.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun CareButtons(isActing: Boolean, onFeed: () -> Unit, onPlay: () -> Unit, onClean: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Уход", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CareButton(Modifier.weight(1f), "🍖", "Покормить", !isActing, onFeed)
                CareButton(Modifier.weight(1f), "🎾", "Играть", !isActing, onPlay)
                CareButton(Modifier.weight(1f), "🛁", "Помыть", !isActing, onClean)
            }
        }
    }
}

@Composable
private fun CareButton(modifier: Modifier, emoji: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Pink, disabledContainerColor = Pink.copy(alpha = 0.4f))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(label, fontSize = 11.sp, color = Color.White)
        }
    }
}

// ==================== TAB 1: EGGS ====================

@Composable
private fun EggsTab(viewModel: PetViewModel) {
    val eggs by viewModel.eggs.collectAsState()
    val isActing by viewModel.isActing.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadEggs() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Мои яйца", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        val unhatched = eggs.filter { !it.isHatched }
        val hatched = eggs.filter { it.isHatched }

        if (unhatched.isEmpty() && hatched.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🥚", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Пока нет яиц", fontSize = 16.sp, color = Color.Gray)
                    Text("Крутите рулетку, чтобы получить!", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }

        if (unhatched.isNotEmpty()) {
            item { Text("Готовы к вылуплению", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Pink) }
            items(unhatched) { egg ->
                EggCard(egg, canHatch = true, isActing = isActing) { viewModel.hatchEgg(egg.id) }
            }
        }
        if (hatched.isNotEmpty()) {
            item { Text("Вылупленные", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Gray) }
            items(hatched) { egg ->
                EggCard(egg, canHatch = false, isActing = false) {}
            }
        }
    }
}

@Composable
private fun EggCard(egg: PetEgg, canHatch: Boolean, isActing: Boolean, onHatch: () -> Unit) {
    val rarityEmoji = when (egg.rarity) {
        "common" -> "⚪"
        "uncommon" -> "🟢"
        "rare" -> "🔵"
        "epic" -> "🟣"
        "legendary" -> "🟡"
        else -> "⚪"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (egg.isHatched) (egg.emoji ?: "🐾") else "🥚", fontSize = 40.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(egg.petName ?: egg.petTypeCode, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("$rarityEmoji ${egg.rarity.replaceFirstChar { it.uppercase() }}", fontSize = 14.sp, color = rarityColor(egg.rarity))
                Text("Источник: ${egg.source}", fontSize = 12.sp, color = Color.Gray)
            }
            if (canHatch) {
                Button(
                    onClick = onHatch, enabled = !isActing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Открыть!")
                }
            }
        }
    }
}

// ==================== TAB 2: ROOM (Furniture) ====================

@Composable
private fun RoomTab(viewModel: PetViewModel, pet: PetResponse) {
    val furnitureShop by viewModel.furnitureShop.collectAsState()
    val ownedFurniture by viewModel.ownedFurniture.collectAsState()
    val isActing by viewModel.isActing.collectAsState()
    var showShop by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadFurnitureShop()
        viewModel.loadOwnedFurniture()
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Комната питомца", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Button(
                    onClick = { showShop = !showShop },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (showShop) Color.Gray else Pink)
                ) {
                    Icon(Icons.Default.ShoppingCart, "Магазин", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showShop) "Комната" else "Магазин")
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PinkLight)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("${pet.coins} монет", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        if (showShop) {
            item { Text("Магазин мебели", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Pink) }
            val ownedCodes = ownedFurniture.map { it.code }.toSet()
            items(furnitureShop) { item ->
                val alreadyOwned = item.code in ownedCodes
                FurnitureShopCard(item, alreadyOwned, isActing, pet.coins) { viewModel.buyFurniture(item.id) }
            }
        } else {
            item { Text("Моя мебель (${ownedFurniture.size})", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
            if (ownedFurniture.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏠", fontSize = 64.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Пока пусто", color = Color.Gray)
                        Text("Купите мебель в магазине!", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
            items(ownedFurniture) { item ->
                OwnedFurnitureCard(item) { viewModel.placeFurniture(item.id, !item.isPlaced) }
            }
        }
    }
}

@Composable
private fun FurnitureShopCard(item: FurnitureItem, owned: Boolean, isActing: Boolean, coins: Int, onBuy: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.emoji, fontSize = 36.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(item.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("😊 +${item.happinessBonus} счастье", fontSize = 12.sp, color = Color(0xFF4CAF50))
            }
            if (owned) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                    Text("✅", modifier = Modifier.padding(8.dp), fontSize = 18.sp)
                }
            } else {
                Button(
                    onClick = onBuy, enabled = !isActing && coins >= item.priceCoins,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("🪙 ${item.priceCoins}", fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OwnedFurnitureCard(item: OwnedFurniture, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (item.isPlaced) PinkLight else Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item.emoji, fontSize = 36.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(if (item.isPlaced) "Размещено ✅" else "Нажмите чтобы разместить", fontSize = 12.sp, color = if (item.isPlaced) Color(0xFF4CAF50) else Color.Gray)
            }
        }
    }
}

// ==================== TAB 3: ADVENTURES ====================

@Composable
private fun AdventuresTab(viewModel: PetViewModel, pet: PetResponse) {
    val adventuresData by viewModel.adventuresData.collectAsState()
    val isActing by viewModel.isActing.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadAdventures() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Приключения", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoChip(Modifier.weight(1f), "⚡ Энергия", "${adventuresData?.energy ?: pet.energy}/100")
                InfoChip(Modifier.weight(1f), "📊 Уровень", "${pet.level}")
            }
        }

        // Active adventure
        val active = adventuresData?.active ?: emptyList()
        if (active.isNotEmpty()) {
            item { Text("В процессе", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color(0xFFFF9800)) }
            items(active) { adv ->
                ActiveAdventureCard(adv, isActing) { viewModel.claimAdventure(adv.id) }
            }
        }

        // Available adventures
        val available = adventuresData?.available ?: emptyList()
        item { Text("Доступные приключения", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) }
        items(available) { adv ->
            AdventureCard(adv, isActing, pet.energy, active.isNotEmpty()) { viewModel.startAdventure(adv.id) }
        }
    }
}

@Composable
private fun ActiveAdventureCard(adv: ActiveAdventure, isActing: Boolean, onClaim: () -> Unit) {
    val endsAtMs = try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(adv.endsAt.take(19))?.time ?: 0L } catch (_: Exception) { 0L }
    val isFinished = System.currentTimeMillis() >= endsAtMs

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isFinished) Color(0xFFFFF3E0) else Color(0xFFF3E5F5)), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(adv.emoji, fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(adv.name, fontWeight = FontWeight.Bold)
                Text(if (isFinished) "Завершено! Заберите награду" else "В процессе...", fontSize = 13.sp, color = if (isFinished) Color(0xFFFF9800) else Color.Gray)
            }
            if (isFinished) {
                Button(onClick = onClaim, enabled = !isActing, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                    Text("Забрать", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AdventureCard(adv: AdventureInfo, isActing: Boolean, energy: Int, hasActive: Boolean, onStart: () -> Unit) {
    val canStart = adv.unlocked && energy >= adv.energyCost && !hasActive
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (adv.unlocked) Color.White else Color(0xFFF5F5F5)), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(adv.emoji, fontSize = 36.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(adv.name, fontWeight = FontWeight.SemiBold)
                    if (!adv.unlocked) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Lock, "Locked", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
                Text(adv.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚡${adv.energyCost}", fontSize = 12.sp, color = Color(0xFFFF9800))
                    Text("⏱${adv.durationMinutes}м", fontSize = 12.sp, color = Color.Gray)
                    Text("+${adv.xpReward}XP", fontSize = 12.sp, color = Color(0xFF4CAF50))
                    Text("+${adv.coinReward}🪙", fontSize = 12.sp, color = Color(0xFFFF9800))
                }
                if (!adv.unlocked) Text("Нужен ур. ${adv.minLevel}", fontSize = 11.sp, color = Color.Red)
            }
            Button(
                onClick = onStart, enabled = !isActing && canStart,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink, disabledContainerColor = Pink.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Go", modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ==================== TAB 4: WISHES ====================

@Composable
private fun WishesTab(viewModel: PetViewModel) {
    val wishes by viewModel.wishes.collectAsState()
    val isActing by viewModel.isActing.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadWishes() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Желания питомца", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Pink, contentColor = Color.White, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Add, "Добавить")
                }
            }
        }

        val active = wishes.filter { !it.isFulfilled }
        val fulfilled = wishes.filter { it.isFulfilled }

        if (active.isEmpty() && fulfilled.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⭐", fontSize = 64.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Нет желаний", color = Color.Gray)
                    Text("Добавьте желание для питомца!", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }

        items(active) { wish ->
            WishCard(wish, isActing, onFulfill = { viewModel.fulfillWish(wish.id) }, onDelete = { viewModel.deleteWish(wish.id) })
        }
        if (fulfilled.isNotEmpty()) {
            item { Text("Исполненные ✨", fontWeight = FontWeight.SemiBold, color = Color(0xFF4CAF50)) }
            items(fulfilled) { wish ->
                WishCard(wish, isActing = false, onFulfill = {}, onDelete = {})
            }
        }
    }

    if (showAddDialog) {
        AddWishDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { text, emoji -> viewModel.createWish(text, emoji); showAddDialog = false }
        )
    }
}

@Composable
private fun WishCard(wish: PetWish, isActing: Boolean, onFulfill: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (wish.isFulfilled) Color(0xFFF1F8E9) else Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(wish.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(wish.wishText, fontSize = 15.sp)
                if (wish.isFulfilled) {
                    Text("Исполнено${wish.fulfilledByName?.let { " — $it" } ?: ""}", fontSize = 12.sp, color = Color(0xFF4CAF50))
                } else {
                    Text("+${wish.xpReward} XP за исполнение", fontSize = 12.sp, color = Pink)
                }
            }
            if (!wish.isFulfilled) {
                IconButton(onClick = onFulfill, enabled = !isActing) {
                    Icon(Icons.Default.CheckCircle, "Исполнить", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Удалить", tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AddWishDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val emojis = listOf("⭐", "💖", "🎁", "🌟", "🎯", "🌈", "🎀", "🧸")
    var selectedEmoji by remember { mutableStateOf("⭐") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Новое желание", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { if (it.length <= 300) text = it },
                    label = { Text("Что хочет питомец?") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pink, cursorColor = Pink)
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { emoji ->
                        Surface(
                            modifier = Modifier.size(40.dp).clickable { selectedEmoji = emoji },
                            shape = CircleShape,
                            color = if (emoji == selectedEmoji) Pink.copy(alpha = 0.2f) else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 20.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(text, selectedEmoji) }, enabled = text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(12.dp)) {
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Color.Gray) } }
    )
}

// ==================== TAB 5: PASSPORT ====================

@Composable
private fun PassportTab(viewModel: PetViewModel) {
    val passport by viewModel.passport.collectAsState()
    val collections by viewModel.collections.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPassport()
        viewModel.loadCollections()
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Паспорт питомца", fontWeight = FontWeight.Bold, fontSize = 20.sp) }

        passport?.let { p ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PinkLight)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(petTypeEmoji(p.pet?.petType ?: "cat"), fontSize = 60.sp)
                        p.pet?.let {
                            Text(it.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text("Уровень ${it.level}/${it.maxLevel}", color = Pink)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PassportStat(Modifier.weight(1f), "📅", "Отметки", "${p.stats.totalCheckins}")
                    PassportStat(Modifier.weight(1f), "🔥", "Серия", "${p.stats.checkinStreak}")
                    PassportStat(Modifier.weight(1f), "🗺️", "Походы", "${p.stats.adventureCount}")
                    PassportStat(Modifier.weight(1f), "🪙", "Монеты", "${p.stats.coins}")
                }
            }

            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Прогресс", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        CollectionProgress("🐾 Питомцы", p.collections.pets.unlocked, p.collections.pets.total)
                        CollectionProgress("🏠 Мебель", p.collections.furniture.owned, p.collections.furniture.total)
                        CollectionProgress("🗺️ Приключения", p.collections.adventures.completed, p.collections.adventures.total)
                        CollectionProgress("🥚 Яйца (открыто)", p.eggs.hatched, p.eggs.total.coerceAtLeast(1))
                        CollectionProgress("⭐ Желания", p.wishes.fulfilled, p.wishes.total.coerceAtLeast(1))
                    }
                }
            }
        }

        // Collections items
        collections?.collections?.forEach { (type, items) ->
            item { Text(collectionTypeLabel(type), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp)) }
            items(items) { item ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.itemEmoji, fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(item.itemName, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PassportStat(modifier: Modifier, emoji: String, label: String, value: String) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun CollectionProgress(label: String, current: Int, total: Int) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp)
            Text("$current/$total", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Pink)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Pink,
            trackColor = Pink.copy(alpha = 0.1f)
        )
    }
}

// ==================== DIALOGS & HELPERS ====================

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Переименовать питомца", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Имя") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pink, cursorColor = Pink)
            )
        },
        confirmButton = {
            Button(onClick = { onRename(name) }, enabled = name.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Pink), shape = RoundedCornerShape(12.dp)) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Color.Gray) } }
    )
}

private fun petTypeEmoji(type: String): String = when (type) {
    "cat" -> "🐱"; "dog" -> "🐶"; "bunny" -> "🐰"; "bear" -> "🧸"
    "hamster" -> "🐹"; "fox" -> "🦊"; "panda" -> "🐼"; "penguin" -> "🐧"
    "owl" -> "🦉"; "unicorn" -> "🦄"; "dragon" -> "🐉"; "phoenix" -> "🔥"
    else -> "🐾"
}

private fun rarityColor(rarity: String): Color = when (rarity) {
    "common" -> Color.Gray
    "uncommon" -> Color(0xFF4CAF50)
    "rare" -> Color(0xFF2196F3)
    "epic" -> Color(0xFF9C27B0)
    "legendary" -> Color(0xFFFF9800)
    else -> Color.Gray
}

private fun collectionTypeLabel(type: String): String = when (type) {
    "pet" -> "🐾 Коллекция питомцев"
    "furniture" -> "🏠 Коллекция мебели"
    "adventure" -> "🗺️ Коллекция приключений"
    else -> type
}
