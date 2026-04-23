use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use anyhow::Result;

#[derive(Serialize, Deserialize, Clone)]
pub struct VpnConfig {
    pub fake_sni: String,
    pub enable_tor: bool,
    pub enable_i2p: bool,
    pub enable_dnscrypt: bool,
    pub dns_server: String,
}

impl Default for VpnConfig {
    fn default() -> Self {
        Self {
            fake_sni: String::from("www.google.com"),
            enable_tor: true,
            enable_i2p: true,
            enable_dnscrypt: true,
            dns_server: String::from("1.1.1.1"),
        }
    }
}

pub struct ConfigManager {
    path: PathBuf,
}

impl ConfigManager {
    pub fn new(path: PathBuf) -> Self {
        Self { path }
    }

    pub fn load(&self) -> Result<VpnConfig> {
        if !self.path.exists() {
            let config = VpnConfig::default();
            self.save(&config)?;
            return Ok(config);
        }
        let content = fs::read_to_string(&self.path)?;
        let config = serde_json::from_str(&content)?;
        Ok(config)
    }

    pub fn save(&self, config: &VpnConfig) -> Result<()> {
        let content = serde_json::to_string_pretty(config)?;
        fs::write(&self.path, content)?;
        Ok(())
    }
}
