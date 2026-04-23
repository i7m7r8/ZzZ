use anyhow::{anyhow, Result};
use nix::sys::signal::{self, Signal};
use nix::unistd::Pid;
use std::path::Path;
use std::process::{Command, Stdio};
use std::time::Duration;

/// Start the `dnscrypt-proxy` binary.
pub fn start_dnscrypt(config_path: &str, native_lib_dir: &str) -> Result<Pid> {
    let bin = Path::new(native_lib_dir).join("libdnscrypt-proxy.so");

    if !bin.exists() {
        return Err(anyhow!("dnscrypt-proxy not found at {}", bin.display()));
    }

    let child = Command::new(&bin)
        .arg("-config")
        .arg(config_path)
        .env("LD_LIBRARY_PATH", native_lib_dir)
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|e| anyhow!("Failed to spawn dnscrypt-proxy: {e}"))?;

    let pid = Pid::from_raw(child.id() as i32);
    std::thread::sleep(Duration::from_millis(400));

    match nix::sys::signal::kill(pid, None) {
        Ok(_) => {
            log::info!("dnscrypt-proxy started with PID {pid}");
            Ok(pid)
        }
        Err(_) => Err(anyhow!("dnscrypt-proxy exited immediately")),
    }
}

/// Stop dnscrypt-proxy.
pub fn stop_dnscrypt(pid: Pid) {
    log::info!("Stopping dnscrypt-proxy PID {pid}");
    let _ = signal::kill(pid, Signal::SIGTERM);
    std::thread::sleep(Duration::from_millis(800));
    if nix::sys::signal::kill(pid, None).is_ok() {
        let _ = signal::kill(pid, Signal::SIGKILL);
    }
}
