package com.zzz.vpn.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zzz.vpn.ui.screens.*
import com.zzz.vpn.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZzzApp(viewModel: MainViewModel, onRequestVpnPermission: () -> Unit) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()

    val items = listOf(
        NavItem("home", Icons.Filled.Shield, "VPN"),
        NavItem("tor", Icons.Filled.Router, "Tor"),
        NavItem("i2p", Icons.Filled.Hub, "I2P"),
        NavItem("dns", Icons.Filled.Dns, "DNS"),
        NavItem("settings", Icons.Filled.Settings, "Settings")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val route = currentRoute?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(viewModel = viewModel, onRequestVpnPermission = onRequestVpnPermission)
            }
            composable("tor") { TorScreen(viewModel = viewModel) }
            composable("i2p") { I2pScreen(viewModel = viewModel) }
            composable("dns") { DnsScreen(viewModel = viewModel) }
            composable("settings") { SettingsScreen(viewModel = viewModel) }
        }
    }
}

data class NavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
