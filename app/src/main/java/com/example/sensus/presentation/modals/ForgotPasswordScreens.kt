package com.example.sensus.presentation.modals

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.common.Result
import com.example.sensus.domain.model.PasswordPolicy
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import kotlinx.coroutines.launch

private const val STEP_REQUEST_EMAIL = 1
private const val STEP_LINK_SENT = 2
private const val STEP_NEW_PASSWORD = 3
private const val BADGE_ALPHA = 0.12f
private const val LARGE_BADGE_DP = 64

@ComposeUiModel
private data class ForgotPasswordFormProps(
    val step: Int,
    val email: String = "",
    val resetToken: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@ComposeUiModel
private data class ForgotPasswordStepActions(
    val onStateChange: (ForgotPasswordFormProps) -> Unit,
    val onSubmitEmail: () -> Unit,
    val onOpenEmailApp: () -> Unit,
    val onSubmitNewPassword: () -> Unit
)

@ComposeUiModel
private data class PasswordInputSpec(
    val label: String,
    val value: String,
    val placeholder: String,
    val visible: Boolean = false,
    val onToggleVisibility: (() -> Unit)? = null
)

@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onPasswordResetSuccess: () -> Unit,
    initialToken: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uiState by remember {
        mutableStateOf(
            ForgotPasswordFormProps(
                step = if (!initialToken.isNullOrBlank()) STEP_NEW_PASSWORD else STEP_REQUEST_EMAIL,
                resetToken = initialToken.orEmpty()
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .padding(16.dp)
    ) {
        ForgotPasswordTopBar(step = uiState.step, onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(24.dp))
        ForgotPasswordStepBody(
            uiState = uiState,
            actions = ForgotPasswordStepActions(
                onStateChange = { uiState = it },
                onSubmitEmail = {
                    submitEmailReset(
                        uiState = uiState,
                        viewModel = viewModel,
                        onUpdate = { uiState = it },
                        launchTask = { block -> scope.launch { block() } }
                    )
                },
                onOpenEmailApp = { openEmailClientSafely(context) },
                onSubmitNewPassword = {
                    submitNewPassword(
                        uiState = uiState,
                        viewModel = viewModel,
                        onUpdate = { uiState = it },
                        onSuccess = onPasswordResetSuccess,
                        launchTask = { block -> scope.launch { block() } }
                    )
                }
            )
        )
    }
}

@Composable
private fun ForgotPasswordStepBody(uiState: ForgotPasswordFormProps, actions: ForgotPasswordStepActions) {
    when (uiState.step) {
        STEP_REQUEST_EMAIL -> RequestResetStepCard(
            email = uiState.email,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            onEmailChange = { actions.onStateChange(uiState.copy(email = it, errorMessage = null)) },
            onSubmit = actions.onSubmitEmail
        )
        STEP_LINK_SENT -> ResetLinkSentStepCard(
            onOpenEmailApp = actions.onOpenEmailApp,
            onAdvanceToNewPassword = { actions.onStateChange(uiState.copy(step = STEP_NEW_PASSWORD)) }
        )
        else -> NewPasswordStepCard(uiState = uiState, actions = actions)
    }
}

@Composable
private fun ForgotPasswordTopBar(step: Int, onNavigateBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SensusPurple)
        }
        Spacer(modifier = Modifier.width(12.dp))
        val title = when (step) {
            STEP_REQUEST_EMAIL -> "¿Olvidaste tu contraseña?"
            STEP_LINK_SENT -> "Revisa tu correo"
            else -> "Nueva contraseña"
        }
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
    }
}

@Composable
private fun RequestResetStepCard(email: String, isLoading: Boolean, errorMessage: String?, onEmailChange: (String) -> Unit, onSubmit: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StepCircleBadge(icon = Icons.Default.Key, description = "Recuperación de contraseña", tint = SensusPurple)
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "Recuperación por correo", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ingresa tu correo electrónico registrado. Te enviaremos un enlace de recuperación " +
                    "con vigencia limitada de 15 minutos.",
                fontSize = 13.sp,
                color = SensusTextSecondary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("ejemplo@correo.com", fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.MailOutline, contentDescription = null, tint = SensusPurple) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage, fontSize = 12.sp, color = SensusRed, modifier = Modifier.align(Alignment.Start))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onSubmit,
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isLoading) "Enviando solicitud..." else "Enviar enlace de recuperación",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ResetLinkSentStepCard(onOpenEmailApp: () -> Unit, onAdvanceToNewPassword: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StepCircleBadge(icon = Icons.Default.MarkEmailRead, description = "Enlace enviado", tint = SensusSuccess, sizeDp = LARGE_BADGE_DP)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "¡Enlace enviado!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hemos enviado las instrucciones para restablecer tu contraseña a tu correo. " +
                    "Recuerda que el enlace tiene una vigencia estricta de 15 minutos.",
                fontSize = 13.sp,
                color = SensusTextSecondary,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onOpenEmailApp,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(imageVector = Icons.Default.MailOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Abrir app de correo", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onAdvanceToNewPassword, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(text = "Ingresar nueva contraseña", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
            }
        }
    }
}

