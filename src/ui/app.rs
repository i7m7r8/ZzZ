use iced::widget::{button, column, text, container, text_input, checkbox, scrollable};
use iced::{Alignment, Element, Length, Sandbox, Settings};
use crate::core::config_manager::VpnConfig;

pub struct ZzZVpnApp {
    is_running: bool,
    status: String,
    config: VpnConfig,
}

#[derive(Debug, Clone)]
pub enum Message {
    ToggleVpn,
    SniChanged(String),
    TorToggled(bool),
    I2pToggled(bool),
    DnsCryptToggled(bool),
}

impl Sandbox for ZzZVpnApp {
    type Message = Message;

    fn new() -> Self {
        Self {
            is_running: false,
            status: String::from("Ready to connect"),
            config: VpnConfig::default(),
        }
    }

    fn title(&self) -> String {
        String::from("ZzZ VPN - Secure Gateway")
    }

    fn update(&mut self, message: Message) {
        match message {
            Message::ToggleVpn => {
                self.is_running = !self.is_running;
                self.status = if self.is_running {
                    format!("Connected via SNI: {}", self.config.fake_sni)
                } else {
                    String::from("Disconnected")
                };
            }
            Message::SniChanged(sni) => {
                self.config.fake_sni = sni;
            }
            Message::TorToggled(val) => {
                self.config.enable_tor = val;
            }
            Message::I2pToggled(val) => {
                self.config.enable_i2p = val;
            }
            Message::DnsCryptToggled(val) => {
                self.config.enable_dnscrypt = val;
            }
        }
    }

    fn view(&self) -> Element<Message> {
        let controls = column![
            text("ZzZ VPN").size(40).style(iced::Color::from_rgb(0.0, 0.5, 1.0)),
            text(&self.status).size(18),
            
            text("Spoof SNI Hostname:").size(16),
            text_input("e.g. www.google.com", &self.config.fake_sni)
                .on_input(Message::SniChanged)
                .padding(10),
            
            checkbox("Enable Tor Network", self.config.enable_tor)
                .on_toggle(Message::TorToggled),
            
            checkbox("Enable I2P Network", self.config.enable_i2p)
                .on_toggle(Message::I2pToggled),
            
            checkbox("Enable DNSCrypt", self.config.enable_dnscrypt)
                .on_toggle(Message::DnsCryptToggled),

            button(if self.is_running { "DISCONNECT" } else { "CONNECT" })
                .on_press(Message::ToggleVpn)
                .width(Length::Fill)
                .padding(15)
                .style(if self.is_running { iced::theme::Button::Destructive } else { iced::theme::Button::Primary }),
        ]
        .spacing(20)
        .max_width(400);

        container(scrollable(controls))
            .width(Length::Fill)
            .height(Length::Fill)
            .center_x()
            .center_y()
            .padding(40)
            .into()
    }
}
