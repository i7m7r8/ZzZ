use std::net::SocketAddr;
use crate::core::sni_spoof::SniProxy;
use crate::core::binary_manager::BinaryManager;
use crate::core::tun_engine::TunEngine;
use anyhow::Result;

pub struct ProxyChain {
    sni_proxy: SniProxy,
    binaries: BinaryManager,
    tun: TunEngine,
}

impl ProxyChain {
    pub fn new(fake_sni: String, binaries_path: std::path::PathBuf) -> Self {
        let listen = "127.0.0.1:1080".parse().unwrap();
        let tor_socks = "127.0.0.1:9050".parse().unwrap();
        
        Self {
            sni_proxy: SniProxy::new(listen, tor_socks, fake_sni),
            binaries: BinaryManager::new(binaries_path),
            tun: TunEngine::new(),
        }
    }

    pub async fn start_all(&mut self) -> Result<()> {
        // 1. Start DNSCrypt for secure lookups
        self.binaries.start_dnscrypt()?;
        
        // 2. Start Tor/I2P Binaries
        self.binaries.start_tor()?;
        self.binaries.start_i2p()?;
        
        // 3. Start SNI Proxy (Connects System -> SNI -> Tor)
        let sni = self.sni_proxy.start();
        
        // 4. Initialize TUN and route traffic to SNI Proxy
        self.tun.start("zzz0", "10.0.0.1", "255.255.255.0")?;
        
        // Keep SNI proxy running
        sni.await?;
        
        Ok(())
    }

    pub fn stop_all(&mut self) {
        self.binaries.stop_all();
    }
}
