use anyhow::{anyhow, Result};
use std::net::{IpAddr, SocketAddr};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::TcpStream;

/// Perform SOCKS5 CONNECT handshake with no-auth
pub async fn connect(stream: &mut TcpStream, target: SocketAddr) -> Result<()> {
    // Greeting: VER=5, NMETHODS=1, METHOD=0x00 (no auth)
    stream.write_all(&[0x05, 0x01, 0x00]).await?;

    let mut resp = [0u8; 2];
    stream.read_exact(&mut resp).await?;
    if resp[0] != 0x05 || resp[1] != 0x00 {
        return Err(anyhow!("SOCKS5 auth rejected: {:?}", resp));
    }

    // CONNECT request
    let mut req = Vec::with_capacity(22);
    req.extend_from_slice(&[0x05, 0x01, 0x00]); // VER CMD RSV

    match target.ip() {
        IpAddr::V4(v4) => {
            req.push(0x01); // ATYP IPv4
            req.extend_from_slice(&v4.octets());
        }
        IpAddr::V6(v6) => {
            req.push(0x04); // ATYP IPv6
            req.extend_from_slice(&v6.octets());
        }
    }
    req.extend_from_slice(&target.port().to_be_bytes());
    stream.write_all(&req).await?;

    // Read response (at least 10 bytes for IPv4 reply)
    let mut hdr = [0u8; 4];
    stream.read_exact(&mut hdr).await?;
    if hdr[0] != 0x05 {
        return Err(anyhow!("SOCKS5 bad version in reply"));
    }
    if hdr[1] != 0x00 {
        return Err(anyhow!("SOCKS5 CONNECT failed, rep={:#04x}", hdr[1]));
    }

    // Drain BND.ADDR + BND.PORT
    match hdr[3] {
        0x01 => { let mut b = [0u8; 6]; stream.read_exact(&mut b).await?; }
        0x03 => {
            let mut len = [0u8; 1];
            stream.read_exact(&mut len).await?;
            let mut b = vec![0u8; len[0] as usize + 2];
            stream.read_exact(&mut b).await?;
        }
        0x04 => { let mut b = [0u8; 18]; stream.read_exact(&mut b).await?; }
        _ => return Err(anyhow!("SOCKS5 unknown ATYP in reply")),
    }

    Ok(())
}

/// SOCKS5 CONNECT using domain name (for hostname-based routing)
pub async fn connect_domain(stream: &mut TcpStream, host: &str, port: u16) -> Result<()> {
    // Greeting
    stream.write_all(&[0x05, 0x01, 0x00]).await?;
    let mut resp = [0u8; 2];
    stream.read_exact(&mut resp).await?;
    if resp[1] != 0x00 {
        return Err(anyhow!("SOCKS5 auth rejected"));
    }

    let host_bytes = host.as_bytes();
    let mut req = Vec::with_capacity(7 + host_bytes.len());
    req.extend_from_slice(&[0x05, 0x01, 0x00, 0x03]);
    req.push(host_bytes.len() as u8);
    req.extend_from_slice(host_bytes);
    req.extend_from_slice(&port.to_be_bytes());
    stream.write_all(&req).await?;

    let mut hdr = [0u8; 4];
    stream.read_exact(&mut hdr).await?;
    if hdr[1] != 0x00 {
        return Err(anyhow!("SOCKS5 CONNECT failed rep={:#04x}", hdr[1]));
    }
    match hdr[3] {
        0x01 => { let mut b = [0u8; 6]; stream.read_exact(&mut b).await?; }
        0x03 => {
            let mut l = [0u8; 1]; stream.read_exact(&mut l).await?;
            let mut b = vec![0u8; l[0] as usize + 2]; stream.read_exact(&mut b).await?;
        }
        0x04 => { let mut b = [0u8; 18]; stream.read_exact(&mut b).await?; }
        _ => {}
    }
    Ok(())
}
