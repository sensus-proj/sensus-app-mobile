package com.example.sensus.presentation.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val MIN_NAME_LENGTH = 3
private const val MAX_NAME_LENGTH = 50
private const val MAX_EMAIL_LENGTH = 60
private const val PHONE_DIGIT_LENGTH = 10
private const val MIN_PASSWORD_LENGTH = 6
private const val MAX_PASSWORD_LENGTH = 30
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

@ComposeUiModel
private data class RegisterFormState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val acceptTerms: Boolean = true
)

@ComposeUiModel
private data class RegisterValidationErrors(
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null
) {
    val hasErrors: Boolean
        get() = listOf(nameError, emailError, phoneError, passwordError, confirmPasswordError, generalError).any { it != null }
}

@ComposeUiModel
private data class RegisterFormProps(
    val form: RegisterFormState,
    val errors: RegisterValidationErrors,
    val remoteError: String?,
    val isRegistering: Boolean,
    val isGuestLoading: Boolean
)

@ComposeUiModel
private data class RegisterFormCallbacks(
    val onFormChange: (RegisterFormState, RegisterValidationErrors) -> Unit,
    val onNavigateToTerms: () -> Unit,
    val onContinueAsGuest: () -> Unit,
    val onSubmit: () -> Unit
)

@ComposeUiModel
private data class RegisterFieldSpec(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val error: String?,
    val keyboardOptions: KeyboardOptions,
    val visualTransformation: VisualTransformation = VisualTransformation.None,
    val trailingIcon: (@Composable () -> Unit)? = null
)

@ComposeUiModel
private data class RegisterPasswordFieldSpec(
    val value: String,
    val label: String,
    val error: String?,
    val isVisible: Boolean,
    val imeAction: ImeAction
)

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onContinueAsGuest: () -> Unit = {}
) {
    val viewModel: RegisterViewModel = hiltViewModel()
    val guestViewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.viewState.collectAsState()
    val guestState by guestViewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var form by remember { mutableStateOf(RegisterFormState()) }
    var errors by remember { mutableStateOf(RegisterValidationErrors()) }

    LaunchedEffect(Unit) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                is RegisterViewEffect.NavigateToHome -> onRegisterSuccess()
                is RegisterViewEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(guestViewModel) {
        guestViewModel.viewEffect.collect { effect ->
            if (effect == LoginViewEffect.NavigateToHomeAsGuest) onContinueAsGuest()
        }
    }
    BackHandler { onNavigateToLogin() }

    AuthAccessScreenLayout(modifier, header = { RegisterHeader(onNavigateToLogin) }) {
        AuthAccessSelector(isRegister = true, onSwitch = onNavigateToLogin)
        AuthDataFrame {
            RegisterFormFields(
                uiState = RegisterFormProps(form, errors, guestState.errorMessage ?: state.errorMessage, state.isLoading, guestState.isGuestLoading),
                callbacks = RegisterFormCallbacks(
                    onFormChange = { updated, cleared ->
                        form = updated
                        errors = cleared
                    },
                    onNavigateToTerms = onNavigateToTerms,
                    onContinueAsGuest = { guestViewModel.dispatchIntent(LoginViewIntent.StartGuestSession) },
                    onSubmit = {
                        guestViewModel.dispatchIntent(LoginViewIntent.DismissError)
                        val result = validateRegistrationForm(form)
                        errors = result
                        if (!result.hasErrors) submitRegistration(viewModel, form)
                    }
                )
            )
        }
    }
}

@Composable
private fun RegisterHeader(onNavigateToLogin: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        AuthAccessBrand(showBackSpace = true)
        IconButton(onClick = onNavigateToLogin, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.auth_back),
                tint = SensusVioletInk
            )
        }
    }
}

@Composable
private fun RegisterFormFields(uiState: RegisterFormProps, callbacks: RegisterFormCallbacks) {
    val form = uiState.form
    val errors = uiState.errors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AuthFormTitle(stringResource(R.string.auth_register_title))
        Text(
            stringResource(R.string.auth_register_description),
            color = SensusTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        val visibleGeneralError = uiState.remoteError ?: errors.generalError
        if (!visibleGeneralError.isNullOrBlank()) {
            Text(text = visibleGeneralError, color = SensusRed, style = MaterialTheme.typography.labelMedium)
        }
        RegisterIdentityFields(form = form, errors = errors, onFormChange = callbacks.onFormChange)
        RegisterSecurityFields(form = form, errors = errors, onFormChange = callbacks.onFormChange)
        TermsCheckboxRow(
            checked = form.acceptTerms,
            onCheckedChange = { checked -> callbacks.onFormChange(form.copy(acceptTerms = checked), errors.copy(generalError = null)) },
            onNavigateToTerms = callbacks.onNavigateToTerms
        )
        val busy = uiState.isRegistering || uiState.isGuestLoading
        RegisterSubmitButton(enabled = !busy, onSubmit = callbacks.onSubmit)
        AuthGuestButton(uiState.isGuestLoading, !busy, callbacks.onContinueAsGuest)
    }
}

@Composable
private fun RegisterIdentityFields(
    form: RegisterFormState,
    errors: RegisterValidationErrors,
    onFormChange: (RegisterFormState, RegisterValidationErrors) -> Unit
) {
    RegisterValidatedField(
        spec = RegisterFieldSpec(form.name, "Nombre Completo", Icons.Default.Person, errors.nameError, KeyboardOptions(imeAction = ImeAction.Next)),
        onValueChange = { input ->
            if (input.length <= MAX_NAME_LENGTH && input.all { it.isLetter() || it.isWhitespace() }) {
                onFormChange(form.copy(name = input), errors.copy(nameError = null))
            }
        }
    )
    RegisterValidatedField(
        spec = RegisterFieldSpec(
            value = form.email,
            label = "Correo Electrónico",
            icon = Icons.Default.Email,
            error = errors.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        ),
        onValueChange = { input ->
            if (input.length <= MAX_EMAIL_LENGTH && !input.contains(" ")) {
                onFormChange(form.copy(email = input), errors.copy(emailError = null))
            }
        }
    )
}

