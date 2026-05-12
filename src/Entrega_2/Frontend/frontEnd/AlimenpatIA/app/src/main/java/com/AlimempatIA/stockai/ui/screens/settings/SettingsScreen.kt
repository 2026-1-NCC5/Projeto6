package com.AlimempatIA.stockai.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.AlimempatIA.stockai.domain.auth.UpdatePasswordResult
import com.AlimempatIA.stockai.domain.auth.UserStore
import com.AlimempatIA.stockai.domain.model.UserRole
import com.AlimempatIA.stockai.navigation.Routes
import com.AlimempatIA.stockai.ui.components.AppBottomNavBar
import com.AlimempatIA.stockai.ui.theme.*
import kotlinx.coroutines.launch

// ── Role visual helpers ────────────────────────────────────────────────────────
private fun roleColor(role: UserRole): Color = when (role) {
    UserRole.OPERADOR          -> Color(0xFF1A73E8)
    UserRole.SUPERVISAO        -> Color(0xFF00BCD4)
    UserRole.CONSELHO_MENTORIA -> Color(0xFF7C4DFF)
    UserRole.COORDENACAO       -> Color(0xFFFFD600)
    UserRole.ADMINISTRADOR     -> Color(0xFFFF1744)
}

private fun roleIcon(role: UserRole) = when (role) {
    UserRole.OPERADOR          -> Icons.Filled.Person
    UserRole.SUPERVISAO        -> Icons.Filled.Visibility
    UserRole.CONSELHO_MENTORIA -> Icons.Filled.Groups
    UserRole.COORDENACAO       -> Icons.Filled.ManageAccounts
    UserRole.ADMINISTRADOR     -> Icons.Filled.AdminPanelSettings
}

// ── Root screen ────────────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(navController: NavController) {
    val user = UserStore.currentUser
    val coroutineScope = rememberCoroutineScope()

    if (user == null) {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        }
        return
    }

    var displayName       by remember { mutableStateOf(user.name) }
    var nameFieldValue    by remember { mutableStateOf(user.name) }
    var nameSuccess       by remember { mutableStateOf(false) }
    var nameError         by remember { mutableStateOf<String?>(null) }

    var oldPassword       by remember { mutableStateOf("") }
    var newPassword       by remember { mutableStateOf("") }
    var confirmPassword   by remember { mutableStateOf("") }
    var oldPwVisible      by remember { mutableStateOf(false) }
    var newPwVisible      by remember { mutableStateOf(false) }
    var confirmPwVisible  by remember { mutableStateOf(false) }
    var passwordSuccess   by remember { mutableStateOf(false) }
    var passwordError     by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = { AppBottomNavBar(navController = navController) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item { ProfileHeader(name = displayName, username = user.username, role = user.role) }

            item {
                SectionCard(title = "Informações do Perfil", icon = Icons.Filled.Person) {
                    OutlinedTextField(
                        value = nameFieldValue,
                        onValueChange = { nameFieldValue = it; nameSuccess = false; nameError = null },
                        label = { Text("Nome completo") },
                        leadingIcon = { Icon(Icons.Filled.Badge, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = settingsFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(nameError != null) {
                        Text(nameError ?: "", color = StatusRed, fontSize = 12.sp)
                    }
                    AnimatedVisibility(nameSuccess) {
                        Text("Nome atualizado!", color = StatusGreen, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val ok = UserStore.updateName(user.id, nameFieldValue.trim())
                                if (ok) {
                                    displayName = nameFieldValue.trim()
                                    nameSuccess = true
                                    nameError = null
                                } else {
                                    nameError = "Nome não pode ser vazio."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Salvar nome")
                    }
                }
            }

            item {
                SectionCard(title = "Alterar Senha", icon = Icons.Filled.Lock) {
                    PasswordField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; passwordSuccess = false; passwordError = null },
                        label = "Senha atual",
                        visible = oldPwVisible,
                        onToggle = { oldPwVisible = !oldPwVisible }
                    )
                    Spacer(Modifier.height(8.dp))
                    PasswordField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordSuccess = false; passwordError = null },
                        label = "Nova senha",
                        visible = newPwVisible,
                        onToggle = { newPwVisible = !newPwVisible }
                    )
                    Spacer(Modifier.height(8.dp))
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; passwordSuccess = false; passwordError = null },
                        label = "Confirmar nova senha",
                        visible = confirmPwVisible,
                        onToggle = { confirmPwVisible = !confirmPwVisible },
                        trailingOk = confirmPassword.isNotBlank() && confirmPassword == newPassword
                    )
                    Spacer(Modifier.height(8.dp))
                    AnimatedVisibility(passwordError != null) {
                        Text(passwordError ?: "", color = StatusRed, fontSize = 12.sp)
                    }
                    AnimatedVisibility(passwordSuccess) {
                        Text("Senha alterada com sucesso!", color = StatusGreen, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                passwordError = null
                                passwordSuccess = false
                                if (newPassword != confirmPassword) {
                                    passwordError = "As senhas não coincidem."
                                    return@launch
                                }
                                when (val r = UserStore.updatePassword(user.id, oldPassword, newPassword)) {
                                    is UpdatePasswordResult.Success -> {
                                        passwordSuccess = true
                                        oldPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                    }
                                    is UpdatePasswordResult.Error -> passwordError = r.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AIGlow)
                    ) {
                        Icon(Icons.Filled.LockReset, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Alterar senha")
                    }
                }
            }

            item { RolesSection(currentRole = user.role) }

            if (user.role == UserRole.ADMINISTRADOR) {
                item {
                    Button(
                        onClick = { navController.navigate(Routes.ADMIN) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.85f))
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Painel Administrativo", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        UserStore.logout()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusRed.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Filled.Logout, null, tint = StatusRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sair da conta", color = StatusRed)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Profile header ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileHeader(name: String, username: String, role: UserRole) {
    val color = roleColor(role)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(color.copy(0.4f), color.copy(0.15f))))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(2).uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("@$username", fontSize = 13.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(role.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
        }

        Icon(roleIcon(role), null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
    }
}

// ── Roles hierarchy section ────────────────────────────────────────────────────
@Composable
private fun RolesSection(currentRole: UserRole) {
    SectionCard(title = "Hierarquia de Perfis", icon = Icons.Filled.AccountTree) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UserRole.entries.forEach { role ->
                val isMe = role == currentRole
                val color = roleColor(role)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isMe) color.copy(alpha = 0.12f) else Color.Transparent)
                        .then(
                            if (isMe) Modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(roleIcon(role), null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = role.label,
                        color = if (isMe) color else TextSecondary,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isMe) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Seu perfil", fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(PrimaryBlue.copy(0.5f)))
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
            }
            content()
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggle: () -> Unit,
    trailingOk: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Lock, null) },
        trailingIcon = {
            if (trailingOk) {
                Icon(Icons.Filled.CheckCircle, null, tint = StatusGreen)
            } else {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        null,
                        tint = TextSecondary
                    )
                }
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = settingsFieldColors()
    )
}

@Composable
private fun settingsFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryBlue,
    unfocusedBorderColor = DividerColor,
    focusedLabelColor = PrimaryBlue,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = PrimaryBlue,
    focusedLeadingIconColor = PrimaryBlue,
    unfocusedLeadingIconColor = TextSecondary
)