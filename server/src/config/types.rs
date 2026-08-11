use serde::{Deserialize, Serialize};

use super::ConfigError;

pub const CONFIG_VERSION: u32 = 1;
const DEFAULT_PORT: u16 = 8765;
const DEFAULT_REQUEST_LIMIT_BYTES: usize = 1024 * 1024;
const DEFAULT_REQUEST_TIMEOUT_SECONDS: u64 = 15;

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(deny_unknown_fields)]
pub struct ServerConfig {
    pub version: u32,
    #[serde(default)]
    pub network: NetworkConfig,
    #[serde(default)]
    pub api: ApiConfig,
    #[serde(default)]
    pub auth: AuthConfig,
}

impl Default for ServerConfig {
    fn default() -> Self {
        Self {
            version: CONFIG_VERSION,
            network: NetworkConfig::default(),
            api: ApiConfig::default(),
            auth: AuthConfig::default(),
        }
    }
}

impl ServerConfig {
    pub fn validate(&self) -> Result<(), ConfigError> {
        if self.version != CONFIG_VERSION {
            return Err(ConfigError::UnsupportedVersion {
                actual: self.version,
                supported: CONFIG_VERSION,
            });
        }
        if self.network.interface.trim().is_empty() {
            return Err(ConfigError::MissingInterface);
        }
        if self.api.max_request_body_bytes == 0 {
            return Err(ConfigError::InvalidRequestLimit);
        }
        if self.api.request_timeout_seconds == 0 {
            return Err(ConfigError::InvalidRequestTimeout);
        }
        if self
            .auth
            .token_env
            .as_deref()
            .is_some_and(|name| name.trim().is_empty())
        {
            return Err(ConfigError::InvalidTokenEnvironment);
        }
        Ok(())
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(deny_unknown_fields)]
pub struct NetworkConfig {
    #[serde(default = "default_interface")]
    pub interface: String,
    pub bind_ip: Option<std::net::IpAddr>,
    #[serde(default = "default_port")]
    pub port: u16,
}

impl Default for NetworkConfig {
    fn default() -> Self {
        Self {
            interface: default_interface(),
            bind_ip: None,
            port: DEFAULT_PORT,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(deny_unknown_fields)]
pub struct ApiConfig {
    #[serde(default = "default_request_limit")]
    pub max_request_body_bytes: usize,
    #[serde(default = "default_request_timeout")]
    pub request_timeout_seconds: u64,
}

impl Default for ApiConfig {
    fn default() -> Self {
        Self {
            max_request_body_bytes: DEFAULT_REQUEST_LIMIT_BYTES,
            request_timeout_seconds: DEFAULT_REQUEST_TIMEOUT_SECONDS,
        }
    }
}

#[derive(Debug, Default, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(deny_unknown_fields)]
pub struct AuthConfig {
    pub token_env: Option<String>,
}

fn default_interface() -> String {
    "tailscale0".to_owned()
}

const fn default_port() -> u16 {
    DEFAULT_PORT
}

const fn default_request_limit() -> usize {
    DEFAULT_REQUEST_LIMIT_BYTES
}

const fn default_request_timeout() -> u64 {
    DEFAULT_REQUEST_TIMEOUT_SECONDS
}
