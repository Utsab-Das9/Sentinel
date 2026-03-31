package com.privacynudge.ui

import com.privacynudge.data.*

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryToggleOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.viewinterop.AndroidView
import com.privacynudge.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.BitmapFactory

/**
 * Internal navigation pages for the Settings section.
 */
enum class SettingsPage {
    MAIN, GENERAL, PROFILE, PRIVACY, NOTIFICATIONS, SECURITY, DATA_STORAGE, HELP, ABOUT, BLOCKED_APPS, ALERTS_HISTORY, PRIVACY_POLICY, TERMS_CONDITIONS, USER_GUIDE, FAQ, REPORT_BUG, CONTACT_SUPPORT
}

/**
 * Root Settings screen with internal navigation.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    when (currentPage) {
        SettingsPage.MAIN -> SettingsMainPage(
            onNavigate = { currentPage = it },
            modifier = modifier
        )
        SettingsPage.GENERAL -> GeneralSettingsPage(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.PROFILE -> ProfileSettingsPage(
            uiState = uiState,
            viewModel = viewModel,
            onLogout = onLogout,
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.PRIVACY -> PrivacyControlsPage(
            uiState = uiState,
            viewModel = viewModel,
            onNavigate = { currentPage = it },
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.NOTIFICATIONS -> NotificationSettingsPage(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.SECURITY -> SecuritySettingsPage(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.DATA_STORAGE -> DataStoragePage(
            viewModel = viewModel,
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.HELP -> HelpSupportPage(
            onNavigate = { currentPage = it },
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.ABOUT -> AboutPage(
            onNavigate = { currentPage = it },
            onBack = { currentPage = SettingsPage.MAIN },
            modifier = modifier
        )
        SettingsPage.BLOCKED_APPS -> BlockedAppsPage(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { currentPage = SettingsPage.PRIVACY },
            modifier = modifier
        )
        SettingsPage.ALERTS_HISTORY -> AlertsHistoryPage(
            uiState = uiState,
            onBack = { currentPage = SettingsPage.PRIVACY },
            modifier = modifier
        )
        SettingsPage.PRIVACY_POLICY -> PrivacyPolicyPage(
            onBack = { currentPage = SettingsPage.ABOUT },
            modifier = modifier
        )
        SettingsPage.TERMS_CONDITIONS -> TermsConditionsPage(
            onBack = { currentPage = SettingsPage.ABOUT },
            modifier = modifier
        )
        SettingsPage.USER_GUIDE -> UserGuidePage(
            onBack = { currentPage = SettingsPage.HELP },
            modifier = modifier
        )
        SettingsPage.FAQ -> FaqPage(
            onBack = { currentPage = SettingsPage.HELP },
            modifier = modifier
        )
        SettingsPage.REPORT_BUG -> ReportBugPage(
            onBack = { currentPage = SettingsPage.HELP },
            modifier = modifier
        )
        SettingsPage.CONTACT_SUPPORT -> ContactSupportPage(
            onBack = { currentPage = SettingsPage.HELP },
            modifier = modifier
        )
    }
}

// ============================================================
// MAIN SETTINGS HUB
// ============================================================

@Composable
private fun SettingsMainPage(
    onNavigate: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Manage your Sentinel preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsCategoryCard(
            icon = Icons.Default.Tune,
            title = "General",
            subtitle = "Live Awareness, VPN, Theme, Auto-start",
            accentColor = Color(0xFF6C63FF),
            onClick = { onNavigate(SettingsPage.GENERAL) }
        )
        SettingsCategoryCard(
            icon = Icons.Default.Person,
            title = "Profile",
            subtitle = "Account info, edit details, logout",
            accentColor = Color(0xFF00BFA5),
            onClick = { onNavigate(SettingsPage.PROFILE) }
        )
        SettingsCategoryCard(
            icon = Icons.Default.Shield,
            title = "Privacy Controls",
            subtitle = "Blocked apps, monitored permissions, reset data",
            accentColor = Color(0xFFFF6B6B),
            onClick = { onNavigate(SettingsPage.PRIVACY) }
        )
        SettingsCategoryCard(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Nudges, silent mode, alert priority",
            accentColor = Color(0xFFFFA726),
            onClick = { onNavigate(SettingsPage.NOTIFICATIONS) }
        )
        SettingsCategoryCard(
            icon = Icons.Default.Lock,
            title = "Security",
            subtitle = "App lock, encrypted storage, clear logs",
            accentColor = Color(0xFFAB47BC),
            onClick = { onNavigate(SettingsPage.SECURITY) }
        )
        SettingsCategoryCard(
            icon = Icons.Default.Storage,
            title = "Data & Storage",
            subtitle = "Cache, history, storage stats",
            accentColor = Color(0xFF42A5F5),
            onClick = { onNavigate(SettingsPage.DATA_STORAGE) }
        )
        SettingsCategoryCard(
            icon = Icons.Default.HelpOutline,
            title = "Help & Support",
            subtitle = "Guide, FAQ, report a bug",
            accentColor = Color(0xFF66BB6A),
            onClick = { onNavigate(SettingsPage.HELP) }
        )
        SettingsCategoryCard(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "Version, developer, privacy policy",
            accentColor = Color(0xFF78909C),
            onClick = { onNavigate(SettingsPage.ABOUT) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// ============================================================
// SUBSECTION HEADER COMPOSABLE
// ============================================================

@Composable
private fun SubsectionHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ============================================================
// SETTINGS TOGGLE ROW
// ============================================================

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        )
    }
}

// ============================================================
// 1. GENERAL SETTINGS
// ============================================================

@Composable
private fun GeneralSettingsPage(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("General", onBack)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    title = "Live Awareness",
                    subtitle = "Real-time permission monitoring",
                    icon = Icons.Default.Visibility,
                    checked = uiState.liveAwarenessEnabled,
                    onCheckedChange = { viewModel.toggleLiveAwareness(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "VPN Monitoring",
                    subtitle = "Monitor app network traffic",
                    icon = Icons.Default.VpnKey,
                    checked = uiState.vpnEnabled,
                    onCheckedChange = { viewModel.toggleVpn(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Notification Sound",
                    subtitle = "Play sound for privacy alerts",
                    icon = Icons.Default.VolumeUp,
                    checked = uiState.notificationSound,
                    onCheckedChange = { viewModel.toggleNotificationSound(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Vibration",
                    subtitle = "Vibrate on privacy alerts",
                    icon = Icons.Default.Vibration,
                    checked = uiState.notificationVibration,
                    onCheckedChange = { viewModel.toggleNotificationVibration(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Dark Mode",
                    subtitle = "Use dark color scheme",
                    icon = Icons.Default.DarkMode,
                    checked = uiState.darkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Auto-start on Boot",
                    subtitle = "Launch Sentinel when device starts",
                    icon = Icons.Default.PowerSettingsNew,
                    checked = uiState.autoStart,
                    onCheckedChange = { viewModel.toggleAutoStart(it) }
                )
            }
        }
    }
}

// ============================================================
// 2. PROFILE MANAGEMENT
// ============================================================

@Composable
private fun ProfileSettingsPage(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Profile", onBack)

        // Avatar
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.onEditProfilePicUriChange(it.toString()) }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    .clickable(enabled = uiState.isEditingProfile) {
                        imagePickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                val currentPicUri = if (uiState.isEditingProfile) uiState.editProfilePicUri else uiState.userProfile.profilePicUri
                if (currentPicUri.isNotEmpty()) {
                    var bitmap by remember(currentPicUri) { mutableStateOf<ImageBitmap?>(null) }
                    val context = LocalContext.current
                    
                    LaunchedEffect(currentPicUri) {
                        try {
                            val uri = Uri.parse(currentPicUri)
                            val inputStream = context.contentResolver.openInputStream(uri)
                            val decoded = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()
                            bitmap = decoded?.asImageBitmap()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.developer_portrait),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.developer_portrait),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp) // Fill the container
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                
                if (uiState.isEditingProfile) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.isEditingProfile) {
                    OutlinedTextField(
                        value = uiState.editName,
                        onValueChange = { viewModel.onEditNameChange(it) },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.editDob,
                        onValueChange = { viewModel.onEditDobChange(it) },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.editPhone,
                        onValueChange = { viewModel.onEditPhoneChange(it) },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.editGmail,
                        onValueChange = { viewModel.onEditGmailChange(it) },
                        label = { Text("Gmail Address") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.setShowPasswordChangeDialog(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Password")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cancelEditProfile() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = { viewModel.saveProfile() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Save") }
                    }
                } else {
                    ProfileInfoRow("Full Name", uiState.userProfile.name, Icons.Default.Person)
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoRow("Date of Birth", formatDob(uiState.userProfile.dob), Icons.Default.CalendarToday)
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoRow("Phone", uiState.userProfile.phone, Icons.Default.Phone)
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    ProfileInfoRow("Gmail", uiState.userProfile.gmail, Icons.Default.Email)

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.startEditingProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = { viewModel.logout(onLogout) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE53935)
            )
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out", fontWeight = FontWeight.Bold)
        }
    }

    if (uiState.showPasswordChangeDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { viewModel.setShowPasswordChangeDialog(false) },
            title = { Text("Change Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMsg = null },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword.isBlank()) {
                            errorMsg = "Password cannot be empty"
                        } else if (newPassword != confirmPassword) {
                            errorMsg = "Passwords do not match"
                        } else {
                            viewModel.updatePassword(newPassword)
                        }
                    }
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowPasswordChangeDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifEmpty { "Not set" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Formats DOB from raw string to dd/MM/yyyy display.
 */
