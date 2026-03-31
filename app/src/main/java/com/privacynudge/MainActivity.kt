package com.privacynudge

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
// VpnState import removed — not used directly in MainActivity
import com.privacynudge.notification.NotificationHelper
import com.privacynudge.ui.*
import com.privacynudge.ui.theme.PrivacyNudgeTheme
import com.privacynudge.vpn.PrivacyNudgeVpnService
import kotlinx.coroutines.launch

/**
 * Navigation destinations for the app.
 */
enum class NavDestination(val label: String) {
    LOGIN("Login"),
    SIGNUP("Signup"),
    DASHBOARD("Dashboard"),
    ALL_APPS("Apps"),
    APP_INSIGHTS("Insights"),
    PERMISSIONS("Privacy"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private var dashboardViewModel: DashboardViewModel? = null
    private var isAppInBackground = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this, 
                "Notifications disabled - nudges won't be visible", 
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val vpnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService()
        }
    }

    private var pendingVpnPackage: String? = null
    private var pendingVpnAppName: String? = null

    fun prepareAndStartVpn(packageName: String?, appName: String?) {
        pendingVpnPackage = packageName
        pendingVpnAppName = appName
        
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, PrivacyNudgeVpnService::class.java).apply {
            action = PrivacyNudgeVpnService.ACTION_START
            pendingVpnPackage?.let { putExtra(PrivacyNudgeVpnService.EXTRA_PACKAGE_NAME, it) }
            pendingVpnAppName?.let { putExtra(PrivacyNudgeVpnService.EXTRA_APP_NAME, it) }
        }
        startService(intent)
    }

    fun stopVpnService() {
        val intent = Intent(this, PrivacyNudgeVpnService::class.java).apply {
            action = PrivacyNudgeVpnService.ACTION_STOP
        }
        startService(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Debug logging to confirm onCreate is reached (visible in logcat)
        Log.d("SentinelLaunch", "MainActivity.onCreate started")

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Initialize notification channels — wrapped in try-catch to prevent
        // crashes if channel creation fails on certain devices
        try {
            NotificationHelper(this)
        } catch (e: Exception) {
            Log.e("SentinelLaunch", "Failed to initialize notification channels", e)
        }

        enableEdgeToEdge()
        setContent {
            val settingsRepository = remember { com.privacynudge.data.SettingsRepository(this) }
            val darkTheme by settingsRepository.darkMode.collectAsState(initial = true)
            
            PrivacyNudgeTheme(darkTheme = darkTheme) {
                MainApp(
                    setDashboardViewModel = { dashboardViewModel = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel?.refresh()
        isAppInBackground = false
    }

    override fun onPause() {
        super.onPause()
        isAppInBackground = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    setDashboardViewModel: (DashboardViewModel) -> Unit
) {
    val context = LocalContext.current
    
    // Auth
    val authRepository = remember { com.privacynudge.data.AuthRepository(context) }
    val authViewModel: AuthViewModel = viewModel()
    val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = null)
    
    // App Lock
    val settingsRepository = remember { com.privacynudge.data.SettingsRepository(context) }
    val appLockEnabled by settingsRepository.appLockEnabled.collectAsState(initial = false)
    val appLockPin by settingsRepository.appLockPin.collectAsState(initial = "")
    val appLockType by settingsRepository.appLockType.collectAsState(initial = "pin")
    var isAppUnlocked by remember { mutableStateOf(false) }
    
    // Reset app unlock when app lock settings change
    LaunchedEffect(appLockEnabled, appLockPin) {
        if (!appLockEnabled || appLockPin.isEmpty()) {
            isAppUnlocked = true
        } else {
            isAppUnlocked = false
        }
    }
    
    var currentDestination by remember { mutableStateOf(NavDestination.LOGIN) }
    
    // Auto-navigate based on Auth State
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated == true) {
            if (currentDestination == NavDestination.LOGIN || currentDestination == NavDestination.SIGNUP) {
                currentDestination = NavDestination.DASHBOARD
            }
        } else if (isAuthenticated == false) {
            currentDestination = NavDestination.LOGIN
            isAppUnlocked = false // Reset app unlock when user logs out
        }
    }
    
    // ViewModels
    val dashboardViewModel: DashboardViewModel = viewModel()
    val allAppsViewModel: AllAppsViewModel = viewModel()
    val permissionsViewModel: PermissionsViewModel = viewModel()
    
    // Pass dashboard viewmodel to activity
    LaunchedEffect(dashboardViewModel) {
        setDashboardViewModel(dashboardViewModel)
    }

    // Show app lock screen if enabled, user is authenticated, and app is not unlocked
    if (isAuthenticated == true && appLockEnabled && appLockPin.isNotEmpty() && !isAppUnlocked) {
        AppLockScreen(
            lockType = appLockType,
            storedPin = appLockPin,
            onUnlocked = { isAppUnlocked = true }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination != NavDestination.LOGIN && currentDestination != NavDestination.SIGNUP) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination == NavDestination.DASHBOARD,
                        onClick = { currentDestination = NavDestination.DASHBOARD },
                        icon = { 
                            Icon(
                                imageVector = if (currentDestination == NavDestination.DASHBOARD) 
                                    Icons.Filled.Shield else Icons.Outlined.Shield,
                                contentDescription = NavDestination.DASHBOARD.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = NavDestination.DASHBOARD.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        alwaysShowLabel = true
                    )
                    NavigationBarItem(
                        selected = currentDestination == NavDestination.ALL_APPS,
                        onClick = { currentDestination = NavDestination.ALL_APPS },
                        icon = { 
                            Icon(
                                imageVector = if (currentDestination == NavDestination.ALL_APPS) 
                                    Icons.Filled.Apps else Icons.Outlined.Apps,
                                contentDescription = NavDestination.ALL_APPS.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = NavDestination.ALL_APPS.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        alwaysShowLabel = true
                    )
                    NavigationBarItem(
                        selected = currentDestination == NavDestination.APP_INSIGHTS,
                        onClick = { currentDestination = NavDestination.APP_INSIGHTS },
                        icon = { 
                            Icon(
                                imageVector = if (currentDestination == NavDestination.APP_INSIGHTS) 
                                    Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                contentDescription = NavDestination.APP_INSIGHTS.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = NavDestination.APP_INSIGHTS.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        alwaysShowLabel = true
                    )
                    NavigationBarItem(
                        selected = currentDestination == NavDestination.PERMISSIONS,
                        onClick = { currentDestination = NavDestination.PERMISSIONS },
                        icon = { 
                            Icon(
                                imageVector = if (currentDestination == NavDestination.PERMISSIONS) 
                                    Icons.Filled.Lock else Icons.Outlined.Lock,
                                contentDescription = NavDestination.PERMISSIONS.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = NavDestination.PERMISSIONS.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        alwaysShowLabel = true
                    )
                    NavigationBarItem(
                        selected = currentDestination == NavDestination.SETTINGS,
                        onClick = { currentDestination = NavDestination.SETTINGS },
                        icon = { 
                            Icon(
                                imageVector = if (currentDestination == NavDestination.SETTINGS) 
                                    Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = NavDestination.SETTINGS.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { 
                            Text(
                                text = NavDestination.SETTINGS.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentDestination) {
            NavDestination.LOGIN -> {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToSignup = {
                        authViewModel.clearError()
                        currentDestination = NavDestination.SIGNUP
                    },
                    onLoginSuccess = { currentDestination = NavDestination.DASHBOARD }
                )
            }
            NavDestination.SIGNUP -> {
                SignupScreen(
                    viewModel = authViewModel,
                    onNavigateBack = {
                        authViewModel.clearError()
                        currentDestination = NavDestination.LOGIN
                    },
                    onSignupSuccess = { currentDestination = NavDestination.DASHBOARD }
                )
            }
            NavDestination.DASHBOARD -> {
                DashboardScreen(
                    modifier = Modifier.padding(innerPadding),
                    viewModel = dashboardViewModel,
                    onStartVpn = { pkg, name -> (context as? MainActivity)?.prepareAndStartVpn(pkg, name) },
                    onStopVpn = { (context as? MainActivity)?.stopVpnService() }
                )
            }
            NavDestination.ALL_APPS -> {
                AllAppsScreen(
                    modifier = Modifier.padding(innerPadding),
                    allAppsViewModel = allAppsViewModel
                )
            }
            NavDestination.APP_INSIGHTS -> {
                AppInsightsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onStartVpn = { pkg, name -> (context as? MainActivity)?.prepareAndStartVpn(pkg, name) },
                    onStopVpn = { (context as? MainActivity)?.stopVpnService() }
                )
            }
            NavDestination.PERMISSIONS -> {
                PermissionsScreen(
                    modifier = Modifier.padding(innerPadding),
                    viewModel = permissionsViewModel
                )
            }
            NavDestination.SETTINGS -> {
                SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onLogout = {
                        currentDestination = NavDestination.LOGIN
                    }
                )
            }
        }
    }
}
