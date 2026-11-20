package com.example.sensus.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.BrandOrange
import com.example.sensus.theme.Ink
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusRedBg
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import com.example.sensus.theme.sensusTextButtonColors

private const val MIN_PHONE_DIGITS = 8
private const val MAX_PHONE_DIGITS = 15
private const val MIN_PASSWORD_LENGTH = 4

@ComposeUiModel
data class LoginCallbacks(
    val onLoginSuccess: () -> Unit = {},
    val onNavigateToRegister: () -> Unit = {},
    val onNavigateToOtp: () -> Unit = {},
    val onContinueAsGuest: () -> Unit = {},
    val onForgotPassword: () -> Unit = {}
)

@ComposeUiModel
private data class LoginFormProps(
    val emailOrPhone: String,
    val password: String,
    val isPasswordVisible: Boolean,
    val visibleError: String?,
    val isLoading: Boolean,
    val isGuestLoading: Boolean
)

@ComposeUiModel
private data class LoginFormHandlers(
    val callbacks: LoginCallbacks,
    val onEmailOrPhoneChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onTogglePasswordVisibility: () -> Unit,
    val onSubmitLogin: () -> Unit,
    val onContinueAsGuest: () -> Unit
)

@Composable
fun LoginScreen(modifier: Modifier = Modifier, callbacks: LoginCallbacks = LoginCallbacks(), viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var localError by remember { mutableStateOf<Int?>(null) }
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                is LoginViewEffect.NavigateToHome -> callbacks.onLoginSuccess()
                is LoginViewEffect.NavigateToHomeAsGuest -> callbacks.onContinueAsGuest()
                is LoginViewEffect.ShowToast -> Unit
            }
        }
    }

    val formProps = LoginFormProps(
        emailOrPhone = emailOrPhone,
        password = password,
        isPasswordVisible = isPasswordVisible,
        visibleError = localError?.let { context.getString(it, MIN_PASSWORD_LENGTH) } ?: state.errorMessage,
        isLoading = state.isLoading,
        isGuestLoading = state.isGuestLoading
    )
    val handlers = LoginFormHandlers(
        callbacks = callbacks,
        onEmailOrPhoneChange = {
            emailOrPhone = it
            localError = null
            viewModel.dispatchIntent(LoginViewIntent.DismissError)
        },
        onPasswordChange = {
            password = it
            localError = null
            viewModel.dispatchIntent(LoginViewIntent.DismissError)
        },
        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
        onSubmitLogin = {
            viewModel.dispatchIntent(LoginViewIntent.DismissError)
            localError = submitLoginIfValid(viewModel, emailOrPhone, password)
        },
        onContinueAsGuest = {
            localError = null
            viewModel.dispatchIntent(LoginViewIntent.StartGuestSession)
        }
    )
    LoginScreenScaffold(form = formProps, handlers = handlers, modifier = modifier)
}

@Composable
private fun LoginScreenScaffold(form: LoginFormProps, handlers: LoginFormHandlers, modifier: Modifier = Modifier) {
    AuthAccessScreenLayout(modifier, header = { AuthAccessBrand(showBackSpace = false) }) {
        AuthAccessSelector(isRegister = false, onSwitch = handlers.callbacks.onNavigateToRegister)
        AuthDataFrame { LoginForm(form, handlers) }
    }
}

@Composable
private fun LoginForm(form: LoginFormProps, handlers: LoginFormHandlers) {
    val busy = form.isLoading || form.isGuestLoading
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AuthFormTitle(stringResource(R.string.login_title))
        Text(
            stringResource(R.string.commerce_login_intro),
            color = SensusTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        LoginIdentifierField(form.emailOrPhone, handlers.onEmailOrPhoneChange)
        LoginPasswordField(form, handlers, busy)
        LoginErrorBanner(form.visibleError)
        LoginSecondaryActions(handlers.callbacks)
        Button(
            onClick = handlers.onSubmitLogin,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            contentPadding = AuthActionPadding
        ) {
            LoginActionContent(stringResource(R.string.login_submit), stringResource(R.string.login_submitting), form.isLoading)
        }
        AuthGuestButton(form.isGuestLoading, !busy, handlers.onContinueAsGuest)
    }
}