@Composable
private fun NewPasswordStepCard(uiState: ForgotPasswordFormProps, actions: ForgotPasswordStepActions) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                StepCircleBadge(icon = Icons.Default.LockReset, description = null, tint = SensusPurple)
            }
            Text(text = "Establece tu nueva contraseña", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                text = "Mínimo 8 caracteres, incluyendo al menos una mayúscula, una minúscula y un número.",
                fontSize = 12.sp,
                color = SensusTextSecondary
            )
            NewPasswordInputField(
                spec = PasswordInputSpec(
                    label = "Nueva contraseña",
                    value = uiState.newPassword,
                    placeholder = "Contraseña segura",
                    visible = uiState.isNewPasswordVisible,
                    onToggleVisibility = { actions.onStateChange(uiState.copy(isNewPasswordVisible = !uiState.isNewPasswordVisible)) }
                ),
                onValueChange = { actions.onStateChange(uiState.copy(newPassword = it, errorMessage = null)) }
            )
            NewPasswordInputField(
                spec = PasswordInputSpec("Confirmar contraseña", uiState.confirmPassword, "Repite tu nueva contraseña"),
                onValueChange = { actions.onStateChange(uiState.copy(confirmPassword = it, errorMessage = null)) }
            )
            uiState.errorMessage?.let { Text(text = it, fontSize = 12.sp, color = SensusRed) }
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = actions.onSubmitNewPassword,
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (uiState.isLoading) "Restableciendo..." else "Restablecer e iniciar sesión",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun StepCircleBadge(icon: ImageVector, description: String?, tint: Color, sizeDp: Int = 56) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = BADGE_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size((sizeDp / 2).dp))
    }
}

@Composable
private fun NewPasswordInputField(spec: PasswordInputSpec, onValueChange: (String) -> Unit) {
    Column {
        Text(text = spec.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = spec.value,
            onValueChange = onValueChange,
            placeholder = { Text(spec.placeholder, fontSize = 13.sp) },
            visualTransformation = if (spec.visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = spec.onToggleVisibility?.let { toggle ->
                {
                    IconButton(onClick = toggle) {
                        Icon(
                            imageVector = if (spec.visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Mostrar contraseña",
                            tint = SensusPurple
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )
    }
}

private fun submitEmailReset(
    uiState: ForgotPasswordFormProps,
    viewModel: ForgotPasswordViewModel,
    onUpdate: (ForgotPasswordFormProps) -> Unit,
    launchTask: (suspend () -> Unit) -> Unit
) {
    if (uiState.email.isBlank() || !uiState.email.contains("@")) {
        onUpdate(uiState.copy(errorMessage = "Ingresa un correo electrónico válido."))
        return
    }
    launchTask {
        onUpdate(uiState.copy(isLoading = true, errorMessage = null))
        when (val result = viewModel.requestPasswordResetUseCase(uiState.email.trim())) {
            is Result.Success -> onUpdate(uiState.copy(isLoading = false, step = STEP_LINK_SENT))
            is Result.Failure -> onUpdate(uiState.copy(isLoading = false, errorMessage = result.error.message))
        }
    }
}

private fun submitNewPassword(
    uiState: ForgotPasswordFormProps,
    viewModel: ForgotPasswordViewModel,
    onUpdate: (ForgotPasswordFormProps) -> Unit,
    onSuccess: () -> Unit,
    launchTask: (suspend () -> Unit) -> Unit
) {
    val validationError = validatePasswordReset(uiState.newPassword, uiState.confirmPassword, uiState.resetToken)
    if (validationError != null) {
        onUpdate(uiState.copy(errorMessage = validationError))
        return
    }
    launchTask {
        onUpdate(uiState.copy(isLoading = true, errorMessage = null))
        when (val result = viewModel.resetPasswordUseCase(uiState.resetToken.trim(), uiState.newPassword)) {
            is Result.Success -> {
                onUpdate(uiState.copy(isLoading = false))
                onSuccess()
            }
            is Result.Failure -> onUpdate(uiState.copy(isLoading = false, errorMessage = result.error.message))
        }
    }
}

private fun validatePasswordReset(newPassword: String, confirmPassword: String, resetToken: String): String? = when {
    !PasswordPolicy.isValid(newPassword) ->
        "La contraseña debe tener mínimo 8 caracteres, mayúscula, minúscula y número."
    newPassword != confirmPassword -> "Las contraseñas no coinciden."
    resetToken.isBlank() -> "El token de recuperación es requerido."
    else -> null
}

private fun openEmailClientSafely(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_EMAIL)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sin cliente de correo predeterminado: el usuario permanece en la pantalla
    }
}
