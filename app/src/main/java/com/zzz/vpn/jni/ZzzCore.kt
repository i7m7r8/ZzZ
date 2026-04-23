package com.zzz.vpn.jni

object ZzzCore {
    init {
        System.loadLibrary("zzz_core")
    }

    // Packet processor (TUN → SOCKS proxy)
    external fun startPacketProcessor(
        tunFd: Int,
        socksHost: String,
        socksPort: Int,
        dnsHost: String,
        dnsPort: Int,
        sniEnabled: Boolean,
        sniHost: String,
        mtu: Int
    ): Int

    external fun stopPacketProcessor()

    // Tor
    external fun startTor(
        torrcPath: String,
        dataDir: String,
        nativeLibDir: String
    ): Int

    external fun stopTor()
    external fun getTorStatus(): String

    // I2P
    external fun startI2p(
        dataDir: String,
        httpPort: Int,
        httpsPort: Int,
        socksPort: Int,
        nativeLibDir: String
    ): Int

    external fun stopI2p()
    external fun getI2pStatus(): String

    // DNSCrypt
    external fun startDnsCrypt(
        configPath: String,
        nativeLibDir: String
    ): Int

    external fun stopDnsCrypt()

    // SNI
    external fun setSniHost(host: String)
    external fun testConnectivity(host: String, port: Int, timeoutMs: Int): Boolean
}