@Composable
private fun RegisterSecurityFields(
    form: RegisterFormState,
    errors: RegisterValidationErrors,
    onFormChange: (RegisterFormState, RegisterValidationErrors) -> Unit
) {
    RegisterValidatedField(
        spec = RegisterFieldSpec(
            value = form.phone,
            label = "Teléfono Celular (10 dígitos)",
            icon = Icons.Default.Phone,
            error = errors.phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
        ),
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit)
            if (digits.length <= PHONE_DIGIT_LENGTH) {
                onFormChange(form.copy(phone = digits), errors.copy(phoneError = null))
            }
        }
    )
    RegisterPasswordField(
        spec = RegisterPasswordFieldSpec(
            value = form.password,
            label = "Contraseña (mínimo 6 caracteres)",
            error = errors.passwordError,
            isVisible = form.isPasswordVisible,
            imeAction = ImeAction.Next
        ),
        onValueChange = { input ->
            if (input.length <= MAX_PASSWORD_LENGTH) {
                onFormChange(form.copy(password = input), errors.copy(passwordError = null, confirmPasswordError = null))
            }
        },
        onToggleVisibility = { onFormChange(form.copy(isPasswordVisible = !form.isPasswordVisible), errors) }
    )
    RegisterPasswordField(
        spec = RegisterPasswordFieldSpec(
            value = form.confirmPassword,
            label = "Confirmar contraseña",
            error = errors.confirmPasswordError,
            isVisible = form.isConfirmPasswordVisible,
            imeAction = ImeAction.Done
        ),
        onValueChange = { input ->
            if (input.length <= MAX_PASSWORD_LENGTH) {
                onFormChange(form.copy(confirmPassword = input), errors.copy(confirmPasswordError = null))
            }
        },
        onToggleVisibility = { onFormChange(form.copy(isConfirmPasswordVisible = !form.isConfirmPasswordVisible), errors) }
    )
}

@Composable
private fun RegisterPasswordField(spec: RegisterPasswordFieldSpec, onValueChange: (String) -> Unit, onToggleVisibility: () -> Unit) {
    RegisterValidatedField(
        spec = RegisterFieldSpec(
            value = spec.value,
            label = spec.label,
            icon = Icons.Default.Lock,
            error = spec.error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = spec.imeAction),
            visualTransformation = if (spec.isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (spec.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (spec.isVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = SensusTextMuted
                    )
                }
            }
        ),
        onValueChange = onValueChange
    )
}

@Composable
private fun RegisterValidatedField(spec: RegisterFieldSpec, onValueChange: (String) -> Unit) {
    Column {
        OutlinedTextField(
            value = spec.value,
            onValueChange = onValueChange,
            label = { Text(spec.label) },
            isError = spec.error != null,
            leadingIcon = { Icon(spec.icon, contentDescription = null, tint = SensusVioletInk) },
            trailingIcon = spec.trailingIcon,
            visualTransformation = spec.visualTransformation,
            singleLine = true,
            keyboardOptions = spec.keyboardOptions,
            shape = RoundedCornerShape(14.dp),
            colors = sensusOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        spec.error?.let { err ->
            Text(text = err, color = SensusRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
        }
    }
}

@Composable
private fun TermsCheckboxRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onNavigateToTerms: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = SensusVioletInk))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Acepto los ", style = MaterialTheme.typography.labelMedium, color = SensusTextSecondary)
        Text(
            text = "Términos y Privacidad",
            style = MaterialTheme.typography.labelMedium,
            color = SensusVioletInk,
            modifier = Modifier.clickable(onClick = onNavigateToTerms)
        )
    }
}

@Composable
private fun RegisterSubmitButton(enabled: Boolean, onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = AuthActionPadding,
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Crear Cuenta", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

private fun validateRegistrationForm(form: RegisterFormState): RegisterValidationErrors = RegisterValidationErrors(
    nameError = if (form.name.trim().length < MIN_NAME_LENGTH) "Ingresa tu nombre completo (mínimo 3 letras)" else null,
    emailError = if (!EMAIL_REGEX.matches(form.email.trim())) "Ingresa un correo electrónico válido (ej. usuario@correo.com)" else null,
    phoneError = if (form.phone.filter(Char::isDigit).length != PHONE_DIGIT_LENGTH) {
        "El número celular debe tener exactamente 10 dígitos"
    } else {
        null
    },
    passwordError = if (form.password.length < MIN_PASSWORD_LENGTH) "La contraseña debe tener al menos 6 caracteres" else null,
    confirmPasswordError = when {
        form.confirmPassword.isEmpty() -> "Confirma tu contraseña"
        form.confirmPassword != form.password -> "Las contraseñas no coinciden"
        else -> null
    },
    generalError = if (!form.acceptTerms) "Debes aceptar los Términos y Privacidad para continuar" else null
)

private fun submitRegistration(viewModel: RegisterViewModel, form: RegisterFormState) {
    viewModel.dispatchIntent(RegisterViewIntent.NameChanged(form.name.trim()))
    viewModel.dispatchIntent(RegisterViewIntent.EmailChanged(form.email.trim()))
    viewModel.dispatchIntent(RegisterViewIntent.PasswordChanged(form.password))
    viewModel.dispatchIntent(RegisterViewIntent.ConfirmPasswordChanged(form.confirmPassword))
    viewModel.dispatchIntent(RegisterViewIntent.SubmitRegister)
}
