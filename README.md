# ZzZ VPN

Privacy-focused VPN for Android bundling **SNI spoofing → Tor → I2P → DNSCrypt**, modelled on InviZible Pro.

## Architecture

```
App Traffic
    │
    ▼
┌─────────────────────────────┐
│       Android TUN Device    │
│       (10.111.222.1/24)     │
└──────────────┬──────────────┘
               │ raw IP packets
               ▼
┌─────────────────────────────┐
│    zzz-core (Rust/JNI)      │
│   Packet Processor          │
│  ┌───────────────────────┐  │
│  │ TCP: SOCKS5 forwarder │  │
│  │ UDP: DNS redirector   │  │
│  │ TLS: SNI injection    │  │
│  └───────────────────────┘  │
└──────────────┬──────────────┘
               │
       ┌───────┴────────┐
       │                │
       ▼                ▼
┌──────────┐    ┌──────────────┐
│  Tor     │    │  DNSCrypt    │
│ (SOCKS5) │    │  (UDP:5300)  │
│  :9050   │    │              │
└──────────┘    └──────────────┘
       │
       ▼
┌──────────┐
│  I2P     │
│ (SOCKS5) │
│  :4447   │
└──────────┘
```

## Connection Modes

| Mode | Path |
|------|------|
| `SNI_TOR_DNS` | App → SNI spoof → Tor → DNSCrypt |
| `SNI_I2P_DNS` | App → SNI spoof → I2P → DNSCrypt |
| `SNI_TOR_I2P_DNS` | App → SNI spoof → Tor + I2P → DNSCrypt |
| `TOR_ONLY` | App → Tor |
| `DNS_ONLY` | App → DNSCrypt only |

## Build from GitHub (Termux workflow)

```bash
# 1. Push to GitHub
git init
git remote add origin https://github.com/YOUR_USER/ZzZ-VPN.git
git add .
git commit -m "Initial commit"
git push -u origin main

# 2. Run bootstrap workflow FIRST (generates gradle-wrapper.jar)
#    Go to Actions → Bootstrap Gradle Wrapper → Run workflow

# 3. The main build.yml triggers automatically on push
#    Go to Actions → Build ZzZ VPN → download APK artifact
```

## First-Time Setup

After cloning:
```bash
# Generate icons locally (optional, CI does this too)
python3 scripts/gen_icons.py
```

## Native Binaries

The CI workflow downloads prebuilt binaries from official sources:

| Binary | Source |
|--------|--------|
| `tor` | Guardian Project / Tor Project |
| `i2pd` | PurpleI2P official releases |
| `dnscrypt-proxy` | DNSCrypt project releases |

These are packaged as `lib*.so` in `jniLibs/` so Android loads them. They are **not compiled from source** in CI — they are downloaded from official release pages.

## SNI Spoofing

Set any domain as your SNI host in Settings. The Rust core rewrites the TLS ClientHello extension before the packet reaches Tor. This makes your traffic look like it's going to e.g. `www.google.com` at the DPI level.

## Features

- ✅ Real Tor binary (not arti)
- ✅ Real i2pd binary
- ✅ Real dnscrypt-proxy binary
- ✅ SNI injection in Rust (TLS ClientHello rewriting)
- ✅ SOCKS5 forwarding via tokio
- ✅ Kill switch
- ✅ Split tunneling
- ✅ obfs4 bridge support
- ✅ Auto-start on boot
- ✅ Compose UI with dark theme
- ✅ GitHub Actions CI → APK release

## Project Structure

```
ZzZ-VPN/
├── .github/workflows/
│   ├── build.yml          # Main CI: Rust → APK → Release
│   └── bootstrap.yml      # One-time: generate gradle wrapper
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── cpp/CMakeLists.txt
│       ├── java/com/zzz/vpn/
│       │   ├── MainActivity.kt
│       │   ├── jni/ZzzCore.kt
│       │   ├── model/Models.kt
│       │   ├── receiver/BootReceiver.kt
│       │   ├── service/{VpnService,TorService,I2pService,DnsCryptService}.kt
│       │   ├── ui/{ZzzApp,screens/,theme/}
│       │   ├── util/PrefsManager.kt
│       │   └── viewmodel/MainViewModel.kt
│       └── res/
├── zzz-core/              # Rust JNI library
│   ├── Cargo.toml
│   └── src/
│       ├── lib.rs          # JNI exports
│       ├── tun/            # TUN packet processor
│       ├── sni/            # TLS SNI injection
│       ├── proxy/socks5.rs # SOCKS5 client
│       ├── tor_mgr/        # Tor process manager
│       ├── i2p_mgr/        # i2pd process manager
│       └── dns_mgr/        # dnscrypt-proxy manager
└── scripts/
    └── gen_icons.py
```

## License

GPL-3.0 — same as InviZible Pro and i2pd.
