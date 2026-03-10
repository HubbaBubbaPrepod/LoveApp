package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.*
import com.example.loveapp.viewmodel.ShopViewModel

private val Gold = Color(0xFFFFD700)
private val GoldDark = Color(0xFFDAA520)
private val Pink = Color(0xFFFF6B9D)
private val PinkLight = Color(0xFFFFF0F5)
private val Green = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit,
    viewModel: ShopViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsState()
    val shopItems by viewModel.shopItems.collectAsState()
    val dailyDeals by viewModel.dailyDeals.collectAsState()
    val missions by viewModel.missions.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val purchaseResult by viewModel.purchaseResult.collectAsState()
    val claimResult by viewModel.claimResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("💰 Обзор", "🛒 Магазин", "🎁 Скидки", "🎯 Задания", "📜 История")

    // Purchase success dialog
    purchaseResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPurchaseResult() },
            title = { Text("Покупка успешна! ✅") },
            text = {
                Column {
                    Text("${result.item?.icon ?: "⭐"} ${result.item?.name ?: ""}")
                    Spacer(Modifier.height(4.dp))
                    Text("Баланс: ${result.balance} монет", color = GoldDark)
                    result.effect?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Эффект: +${it.amount} ${it.type}", color = Green)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPurchaseResult() }) { Text("OK") }
            }
        )
    }

    // Mission claim dialog
    claimResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearClaimResult() },
            title = { Text("Награда получена! 🎉") },
            text = {
                Column {
                    Text("💰 +${result.coinsEarned} монет")
                    if (result.xpEarned > 0) Text("⭐ +${result.xpEarned} XP")
                    Spacer(Modifier.height(4.dp))
                    Text("Баланс: ${result.balance} монет", color = GoldDark)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearClaimResult() }) { Text("OK") }
            }
        )
    }

    // Error dialog
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Ошибка") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Магазин")
                        Spacer(Modifier.width(8.dp))
                        summary?.let {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Gold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "💰 ${it.coins}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = GoldDark,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = {
                        selectedTab = i
                        if (i == 4) viewModel.loadTransactions()
                    }) {
                        Text(title, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Pink)
                }
            } else {
                when (selectedTab) {
                    0 -> SummaryTab(summary, viewModel)
                    1 -> ShopItemsTab(shopItems, summary?.coins ?: 0, viewModel)
                    2 -> DailyDealsTab(dailyDeals, summary?.coins ?: 0, viewModel)
                    3 -> MissionsTab(missions, viewModel)
                    4 -> TransactionsTab(transactions)
                }
            }
        }
    }
}

@Composable
private fun SummaryTab(summary: EconomySummary?, viewModel: ShopViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Balance card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💰", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${summary?.coins ?: 0}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldDark
                    )
                    Text("золотых монет", color = Color.Gray)
                    if ((summary?.premiumMultiplier ?: 1f) > 1f) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Pink.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "⭐ Premium x${summary?.premiumMultiplier}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = Pink,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            // Today's stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = "📈",
                    label = "Сегодня заработано",
                    value = "+${summary?.todayEarned ?: 0}",
                    valueColor = Green
                )
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = "📉",
                    label = "Сегодня потрачено",
                    value = "-${summary?.todaySpent ?: 0}",
                    valueColor = Color.Red
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = "🏆",
                    label = "Всего заработано",
                    value = "${summary?.totalEarned ?: 0}",
                    valueColor = GoldDark
                )
                StatMiniCard(
                    modifier = Modifier.weight(1f),
                    icon = "🎯",
                    label = "Заданий выполнено",
                    value = "${summary?.missionsCompletedToday ?: 0}",
                    valueColor = Pink
                )
            }
        }

        item {
            // Quick actions
            Text("Способы заработка", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        val earnWays = listOf(
            "📅" to "Ежедневный вход — +5-35 монет",
            "🎰" to "Спин колеса — до 100 монет",
            "🎯" to "Задания — 10-50 монет каждое",
            "🗺️" to "Приключения — 5-60 монет",
            "⬆️" to "Повышение уровня — 50-2000 монет",
            "🥚" to "Дубликаты яиц — 20-500 монет"
        )
        items(earnWays) { (icon, text) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Text(text, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    modifier: Modifier = Modifier,
    icon: String,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, color = valueColor, fontSize = 18.sp)
            Text(label, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShopItemsTab(items: List<ShopItem>, coins: Int, viewModel: ShopViewModel) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = remember(items) { items.map { it.category }.distinct() }
    val filtered = remember(items, selectedCategory) {
        if (selectedCategory == null) items else items.filter { it.category == selectedCategory }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Category filter chips
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Все") }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(categoryLabel(cat)) }
                    )
                }
            }
        }

        items(filtered) { item ->
            ShopItemCard(item, coins, onBuy = { viewModel.buyItem(item.id) })
        }

        if (filtered.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Нет товаров", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun ShopItemCard(item: ShopItem, coins: Int, onBuy: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Купить ${item.name}?") },
            text = {
                Column {
                    Text("${item.icon} ${item.description}")
                    Spacer(Modifier.height(4.dp))
                    Text("Цена: ${item.priceCoins} 💰", fontWeight = FontWeight.Bold)
                    Text("У вас: $coins 💰", color = if (coins >= item.priceCoins) Green else Color.Red)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showConfirm = false; onBuy() },
                    enabled = coins >= item.priceCoins
                ) { Text("Купить") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Отмена") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(item.description, color = Color.Gray, fontSize = 12.sp, maxLines = 2)
                Text(
                    categoryLabel(item.category),
                    fontSize = 11.sp,
                    color = Pink
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { showConfirm = true },
                enabled = coins >= item.priceCoins,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (coins >= item.priceCoins) GoldDark else Color.Gray
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("${item.priceCoins} 💰", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DailyDealsTab(deals: List<DailyDeal>, coins: Int, viewModel: ShopViewModel) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (deals.isEmpty()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PinkLight)
                ) {
                    Column(
                        Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎁", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Нет активных предложений", fontWeight = FontWeight.Bold)
                        Text("Заходи позже!", color = Color.Gray)
                    }
                }
            }
        }

        items(deals) { deal ->
            DailyDealCard(deal, coins, onBuy = { viewModel.buyDeal(deal.id) })
        }
    }
}

