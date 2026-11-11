package com.example.sensus.presentation.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val SUBTITLE_ALPHA = 0.85f
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
    val acceptTerms: Boolean = true
)

@ComposeUiModel
private data class RegisterValidationErrors(
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null
) {
    val hasErrors: Boolean
        get() = nameError != null || emailError != null || phoneError != null || passwordError != null || generalError != null
}

@ComposeUiModel
private data class RegisterFormProps(
    val form: RegisterFormState,
    val errors: RegisterValidationErrors,
    val remoteError: String?
)

@ComposeUiModel
private data class RegisterFormCallbacks(
    val onFormChange: (RegisterFormState, RegisterValidationErrors) -> Unit,
    val onNavigateToTerms: () -> Unit,
    val onNavigateToLogin: () -> Unit,
    val onSubmit: () -> Unit
)

@ComposeUiModel
private data class RegisterFieldSpec(
    val value: String,
    val label: String,
    val icon: ImageVector,
    val error: String?,
    val keyboardOptions: KeyboardOptions,
    val visualTransformation: VisualTransformation = VisualTransformation.None
)

@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.viewState.collectAsState()
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
    BackHandler { onNavigateToLogin() }

    Box(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding()) {
            RegisterHeader(onNavigateToLogin = onNavigateToLogin)
            RegisterFormContainer(
                uiState = RegisterFormProps(form, errors, state.errorMessage),
                callbacks = RegisterFormCallbacks(
                    onFormChange = { updated, cleared ->
                        form = updated
                        errors = cleared
                    },
                    onNavigateToTerms = onNavigateToTerms,
                    onNavigateToLogin = onNavigateToLogin,
                    onSubmit = {
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = SensusPurple, shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Column {
            IconButton(onClick = onNavigateToLogin, modifier = Modifier.size(36.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Crea tu Cuenta", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = "Únete para descubrir ofertas exclusivas en tu zona",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = SUBTITLE_ALPHA)
            )
        }
    }
}

@Composable
private fun RegisterFormContainer(uiState: RegisterFormProps, callbacks: RegisterFormCallbacks) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RegisterFormFields(uiState = uiState, callbacks = callbacks)
        }
        Spacer(modifier = Modifier.height(20.dp))
        RegisterLoginFooter(onNavigateToLogin = callbacks.onNavigateToLogin)
    }
}

@Composable
private fun RegisterFormFields(uiState: RegisterFormProps, callbacks: RegisterFormCallbacks) {
    val form = uiState.form
    val errors = uiState.errors
    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val visibleGeneralError = uiState.remoteError ?: errors.generalError
        if (!visibleGeneralError.isNullOrBlank()) {
            Text(text = visibleGeneralError, color = SensusRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        RegisterIdentityFields(form = form, errors = errors, onFormChange = callbacks.onFormChange)
        RegisterSecurityFields(form = form, errors = errors, onFormChange = callbacks.onFormChange)
        TermsCheckboxRow(
            checked = form.acceptTerms,
            onCheckedChange = { checked -> callbacks.onFormChange(form.copy(acceptTerms = checked), errors.copy(generalError = null)) },
            onNavigateToTerms = callbacks.onNavigateToTerms
        )
        RegisterSubmitButton(onSubmit = callbacks.onSubmit)
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
    RegisterValidatedField(
        spec = RegisterFieldSpec(
            value = form.password,
            label = "Contraseña (mínimo 6 caracteres)",
            icon = Icons.Default.Lock,
            error = errors.passwordError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = PasswordVisualTransformation()
        ),
        onValueChange = { input ->
            if (input.length <= MAX_PASSWORD_LENGTH) {
                onFormChange(form.copy(password = input), errors.copy(passwordError = null))
            }
        }
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
            leadingIcon = { Icon(spec.icon, contentDescription = null, tint = SensusPurple) },
            visualTransformation = spec.visualTransformation,
            singleLine = true,
            keyboardOptions = spec.keyboardOptions,
            shape = RoundedCornerShape(14.dp),
            colors = sensusOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        spec.error?.let { err ->
            Text(text = err, color = SensusRed, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
        }
    }
}

@Composable
private fun TermsCheckboxRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, onNavigateToTerms: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, colors = CheckboxDefaults.colors(checkedColor = SensusPurple))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Acepto los ", fontSize = 12.sp, color = SensusTextSecondary)
        Text(
            text = "Términos y Privacidad",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = SensusPurple,
            modifier = Modifier.clickable(onClick = onNavigateToTerms)
        )
    }
}

@Composable
private fun RegisterSubmitButton(onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Text(text = "Crear Cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun RegisterLoginFooter(onNavigateToLogin: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "¿Ya tienes una cuenta? ", fontSize = 14.sp, color = SensusTextSecondary)
        Text(
            text = "Inicia sesión",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = SensusPurple,
            modifier = Modifier.clickable(onClick = onNavigateToLogin)
        )
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
    generalError = if (!form.acceptTerms) "Debes aceptar los Términos y Privacidad para continuar" else null
)

private fun submitRegistration(viewModel: RegisterViewModel, form: RegisterFormState) {
    viewModel.dispatchIntent(RegisterViewIntent.NameChanged(form.name.trim()))
    viewModel.dispatchIntent(RegisterViewIntent.EmailChanged(form.email.trim()))
    viewModel.dispatchIntent(RegisterViewIntent.PasswordChanged(form.password))
    viewModel.dispatchIntent(RegisterViewIntent.ConfirmPasswordChanged(form.password))
    viewModel.dispatchIntent(RegisterViewIntent.SubmitRegister)
}
