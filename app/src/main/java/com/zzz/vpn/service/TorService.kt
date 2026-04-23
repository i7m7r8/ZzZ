package com.zzz.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.zzz.vpn.jni.ZzzCore
import com.zzz.vpn.model.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class TorService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.zzz.vpn.tor.START"
        const val ACTION_STOP = "com.zzz.vpn.tor.STOP"
        const val EXTRA_BRIDGES = "bridges"
        const val CHANNEL_ID = "zzz_tor_channel"
        const val NOTIF_ID = 1002
        private const val TAG = "TorService"

        private val _stateFlow = MutableStateFlow(VpnState.STOPPED)
        val stateFlow: StateFlow<VpnState> = _stateFlow
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val bridges = intent.getStringExtra(EXTRA_BRIDGES) ?: ""
                startForeground(NOTIF_ID, buildNotification("Starting Tor..."))
                scope.launch { startTor(bridges) }
            }
            ACTION_STOP -> {
                stopTor()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private suspend fun startTor(bridges: String) {
        _stateFlow.value = VpnState.STARTING
        try {
            val dataDir = File(filesDir, "tor").also { it.mkdirs() }
            val torrc = buildTorrc(dataDir, bridges)
            val torrcFile = File(dataDir, "torrc")
            torrcFile.writeText(torrc)

            val result = ZzzCore.startTor(
                torrcPath = torrcFile.absolutePath,
                dataDir = dataDir.absolutePath,
                nativeLibDir = applicationInfo.nativeLibraryDir
            )

            if (result == 0) {
                _stateFlow.value = VpnState.RUNNING
                updateNotification("Tor connected")
            } else {
                _stateFlow.value = VpnState.ERROR
                updateNotification("Tor failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tor start error", e)
            _stateFlow.value = VpnState.ERROR
        }
    }

    private fun buildTorrc(dataDir: File, bridges: String): String {
        val sb = StringBuilder()
        sb.appendLine("DataDirectory ${dataDir.absolutePath}")
        sb.appendLine("SocksPort 9050")
        sb.appendLine("ControlPort 9051")
        sb.appendLine("DNSPort 5400")
        sb.appendLine("TransPort 9040")
        sb.appendLine("Log notice stdout")
        sb.appendLine("GeoIPFile ${dataDir.absolutePath}/geoip")
        sb.appendLine("GeoIPv6File ${dataDir.absolutePath}/geoip6")
        sb.appendLine("VirtualAddrNetworkIPv4 10.192.0.0/10")
        sb.appendLine("AutomapHostsOnResolve 1")
        sb.appendLine("AutomapHostsSuffixes .onion,.exit")

        if (bridges.isNotBlank()) {
            sb.appendLine("UseBridges 1")
            sb.appendLine("ClientTransportPlugin obfs4 exec ${dataDir.absolutePath}/obfs4proxy")
            for (bridge in bridges.lines()) {
                if (bridge.isNotBlank()) sb.appendLine("Bridge $bridge")
            }
        }
        return sb.toString()
    }

    private fun stopTor() {
        ZzzCore.stopTor()
        _stateFlow.value = VpnState.STOPPED
    }

    override fun onDestroy() {
        stopTor()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Tor Service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZzZ VPN — Tor")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))
    }
}