@Composable
private fun DailyDealCard(deal: DailyDeal, coins: Int, onBuy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (deal.alreadyPurchased)
                Color.Gray.copy(alpha = 0.1f)
            else
                Gold.copy(alpha = 0.08f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(deal.icon, fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(deal.itemName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(deal.description, color = Color.Gray, fontSize = 13.sp)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Red.copy(alpha = 0.15f)
                ) {
                    Text(
                        "-${deal.discountPercent}%",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${deal.originalPrice} 💰",
                    textDecoration = TextDecoration.LineThrough,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${deal.dealPrice} 💰",
                    fontWeight = FontWeight.Bold,
                    color = GoldDark,
                    fontSize = 18.sp
                )
                Spacer(Modifier.weight(1f))
                if (deal.alreadyPurchased) {
                    Icon(Icons.Default.CheckCircle, "Куплено", tint = Green)
                    Spacer(Modifier.width(4.dp))
                    Text("Куплено", color = Green, fontWeight = FontWeight.Bold)
                } else {
                    Button(
                        onClick = onBuy,
                        enabled = coins >= deal.dealPrice,
                        colors = ButtonDefaults.buttonColors(containerColor = Pink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Купить")
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionsTab(missions: List<DailyMission>, viewModel: ShopViewModel) {
    val completedCount = missions.count { it.isCompleted }
    val totalCount = missions.size

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Pink.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TaskAlt, null, tint = Pink)
                        Spacer(Modifier.width(8.dp))
                        Text("Ежедневные задания", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Pink,
                        trackColor = Color.Gray.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("$completedCount / $totalCount выполнено", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }

        items(missions) { mission ->
            MissionCard(mission, onClaim = { viewModel.claimMission(mission.id) })
        }
    }
}

@Composable
private fun MissionCard(mission: DailyMission, onClaim: () -> Unit) {
    val progress = if (mission.targetCount > 0) mission.currentCount.toFloat() / mission.targetCount else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                mission.isClaimed -> Green.copy(alpha = 0.08f)
                mission.isCompleted -> Gold.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(mission.icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(mission.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(mission.description, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (mission.isCompleted) Green else Pink,
                    trackColor = Color.Gray.copy(alpha = 0.2f)
                )
                Text(
                    "${mission.currentCount}/${mission.targetCount}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💰 ${mission.rewardCoins}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldDark)
                if (mission.rewardXp > 0) {
                    Text("⭐ ${mission.rewardXp}", fontSize = 11.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))
                when {
                    mission.isClaimed -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Green.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "✅",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    mission.isCompleted -> {
                        Button(
                            onClick = onClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = Green),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Забрать", fontSize = 12.sp)
                        }
                    }
                    else -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Gray.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "⏳",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTab(transactionsResp: TransactionsResponse?) {
    val txList = transactionsResp?.transactions ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (txList.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Text("Нет транзакций", color = Color.Gray)
                    }
                }
            }
        }

        items(txList) { tx ->
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (tx.amount > 0) "📈" else "📉",
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            tx.description ?: txTypeLabel(tx.txType),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            tx.createdAt.take(16).replace("T", " "),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${if (tx.amount > 0) "+" else ""}${tx.amount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (tx.amount > 0) Green else Color.Red
                    )
                }
            }
        }
    }
}

private fun categoryLabel(cat: String): String = when (cat) {
    "boosts" -> "Бусты"
    "eggs" -> "Яйца"
    "premium" -> "Премиум"
    "spins" -> "Спины"
    "adventures" -> "Приключения"
    "special" -> "Особые"
    "themes" -> "Темы"
    "cosmetics" -> "Косметика"
    else -> cat.replaceFirstChar { it.uppercaseChar() }
}

private fun txTypeLabel(type: String): String = when (type) {
    "shop_purchase" -> "Покупка в магазине"
    "deal_purchase" -> "Выгодная покупка"
    "mission_reward" -> "Награда за задание"
    "checkin" -> "Ежедневный вход"
    "spin" -> "Лотерея"
    "adventure" -> "Приключение"
    "level_up" -> "Повышение уровня"
    "egg_duplicate" -> "Дубликат яйца"
    "furniture_purchase" -> "Покупка мебели"
    else -> type.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
}
