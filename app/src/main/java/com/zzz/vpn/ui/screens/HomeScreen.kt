package com.zzz.vpn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zzz.vpn.model.ConnectionMode
import com.zzz.vpn.model.VpnState
import com.zzz.vpn.viewmodel.MainViewModel

@Composable
fun HomeScreen(viewModel: MainViewModel, onRequestVpnPermission: () -> Unit) {
    val vpnState by viewModel.vpnState.collectAsState()
    val torState by viewModel.torState.collectAsState()
    val i2pState by viewModel.i2pState.collectAsState()
    val dnsState by viewModel.dnsState.collectAsState()
    val config by viewModel.config.collectAsState()

    val isRunning = vpnState == VpnState.RUNNING
    val isStarting = vpnState == VpnState.STARTING

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isStarting) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "ZzZ VPN",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            when (config.connectionMode) {
                ConnectionMode.SNI_TOR_DNS -> "SNI → Tor → DNSCrypt"
                ConnectionMode.SNI_I2P_DNS -> "SNI → I2P → DNSCrypt"
                ConnectionMode.SNI_TOR_I2P_DNS -> "SNI → Tor + I2P → DNSCrypt"
                ConnectionMode.TOR_ONLY -> "Tor Only"
                ConnectionMode.DNS_ONLY -> "DNSCrypt Only"
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(40.dp))

        // Big power button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .scale(pulseScale)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val strokeWidth = 4.dp.toPx()
                val color = if (isRunning) primaryColor else if (vpnState == VpnState.ERROR) errorColor else Color(0xFF334155)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = 0.15f), Color.Transparent),
                        center = center,
                        radius = radius
                    )
                )
                drawCircle(
                    color = color,
                    radius = radius - strokeWidth / 2,
                    style = Stroke(strokeWidth)
                )
                if (isRunning) {
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = radius + 12.dp.toPx(),
                        style = Stroke(2.dp.toPx())
                    )
                }
            }
            IconButton(
                onClick = {
                    if (vpnState == VpnState.STOPPED) onRequestVpnPermission()
                    else viewModel.toggleVpn()
                },
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.PowerSettingsNew else Icons.Filled.Power,
                    contentDescription = "Toggle VPN",
                    modifier = Modifier.size(64.dp),
                    tint = if (isRunning) MaterialTheme.colorScheme.primary
                    else if (vpnState == VpnState.ERROR) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = when (vpnState) {
                VpnState.STOPPED -> "Disconnected"
                VpnState.STARTING -> "Connecting..."
                VpnState.RUNNING -> "Connected"
                VpnState.ERROR -> "Error"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = when (vpnState) {
                VpnState.RUNNING -> MaterialTheme.colorScheme.primary
                VpnState.ERROR -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(Modifier.height(32.dp))

        // Status cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusChip("Tor", torState, Modifier.weight(1f))
            StatusChip("I2P", i2pState, Modifier.weight(1f))
            StatusChip("DNS", dnsState, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // SNI info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SNI Spoof", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = config.sniEnabled,
                        onCheckedChange = { viewModel.updateConfig(config.copy(sniEnabled = it)) }
                    )
                }
                if (config.sniEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Host: ${config.sniHost}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Kill switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.GppBad, null,
                    tint = if (config.killSwitch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kill Switch", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Block traffic if VPN drops", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Switch(
                    checked = config.killSwitch,
                    onCheckedChange = { viewModel.updateConfig(config.copy(killSwitch = it)) }
                )
            }
        }
    }
}

@Composable
fun StatusChip(label: String, state: VpnState, modifier: Modifier = Modifier) {
    val color = when (state) {
        VpnState.RUNNING -> MaterialTheme.colorScheme.primary
        VpnState.STARTING -> Color(0xFFFFC107)
        VpnState.ERROR -> MaterialTheme.colorScheme.error
        VpnState.STOPPED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                state.name.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 11.sp,
                color = color
            )
        }
    }
}
