package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.data.api.models.SubscriptionPlanResponse
import com.example.loveapp.viewmodel.PremiumViewModel

private val GoldGradient = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8C00)))
private val GoldColor = Color(0xFFFFD700)
private val DarkGold = Color(0xFFFF8C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val plans by viewModel.plans.collectAsState()
    val status by viewModel.status.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val success by viewModel.successMessage.collectAsState()

    var selectedPlanId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = { Text("Couples Premium ✨") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E),
                    titleContentColor = GoldColor,
                    navigationIconContentColor = GoldColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1C1C1E))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Premium badge
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = GoldColor,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Couples Premium",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = GoldColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Откройте все возможности вашего приложения",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Current status
            if (status?.isPremium == true) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Премиум активен", fontWeight = FontWeight.Bold, color = Color(0xFF30D158))
                            Text("Тариф: ${status?.planTitle ?: ""}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            if (status?.expiresAt != null) {
                                Text("До: ${status?.expiresAt?.take(10) ?: ""}", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Features list
            Text(
                "Что входит в Premium",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            PremiumFeatureItem(Icons.Default.AutoGraph, "Продвинутая аналитика", "Детальные графики настроения и статистика")
            PremiumFeatureItem(Icons.Default.LocationOn, "История местоположений", "30 дней отслеживания с партнёром")
            PremiumFeatureItem(Icons.Default.Widgets, "Премиум виджеты", "Виджеты на рабочий стол с вашей статистикой")
            PremiumFeatureItem(Icons.Default.Pets, "Декор для питомца", "Уникальная мебель и украшения для комнаты")
            PremiumFeatureItem(Icons.Default.Star, "Без рекламы", "Полностью чистый интерфейс без баннеров")

            Spacer(Modifier.height(24.dp))

            // Plans
            if (plans.isNotEmpty()) {
                Text(
                    "Выберите тариф",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                plans.forEach { plan ->
                    val isSelected = selectedPlanId == plan.id
                    PlanCard(
                        plan = plan,
                        isSelected = isSelected,
                        isPremium = status?.isPremium == true,
                        onClick = { selectedPlanId = plan.id }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Subscribe button
            if (status?.isPremium != true && selectedPlanId != null) {
                Button(
                    onClick = {
                        val plan = plans.find { it.id == selectedPlanId }
                        if (plan?.googlePlayId != null) {
                            // In production: launch Google Play billing flow
                            // For now: simulate verification
                            viewModel.verifyPurchase("demo_token_${System.currentTimeMillis()}", plan.googlePlayId, null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGold),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Оформить подписку", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Cancel button for active subscribers
            if (status?.isPremium == true && status?.autoRenew == true) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.cancelSubscription() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    Text("Отменить автопродление", color = Color.White.copy(alpha = 0.6f))
                }
            }

            // Error / Success
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error ?: "", color = Color(0xFFFF375F), fontSize = 13.sp)
            }
            if (success != null) {
                Spacer(Modifier.height(12.dp))
                Text(success ?: "", color = Color(0xFF30D158), fontSize = 13.sp)
            }

            // Restore purchase
            Spacer(Modifier.height(24.dp))
            Text(
                "Уже покупали? Восстановить покупку",
                color = GoldColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PremiumFeatureItem(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GoldColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = GoldColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, color = Color.White, fontSize = 14.sp)
            Text(desc, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlanResponse,
    isSelected: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) GoldColor else Color.White.copy(alpha = 0.1f)
    val bgColor = if (isSelected) Color(0xFF3A3A3C) else Color(0xFF2C2C2E)

    Card(
        onClick = { if (!isPremium) onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(plan.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text(plan.description, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                val priceStr = formatPrice(plan.priceCents, plan.currency)
                Text(priceStr, fontWeight = FontWeight.Bold, color = GoldColor, fontSize = 18.sp)
                if (plan.planType == "yearly") {
                    Text("экономия 44%", color = Color(0xFF30D158), fontSize = 10.sp)
                }
            }
        }
    }
}

private fun formatPrice(cents: Int, currency: String): String {
    val amount = cents / 100
    return when (currency) {
        "RUB" -> "$amount ₽"
        "USD" -> "$$amount"
        else -> "$amount $currency"
    }
}
