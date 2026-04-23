package com.zzz.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zzz.vpn.MainActivity
import com.zzz.vpn.R
import com.zzz.vpn.jni.ZzzCore
import com.zzz.vpn.model.AppConfig
import com.zzz.vpn.model.ConnectionMode
import com.zzz.vpn.model.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ZzzVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.zzz.vpn.START"
        const val ACTION_STOP = "com.zzz.vpn.STOP"
        const val EXTRA_CONFIG = "config"
        const val CHANNEL_ID = "zzz_vpn_channel"
        const val NOTIF_ID = 1001
        private const val TAG = "ZzzVpnService"

        private val _stateFlow = MutableStateFlow(VpnState.STOPPED)
        val stateFlow: StateFlow<VpnState> = _stateFlow
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnFd: ParcelFileDescriptor? = null
    private var config: AppConfig = AppConfig()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                config = intent.getParcelableExtra(EXTRA_CONFIG) ?: AppConfig()
                startForeground(NOTIF_ID, buildNotification("Starting..."))
                scope.launch { startVpn() }
            }
            ACTION_STOP -> {
                stopVpn()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private suspend fun startVpn() {
        _stateFlow.value = VpnState.STARTING
        try {
            val builder = Builder()
                .setSession("ZzZ VPN")
                .setMtu(config.vpnMtu)
                .addAddress(config.vpnAddress, 24)
                .addDnsServer(config.vpnDns)

            // Route all traffic
            builder.addRoute("0.0.0.0", 0)
            builder.addRoute("::", 0)

            // Bypass LAN if configured
            if (config.bypassLan) {
                builder.addRoute("10.0.0.0", 8)
                builder.addRoute("172.16.0.0", 12)
                builder.addRoute("192.168.0.0", 16)
                // Re-add our VPN address range
                builder.addRoute("10.111.222.0", 24)
            }

            // Allow our own app to bypass VPN (to reach Tor/I2P/DNS proxies)
            builder.addDisallowedApplication(packageName)

            // Split tunneling
            if (config.splitTunnelingEnabled) {
                if (config.splitTunnelingMode) {
                    // Exclude mode
                    for (pkg in config.splitTunnelingApps) {
                        try { builder.addDisallowedApplication(pkg) } catch (e: Exception) { }
                    }
                } else {
                    // Include mode
                    for (pkg in config.splitTunnelingApps) {
                        try { builder.addAllowedApplication(pkg) } catch (e: Exception) { }
                    }
                }
            }

            vpnFd = builder.establish()
            val fd = vpnFd?.fd ?: throw IllegalStateException("Failed to establish VPN")

            // Determine proxy settings based on connection mode
            val (socksHost, socksPort, dnsPort) = when (config.connectionMode) {
                ConnectionMode.SNI_TOR_DNS, ConnectionMode.SNI_TOR_I2P_DNS, ConnectionMode.TOR_ONLY ->
                    Triple("127.0.0.1", config.torSocksPort, config.torDnsPort)
                ConnectionMode.SNI_I2P_DNS ->
                    Triple("127.0.0.1", config.i2pSocksPort, config.dnsCryptPort)
                ConnectionMode.DNS_ONLY ->
                    Triple("127.0.0.1", config.torSocksPort, config.dnsCryptPort)
            }

            // Start native packet processor
            ZzzCore.startPacketProcessor(
                tunFd = fd,
                socksHost = socksHost,
                socksPort = socksPort,
                dnsHost = "127.0.0.1",
                dnsPort = dnsPort,
                sniEnabled = config.sniEnabled,
                sniHost = config.sniHost,
                mtu = config.vpnMtu
            )

            _stateFlow.value = VpnState.RUNNING
            updateNotification("Running — ${config.connectionMode.name.replace('_', '→')}")
        } catch (e: Exception) {
            Log.e(TAG, "VPN start error", e)
            _stateFlow.value = VpnState.ERROR
        }
    }

    private fun stopVpn() {
        ZzzCore.stopPacketProcessor()
        vpnFd?.close()
        vpnFd = null
        _stateFlow.value = VpnState.STOPPED
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpn()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "VPN Status", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "ZzZ VPN connection status" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZzZ VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(text))
    }
}
