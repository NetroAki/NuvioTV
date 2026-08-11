use std::time::Duration;

use reqwest::redirect::Policy;

use super::target::ManifestTarget;

#[derive(Clone)]
pub struct ManifestFetcher {
    max_bytes: usize,
}

impl ManifestFetcher {
    pub const fn new(max_bytes: usize) -> Self {
        Self { max_bytes }
    }

    pub async fn fetch(&self, target: &ManifestTarget) -> Result<Vec<u8>, ManifestFetchError> {
        let client = reqwest::Client::builder()
            .redirect(Policy::none())
            .no_proxy()
            .resolve(&target.host, target.address)
            .timeout(Duration::from_secs(10))
            .build()
            .map_err(|_| ManifestFetchError::Network)?;
        let mut response = client
            .get(target.url.clone())
            .send()
            .await
            .map_err(|_| ManifestFetchError::Network)?;
        if !response.status().is_success() {
            return Err(ManifestFetchError::UpstreamStatus);
        }
        if response
            .content_length()
            .is_some_and(|length| length > self.max_bytes as u64)
        {
            return Err(ManifestFetchError::TooLarge);
        }

        let mut body = Vec::with_capacity(
            response
                .content_length()
                .unwrap_or_default()
                .min(self.max_bytes as u64) as usize,
        );
        while let Some(chunk) = response
            .chunk()
            .await
            .map_err(|_| ManifestFetchError::Network)?
        {
            if body.len().saturating_add(chunk.len()) > self.max_bytes {
                return Err(ManifestFetchError::TooLarge);
            }
            body.extend_from_slice(&chunk);
        }
        validate_manifest(&body)?;
        Ok(body)
    }
}

pub fn parse_manifest(body: &[u8]) -> Result<serde_json::Value, ManifestFetchError> {
    let value: serde_json::Value =
        serde_json::from_slice(body).map_err(|_| ManifestFetchError::InvalidManifest)?;
    let object = value
        .as_object()
        .ok_or(ManifestFetchError::InvalidManifest)?;
    for field in ["id", "name", "version"] {
        if !object.get(field).is_some_and(serde_json::Value::is_string) {
            return Err(ManifestFetchError::InvalidManifest);
        }
    }
    Ok(value)
}

fn validate_manifest(body: &[u8]) -> Result<(), ManifestFetchError> {
    parse_manifest(body).map(|_| ())
}

#[derive(Debug, Clone, Copy)]
pub enum ManifestFetchError {
    Network,
    UpstreamStatus,
    TooLarge,
    InvalidManifest,
}

impl ManifestFetchError {
    pub const fn code(self) -> &'static str {
        match self {
            Self::Network => "network_error",
            Self::UpstreamStatus => "upstream_status",
            Self::TooLarge => "manifest_too_large",
            Self::InvalidManifest => "invalid_manifest",
        }
    }

    pub const fn message(self) -> &'static str {
        match self {
            Self::Network => "The addon could not be reached",
            Self::UpstreamStatus => "The addon returned an unsuccessful response",
            Self::TooLarge => "The addon manifest exceeded the configured size limit",
            Self::InvalidManifest => "The addon returned an invalid manifest",
        }
    }
}
