package com.zzz.vpn.util

import android.content.Context
import android.content.SharedPreferences
import com.zzz.vpn.model.AppConfig
import com.zzz.vpn.model.ConnectionMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zzz_prefs", Context.MODE_PRIVATE)

    suspend fun saveConfig(config: AppConfig) = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putBoolean("sni_enabled", config.sniEnabled)
            putString("sni_host", config.sniHost)
            putInt("sni_port", config.sniPort)
            putString("connection_mode", config.connectionMode.name)
            putBoolean("tor_enabled", config.torEnabled)
            putInt("tor_socks_port", config.torSocksPort)
            putInt("tor_control_port", config.torControlPort)
            putInt("tor_trans_port", config.torTransPort)
            putInt("tor_dns_port", config.torDnsPort)
            putString("tor_bridges", config.torBridges)
            putBoolean("tor_use_bridges", config.torUseBridges)
            putBoolean("i2p_enabled", config.i2pEnabled)
            putInt("i2p_http_port", config.i2pHttpPort)
            putInt("i2p_https_port", config.i2pHttpsPort)
            putInt("i2p_socks_port", config.i2pSocksPort)
            putBoolean("dns_enabled", config.dnsCryptEnabled)
            putInt("dns_port", config.dnsCryptPort)
            putString("dns_server", config.dnsCryptServer)
            putString("dns_fallback", config.dnsCryptFallback)
            putInt("vpn_mtu", config.vpnMtu)
            putBoolean("kill_switch", config.killSwitch)
            putBoolean("bypass_lan", config.bypassLan)
            putBoolean("auto_start", config.autoStart)
            putBoolean("split_enabled", config.splitTunnelingEnabled)
            putStringSet("split_apps", config.splitTunnelingApps.toSet())
            putBoolean("split_mode", config.splitTunnelingMode)
        }.apply()
    }

    suspend fun loadConfig(): AppConfig = withContext(Dispatchers.IO) {
        AppConfig(
            sniEnabled = prefs.getBoolean("sni_enabled", true),
            sniHost = prefs.getString("sni_host", "www.google.com") ?: "www.google.com",
            sniPort = prefs.getInt("sni_port", 443),
            connectionMode = ConnectionMode.valueOf(
                prefs.getString("connection_mode", ConnectionMode.SNI_TOR_DNS.name)
                    ?: ConnectionMode.SNI_TOR_DNS.name
            ),
            torEnabled = prefs.getBoolean("tor_enabled", true),
            torSocksPort = prefs.getInt("tor_socks_port", 9050),
            torControlPort = prefs.getInt("tor_control_port", 9051),
            torTransPort = prefs.getInt("tor_trans_port", 9040),
            torDnsPort = prefs.getInt("tor_dns_port", 5400),
            torBridges = prefs.getString("tor_bridges", "") ?: "",
            torUseBridges = prefs.getBoolean("tor_use_bridges", false),
            i2pEnabled = prefs.getBoolean("i2p_enabled", false),
            i2pHttpPort = prefs.getInt("i2p_http_port", 4444),
            i2pHttpsPort = prefs.getInt("i2p_https_port", 4445),
            i2pSocksPort = prefs.getInt("i2p_socks_port", 4447),
            dnsCryptEnabled = prefs.getBoolean("dns_enabled", true),
            dnsCryptPort = prefs.getInt("dns_port", 5300),
            dnsCryptServer = prefs.getString("dns_server", "cloudflare") ?: "cloudflare",
            dnsCryptFallback = prefs.getString("dns_fallback", "9.9.9.9") ?: "9.9.9.9",
            vpnMtu = prefs.getInt("vpn_mtu", 1500),
            killSwitch = prefs.getBoolean("kill_switch", true),
            bypassLan = prefs.getBoolean("bypass_lan", true),
            autoStart = prefs.getBoolean("auto_start", false),
            splitTunnelingEnabled = prefs.getBoolean("split_enabled", false),
            splitTunnelingApps = prefs.getStringSet("split_apps", emptySet())?.toList() ?: emptyList(),
            splitTunnelingMode = prefs.getBoolean("split_mode", true)
        )
    }
}
