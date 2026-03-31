package com.privacynudge.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ImageView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.privacynudge.data.*

/**
 * Search bar for filtering apps.
 */
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search apps...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
    )
}


/**
 * Sort dropdown menu.
 */
@Composable
fun SortDropdown(
    currentOption: SortOption,
    onOptionSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(currentOption.displayName, style = MaterialTheme.typography.labelMedium)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (option == currentOption) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

/**
 * List item for an app in the User Apps list.
 */
@Composable
fun AllAppsListItem(
    app: InstalledApp,
    permissionIcons: String,
    updateDateString: String,
    installDateString: String,
    onClick: () -> Unit,
    // Whether this app has PRS < 0.5 (Safe = low privacy risk)
    isSafe: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (app.icon != null) {
                    // Use AndroidView to display the Drawable directly to avoid complex conversion
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
                    Text(
                        text = app.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // PRS Score and Risk Level Tag
                    val riskColor = when (app.prsRiskLevel) {
                        RiskLevel.HIGH -> Color(0xFFF44336)
                        RiskLevel.MEDIUM -> Color(0xFFFF9800)
                        RiskLevel.LOW -> Color(0xFF4CAF50)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = riskColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "PRS: ${"%.2f".format(app.prsScore)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = riskColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = riskColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = when (app.prsRiskLevel) {
                                    RiskLevel.HIGH -> "High"
                                    RiskLevel.MEDIUM -> "Medium"
                                    RiskLevel.LOW -> "Low"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = riskColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        // Metadata: Install and Update dates
                        Text(
                            text = "Installed $installDateString",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Updated $updateDateString",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // ── Safe Button: Informational & Non-destructive ──
                    if (isSafe) {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.08f),
                                contentColor = Color(0xFF4CAF50)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Safe",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Permission icons
                Text(
                    text = permissionIcons,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}




/**
 * Compact badge indicating whether a permission is Prerequisite or Optional.
 *
 * • Prerequisite → deep blue  (core functionality)
 * • Optional     → amber      (extra, may increase privacy risk)
 */
@Composable
fun UsageRoleBadge(
    role: PermissionUsageRole,
    modifier: Modifier = Modifier
) {
    val color = Color(role.colorHex)
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Text(
            text = role.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Small risk badge for list items.
 */

/**
 * Dangerous permissions toggle button.
 */
@Composable
fun DangerousOnlyToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(0xFFF44336)
    OutlinedButton(
        onClick = onToggle,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = if (isEnabled) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = color.copy(alpha = 0.12f),
                contentColor = color
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        border = BorderStroke(
            1.dp,
            if (isEnabled) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Unreliable",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Medium
        )
    }
}





/**
 * Reliable apps toggle button.
 * Shows apps where PRS < 0.5.
 */
@Composable
fun ReliableOnlyToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(0xFF4CAF50)
    OutlinedButton(
        onClick = onToggle,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = if (isEnabled) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = color.copy(alpha = 0.12f),
                contentColor = color
            )
        } else {
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        border = BorderStroke(
            1.dp,
            if (isEnabled) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Reliable",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Medium
        )
    }
}


/**
 * Header for permission groups in the detail sheet.

 * Use red/unreliable and green/reliable accents to distinguish sections.
 */
@Composable
fun PermissionSectionHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Interactive permission row for the app detail sheet.
 *
 * Displays the permission icon, label, category badge, and a Switch that
 * reflects the current grant state. Because Android does not allow apps to
 * revoke/grant permissions programmatically, toggling the switch calls
 * [onToggleRequested] so the parent can show a confirmation dialog and then
 * navigate to the system App Info screen.
 *
 * Color coding:
 *  • DANGEROUS  → Red
 *  • NORMAL     → Green
 *  • SIGNATURE  → Purple
 *  • SPECIAL    → Orange
 */
@Composable
fun PermissionToggleItem(
    permission: DetailedPermission,
    onToggleRequested: (DetailedPermission) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine category accent colour
    val categoryColor = when (permission.category) {
        PermissionCategory.DANGEROUS  -> Color(0xFFF44336)
        PermissionCategory.NORMAL     -> Color(0xFF4CAF50)
        PermissionCategory.SIGNATURE  -> Color(0xFF9C27B0)
        PermissionCategory.SPECIAL    -> Color(0xFFFF9800)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Permission icon circle ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = DangerousPermissions.getIcon(permission.name),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Label + badges + necessity reason ──────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Badges row: grant status + necessity classification.
            // The permission type (Dangerous/Normal) is intentionally omitted here
            // to keep the view minimal and accessible to non-technical users.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Granted / Denied status — colour-coded by category for visual hierarchy
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (permission.isGranted)
                        categoryColor.copy(alpha = 0.12f)
                    else
                        Color.Gray.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = if (permission.isGranted) "GRANTED" else "DENIED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (permission.isGranted) categoryColor else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                // Necessity badge: PREREQUISITE (blue) or OPTIONAL (amber)
                UsageRoleBadge(role = permission.usageRole)
            }
            // Row 2: one-line reason explaining the classification
            if (permission.usageReason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = permission.usageReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Toggle switch ───────────────────────────────────────────────────
        // The switch visually reflects the current state but cannot change
        // permissions directly. Tapping it will trigger a dialog → settings.
        Switch(
            checked = permission.isGranted,
            onCheckedChange = { onToggleRequested(permission) },
            colors = SwitchDefaults.colors(
                checkedTrackColor = categoryColor.copy(alpha = 0.7f),
                checkedThumbColor = categoryColor,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

/**
 * Dialog shown before navigating the user to system permission settings.
 *
 * Android does not allow apps to change permissions silently. This dialog
 * explains why the user is being redirected and gives them the choice to
 * proceed or cancel, ensuring a non-destructive experience.
 *
 * @param permission  The permission the user wants to change.
 * @param packageName The package name of the target app.
 * @param onDismiss   Called when the user cancels.
 */
@Composable
fun PermissionSettingsWarningDialog(
    permission: DetailedPermission,
    packageName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            // Use a warning-style icon to convey the action's importance
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Change Permission",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "To change the \"${permission.label}\" permission, Android requires you to use the system Settings app.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tapping \"Open Settings\" will take you to this app's permission page where you can grant or revoke access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Navigate to the system App Info > Permissions screen
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

