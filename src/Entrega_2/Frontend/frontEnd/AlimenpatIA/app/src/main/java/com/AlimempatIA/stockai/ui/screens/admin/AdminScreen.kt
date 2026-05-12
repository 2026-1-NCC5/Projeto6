package com.AlimempatIA.stockai.ui.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.AlimempatIA.stockai.domain.auth.RegisterResult
import com.AlimempatIA.stockai.domain.auth.UserStore
import com.AlimempatIA.stockai.domain.model.User
import com.AlimempatIA.stockai.domain.model.UserRole
import com.AlimempatIA.stockai.navigation.Routes
import com.AlimempatIA.stockai.ui.components.AppBottomNavBar
import com.AlimempatIA.stockai.ui.theme.*
import kotlinx.coroutines.launch

// ── Helpers de cor/icone ─────────────────────────────────
fun roleColor(role: UserRole): Color = when (role) {
    UserRole.OPERADOR          -> Color(0xFF1A73E8)
    UserRole.SUPERVISAO        -> Color(0xFF00BCD4)
    UserRole.CONSELHO_MENTORIA -> Color(0xFF7C4DFF)
    UserRole.COORDENACAO       -> Color(0xFFFFD600)
    UserRole.ADMINISTRADOR     -> Color(0xFFFF1744)
}

fun roleIcon(role: UserRole) = when (role) {
    UserRole.OPERADOR          -> Icons.Filled.Person
    UserRole.SUPERVISAO        -> Icons.Filled.Visibility
    UserRole.CONSELHO_MENTORIA -> Icons.Filled.Groups
    UserRole.COORDENACAO       -> Icons.Filled.ManageAccounts
    UserRole.ADMINISTRADOR     -> Icons.Filled.AdminPanelSettings
}

private val ALL_ROLES       = UserRole.entries.toList()
private val CREATABLE_ROLES = listOf(
    UserRole.SUPERVISAO, UserRole.CONSELHO_MENTORIA,
    UserRole.COORDENACAO, UserRole.ADMINISTRADOR
)

