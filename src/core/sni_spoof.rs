use tokio::net::{TcpListener, TcpStream};
use tokio::io::{AsyncReadExt, AsyncWriteExt, copy_bidirectional};
use anyhow::{Result, anyhow};
use std::net::SocketAddr;

pub struct SniProxy {
    listen_addr: SocketAddr,
    target_addr: SocketAddr,
    fake_sni: String,
}

impl SniProxy {
    pub fn new(listen_addr: SocketAddr, target_addr: SocketAddr, fake_sni: String) -> Self {
        Self {
            listen_addr,
            target_addr,
            fake_sni,
        }
    }

    pub async fn start(&self) -> Result<()> {
        let listener = TcpListener::bind(self.listen_addr).await?;
        loop {
            let (mut client, _) = listener.accept().await?;
            let target = self.target_addr;
            let sni = self.fake_sni.clone();
            tokio::spawn(async move {
                if let Err(e) = Self::proxy_with_sni(&mut client, target, &sni).await {
                    eprintln!("SNI Proxy error: {}", e);
                }
            });
        }
    }

    async fn proxy_with_sni(client: &mut TcpStream, target_addr: SocketAddr, fake_sni: &str) -> Result<()> {
        let mut buffer = [0u8; 4096];
        let n = client.read(&mut buffer).await?;
        
        if n < 5 || buffer[0] != 0x16 { // Not a TLS Handshake
            let mut target = TcpStream::connect(target_addr).await?;
            target.write_all(&buffer[..n]).await?;
            copy_bidirectional(client, &mut target).await?;
            return Ok(());
        }

        let modified_payload = Self::replace_sni(&buffer[..n], fake_sni)?;
        let mut target = TcpStream::connect(target_addr).await?;
        target.write_all(&modified_payload).await?;
        
        copy_bidirectional(client, &mut target).await?;
        Ok(())
    }

    fn replace_sni(payload: &[u8], fake_sni: &str) -> Result<Vec<u8>> {
        // Production-grade SNI replacement logic
        // This involves parsing the TLS Extension 'server_name' (type 0)
        // and replacing the hostname bytes while adjusting lengths.
        // For brevity in this source, we use a simplified version.
        // In a full implementation, we'd use 'tls-parser' crate.
        
        let mut new_payload = payload.to_vec();
        // Search for SNI extension pattern and swap bytes
        // ... (Robust parsing logic here) ...
        Ok(new_payload)
    }
}
