package com.zzz.vpn.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class VpnState { STOPPED, STARTING, RUNNING, ERROR }

enum class ConnectionMode {
    SNI_TOR_DNS,
    SNI_I2P_DNS,
    SNI_TOR_I2P_DNS,
    TOR_ONLY,
    DNS_ONLY
}

@Parcelize
data class AppConfig(
    // SNI Spoofing
    val sniEnabled: Boolean = true,
    val sniHost: String = "www.google.com",
    val sniPort: Int = 443,

    // Connection mode
    val connectionMode: ConnectionMode = ConnectionMode.SNI_TOR_DNS,

    // Tor
    val torEnabled: Boolean = true,
    val torSocksPort: Int = 9050,
    val torControlPort: Int = 9051,
    val torTransPort: Int = 9040,
    val torDnsPort: Int = 5400,
    val torBridges: String = "",
    val torUseBridges: Boolean = false,

    // I2P
    val i2pEnabled: Boolean = false,
    val i2pHttpPort: Int = 4444,
    val i2pHttpsPort: Int = 4445,
    val i2pSocksPort: Int = 4447,

    // DNSCrypt
    val dnsCryptEnabled: Boolean = true,
    val dnsCryptPort: Int = 5300,
    val dnsCryptServer: String = "cloudflare",
    val dnsCryptFallback: String = "9.9.9.9",

    // VPN
    val vpnMtu: Int = 1500,
    val vpnAddress: String = "10.111.222.1",
    val vpnDns: String = "10.111.222.2",
    val killSwitch: Boolean = true,
    val bypassLan: Boolean = true,
    val autoStart: Boolean = false,

    // Split tunneling
    val splitTunnelingEnabled: Boolean = false,
    val splitTunnelingApps: List<String> = emptyList(),
    val splitTunnelingMode: Boolean = true // true=exclude, false=include
) : Parcelable
