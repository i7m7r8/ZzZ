use std::process::{Command, Child};
use std::path::PathBuf;
use anyhow::{Result, anyhow};
use sysinfo::{System, ProcessExt, SystemExt};

pub struct BinaryManager {
    tor_process: Option<Child>,
    i2p_process: Option<Child>,
    dnscrypt_process: Option<Child>,
    base_path: PathBuf,
}

impl BinaryManager {
    pub fn new(base_path: PathBuf) -> Self {
        Self {
            tor_process: None,
            i2p_process: None,
            dnscrypt_process: None,
            base_path,
        }
    }

    pub fn start_tor(&mut self) -> Result<()> {
        let path = self.base_path.join("tor");
        let child = Command::new(path)
            .arg("-f")
            .arg(self.base_path.join("torrc"))
            .spawn()?;
        self.tor_process = Some(child);
        Ok(())
    }

    pub fn start_i2p(&mut self) -> Result<()> {
        let path = self.base_path.join("i2pd");
        let child = Command::new(path)
            .arg("--conf")
            .arg(self.base_path.join("i2pd.conf"))
            .spawn()?;
        self.i2p_process = Some(child);
        Ok(())
    }

    pub fn start_dnscrypt(&mut self) -> Result<()> {
        let path = self.base_path.join("dnscrypt-proxy");
        let child = Command::new(path)
            .arg("-config")
            .arg(self.base_path.join("dnscrypt-proxy.toml"))
            .spawn()?;
        self.dnscrypt_process = Some(child);
        Ok(())
    }

    pub fn stop_all(&mut self) {
        if let Some(mut child) = self.tor_process.take() {
            let _ = child.kill();
        }
        if let Some(mut child) = self.i2p_process.take() {
            let _ = child.kill();
        }
        if let Some(mut child) = self.dnscrypt_process.take() {
            let _ = child.kill();
        }
    }

    pub fn is_running(&self) -> bool {
        // Implement process check logic using sysinfo
        true
    }
}
