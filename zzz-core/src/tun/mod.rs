use anyhow::{anyhow, Result};
use bytes::{Bytes, BytesMut};
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::os::unix::io::FromRawFd;
use std::sync::Arc;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio::net::{TcpStream, UdpSocket};
use tokio::sync::Semaphore;
use tokio::task::JoinHandle;

use crate::proxy::socks5;
use crate::sni::inject_sni;

pub struct ProcessorConfig {
    pub tun_fd: i32,
    pub socks_host: String,
    pub socks_port: u16,
    pub dns_host: String,
    pub dns_port: u16,
    pub sni_enabled: bool,
    pub sni_host: String,
    pub mtu: usize,
}

pub struct ProcessorHandle {
    stop_tx: tokio::sync::watch::Sender<bool>,
    _task: JoinHandle<()>,
}

impl ProcessorHandle {
    pub fn stop(self) {
        let _ = self.stop_tx.send(true);
    }
}

pub async fn start_processor(cfg: ProcessorConfig) -> Result<ProcessorHandle> {
    let (stop_tx, stop_rx) = tokio::sync::watch::channel(false);
    let cfg = Arc::new(cfg);
    let stop_rx2 = stop_rx.clone();

    let task = tokio::spawn(async move {
        if let Err(e) = run_processor(cfg, stop_rx2).await {
            log::error!("Packet processor error: {e}");
        }
    });

    Ok(ProcessorHandle { stop_tx, _task: task })
}

async fn run_processor(
    cfg: Arc<ProcessorConfig>,
    mut stop_rx: tokio::sync::watch::Receiver<bool>,
) -> Result<()> {
    // Open TUN fd as async file
    let tun_file = unsafe { std::fs::File::from_raw_fd(cfg.tun_fd) };
    let mut tun_async = tokio::fs::File::from_std(tun_file);

    let sem = Arc::new(Semaphore::new(256)); // max concurrent connections
    let mut buf = vec![0u8; cfg.mtu + 4];

    loop {
        tokio::select! {
            _ = stop_rx.changed() => {
                if *stop_rx.borrow() { break; }
            }
            n = tun_async.read(&mut buf) => {
                let n = n?;
                if n < 20 { continue; }
                let packet = Bytes::copy_from_slice(&buf[..n]);
                let cfg2 = cfg.clone();
                let permit = sem.clone().acquire_owned().await?;
                tokio::spawn(async move {
                    if let Err(e) = handle_packet(packet, cfg2).await {
                        log::debug!("Packet handle error: {e}");
                    }
                    drop(permit);
                });
            }
        }
    }
    Ok(())
}

async fn handle_packet(pkt: Bytes, cfg: Arc<ProcessorConfig>) -> Result<()> {
    // Parse IP header
    if pkt.len() < 20 { return Ok(()); }
    let version = (pkt[0] >> 4) & 0xF;
    if version != 4 { return Ok(()); } // IPv6 TODO

    let protocol = pkt[9];
    let src_ip = Ipv4Addr::new(pkt[12], pkt[13], pkt[14], pkt[15]);
    let dst_ip = Ipv4Addr::new(pkt[16], pkt[17], pkt[18], pkt[19]);
    let ihl = ((pkt[0] & 0xF) * 4) as usize;

    match protocol {
        6 => handle_tcp(pkt, ihl, src_ip, dst_ip, &cfg).await,
        17 => handle_udp(pkt, ihl, src_ip, dst_ip, &cfg).await,
        _ => Ok(()),
    }
}

async fn handle_tcp(
    pkt: Bytes,
    ihl: usize,
    _src_ip: Ipv4Addr,
    dst_ip: Ipv4Addr,
    cfg: &ProcessorConfig,
) -> Result<()> {
    if pkt.len() < ihl + 4 { return Ok(()); }
    let dst_port = u16::from_be_bytes([pkt[ihl + 2], pkt[ihl + 3]]);
    let tcp_payload_offset = ihl + ((pkt[ihl + 12] >> 4) as usize * 4);

    // Check for TLS ClientHello for SNI injection
    let is_tls = dst_port == 443 && pkt.len() > tcp_payload_offset + 6
        && pkt[tcp_payload_offset] == 0x16  // TLS handshake
        && pkt[tcp_payload_offset + 1] == 0x03
        && pkt[tcp_payload_offset + 5] == 0x01; // ClientHello

    // Connect via SOCKS5 to the destination
    let target_addr = SocketAddr::new(IpAddr::V4(dst_ip), dst_port);
    let proxy_addr = format!("{}:{}", cfg.socks_host, cfg.socks_port);

    let mut proxy_stream = TcpStream::connect(&proxy_addr).await
        .map_err(|e| anyhow!("proxy connect fail: {e}"))?;

    // SOCKS5 handshake
    socks5::connect(&mut proxy_stream, target_addr).await?;

    // If TLS and SNI enabled, inject SNI before forwarding
    if is_tls && cfg.sni_enabled && !cfg.sni_host.is_empty() {
        let payload = pkt.slice(tcp_payload_offset..);
        let modified = inject_sni(payload, &cfg.sni_host)?;
        proxy_stream.write_all(&modified).await?;
    } else if pkt.len() > tcp_payload_offset {
        proxy_stream.write_all(&pkt[tcp_payload_offset..]).await?;
    }

    Ok(())
}

async fn handle_udp(
    pkt: Bytes,
    ihl: usize,
    _src_ip: Ipv4Addr,
    dst_ip: Ipv4Addr,
    cfg: &ProcessorConfig,
) -> Result<()> {
    if pkt.len() < ihl + 8 { return Ok(()); }
    let dst_port = u16::from_be_bytes([pkt[ihl + 2], pkt[ihl + 3]]);
    let payload_offset = ihl + 8;

    // DNS traffic → redirect to our DNS proxy
    if dst_port == 53 {
        let dns_addr = format!("{}:{}", cfg.dns_host, cfg.dns_port);
        let sock = UdpSocket::bind("0.0.0.0:0").await?;
        sock.send_to(&pkt[payload_offset..], &dns_addr).await?;
        // Response would be handled by full tun bidirectional implementation
    }

    Ok(())
}
