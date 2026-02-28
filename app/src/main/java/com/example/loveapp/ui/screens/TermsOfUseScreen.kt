package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loveapp.ui.components.IOSTopAppBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            IOSTopAppBar(title = "Условия использования", onBackClick = onNavigateBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            item { TermsSection("Дата вступления в силу: 1 марта 2026 г.") }
            item { Spacer(Modifier.height(12.dp)) }

            item { TermsHeading("1. Принятие условий") }
            item { TermsBody("Используя приложение Love App («Приложение»), вы соглашаетесь с настоящими Условиями использования («Условия»). Если вы не согласны с какими-либо пунктами, пожалуйста, прекратите использование Приложения.") }

            item { TermsHeading("2. Описание сервиса") }
            item { TermsBody("Love App — это мобильное приложение для пар, которое позволяет:\n\n• Отслеживать настроение и делиться им с партнёром.\n• Вести личные списки желаний и заметок, видимые связанному партнёру.\n• Просматривать активность партнёра.\n• Вести менструальный календарь с возможностью показа партнёру.\n• Отмечать важные даты в личном календаре событий.\n• Получать push-уведомления об активности партнёра.") }

            item { TermsHeading("3. Регистрация и аккаунт") }
            item { TermsBody("Для использования Приложения необходима регистрация. Вы обязуетесь:\n\n• Предоставлять достоверные данные при регистрации.\n• Не передавать доступ к своему аккаунту третьим лицам.\n• Немедленно уведомить нас о несанкционированном использовании вашего аккаунта.\n\nМы оставляем за собой право заблокировать или удалить аккаунт в случае нарушения настоящих Условий.") }

            item { TermsHeading("4. Контент пользователя") }
            item { TermsBody("Вы несёте ответственность за весь контент, который создаёте в Приложении. Запрещается размещать контент:\n\n• Нарушающий законодательство Российской Федерации.\n• Оскорбляющий, угрожающий или дискриминирующий других лиц.\n• Содержащий вредоносное программное обеспечение.\n• Нарушающий авторские или иные права третьих лиц.") }

            item { TermsHeading("5. Конфиденциальность") }
            item { TermsBody("Ваши данные обрабатываются в соответствии с нашей Политикой конфиденциальности, которая является неотъемлемой частью настоящих Условий.") }

            item { TermsHeading("6. Ограничение ответственности") }
            item { TermsBody("Приложение предоставляется «как есть». Мы не несём ответственности за:\n\n• Перебои в работе Приложения по независящим от нас причинам.\n• Потерю данных вследствие технических сбоев.\n• Действия пользователей в отношении друг друга.\n• Косвенный или случайный ущерб, связанный с использованием Приложения.") }

            item { TermsHeading("7. Интеллектуальная собственность") }
            item { TermsBody("Все права на Приложение, его дизайн, логотип, название и программный код принадлежат разработчикам Love App. Запрещается копировать, модифицировать или распространять Приложение без письменного разрешения правообладателей.") }

            item { TermsHeading("8. Прекращение использования") }
            item { TermsBody("Вы можете прекратить использование Приложения в любой момент, обратившись к нам с запросом на удаление аккаунта. Мы также вправе ограничить доступ к Приложению при нарушении настоящих Условий.") }

            item { TermsHeading("9. Изменение условий") }
            item { TermsBody("Мы оставляем за собой право изменять настоящие Условия. Актуальная версия всегда доступна в настройках Приложения. Продолжение использования Приложения после внесения изменений означает ваше согласие с новой редакцией.") }

            item { TermsHeading("10. Разрешение споров") }
            item { TermsBody("В случае возникновения спора вы обязуетесь направить претензию на контактный email, указанный в разделе 11. Срок рассмотрения претензии — 30 календарных дней. Если спор не урегулирован в досудебном порядке, он передаётся на рассмотрение в суд по месту нахождения оператора в соответствии с законодательством РФ.") }

            item { TermsHeading("11. Применимое право") }
            item { TermsBody("Настоящие Условия регулируются законодательством Российской Федерации.") }

            item { TermsHeading("12. Контактная информация") }
            item { TermsBody("По вопросам, связанным с Условиями использования, обращайтесь:\nEmail: daniilkhabibulin1234@gmail.com") }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun TermsHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun TermsSection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

@Composable
private fun TermsBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        lineHeight = 22.sp
    )
}
