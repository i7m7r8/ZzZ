package com.zzz.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zzz.vpn.model.ConnectionMode
import com.zzz.vpn.viewmodel.MainViewModel

// ─── Shared Components ────────────────────────────────────────────────────────

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
fun PortField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

// ─── I2P Screen ───────────────────────────────────────────────────────────────

@Composable
fun I2pScreen(viewModel: MainViewModel) {
    val config by viewModel.config.collectAsState()
    val i2pState by viewModel.i2pState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Hub, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("I2P Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(8.dp))
        Text("Status: ${i2pState.name}", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(24.dp))

        SettingsCard {
            ToggleRow("Enable I2P", config.i2pEnabled) {
                viewModel.updateConfig(config.copy(i2pEnabled = it))
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                Text("Ports", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                PortField("HTTP Proxy Port", config.i2pHttpPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(i2pHttpPort = p)) }
                }
                Spacer(Modifier.height(8.dp))
                PortField("HTTPS Proxy Port", config.i2pHttpsPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(i2pHttpsPort = p)) }
                }
                Spacer(Modifier.height(8.dp))
                PortField("SOCKS Port", config.i2pSocksPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(i2pSocksPort = p)) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(Modifier.padding(16.dp)) {
                Icon(Icons.Filled.Info, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "I2P requires ~5 min to build tunnels on first start. " +
                    "The i2pd binary is bundled in the native libs.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── DNS Screen ───────────────────────────────────────────────────────────────

@Composable
fun DnsScreen(viewModel: MainViewModel) {
    val config by viewModel.config.collectAsState()
    val dnsState by viewModel.dnsState.collectAsState()

    val servers = listOf("cloudflare", "cloudflare-security", "google", "quad9-dnscrypt-ip4-filter-pri", "adguard-dns")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Dns, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("DNSCrypt Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(8.dp))
        Text("Status: ${dnsState.name}", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(24.dp))

        SettingsCard {
            ToggleRow("Enable DNSCrypt", config.dnsCryptEnabled) {
                viewModel.updateConfig(config.copy(dnsCryptEnabled = it))
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                Text("Server", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                servers.forEach { server ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = config.dnsCryptServer == server,
                            onClick = { viewModel.updateConfig(config.copy(dnsCryptServer = server)) }
                        )
                        Text(server, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                PortField("Listen Port", config.dnsCryptPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(dnsCryptPort = p)) }
                }
            }
        }
    }
}

// ─── Settings Screen ──────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val config by viewModel.config.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Settings, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(24.dp))

        // SNI config
        SettingsCard {
            Column {
                Text("SNI Spoofing", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                ToggleRow("Enable SNI Spoof", config.sniEnabled) {
                    viewModel.updateConfig(config.copy(sniEnabled = it))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = config.sniHost,
                    onValueChange = { viewModel.updateConfig(config.copy(sniHost = it)) },
                    label = { Text("SNI Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("www.google.com") }
                )
                Spacer(Modifier.height(8.dp))
                PortField("SNI Port", config.sniPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(sniPort = p)) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Connection mode
        SettingsCard {
            Column {
                Text("Connection Mode", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                ConnectionMode.entries.forEach { mode ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = config.connectionMode == mode,
                            onClick = { viewModel.updateConfig(config.copy(connectionMode = mode)) }
                        )
                        Text(
                            mode.name.replace('_', ' → ').replace("→ ", "→ "),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                Text("VPN", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                ToggleRow("Kill Switch", config.killSwitch) {
                    viewModel.updateConfig(config.copy(killSwitch = it))
                }
                ToggleRow("Bypass LAN", config.bypassLan) {
                    viewModel.updateConfig(config.copy(bypassLan = it))
                }
                ToggleRow("Auto-Start on Boot", config.autoStart) {
                    viewModel.updateConfig(config.copy(autoStart = it))
                }
                Spacer(Modifier.height(8.dp))
                PortField("MTU", config.vpnMtu.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(vpnMtu = p)) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                ToggleRow("Split Tunneling", config.splitTunnelingEnabled) {
                    viewModel.updateConfig(config.copy(splitTunnelingEnabled = it))
                }
                if (config.splitTunnelingEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Split tunneling: configure excluded apps in the app list below.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