// ── Root ──────────────────────────────────────────────────
@Composable
fun AdminScreen(navController: NavController) {
    val currentUser = UserStore.currentUser
    val coroutineScope = rememberCoroutineScope()

    if (currentUser == null || currentUser.role != UserRole.ADMINISTRADOR) {
        LaunchedEffect(Unit) {
            navController.navigate(Routes.DASHBOARD) { popUpTo(0) { inclusive = false } }
        }
        return
    }

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<User?>(null) }
    var deleteTarget by remember { mutableStateOf<User?>(null) }
    var filterRole by remember { mutableStateOf<UserRole?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        users = UserStore.getAllUsers()
        android.util.Log.d("AdminScreen", "=== Get ${users.size} usuario ===")
        users.forEach { user ->
            android.util.Log.d("AdminScreen", "user: ${user.name} - ${user.role}")
        }
        isLoading = false
    }

    fun refresh() {
        coroutineScope.launch {
            users = UserStore.getAllUsers(forceRefresh = true)
            android.util.Log.d("AdminScreen", "Após atualizar: ${users.size} usuários")
        }
    }

    val filtered = if (filterRole == null) users else users.filter { it.role == filterRole }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = { AdminTopBar(currentUser, navController) },
        bottomBar = { AppBottomNavBar(navController = navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                icon = { Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(20.dp)) },
                text = { Text("Novo Usuario", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }
    ) { pv ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(pv)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item { StatsSection(users = users) }
                item { FilterSection(selected = filterRole, onSelect = { filterRole = it }) }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Membros cadastrados",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary
                        )
                        Text(
                            "${filtered.size} encontrado${if (filtered.size != 1) "s" else ""}",
                            fontSize = 12.sp, color = TextDisabled
                        )
                    }
                }
                items(filtered, key = { it.id }) { user ->
                    UserCard(
                        user     = user,
                        isSelf   = user.id == currentUser.id,
                        onEdit   = { editTarget = user },
                        onDelete = { deleteTarget = user }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            onDismiss = { showCreateDialog = false },
            onCreated = {
                coroutineScope.launch {
                    showCreateDialog = false
                    refresh()
                }
            }
        )
    }

    editTarget?.let { target ->
        EditUserDialog(
            user = target,
            isSelf = target.id == currentUser.id,
            onDismiss = { editTarget = null },
            onSaved = {
                coroutineScope.launch {
                    editTarget = null
                    refresh()
                }
            }
        )
    }

    deleteTarget?.let { target ->
        DeleteConfirmDialog(
            user = target,
            onConfirm = {
                coroutineScope.launch {
                    UserStore.deleteUser(target.id)
                    deleteTarget = null
                    refresh()
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

// ── Top bar ────────────────────────────────────────────────
@Composable
private fun AdminTopBar(currentUser: User, navController: NavController) {
    Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardDark)
                    .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StatusRed.copy(0.12f))
                    .border(1.dp, StatusRed.copy(0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AdminPanelSettings, null, tint = StatusRed, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Painel Administrativo",
                    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary, letterSpacing = (-0.3).sp
                )
                Text("Ola, ${currentUser.name}", fontSize = 11.sp, color = TextSecondary)
            }
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

// ── Stats ──────────────────────────────────────────────────
@Composable
private fun StatsSection(users: List<User>) {
    val countByRole = UserRole.entries.associateWith { r -> users.count { it.role == r } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, PrimaryBlue.copy(0.25f), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(PrimaryBlue.copy(0.15f))
                    .border(1.dp, PrimaryBlue.copy(0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Group, null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("${users.size}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryBlue)
                Text("Total de usuarios cadastrados", fontSize = 12.sp, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(StatusGreen.copy(0.12f))
                    .border(1.dp, StatusGreen.copy(0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Ativo", fontSize = 11.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CREATABLE_ROLES.take(2).forEach { role ->
                RoleStatCard(Modifier.weight(1f), countByRole[role] ?: 0, role)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CREATABLE_ROLES.drop(2).forEach { role ->
                RoleStatCard(Modifier.weight(1f), countByRole[role] ?: 0, role)
            }
        }
    }
}

@Composable
private fun RoleStatCard(modifier: Modifier, count: Int, role: UserRole) {
    val color = roleColor(role)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, color.copy(0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                .background(color.copy(0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(roleIcon(role), null, tint = color, modifier = Modifier.size(19.dp))
        }
        Column {
            Text("$count", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(role.label.split(" ").first(), fontSize = 10.sp, color = TextSecondary, maxLines = 1)
        }
    }
}

// ── Filter ─────────────────────────────────────────────────
@Composable
private fun FilterSection(selected: UserRole?, onSelect: (UserRole?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "FILTRAR POR PERFIL",
            fontSize = 10.sp, color = TextDisabled,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AdminChip("Todos", selected == null, PrimaryBlue) { onSelect(null) }
            ALL_ROLES.take(3).forEach { role ->
                AdminChip(role.label.split(" ").first(), selected == role, roleColor(role)) { onSelect(role) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ALL_ROLES.drop(3).forEach { role ->
                AdminChip(role.label.split(" ").first(), selected == role, roleColor(role)) { onSelect(role) }
            }
        }
    }
}

@Composable
private fun AdminChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(0.18f) else CardDark)
            .border(1.dp, if (selected) color.copy(0.5f) else DividerColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            label, fontSize = 12.sp,
            color = if (selected) color else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ── User card ──────────────────────────────────────────────
@Composable
private fun UserCard(user: User, isSelf: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color    = roleColor(user.role)
    val initials = user.name.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(color, color.copy(0.2f))))
                .align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(color.copy(0.12f))
                    .border(2.dp, color.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        user.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelf) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryBlue.copy(0.2f))
                                .border(1.dp, PrimaryBlue.copy(0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Voce", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("@${user.username}", fontSize = 12.sp, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(0.1f))
                        .border(1.dp, color.copy(0.25f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(roleIcon(user.role), null, tint = color, modifier = Modifier.size(12.dp))
                    Text(user.role.label, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue.copy(0.1f))
                        .border(1.dp, PrimaryBlue.copy(0.3f), RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Edit, null, tint = PrimaryBlue, modifier = Modifier.size(17.dp))
                }
                if (!isSelf) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            .background(StatusRed.copy(0.08f))
                            .border(1.dp, StatusRed.copy(0.25f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Filled.PersonRemove, null, tint = StatusRed, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

// ── Edit dialog ────────────────────────────────────────────
@Composable
private fun EditUserDialog(user: User, isSelf: Boolean, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var selectedTab     by remember { mutableIntStateOf(0) }
    var nameValue       by remember { mutableStateOf(user.name) }
    var selectedRole    by remember { mutableStateOf(user.role) }
    var roleExpanded    by remember { mutableStateOf(false) }
    var profileMsg      by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPwVisible    by remember { mutableStateOf(false) }
    var confPwVisible   by remember { mutableStateOf(false) }
    var passwordMsg     by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDark)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                        .background(Brush.horizontalGradient(listOf(roleColor(user.role), roleColor(user.role).copy(0.2f))))
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 18.dp, bottom = 14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val color = roleColor(user.role)
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape)
                            .background(color.copy(0.15f)).border(1.5.dp, color.copy(0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.split(" ").filter { it.isNotBlank() }.take(2)
                                .joinToString("") { it.first().uppercase() },
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("@${user.username}", fontSize = 12.sp, color = TextSecondary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CardDark,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryBlue
                    )
                }
            ) {
                listOf("Perfil", "Senha").forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i; profileMsg = null; passwordMsg = null },
                        text = {
                            Text(
                                title, fontSize = 13.sp,
                                fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == i) PrimaryBlue else TextSecondary
                            )
                        }
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selectedTab) {
                    0 -> {
                        OutlinedTextField(
                            value = nameValue, onValueChange = { nameValue = it; profileMsg = null },
                            label = { Text("Nome completo") },
                            leadingIcon = { Icon(Icons.Filled.Badge, null) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            colors = adminFieldColors()
                        )
                        if (!isSelf) {
                            Box {
                                OutlinedTextField(
                                    value = selectedRole.label, onValueChange = {},
                                    readOnly = true, label = { Text("Perfil de acesso") },
                                    leadingIcon = { Icon(roleIcon(selectedRole), null, tint = roleColor(selectedRole)) },
                                    trailingIcon = {
                                        IconButton(onClick = { roleExpanded = true }) {
                                            Icon(Icons.Filled.ArrowDropDown, null, tint = TextSecondary)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(), colors = adminFieldColors()
                                )
                                DropdownMenu(
                                    expanded = roleExpanded, onDismissRequest = { roleExpanded = false },
                                    modifier = Modifier.background(CardDarkElevated)
                                ) {
                                    ALL_ROLES.forEach { role ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(roleIcon(role), null, tint = roleColor(role), modifier = Modifier.size(18.dp))
                                                    Text(role.label, color = TextPrimary)
                                                }
                                            },
                                            onClick = { selectedRole = role; roleExpanded = false }
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(CardDarkElevated).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Info, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Text("Seu proprio perfil nao pode ser alterado.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        FeedbackRow(msg = profileMsg)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val ok = UserStore.updateName(user.id, nameValue.trim())
                                    if (!ok) {
                                        profileMsg = false to "Nome nao pode ser vazio."
                                        return@launch
                                    }
                                    if (!isSelf) {
                                        UserStore.setRole(user.id, selectedRole)
                                    }
                                    profileMsg = true to "Dados atualizados!"
                                    onSaved()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Salvar alteracoes", fontWeight = FontWeight.Bold)
                        }
                    }
                    1 -> {
                        PasswordFieldAdmin(newPassword, { newPassword = it; passwordMsg = null }, "Nova senha", newPwVisible, { newPwVisible = !newPwVisible })
                        PasswordFieldAdmin(confirmPassword, { confirmPassword = it; passwordMsg = null }, "Confirmar senha", confPwVisible, { confPwVisible = !confPwVisible },
                            trailingOk = confirmPassword.isNotBlank() && confirmPassword == newPassword)
                        FeedbackRow(msg = passwordMsg)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    when {
                                        newPassword.length < 6 -> passwordMsg = false to "Minimo 6 caracteres."
                                        newPassword != confirmPassword -> passwordMsg = false to "Senhas nao coincidem."
                                        else -> {
                                            UserStore.adminResetPassword(user.id, newPassword)
                                            passwordMsg = true to "Senha redefinida!"
                                            newPassword = ""; confirmPassword = ""
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AIGlow)
                        ) {
                            Icon(Icons.Filled.LockReset, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Redefinir senha", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── Create dialog ──────────────────────────────────────────
@Composable
private fun CreateUserDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    var name            by remember { mutableStateOf("") }
    var username        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole    by remember { mutableStateOf(UserRole.SUPERVISAO) }
    var error           by remember { mutableStateOf<String?>(null) }
    var expanded        by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(CardDark).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryBlue.copy(0.3f)))))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 18.dp, bottom = 14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(PrimaryBlue.copy(0.15f)).border(1.dp, PrimaryBlue.copy(0.35f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.PersonAdd, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Novo Usuario", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Preencha os dados do membro", fontSize = 12.sp, color = TextSecondary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it; error = null }, label = { Text("Nome completo") },
                    leadingIcon = { Icon(Icons.Filled.Badge, null) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = adminFieldColors())
                OutlinedTextField(username, { username = it; error = null }, label = { Text("Usuario (login)") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), colors = adminFieldColors())
                OutlinedTextField(
                    password, { password = it; error = null }, label = { Text("Senha") },
                    leadingIcon = { Icon(Icons.Filled.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = TextSecondary)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true, modifier = Modifier.fillMaxWidth(), colors = adminFieldColors()
                )
                Box {
                    OutlinedTextField(
                        selectedRole.label, {}, readOnly = true, label = { Text("Perfil de acesso") },
                        leadingIcon = { Icon(roleIcon(selectedRole), null, tint = roleColor(selectedRole)) },
                        trailingIcon = { IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.ArrowDropDown, null, tint = TextSecondary) } },
                        modifier = Modifier.fillMaxWidth(), colors = adminFieldColors()
                    )
                    DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(CardDarkElevated)) {
                        CREATABLE_ROLES.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(roleIcon(role), null, tint = roleColor(role), modifier = Modifier.size(18.dp))
                                        Text(role.label, color = TextPrimary)
                                    }
                                },
                                onClick = { selectedRole = role; expanded = false }
                            )
                        }
                    }
                }
                AnimatedVisibility(error != null) {
                    FeedbackRow(msg = error?.let { false to it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                    ) { Text("Cancelar", color = TextSecondary) }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                when (val r = UserStore.register(
                                    name = name.trim(),
                                    username = username.trim(),
                                    password = password,
                                    role = selectedRole
                                )) {
                                    is RegisterResult.Success -> {
                                        onCreated()
                                    }
                                    is RegisterResult.Error -> {
                                        error = r.message
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Criar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Delete dialog ──────────────────────────────────────────
@Composable
private fun DeleteConfirmDialog(user: User, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        icon = {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(StatusRed.copy(0.12f))
                    .border(1.dp, StatusRed.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PersonRemove, null, tint = StatusRed, modifier = Modifier.size(24.dp))
            }
        },
        title = { Text("Remover usuario?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${user.name} (@${user.username}) sera removido permanentemente do sistema.",
                    color = TextSecondary, fontSize = 14.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StatusYellow.copy(0.08f))
                        .border(1.dp, StatusYellow.copy(0.25f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Warning, null, tint = StatusYellow, modifier = Modifier.size(16.dp))
                    Text("Essa acao nao pode ser desfeita.", color = StatusYellow, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Remover", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
            ) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

// ── Shared composables ─────────────────────────────────────────
@Composable
private fun FeedbackRow(msg: Pair<Boolean, String>?) {
    AnimatedVisibility(msg != null) {
        if (msg == null) return@AnimatedVisibility
        val (ok, text) = msg
        val color = if (ok) StatusGreen else StatusRed
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(0.1f))
                .border(1.dp, color.copy(0.3f), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.Error,
                null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PasswordFieldAdmin(
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
        colors = adminFieldColors()
    )
}

@Composable
private fun adminFieldColors() = OutlinedTextFieldDefaults.colors(
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