private fun formatDob(dob: String): String {
    if (dob.isBlank()) return ""
    val digitsOnly = dob.replace("/", "").replace("-", "")
    return when {
        digitsOnly.length == 8 -> "${digitsOnly.substring(0, 2)}/${digitsOnly.substring(2, 4)}/${digitsOnly.substring(4, 8)}"
        dob.contains("/") || dob.contains("-") -> dob
        else -> dob
    }
}

// ============================================================
// 3. PRIVACY CONTROLS
// ============================================================

@Composable
private fun PrivacyControlsPage(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onNavigate: (SettingsPage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Privacy Controls", onBack)

        Text(
            text = "Permission Monitoring",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    title = "Camera",
                    subtitle = "Monitor camera access",
                    icon = Icons.Default.CameraAlt,
                    checked = uiState.monitorCamera,
                    onCheckedChange = { viewModel.toggleMonitorCamera(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Location",
                    subtitle = "Monitor location access",
                    icon = Icons.Default.LocationOn,
                    checked = uiState.monitorLocation,
                    onCheckedChange = { viewModel.toggleMonitorLocation(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Microphone",
                    subtitle = "Monitor microphone access",
                    icon = Icons.Default.Mic,
                    checked = uiState.monitorMicrophone,
                    onCheckedChange = { viewModel.toggleMonitorMicrophone(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Contacts",
                    subtitle = "Monitor contacts access",
                    icon = Icons.Default.Contacts,
                    checked = uiState.monitorContacts,
                    onCheckedChange = { viewModel.toggleMonitorContacts(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Storage",
                    subtitle = "Monitor storage access",
                    icon = Icons.Default.Folder,
                    checked = uiState.monitorStorage,
                    onCheckedChange = { viewModel.toggleMonitorStorage(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(SettingsPage.BLOCKED_APPS) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF6B6B))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage Blocked Apps", fontWeight = FontWeight.Medium)
                        Text("View apps blocked via VPN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(SettingsPage.ALERTS_HISTORY) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFFFFA726))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Privacy Alerts History", fontWeight = FontWeight.Medium)
                        Text("View past privacy notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// ============================================================
// 4. NOTIFICATION SETTINGS
// ============================================================

@Composable
private fun NotificationSettingsPage(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Notifications", onBack)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    title = "Privacy Nudges",
                    subtitle = "Receive privacy alert notifications",
                    icon = Icons.Default.Notifications,
                    checked = uiState.nudgesEnabled,
                    onCheckedChange = { viewModel.toggleNudges(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Silent Mode",
                    subtitle = "Deliver notifications silently",
                    icon = Icons.Default.NotificationsOff,
                    checked = uiState.silentMode,
                    onCheckedChange = { viewModel.toggleSilentMode(it) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "High-Risk Priority",
                    subtitle = "Prioritize alerts for dangerous apps",
                    icon = Icons.Default.Warning,
                    checked = uiState.highRiskAlerts,
                    onCheckedChange = { viewModel.toggleHighRiskAlerts(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Alert Frequency",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                listOf("low" to "Low", "medium" to "Medium", "high" to "High").forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAlertFrequency(value) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.alertFrequency == value,
                            onClick = { viewModel.setAlertFrequency(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// ============================================================
// 5. SECURITY SETTINGS
// ============================================================

@Composable
private fun SecuritySettingsPage(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Security", onBack)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsToggleRow(
                    title = "App Lock",
                    subtitle = if (uiState.appLockPin.isNotEmpty()) 
                        "Require ${uiState.appLockType.uppercase()} to open Sentinel" 
                    else 
                        "Set up PIN or password protection",
                    icon = Icons.Default.Fingerprint,
                    checked = uiState.appLockEnabled,
                    onCheckedChange = { viewModel.toggleAppLock(it) }
                )
                
                // Show setup button if app lock is enabled but no PIN is set
                if (uiState.appLockEnabled && uiState.appLockPin.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setShowAppLockSetupDialog(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change ${uiState.appLockType.uppercase()}", fontSize = 12.sp)
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsToggleRow(
                    title = "Encrypted Storage",
                    subtitle = "Encrypt locally stored data",
                    icon = Icons.Default.EnhancedEncryption,
                    checked = uiState.encryptedStorage,
                    onCheckedChange = { viewModel.toggleEncryptedStorage(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearLogs() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFFF6B6B))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Local Logs", fontWeight = FontWeight.Medium)
                        Text("Remove all stored log data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // App Lock Setup Dialog
    if (uiState.showAppLockSetupDialog) {
        AppLockSetupDialog(
            currentPin = uiState.appLockPin,
            currentType = uiState.appLockType,
            onDismiss = { viewModel.setShowAppLockSetupDialog(false) },
            onSetup = { pin, type -> viewModel.setupAppLock(pin, type) }
        )
    }
}

// ============================================================
// 6. DATA & STORAGE
// ============================================================

@Composable
private fun DataStoragePage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Data & Storage", onBack)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ActionRow(
                    title = "Clear Cache",
                    subtitle = "Free up temporary storage",
                    icon = Icons.Default.Refresh,
                    iconTint = Color(0xFF42A5F5),
                    onClick = { viewModel.clearCache() }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                ActionRow(
                    title = "Delete Analysis History",
                    subtitle = "Remove all privacy analysis records",
                    icon = Icons.Default.DeleteForever,
                    iconTint = Color(0xFFFF6B6B),
                    onClick = { viewModel.clearLogs() }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Storage Usage",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StorageStatRow("App Size", "~12 MB")
                StorageStatRow("Cache", "~2 MB")
                StorageStatRow("User Data", "~1 MB")
                StorageStatRow("Total", "~15 MB")
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StorageStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================================
// 7. HELP & SUPPORT
// ============================================================

@Composable
private fun HelpSupportPage(
    onNavigate: (SettingsPage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Help & Support", onBack)

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                HelpItem(
                    title = "User Guide",
                    subtitle = "Learn how to use Sentinel features",
                    icon = Icons.Default.MenuBook,
                    onClick = { onNavigate(SettingsPage.USER_GUIDE) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                HelpItem(
                    title = "FAQ",
                    subtitle = "Frequently asked questions",
                    icon = Icons.Default.QuestionAnswer,
                    onClick = { onNavigate(SettingsPage.FAQ) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                HelpItem(
                    title = "Report a Bug",
                    subtitle = "Help us improve Sentinel",
                    icon = Icons.Default.BugReport,
                    onClick = { onNavigate(SettingsPage.REPORT_BUG) }
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                HelpItem(
                    title = "Contact Support",
                    subtitle = "Get in touch with the development team",
                    icon = Icons.Default.SupportAgent,
                    onClick = { onNavigate(SettingsPage.CONTACT_SUPPORT) }
                )
            }
        }
    }
}

@Composable
private fun HelpItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

// ============================================================
// 8. ABOUT APPLICATION
// ============================================================

@Composable
private fun AboutPage(
    onNavigate: (SettingsPage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("About Sentinel", onBack)

        // App Logo & Name
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sentinel_logo),
                        contentDescription = "Sentinel Logo",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SENTINEL",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Sentinel is a privacy-focused Android application designed to provide transparency into how other apps on your device use their permissions and communicate with the network.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AboutInfoRow("Developer", "Utsab Das")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                AboutInfoRow("Platform", "Android (Kotlin)")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                AboutInfoRow("Architecture", "MVVM + Jetpack Compose")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                AboutInfoRow("Core Tech", "VPN Monitoring, PRS Algorithm")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                AboutInfoRow("SDK", "Android SDK 34")
            }
        }


        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(SettingsPage.PRIVACY_POLICY) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Privacy Policy", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(SettingsPage.TERMS_CONDITIONS) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Terms & Conditions", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ============================================================
// 9. BLOCKED APPS MANAGEMENT
// ============================================================

@Composable
private fun BlockedAppsPage(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Blocked Apps", onBack)

        Text(
            text = "Prevent selected applications from accessing the internet. Toggle the switch to restrict network traffic for specific apps while Sentinel is active.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (uiState.installedApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.installedApps) { app ->
                    val isBlocked = uiState.blockedPackages.contains(app.packageName)
                    
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isBlocked) 
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            else 
                                MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        border = if (isBlocked) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App Icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.icon != null) {
                                    AndroidView(
                                        factory = { context ->
                                            ImageView(context).apply {
                                                scaleType = ImageView.ScaleType.FIT_CENTER
                                                setImageDrawable(app.icon)
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isBlocked) "Access Blocked" else "Access Allowed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            Switch(
                                checked = isBlocked,
                                onCheckedChange = { viewModel.togglePackageBlock(app.packageName) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.error,
                                    checkedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 10. PRIVACY ALERTS HISTORY
// ============================================================

@Composable
private fun AlertsHistoryPage(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Alerts History", onBack)

        if (uiState.nudgeEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.HistoryToggleOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No privacy alerts logged yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.nudgeEvents) { event ->
                    AlertHistoryItem(event)
                }
            }
        }
    }
}

@Composable
private fun AlertHistoryItem(event: NudgeEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatEventTime(event.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = event.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (event.domain.isNotEmpty()) {
                    Text(
                        text = "Domain: ${event.domain}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatEventTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        else -> {
            val date = java.util.Date(timestamp)
            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(date)
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ============================================================
// 11. PRIVACY POLICY
// ============================================================

@Composable
private fun PrivacyPolicyPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Privacy Policy", onBack)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Last Updated: March 2024",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PolicySection(
                title = "1. Information We Collect",
                content = "Sentinel monitors your device's network traffic and permission usage to provide real-time privacy insights. We do not transmit this data to external servers; all analysis happens locally on your device."
            )
            
            PolicySection(
                title = "2. Local Processing",
                content = "Your data usage stats, app permissions, and network logs are stored securely in local app storage and cleared when requested or when the app is uninstalled."
            )
            
            PolicySection(
                title = "3. Security",
                content = "Sentinel uses a local VPN service to monitor DNS queries. This VPN is entirely local and does not route your traffic through any remote servers."
            )
            
            PolicySection(
                title = "4. Data Sharing",
                content = "We do not sell, trade, or otherwise transfer your personal information to outside parties. Your privacy is our core mission."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================================
// 12. TERMS & CONDITIONS
// ============================================================

@Composable
private fun TermsConditionsPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Terms & Conditions", onBack)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Effective Date: March 2024",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PolicySection(
                title = "1. Acceptance of Terms",
                content = "By using Sentinel, you agree to these terms. Sentinel is provided 'as is' without warranties of any kind."
            )
            
            PolicySection(
                title = "2. Purpose",
                content = "Sentinel is designed to provide privacy awareness. It is not a replacement for professional security software or standard system protections."
            )
            
            PolicySection(
                title = "3. VPN Usage",
                content = "The local VPN service is used solely for on-device traffic monitoring. You must grant the necessary system permissions for the app to function correctly."
            )
            
            PolicySection(
                title = "4. Modifications",
                content = "We reserve the right to modify these terms at any time. Your continued use of the app signifies acceptance of updated terms."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


// ============================================================
// 13. USER GUIDE
// ============================================================

@Composable
private fun UserGuidePage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("User Guide", onBack)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            GuideSection(
                title = "Getting Started",
                content = "To start protecting your privacy, navigate to the Dashboard and ensure the Sentinel service is active. Sentinel will immediately begin monitoring app permissions and network requests."
            )
            
            GuideSection(
                title = "Managing Blocked Apps",
                content = "You can prevent specific apps from accessing the internet. Go to Settings > Privacy Controls > Manage Blocked Apps and toggle the switch for any application you wish to restrict."
            )
            
            GuideSection(
                title = "Understanding Privacy Nudges",
                content = "When an app contacts a known tracker or accesses sensitive data unexpectedly, Sentinel sends a 'Nudge'. Tap on these notifications to see detailed information about the event."
            )
            
            GuideSection(
                title = "Interpreting PRS Scores",
                content = "The Privacy Risk Score (PRS) is a dynamic metric ranging from 0 to 1. Higher scores indicate apps with more invasive permission sets or higher network activity."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================================
// 14. FAQ
// ============================================================

@Composable
private fun FaqPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("FAQ", onBack)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            FaqItem(
                question = "Does Sentinel slow down my phone?",
                answer = "Sentinel is optimized for minimal resource usage. The local VPN service has negligible impact on battery and device performance."
            )
            
            FaqItem(
                question = "Is my data uploaded to the cloud?",
                answer = "No. Sentinel is designed with a 'Privacy-First' approach. All analysis and monitoring happen strictly on your device."
            )
            
            FaqItem(
                question = "Why does Sentinel need a VPN?",
                answer = "The local VPN permission is required to monitor DNS queries made by other apps. No traffic ever leaves your device through this service."
            )
            
            FaqItem(
                question = "How are apps classified as 'Unreliable'?",
                answer = "Apps are classified based on their Privacy Risk Score (PRS), which factors in granted sensitive permissions and connections to known tracking domains."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================================
// 15. REPORT A BUG
// ============================================================

@Composable
private fun ReportBugPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Report a Bug", onBack)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Found an issue?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Help us make Sentinel better by reporting bugs. Please include steps to reproduce the issue and your device model.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { /* TODO: Implement email intent */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Email Bug Report")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "support@sentinel-app.dev",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ============================================================
// 16. CONTACT SUPPORT
// ============================================================

@Composable
private fun ContactSupportPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SubsectionHeader("Contact Support", onBack)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "How can we help?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SupportChannelRow(
                        title = "Email Support",
                        value = "support@sentinel-app.dev",
                        icon = Icons.Default.Email
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                    SupportChannelRow(
                        title = "Developer Site",
                        value = "www.sentinel-app.dev",
                        icon = Icons.Default.Language
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Typical response time: 24-48 hours",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GuideSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = answer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SupportChannelRow(title: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
// ============================================================
// APP LOCK SETUP DIALOG
// ============================================================

@Composable
private fun AppLockSetupDialog(
    currentPin: String,
    currentType: String,
    onDismiss: () -> Unit,
    onSetup: (String, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType.ifEmpty { "pin" }) }
    var pinValue by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val isEditing = currentPin.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (isEditing) "Change App Lock" else "Set Up App Lock") 
        },
        text = {
            Column {
                // Type Selection
                Text(
                    text = "Choose protection type:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedType = "pin"; errorMsg = null },
                        label = { Text("PIN") },
                        selected = selectedType == "pin",
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        onClick = { selectedType = "password"; errorMsg = null },
                        label = { Text("Password") },
                        selected = selectedType == "password",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // PIN/Password Input
                OutlinedTextField(
                    value = pinValue,
                    onValueChange = { 
                        if (selectedType == "pin") {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                pinValue = it
                                errorMsg = null
                            }
                        } else {
                            pinValue = it
                            errorMsg = null
                        }
                    },
                    label = { Text(if (selectedType == "pin") "Enter 4-6 digit PIN" else "Enter Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = if (selectedType == "pin") 
                        KeyboardOptions(keyboardType = KeyboardType.NumberPassword) 
                    else 
                        KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = if (selectedType == "pin") {
                        { Text("4-6 digits only") }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Confirm PIN/Password
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { 
                        confirmPin = it
                        errorMsg = null
                    },
                    label = { Text("Confirm ${if (selectedType == "pin") "PIN" else "Password"}") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = if (selectedType == "pin") 
                        KeyboardOptions(keyboardType = KeyboardType.NumberPassword) 
                    else 
                        KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pinValue.isBlank() -> {
                            errorMsg = "${if (selectedType == "pin") "PIN" else "Password"} cannot be empty"
                        }
                        selectedType == "pin" && pinValue.length < 4 -> {
                            errorMsg = "PIN must be at least 4 digits"
                        }
                        selectedType == "password" && pinValue.length < 4 -> {
                            errorMsg = "Password must be at least 4 characters"
                        }
                        pinValue != confirmPin -> {
                            errorMsg = "${if (selectedType == "pin") "PINs" else "Passwords"} do not match"
                        }
                        else -> {
                            onSetup(pinValue, selectedType)
                        }
                    }
                }
            ) { 
                Text(if (isEditing) "Update" else "Set Up") 
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}