@Composable
private fun LoginSecondaryActions(callbacks: LoginCallbacks) {
    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(colors = sensusTextButtonColors(), onClick = callbacks.onForgotPassword, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.login_forgot_password), style = MaterialTheme.typography.labelMedium)
        }
        TextButton(colors = sensusTextButtonColors(), onClick = callbacks.onNavigateToOtp, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.login_sms), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun AuthGuestButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = Ink),
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        contentPadding = AuthActionPadding
    ) {
        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(20.dp))
        LoginActionContent(
            stringResource(R.string.login_guest),
            stringResource(R.string.login_guest_loading),
            isLoading,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun LoginIdentifierField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.login_identifier)) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(14.dp),
        colors = sensusOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoginPasswordField(form: LoginFormProps, handlers: LoginFormHandlers, busy: Boolean) {
    OutlinedTextField(
        value = form.password,
        onValueChange = handlers.onPasswordChange,
        label = { Text(stringResource(R.string.login_password)) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = handlers.onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (form.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = stringResource(
                        if (form.isPasswordVisible) R.string.login_hide_password else R.string.login_show_password
                    )
                )
            }
        },
        visualTransformation = if (form.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (!busy) handlers.onSubmitLogin() }),
        shape = RoundedCornerShape(14.dp),
        colors = sensusOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoginErrorBanner(message: String?) {
    AnimatedVisibility(visible = !message.isNullOrBlank()) {
        Surface(
            color = SensusRedBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = SensusRed, modifier = Modifier.size(20.dp))
                Text(message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = SensusTextPrimary)
            }
        }
    }
}

@Composable
private fun LoginActionContent(label: String, loadingLabel: String, isLoading: Boolean, modifier: Modifier = Modifier) {
    Crossfade(targetState = isLoading, modifier = modifier, label = "loginLoading") { loading ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
            }
            Text(if (loading) loadingLabel else label, textAlign = TextAlign.Center)
        }
    }
}

@Preview(name = "Compact login", widthDp = 320, heightDp = 560, fontScale = 1.3f)
@Composable
internal fun LoginCompactPreview() {
    SensusTheme {
        LoginScreenScaffold(
            form = LoginFormProps("", "", false, null, false, false),
            handlers = LoginFormHandlers(LoginCallbacks(), {}, {}, {}, {}, {})
        )
    }
}

private fun submitLoginIfValid(viewModel: LoginViewModel, emailOrPhone: String, password: String): Int? {
    val cleanEmail = emailOrPhone.trim()
    val cleanPass = password.trim()
    val validationError = validateLoginCredentials(cleanEmail, cleanPass)
    if (validationError == null) {
        viewModel.dispatchIntent(LoginViewIntent.EmailChanged(cleanEmail))
        viewModel.dispatchIntent(LoginViewIntent.PasswordChanged(cleanPass))
        viewModel.dispatchIntent(LoginViewIntent.SubmitLogin)
    }
    return validationError
}

private fun validateLoginCredentials(cleanEmail: String, cleanPass: String): Int? = when {
    cleanEmail.isBlank() -> R.string.login_error_identifier_required
    !isValidLoginIdentifier(cleanEmail) -> R.string.login_error_identifier_invalid
    cleanPass.isBlank() -> R.string.login_error_password_required
    cleanPass.length < MIN_PASSWORD_LENGTH -> R.string.login_error_password_length
    else -> null
}

private fun isValidLoginIdentifier(cleanEmail: String): Boolean {
    val isPhone = cleanEmail.length in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS && cleanEmail.all(Char::isDigit)
    val isEmail = cleanEmail.contains("@") && cleanEmail.contains(".") && !cleanEmail.contains(" ")
    return isPhone || isEmail
}
