use serde::Serialize;

pub const API_VERSION: u32 = 1;

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct ApiEnvelope<T> {
    pub api_version: u32,
    pub data: T,
}

impl<T> ApiEnvelope<T> {
    pub const fn new(data: T) -> Self {
        Self {
            api_version: API_VERSION,
            data,
        }
    }
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct HealthResponse {
    pub status: &'static str,
    pub server_version: &'static str,
}

#[derive(Debug, Clone, Serialize, PartialEq, Eq)]
#[serde(rename_all = "camelCase")]
pub struct ErrorResponse {
    pub code: &'static str,
    pub message: &'static str,
}
