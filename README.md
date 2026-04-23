# ZzZ VPN

Military-grade, cross-platform VPN with SNI spoofing, Tor, I2P, and DNSCrypt integration.

## Features
- **SNI Spoofing**: Bypass deep packet inspection (DPI).
- **Tor & I2P**: Multi-layered anonymity.
- **DNSCrypt**: Secure DNS queries.
- **Rust Core**: High performance and safety.
- **Iced UI**: Modern, cross-platform interface.

## Build Instructions
1. Install Rust: `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`
2. Clone the repo.
3. Place your platform-specific binaries in the `binaries/` folder.
4. Run: `cargo run --release`

## Architecture
Traffic Flow: `System -> TUN -> SNI Proxy -> (Tor/I2P/DNSCrypt) -> Internet`
