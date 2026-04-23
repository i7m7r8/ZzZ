package com.zzz.vpn.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zzz.vpn.service.ZzzVpnService
import com.zzz.vpn.util.PrefsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val prefs = PrefsManager(context)
                val config = prefs.loadConfig()
                if (config.autoStart) {
                    context.startService(
                        Intent(context, ZzzVpnService::class.java).apply {
                            action = ZzzVpnService.ACTION_START
                            putExtra(ZzzVpnService.EXTRA_CONFIG, config)
                        }
                    )
                }
            }
        }
    }
}
