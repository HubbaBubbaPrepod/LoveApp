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
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            IOSTopAppBar(title = "Политика конфиденциальности", onBackClick = onNavigateBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            item { PolicySection("Дата вступления в силу: 1 марта 2026 г.") }
            item { Spacer(Modifier.height(12.dp)) }

            item { PolicyHeading("1. Общие положения") }
            item { PolicyBody("Оператором персональных данных является физическое лицо Хабибулин Даниил Олегович (далее — «Оператор», «мы»).\nАдрес для официальных запросов: 460024, г. Оренбург, ул. Туркестанская, д. 12А, кв. 13.\nКонтактный email: daniilkhabibulin1234@gmail.com\n\nНастоящая Политика конфиденциальности информирует вас о том, какие персональные данные собирает, использует и защищает приложение Love App («Приложение») в отношении пользователей («Пользователь», «вы»). Регистрируясь в Приложении, вы выражаете согласие на обработку ваших персональных данных в соответствии с настоящей Политикой.") }

            item { PolicyHeading("2. Собираемые данные") }
            item { PolicyBody("Мы можем собирать следующие данные:\n\n• Регистрационные данные: имя пользователя, адрес электронной почты, отображаемое имя, пол (обязательное поле для корректной работы функций).\n• Данные об отношениях: дата рождения пользователя и партнёра (указывается добровольно в разделе «Отношения» для отображения дней рождения).\n• Контент пользователя: заметки, желания, записи о настроении, активностях, данные менструального цикла, события календаря — только те, которые вы создаёте самостоятельно.\n• Технические данные: FCM-токен для отправки push-уведомлений.\n• Данные аутентификации через Google: email и имя из вашего Google-аккаунта при входе через Google.\n\nОбратите внимание: данные менструального цикла относятся к особым категориям персональных данных. Мы обрабатываем их исключительно в целях функционирования Приложения и не передаём третьим лицам.") }

            item { PolicyHeading("3. Цели использования данных") }
            item { PolicyBody("Ваши данные используются исключительно для:\n\n• Предоставления функциональности Приложения (синхронизация между вами и вашим партнёром).\n• Отправки push-уведомлений об активности партнёра. Вы можете отключить уведомления в настройках Приложения или в системных настройках устройства.\n• Аутентификации и идентификации пользователя.\n• Обеспечения стабильной работы сервиса.") }

            item { PolicyHeading("4. Сторонние сервисы и передача данных") }
            item { PolicyBody("Мы не продаём и не передаём ваши персональные данные третьим лицам в коммерческих целях. Ваши данные видны только вашему связанному партнёру в рамках совместного использования Приложения. Посторонние лица доступа к вашим данным не имеют.\n\nПриложение использует следующие сторонние сервисы:\n\n• Firebase Cloud Messaging (Google LLC) — доставка push-уведомлений на ваше устройство. Для этого используется FCM-токен, привязанный к вашему устройству. Сервис работает в соответствии с политикой конфиденциальности Google.\n• Аутентификация Google (Google LLC) — при входе через Google-аккаунт. Политика: https://policies.google.com/privacy\n\nПри использовании Firebase Cloud Messaging технические данные (FCM-токен, идентификатор устройства) могут передаваться на серверы Google LLC, расположенные за пределами Российской Федерации, исключительно для доставки push-уведомлений. Такая передача осуществляется на основании вашего согласия и в соответствии с требованиями ст. 12 ФЗ-152.\n\nПриложение не использует файлы cookie, рекламные SDK и сервисы аналитики. Firebase SDK может технически обрабатывать идентификатор устройства исключительно для маршрутизации push-уведомлений.") }

            item { PolicyHeading("5. Правовая основа обработки") }
            item { PolicyBody("Обработка персональных данных осуществляется на основании согласия субъекта персональных данных в соответствии с Федеральным законом от 27.07.2006 № 152-ФЗ «О персональных данных».\n\nДанные менструального цикла относятся к особым категориям персональных данных (ст. 10 ФЗ-152). Их обработка возможна только при наличии вашего явного письменного согласия, которое вы даёте при регистрации в Приложении.") }

            item { PolicyHeading("6. Права пользователя") }
            item { PolicyBody("В соответствии с ФЗ-152 вы вправе:\n\n• Получить информацию о ваших персональных данных, хранящихся у Оператора.\n• Требовать уточнения, блокирования или уничтожения неполных, устаревших или незаконно полученных данных.\n• Отозвать согласие на обработку в любой момент, направив запрос на email: daniilkhabibulin1234@gmail.com. После отзыва обработка будет прекращена, а данные удалены в течение 30 дней. Обратите внимание: отзыв согласия означает невозможность дальнейшего использования Приложения.\n• Обжаловать действия Оператора в Роскомнадзор (сайт: rkn.gov.ru), если ваши права нарушены.") }

            item { PolicyHeading("7. Хранение данных и локализация") }
            item { PolicyBody("Данные хранятся на защищённых серверах. В соответствии с требованиями ст. 18 ФЗ-149 (в ред. ФЗ-242) базы данных российских пользователей локализованы на территории Российской Федерации. Мы принимаем технические и организационные меры для защиты ваших данных от несанкционированного доступа, изменения или уничтожения.\n\nСрок хранения данных: в течение всего времени действия аккаунта и 30 дней после его удаления.") }

            item { PolicyHeading("8. Удаление данных") }
            item { PolicyBody("Вы можете запросить удаление своих данных, обратившись к нам по контактному адресу, указанному ниже. После удаления аккаунта все связанные персональные данные будут удалены с наших серверов в течение 30 дней.") }

            item { PolicyHeading("9. Несовершеннолетние") }
            item { PolicyBody("Приложение не предназначено для лиц младше 16 лет. Мы не осуществляем сбор данных несовершеннолетних намеренно.") }

            item { PolicyHeading("10. Изменения политики") }
            item { PolicyBody("Мы оставляем за собой право изменять данную Политику. При существенных изменениях мы уведомим вас через Приложение. Продолжение использования Приложения после внесения изменений означает ваше согласие с новой редакцией.") }

            item { PolicyHeading("11. Контактная информация") }
            item { PolicyBody("По вопросам, связанным с конфиденциальностью ваших данных, обращайтесь:\nEmail: daniilkhabibulin1234@gmail.com") }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PolicyHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun PolicySection(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
}

@Composable
private fun PolicyBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        lineHeight = 22.sp
    )
}
