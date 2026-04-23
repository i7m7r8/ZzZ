mod core;
mod ui;

use iced::{Sandbox, Settings};
use ui::app::ZzZVpnApp;
use crate::core::binary_manager::BinaryManager;
use std::path::PathBuf;

#[tokio::main]
async fn main() -> iced::Result {
    println!("Starting ZzZ VPN...");
    
    // Initialize core components in the background if needed
    // let mut bm = BinaryManager::new(PathBuf::from("./binaries"));
    
    ZzZVpnApp::run(Settings::default())
}
