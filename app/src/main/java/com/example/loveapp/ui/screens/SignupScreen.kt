package com.example.loveapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loveapp.R
import com.example.loveapp.ui.components.GoogleSignInButton
import com.example.loveapp.utils.rememberResponsiveConfig
import com.example.loveapp.viewmodel.AuthViewModel

@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToProfileSetup: () -> Unit = {},
    onNavigateToLogin: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToTermsOfUse: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    val authSuccess by viewModel.authSuccessEvent.collectAsState(initial = false)
    val needsProfileSetup by viewModel.needsProfileSetupEvent.collectAsState(initial = false)
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
    val r = rememberResponsiveConfig()

    LaunchedEffect(authSuccess) {
        if (authSuccess) {
            viewModel.clearAuthSuccessEvent()
            onSignupSuccess()
        }
    }

    LaunchedEffect(needsProfileSetup) {
        if (needsProfileSetup) {
            viewModel.clearNeedsProfileSetupEvent()
            onNavigateToProfileSetup()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = r.hPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = r.maxContentWidth).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.displaySmall,
            fontSize = r.titleFontSize,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = r.vSpacingLarge)
        )

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text(stringResource(R.string.display_name)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = r.vSpacingMedium)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = r.vSpacingMedium)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = r.vSpacingMedium),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = r.vSpacingMedium),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Text(stringResource(R.string.gender), style = MaterialTheme.typography.labelMedium, fontSize = r.captionFontSize, modifier = Modifier.align(Alignment.Start))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = r.vSpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = gender == "female",
                    onClick = { gender = "female" }
                )
                Text(stringResource(R.string.female), color = MaterialTheme.colorScheme.onSurface, fontSize = r.bodyFontSize)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = gender == "male",
                    onClick = { gender = "male" }
                )
                Text(stringResource(R.string.male), color = MaterialTheme.colorScheme.onSurface, fontSize = r.bodyFontSize)
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontSize = r.captionFontSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = r.vSpacingSmall)
            )
        }

        Button(
            onClick = { viewModel.signup(username, email, password, displayName, gender) },
            modifier = Modifier
                .fillMaxWidth()
                .height(r.buttonHeight),
            enabled = !isLoading && username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && displayName.isNotEmpty() && gender.isNotEmpty()
        ) {
            Text(
                if (isLoading) stringResource(R.string.loading) else stringResource(R.string.sign_up),
                fontSize = r.bodyFontSize
            )
        }

        Spacer(modifier = Modifier.height(r.vSpacingSmall))

        GoogleSignInButton(
            isLoading = isLoading,
            onIdToken = { idToken -> viewModel.loginWithGoogle(idToken) },
            onError   = { viewModel.setErrorMessage(it) }
        )

        Spacer(modifier = Modifier.height(r.vSpacingMedium))

        TextButton(onClick = onNavigateToLogin) {
            Text(stringResource(R.string.have_account), fontSize = r.bodyFontSize)
        }

        Spacer(modifier = Modifier.height(r.vSpacingSmall))

        Text(
            text = "Регистрируясь, вы соглашаетесь с",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = r.captionFontSize
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            TextButton(onClick = onNavigateToPrivacyPolicy, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(
                    text = "Политикой конфиденциальности",
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = r.captionFontSize
                )
            }
            TextButton(onClick = onNavigateToTermsOfUse, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text(
                    text = "Условиями использования",
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = r.captionFontSize
                )
            }
        }
        } // inner content Column
    } // outer scroll Column
}
