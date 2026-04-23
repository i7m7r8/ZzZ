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

class I2pService : LifecycleService() {

    companion object {
        const val ACTION_START = "com.zzz.vpn.i2p.START"
        const val ACTION_STOP = "com.zzz.vpn.i2p.STOP"
        const val CHANNEL_ID = "zzz_i2p_channel"
        const val NOTIF_ID = 1003
        private const val TAG = "I2pService"

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
                startForeground(NOTIF_ID, buildNotification("Starting I2P..."))
                scope.launch { startI2p() }
            }
            ACTION_STOP -> {
                stopI2p()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private suspend fun startI2p() {
        _stateFlow.value = VpnState.STARTING
        try {
            val dataDir = File(filesDir, "i2p").also { it.mkdirs() }
            val result = ZzzCore.startI2p(
                dataDir = dataDir.absolutePath,
                httpPort = 4444,
                httpsPort = 4445,
                socksPort = 4447,
                nativeLibDir = applicationInfo.nativeLibraryDir
            )
            if (result == 0) {
                _stateFlow.value = VpnState.RUNNING
                updateNotification("I2P connected")
            } else {
                _stateFlow.value = VpnState.ERROR
                updateNotification("I2P failed (code $result)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "I2P start error", e)
            _stateFlow.value = VpnState.ERROR
        }
    }

    private fun stopI2p() {
        ZzzCore.stopI2p()
        _stateFlow.value = VpnState.STOPPED
    }

    override fun onDestroy() {
        stopI2p()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "I2P Service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZzZ VPN — I2P")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))
    }
}
