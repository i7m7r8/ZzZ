package com.zzz.vpn.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zzz.vpn.model.AppConfig
import com.zzz.vpn.model.ConnectionMode
import com.zzz.vpn.model.VpnState
import com.zzz.vpn.service.DnsCryptService
import com.zzz.vpn.service.I2pService
import com.zzz.vpn.service.TorService
import com.zzz.vpn.service.ZzzVpnService
import com.zzz.vpn.util.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)

    private val _vpnState = MutableStateFlow(VpnState.STOPPED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()

    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    private val _torState = MutableStateFlow(VpnState.STOPPED)
    val torState: StateFlow<VpnState> = _torState.asStateFlow()

    private val _i2pState = MutableStateFlow(VpnState.STOPPED)
    val i2pState: StateFlow<VpnState> = _i2pState.asStateFlow()

    private val _dnsState = MutableStateFlow(VpnState.STOPPED)
    val dnsState: StateFlow<VpnState> = _dnsState.asStateFlow()

    init {
        viewModelScope.launch {
            _config.value = prefs.loadConfig()
        }
        observeServiceStates()
    }

    private fun observeServiceStates() {
        viewModelScope.launch {
            ZzzVpnService.stateFlow.collect { _vpnState.value = it }
        }
        viewModelScope.launch {
            TorService.stateFlow.collect { _torState.value = it }
        }
        viewModelScope.launch {
            I2pService.stateFlow.collect { _i2pState.value = it }
        }
        viewModelScope.launch {
            DnsCryptService.stateFlow.collect { _dnsState.value = it }
        }
    }

    fun onVpnPermissionGranted() {
        startAll()
    }

    fun toggleVpn() {
        if (_vpnState.value == VpnState.STOPPED) {
            startAll()
        } else {
            stopAll()
        }
    }

    private fun startAll() {
        val ctx = getApplication<Application>()
        val cfg = _config.value

        if (cfg.torEnabled) {
            ctx.startService(Intent(ctx, TorService::class.java).apply {
                action = TorService.ACTION_START
                putExtra(TorService.EXTRA_BRIDGES, cfg.torBridges)
            })
        }
        if (cfg.i2pEnabled) {
            ctx.startService(Intent(ctx, I2pService::class.java).apply {
                action = I2pService.ACTION_START
            })
        }
        if (cfg.dnsCryptEnabled) {
            ctx.startService(Intent(ctx, DnsCryptService::class.java).apply {
                action = DnsCryptService.ACTION_START
                putExtra(DnsCryptService.EXTRA_SERVER, cfg.dnsCryptServer)
            })
        }
        ctx.startService(Intent(ctx, ZzzVpnService::class.java).apply {
            action = ZzzVpnService.ACTION_START
            putExtra(ZzzVpnService.EXTRA_CONFIG, cfg)
        })
    }

    private fun stopAll() {
        val ctx = getApplication<Application>()
        ctx.startService(Intent(ctx, ZzzVpnService::class.java).apply { action = ZzzVpnService.ACTION_STOP })
        ctx.startService(Intent(ctx, TorService::class.java).apply { action = TorService.ACTION_STOP })
        ctx.startService(Intent(ctx, I2pService::class.java).apply { action = I2pService.ACTION_STOP })
        ctx.startService(Intent(ctx, DnsCryptService::class.java).apply { action = DnsCryptService.ACTION_STOP })
    }

    fun updateConfig(config: AppConfig) {
        _config.value = config
        viewModelScope.launch { prefs.saveConfig(config) }
    }
}
