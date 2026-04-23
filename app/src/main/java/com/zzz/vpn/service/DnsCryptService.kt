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

class DnsCryptService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.zzz.vpn.dns.START"
        const val ACTION_STOP = "com.zzz.vpn.dns.STOP"
        const val EXTRA_SERVER = "server"
        const val CHANNEL_ID = "zzz_dns_channel"
        const val NOTIF_ID = 1004
        private const val TAG = "DnsCryptService"

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
                val server = intent.getStringExtra(EXTRA_SERVER) ?: "cloudflare"
                startForeground(NOTIF_ID, buildNotification("Starting DNSCrypt..."))
                scope.launch { startDnsCrypt(server) }
            }
            ACTION_STOP -> {
                stopDnsCrypt()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private suspend fun startDnsCrypt(server: String) {
        _stateFlow.value = VpnState.STARTING
        try {
            val dataDir = File(filesDir, "dnscrypt").also { it.mkdirs() }
            val config = buildDnsCryptConfig(server)
            val configFile = File(dataDir, "dnscrypt-proxy.toml")
            configFile.writeText(config)

            val result = ZzzCore.startDnsCrypt(
                configPath = configFile.absolutePath,
                nativeLibDir = applicationInfo.nativeLibraryDir
            )
            if (result == 0) {
                _stateFlow.value = VpnState.RUNNING
                updateNotification("DNSCrypt active ($server)")
            } else {
                _stateFlow.value = VpnState.ERROR
                updateNotification("DNSCrypt failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "DNSCrypt start error", e)
            _stateFlow.value = VpnState.ERROR
        }
    }

    private fun buildDnsCryptConfig(server: String): String = """
listen_addresses = ['127.0.0.1:5300']
server_names = ['$server']
log_level = 2
log_file = '${filesDir.absolutePath}/dnscrypt/dnscrypt.log'

[query_log]
  file = '${filesDir.absolutePath}/dnscrypt/query.log'

[sources]
  [sources.public-resolvers]
    urls = ['https://raw.githubusercontent.com/DNSCrypt/dnscrypt-resolvers/master/v3/public-resolvers.md']
    cache_file = '${filesDir.absolutePath}/dnscrypt/public-resolvers.md'
    minisign_key = 'RWQf6LRCGA9i53mlYecO4IzT51TGPpvWucNSCh1CBM0QTaLn73Y7GFO3'
    refresh_delay = 72

[broken_implementations]
  fragments_blocked = ['cisco', 'cisco-ipv6', 'cisco-familyshield']
""".trimIndent()

    private fun stopDnsCrypt() {
        ZzzCore.stopDnsCrypt()
        _stateFlow.value = VpnState.STOPPED
    }

    override fun onDestroy() {
        stopDnsCrypt()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "DNSCrypt Service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZzZ VPN — DNSCrypt")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))
    }
}
