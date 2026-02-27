package com.example.loveapp.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.loveapp.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

private const val WEB_CLIENT_ID =
    "833288193423-pdau5bt5ffa4tjvioss2tut96s8frkd1.apps.googleusercontent.com"

/**
 * iOS-style outlined Google Sign-In button using Android Credential Manager.
 * Calls [onIdToken] with the Firebase/Google ID token on success,
 * or [onError] with a user-facing message on failure.
 */
@Composable
fun GoogleSignInButton(
    isLoading: Boolean,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            scope.launch { launchGoogleSignIn(context, onIdToken, onError) }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text("Войти через Google", fontWeight = FontWeight.Medium)
            }
        }
    }
}

private suspend fun launchGoogleSignIn(
    context: Context,
    onIdToken: (String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val activity = context as? Activity
            ?: return onError("Не удалось запустить Google Sign-In")

        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)   // показывать все аккаунты
            .setServerClientId(WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val tokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            onIdToken(tokenCredential.idToken)
        } else {
            onError("Неподдерживаемый тип аутентификации")
        }
    } catch (e: GetCredentialCancellationException) {
        // Пользователь отменил — не показываем ошибку
    } catch (e: GetCredentialException) {
        onError(e.errorMessage?.toString() ?: "Ошибка Google Sign-In")
    } catch (e: Exception) {
        onError(e.message ?: "Ошибка Google Sign-In")
    }
}
