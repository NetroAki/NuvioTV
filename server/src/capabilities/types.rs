use serde::Serialize;

use crate::network::ResolvedBind;

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct ServerCapabilities {
    pub api_versions: Vec<u32>,
    pub transport: TransportCapabilities,
    pub system: SystemCapabilities,
    pub media: MediaCapabilities,
}

impl ServerCapabilities {
    pub async fn discover(bind: &ResolvedBind) -> Self {
        super::discovery::discover(bind).await
    }
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct TransportCapabilities {
    pub interface: String,
    pub address_family: String,
    pub tailnet_only: bool,
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct SystemCapabilities {
    pub logical_cpus: usize,
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct MediaCapabilities {
    pub ffmpeg_available: bool,
    pub ffprobe_available: bool,
    pub hardware_accelerators: Vec<String>,
}
