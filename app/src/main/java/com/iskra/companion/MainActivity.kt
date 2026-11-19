package com.iskra.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iskra.companion.data.repository.TelemetryRepository
import com.iskra.companion.ui.screens.BatteryCellsScreen
import com.iskra.companion.ui.screens.CarScreen
import com.iskra.companion.ui.screens.ChargingScreen
import com.iskra.companion.ui.screens.DashboardScreen
import com.iskra.companion.ui.screens.DiagnosticsScreen
import com.iskra.companion.ui.screens.SettingsScreen
import com.iskra.companion.ui.screens.VehicleTpmsScreen
import com.iskra.companion.ui.theme.CockpitBg
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.IskraCompanionTheme
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val repository by lazy { TelemetryRepository.getInstance(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkBluetoothPermissions()

        setContent {
            IskraCompanionTheme {
                MainApp(repository = repository)
            }
        }
    }

    private fun checkBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

/** The three main tabs. Everything else is a detail page opened from the Car screen. */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Car : Screen("car", "Car", Icons.Default.DirectionsCar)
    data object Charging : Screen("charging", "Charge", Icons.Default.EvStation)
    data object Settings : Screen("settings", "Setup", Icons.Default.Settings)
}

private object Detail {
    const val POWER = "detail/power"
    const val BATTERY = "detail/battery"
    const val TYRES = "detail/tyres"
    const val DIAGNOSTICS = "detail/diagnostics"
}

private fun NavController.openTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MainApp(repository: TelemetryRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val telemetry by repository.telemetry.collectAsStateWithLifecycle()
    val powerHistory by repository.powerHistory.collectAsStateWithLifecycle()
    val connectionState by repository.bluetoothManager.connectionState.collectAsStateWithLifecycle()
    val discoveredDevices by repository.bluetoothManager.discoveredDevices.collectAsStateWithLifecycle()
    val isDemoMode by repository.isDemoMode.collectAsStateWithLifecycle()
    val selectedVehicleModel by repository.selectedVehicleModel.collectAsStateWithLifecycle()

    val tabs = listOf(Screen.Car, Screen.Charging, Screen.Settings)
    val onDetail = currentRoute?.startsWith("detail/") == true
    val onCarTab = currentRoute == Screen.Car.route || onDetail

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CockpitBg,
        bottomBar = {
            NavigationBar(
                containerColor = CockpitBg,
                tonalElevation = 0.dp
            ) {
                tabs.forEach { screen ->
                    val isSelected = if (screen == Screen.Car) onCarTab else currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = {
                            Text(
                                screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HyundaiBlue,
                            selectedTextColor = HyundaiBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            if (screen == Screen.Car && onDetail) {
                                navController.popBackStack(Screen.Car.route, inclusive = false)
                            } else if (currentRoute != screen.route) {
                                navController.openTab(screen.route)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Car.route,
            modifier = Modifier.padding(innerPadding),
            // Navigation's default is a 700 ms crossfade, which feels sluggish. Keep it snappy.
            enterTransition = { fadeIn(tween(120)) },
            exitTransition = { fadeOut(tween(70)) },
            popEnterTransition = { fadeIn(tween(120)) },
            popExitTransition = { fadeOut(tween(70)) }
        ) {
            composable(Screen.Car.route) {
                CarScreen(
                    telemetry = telemetry,
                    onOpenPower = { navController.navigate(Detail.POWER) { launchSingleTop = true } },
                    onOpenBattery = { navController.navigate(Detail.BATTERY) { launchSingleTop = true } },
                    onOpenTyres = { navController.navigate(Detail.TYRES) { launchSingleTop = true } },
                    onOpenDiagnostics = { navController.navigate(Detail.DIAGNOSTICS) { launchSingleTop = true } },
                    onOpenSetup = { navController.openTab(Screen.Settings.route) }
                )
            }
            composable(Screen.Charging.route) {
                ChargingScreen(telemetry = telemetry)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    connectionState = connectionState,
                    discoveredDevices = discoveredDevices,
                    isDemoMode = isDemoMode,
                    selectedVehicleModel = selectedVehicleModel,
                    onStartScan = { repository.bluetoothManager.startScan() },
                    onConnectDevice = { device -> repository.connectToDevice(device) },
                    onToggleDemoMode = { enabled -> repository.setDemoMode(enabled) },
                    onSelectScenario = { scenario -> repository.setSimulationScenario(scenario) },
                    onSelectVehicleModel = { model -> repository.selectedVehicleModel.value = model }
                )
            }
            composable(Detail.POWER) {
                DetailPage("Power & efficiency", onBack = { navController.popBackStack() }) {
                    DashboardScreen(
                        telemetry = telemetry,
                        powerHistory = powerHistory,
                        onNavigateToSettings = { navController.openTab(Screen.Settings.route) }
                    )
                }
            }
            composable(Detail.BATTERY) {
                DetailPage("Battery cells", onBack = { navController.popBackStack() }) {
                    BatteryCellsScreen(telemetry = telemetry)
                }
            }
            composable(Detail.TYRES) {
                DetailPage("Tyres & climate", onBack = { navController.popBackStack() }) {
                    VehicleTpmsScreen(telemetry = telemetry)
                }
            }
            composable(Detail.DIAGNOSTICS) {
                DetailPage("Diagnostics", onBack = { navController.popBackStack() }) {
                    DiagnosticsScreen(
                        telemetry = telemetry,
                        onExportCsv = { repository.generateCsvExport() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(CockpitBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}
