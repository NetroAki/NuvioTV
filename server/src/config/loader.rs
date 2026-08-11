use std::{fs, path::Path};

use thiserror::Error;

use super::ServerConfig;

impl ServerConfig {
    pub fn load(path: Option<&Path>) -> Result<Self, ConfigError> {
        let config = match path {
            Some(path) => {
                let raw = fs::read_to_string(path).map_err(|source| ConfigError::Read {
                    path: path.display().to_string(),
                    source,
                })?;
                toml::from_str(&raw).map_err(ConfigError::Parse)?
            }
            None => Self::default(),
        };
        config.validate()?;
        Ok(config)
    }
}

#[derive(Debug, Error)]
pub enum ConfigError {
    #[error("failed to read configuration {path}: {source}")]
    Read {
        path: String,
        #[source]
        source: std::io::Error,
    },
    #[error("invalid TOML configuration: {0}")]
    Parse(#[from] toml::de::Error),
    #[error("unsupported configuration version {actual}; this build supports {supported}")]
    UnsupportedVersion { actual: u32, supported: u32 },
    #[error("network.interface must not be blank")]
    MissingInterface,
    #[error("api.max_request_body_bytes must be greater than zero")]
    InvalidRequestLimit,
    #[error("api.request_timeout_seconds must be greater than zero")]
    InvalidRequestTimeout,
    #[error("addons.cache_path must not be empty")]
    InvalidAddonCachePath,
    #[error("addons.manifest_ttl_seconds must be greater than zero")]
    InvalidManifestTtl,
    #[error("addons.max_manifest_bytes must be greater than zero")]
    InvalidManifestLimit,
    #[error("addons.max_addons_per_request must be between 1 and 128")]
    InvalidAddonBatchLimit,
    #[error("auth.token_env must not be blank")]
    InvalidTokenEnvironment,
    #[error("configured token environment variable {variable} is not set")]
    MissingTokenEnvironment { variable: String },
    #[error("configured token environment variable {variable} is empty")]
    EmptyTokenEnvironment { variable: String },
}
