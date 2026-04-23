mod sni;
mod tun;
mod proxy;
mod tor_mgr;
mod i2p_mgr;
mod dns_mgr;

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jstring, JNI_TRUE, JNI_FALSE};
use once_cell::sync::Lazy;
use parking_lot::Mutex;
use std::sync::Arc;
use tokio::runtime::Runtime;

static RT: Lazy<Runtime> = Lazy::new(|| {
    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .worker_threads(4)
        .build()
        .expect("tokio runtime")
});

static STATE: Lazy<Mutex<AppState>> = Lazy::new(|| Mutex::new(AppState::default()));

#[derive(Default)]
struct AppState {
    packet_processor: Option<tun::ProcessorHandle>,
    tor_pid: Option<nix::unistd::Pid>,
    i2p_pid: Option<nix::unistd::Pid>,
    dns_pid: Option<nix::unistd::Pid>,
    sni_host: String,
}

fn setup_logger() {
    #[cfg(target_os = "android")]
    {
        android_logger::init_once(
            android_logger::Config::default()
                .with_max_level(log::LevelFilter::Debug)
                .with_tag("ZzzCore"),
        );
    }
}

// ─── Packet Processor ─────────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_startPacketProcessor(
    mut env: JNIEnv,
    _class: JClass,
    tun_fd: jint,
    socks_host: JString,
    socks_port: jint,
    dns_host: JString,
    dns_port: jint,
    sni_enabled: jboolean,
    sni_host: JString,
    mtu: jint,
) -> jint {
    setup_logger();
    let socks_host: String = env.get_string(&socks_host).unwrap().into();
    let dns_host: String = env.get_string(&dns_host).unwrap().into();
    let sni_host: String = env.get_string(&sni_host).unwrap().into();

    let cfg = tun::ProcessorConfig {
        tun_fd: tun_fd as i32,
        socks_host,
        socks_port: socks_port as u16,
        dns_host,
        dns_port: dns_port as u16,
        sni_enabled: sni_enabled == JNI_TRUE,
        sni_host: sni_host.clone(),
        mtu: mtu as usize,
    };

    {
        let mut state = STATE.lock();
        state.sni_host = sni_host;
    }

    let handle = RT.block_on(async { tun::start_processor(cfg).await });
    match handle {
        Ok(h) => {
            STATE.lock().packet_processor = Some(h);
            0
        }
        Err(e) => {
            log::error!("startPacketProcessor error: {e}");
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_stopPacketProcessor(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Some(h) = STATE.lock().packet_processor.take() {
        h.stop();
    }
}

// ─── Tor ──────────────────────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_startTor(
    mut env: JNIEnv,
    _class: JClass,
    torrc_path: JString,
    data_dir: JString,
    native_lib_dir: JString,
) -> jint {
    setup_logger();
    let torrc: String = env.get_string(&torrc_path).unwrap().into();
    let data: String = env.get_string(&data_dir).unwrap().into();
    let lib_dir: String = env.get_string(&native_lib_dir).unwrap().into();

    match tor_mgr::start_tor(&torrc, &data, &lib_dir) {
        Ok(pid) => {
            STATE.lock().tor_pid = Some(pid);
            0
        }
        Err(e) => {
            log::error!("startTor error: {e}");
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_stopTor(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Some(pid) = STATE.lock().tor_pid.take() {
        tor_mgr::stop_tor(pid);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_getTorStatus(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let status = if STATE.lock().tor_pid.is_some() { "RUNNING" } else { "STOPPED" };
    env.new_string(status).unwrap().into_raw()
}

// ─── I2P ──────────────────────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_startI2p(
    mut env: JNIEnv,
    _class: JClass,
    data_dir: JString,
    http_port: jint,
    https_port: jint,
    socks_port: jint,
    native_lib_dir: JString,
) -> jint {
    setup_logger();
    let data: String = env.get_string(&data_dir).unwrap().into();
    let lib_dir: String = env.get_string(&native_lib_dir).unwrap().into();

    match i2p_mgr::start_i2p(&data, http_port as u16, https_port as u16, socks_port as u16, &lib_dir) {
        Ok(pid) => {
            STATE.lock().i2p_pid = Some(pid);
            0
        }
        Err(e) => {
            log::error!("startI2p error: {e}");
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_stopI2p(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Some(pid) = STATE.lock().i2p_pid.take() {
        i2p_mgr::stop_i2p(pid);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_getI2pStatus(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let status = if STATE.lock().i2p_pid.is_some() { "RUNNING" } else { "STOPPED" };
    env.new_string(status).unwrap().into_raw()
}

// ─── DNSCrypt ─────────────────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_startDnsCrypt(
    mut env: JNIEnv,
    _class: JClass,
    config_path: JString,
    native_lib_dir: JString,
) -> jint {
    setup_logger();
    let cfg: String = env.get_string(&config_path).unwrap().into();
    let lib_dir: String = env.get_string(&native_lib_dir).unwrap().into();

    match dns_mgr::start_dnscrypt(&cfg, &lib_dir) {
        Ok(pid) => {
            STATE.lock().dns_pid = Some(pid);
            0
        }
        Err(e) => {
            log::error!("startDnsCrypt error: {e}");
            -1
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_stopDnsCrypt(
    _env: JNIEnv,
    _class: JClass,
) {
    if let Some(pid) = STATE.lock().dns_pid.take() {
        dns_mgr::stop_dnscrypt(pid);
    }
}

// ─── SNI ──────────────────────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_setSniHost(
    mut env: JNIEnv,
    _class: JClass,
    host: JString,
) {
    let h: String = env.get_string(&host).unwrap().into();
    STATE.lock().sni_host = h;
}

#[no_mangle]
pub extern "system" fn Java_com_zzz_vpn_jni_ZzzCore_testConnectivity(
    mut env: JNIEnv,
    _class: JClass,
    host: JString,
    port: jint,
    timeout_ms: jint,
) -> jboolean {
    let h: String = env.get_string(&host).unwrap().into();
    let ok = RT.block_on(async {
        let addr = format!("{h}:{}", port as u16);
        let timeout = std::time::Duration::from_millis(timeout_ms as u64);
        tokio::time::timeout(timeout, tokio::net::TcpStream::connect(&addr))
            .await
            .is_ok()
    });
    if ok { JNI_TRUE } else { JNI_FALSE }
}
