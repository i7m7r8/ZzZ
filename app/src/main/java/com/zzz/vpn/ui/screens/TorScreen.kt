package com.zzz.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zzz.vpn.viewmodel.MainViewModel

@Composable
fun TorScreen(viewModel: MainViewModel) {
    val config by viewModel.config.collectAsState()
    val torState by viewModel.torState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Router, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("Tor Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(8.dp))
        Text("Status: ${torState.name}", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(24.dp))

        SettingsCard {
            ToggleRow("Enable Tor", config.torEnabled) {
                viewModel.updateConfig(config.copy(torEnabled = it))
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                Text("Ports", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                PortField("SOCKS Port", config.torSocksPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(torSocksPort = p)) }
                }
                Spacer(Modifier.height(8.dp))
                PortField("Control Port", config.torControlPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(torControlPort = p)) }
                }
                Spacer(Modifier.height(8.dp))
                PortField("DNS Port", config.torDnsPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(torDnsPort = p)) }
                }
                Spacer(Modifier.height(8.dp))
                PortField("Trans Port", config.torTransPort.toString()) {
                    it.toIntOrNull()?.let { p -> viewModel.updateConfig(config.copy(torTransPort = p)) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Column {
                ToggleRow("Use Bridges (obfs4)", config.torUseBridges) {
                    viewModel.updateConfig(config.copy(torUseBridges = it))
                }
                if (config.torUseBridges) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = config.torBridges,
                        onValueChange = { viewModel.updateConfig(config.copy(torBridges = it)) },
                        label = { Text("Bridges (one per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        placeholder = { Text("obfs4 1.2.3.4:1234 cert=... iat-mode=0") }
                    )
                }
            }
        }
    }
}
