package com.example.sensus.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.R
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg

private const val SUBTITLE_ALPHA = 0.85f
private const val ERROR_BG_ALPHA = 0.08f
private const val ERROR_BORDER_ALPHA = 0.4f
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
    val state by viewModel.viewState.collectAsState()
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
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
        visibleError = state.errorMessage ?: localErrorMessage,
        isLoading = state.isLoading,
        isGuestLoading = state.isGuestLoading
    )
    val handlers = LoginFormHandlers(
        callbacks = callbacks,
        onEmailOrPhoneChange = {
            emailOrPhone = it
            localErrorMessage = null
        },
        onPasswordChange = {
            password = it
            localErrorMessage = null
        },
        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
        onSubmitLogin = { localErrorMessage = submitLoginIfValid(viewModel, emailOrPhone, password) },
        onContinueAsGuest = { viewModel.dispatchIntent(LoginViewIntent.StartGuestSession) }
    )
    LoginScreenScaffold(uiState = formProps, handlers = handlers, modifier = modifier)
}

@Composable
private fun LoginScreenScaffold(uiState: LoginFormProps, handlers: LoginFormHandlers, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            LoginHeader()
            LoginBody(uiState = uiState, handlers = handlers)
        }
    }
}

@Composable
private fun LoginHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = SensusPurple,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(horizontal = 24.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Surface(shape = CircleShape, color = SensusVioletBg, shadowElevation = 6.dp, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.sensus_logo),
                        contentDescription = "Logo Sensus",
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "Sensus", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Descuentos locales en tu zona",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LoginBody(uiState: LoginFormProps, handlers: LoginFormHandlers) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        LoginFormCard(uiState = uiState, handlers = handlers)
        Spacer(modifier = Modifier.height(20.dp))
        LoginRegisterFooter(onNavigateToRegister = handlers.callbacks.onNavigateToRegister)
    }
}

@Composable
private fun LoginFormCard(uiState: LoginFormProps, handlers: LoginFormHandlers) {
    val hasError = uiState.visibleError != null
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text("Iniciar Sesión", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                text = "Ingresa para acceder a cupones exclusivos de tu colonia",
                fontSize = 13.sp,
                color = SensusTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            LoginIdentifierField(uiState.emailOrPhone, hasError, handlers.onEmailOrPhoneChange)
            Spacer(modifier = Modifier.height(14.dp))
            LoginPasswordField(uiState.password, uiState.isPasswordVisible, hasError, handlers.onPasswordChange, handlers.onTogglePasswordVisibility)
            LoginSecondaryLinks(handlers.callbacks.onNavigateToOtp, handlers.callbacks.onForgotPassword)
            LoginErrorBanner(uiState.visibleError)
            LoginSubmitButton(uiState.isLoading, uiState.isGuestLoading, handlers.onSubmitLogin)
            Spacer(modifier = Modifier.height(18.dp))
            LoginDividerRow()
            Spacer(modifier = Modifier.height(14.dp))
            GuestContinueButton(uiState.isGuestLoading, handlers.onContinueAsGuest)
        }
    }
}

@Composable
private fun LoginIdentifierField(value: String, isError: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Correo o Teléfono") },
        leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = if (isError) SensusRed else SensusPurple) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(14.dp),
        colors = loginFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoginPasswordField(value: String, isVisible: Boolean, isError: Boolean, onValueChange: (String) -> Unit, onToggleVisibility: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Contraseña") },
        leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = if (isError) SensusRed else SensusPurple) },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = SensusTextMuted
                )
            }
        },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        shape = RoundedCornerShape(14.dp),
        colors = loginFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SensusPurple,
    unfocusedBorderColor = SensusCardBorder,
    focusedLabelColor = SensusPurple,
    errorBorderColor = SensusRed,
    errorLabelColor = SensusRed
)

@Composable
private fun LoginSecondaryLinks(onNavigateToOtp: () -> Unit, onForgotPassword: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onNavigateToOtp) {
            Text(text = "Entrar con código SMS", fontSize = 12.sp, color = SensusPurple, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onForgotPassword) {
            Text(text = "¿Olvidaste tu contraseña?", fontSize = 12.sp, color = SensusTextSecondary)
        }
    }
}

@Composable
private fun LoginErrorBanner(visibleError: String?) {
    if (visibleError.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        return
    }
    Surface(
        color = SensusRed.copy(alpha = ERROR_BG_ALPHA),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SensusRed.copy(alpha = ERROR_BORDER_ALPHA)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = SensusRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = visibleError, color = SensusRed, fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun LoginSubmitButton(isLoading: Boolean, isGuestLoading: Boolean, onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        enabled = !isLoading && !isGuestLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(text = if (isLoading) "Iniciando..." else "Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LoginDividerRow() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = SensusCardBorder)
        Text(text = "  O  ", fontSize = 12.sp, color = SensusTextMuted)
        HorizontalDivider(modifier = Modifier.weight(1f), color = SensusCardBorder)
    }
}

@Composable
private fun GuestContinueButton(isGuestLoading: Boolean, onContinueAsGuest: () -> Unit) {
    OutlinedButton(
        onClick = onContinueAsGuest,
        enabled = !isGuestLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SensusPurple),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = "Continuar como invitado", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
    }
}

@Composable
private fun LoginRegisterFooter(onNavigateToRegister: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "¿No tienes una cuenta? ", fontSize = 14.sp, color = SensusTextSecondary)
        Text(
            text = "Regístrate aquí",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SensusPurple,
            modifier = Modifier.clickable(onClick = onNavigateToRegister)
        )
    }
}

private fun submitLoginIfValid(viewModel: LoginViewModel, emailOrPhone: String, password: String): String? {
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

private fun validateLoginCredentials(cleanEmail: String, cleanPass: String): String? = when {
    cleanEmail.isBlank() -> "Ingresa tu correo o teléfono."
    !isValidLoginIdentifier(cleanEmail) -> "El correo electrónico o teléfono no es válido."
    cleanPass.isBlank() -> "Ingresa tu contraseña."
    cleanPass.length < MIN_PASSWORD_LENGTH -> "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres."
    else -> null
}

private fun isValidLoginIdentifier(cleanEmail: String): Boolean {
    val isPhone = cleanEmail.length in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS && cleanEmail.all(Char::isDigit)
    val isEmail = cleanEmail.contains("@") && cleanEmail.contains(".") && !cleanEmail.contains(" ")
    return isPhone || isEmail
}
