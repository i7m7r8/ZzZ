use anyhow::{anyhow, Result};
use nix::sys::signal::{self, Signal};
use nix::unistd::Pid;
use std::path::Path;
use std::process::{Command, Stdio};
use std::time::Duration;

/// Start the `tor` binary from the app's native lib directory.
/// Returns the child PID on success.
pub fn start_tor(torrc_path: &str, data_dir: &str, native_lib_dir: &str) -> Result<Pid> {
    let tor_bin = Path::new(native_lib_dir).join("libtor.so");

    if !tor_bin.exists() {
        return Err(anyhow!("tor binary not found at {}", tor_bin.display()));
    }

    // Copy geoip files from assets if not present
    let geoip = Path::new(data_dir).join("geoip");
    let geoip6 = Path::new(data_dir).join("geoip6");

    let child = Command::new(&tor_bin)
        .arg("-f")
        .arg(torrc_path)
        .env("HOME", data_dir)
        .env("LD_LIBRARY_PATH", native_lib_dir)
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|e| anyhow!("Failed to spawn tor: {e}"))?;

    let pid = Pid::from_raw(child.id() as i32);

    // Give Tor a moment to start and verify it didn't immediately exit
    std::thread::sleep(Duration::from_millis(500));

    // Check process is still running
    match nix::sys::signal::kill(pid, None) {
        Ok(_) => {
            log::info!("Tor started with PID {pid}");
            Ok(pid)
        }
        Err(_) => Err(anyhow!("Tor exited immediately")),
    }
}

/// Send SIGTERM to the Tor process, then SIGKILL if it doesn't exit.
pub fn stop_tor(pid: Pid) {
    log::info!("Stopping Tor PID {pid}");
    let _ = signal::kill(pid, Signal::SIGTERM);
    std::thread::sleep(Duration::from_millis(1500));
    // Force kill if still running
    if nix::sys::signal::kill(pid, None).is_ok() {
        let _ = signal::kill(pid, Signal::SIGKILL);
    }
}
