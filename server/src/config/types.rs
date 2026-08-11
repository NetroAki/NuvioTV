use serde::{Deserialize, Serialize};

use super::ConfigError;

pub const CONFIG_VERSION: u32 = 1;
const DEFAULT_PORT: u16 = 8765;
const DEFAULT_REQUEST_LIMIT_BYTES: usize = 1024 * 1024;
const DEFAULT_REQUEST_TIMEOUT_SECONDS: u64 = 15;
const DEFAULT_MANIFEST_TTL_SECONDS: u64 = 6 * 60 * 60;
const DEFAULT_MAX_MANIFEST_BYTES: usize = 256 * 1024;
const DEFAULT_MAX_ADDONS_PER_REQUEST: usize = 32;

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
    #[serde(default)]
    pub addons: AddonsConfig,
}

impl Default for ServerConfig {
    fn default() -> Self {
        Self {
            version: CONFIG_VERSION,
            network: NetworkConfig::default(),
            api: ApiConfig::default(),
            auth: AuthConfig::default(),
            addons: AddonsConfig::default(),
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
        if self.addons.cache_path.as_os_str().is_empty() {
            return Err(ConfigError::InvalidAddonCachePath);
        }
        if self.addons.manifest_ttl_seconds == 0 {
            return Err(ConfigError::InvalidManifestTtl);
        }
        if self.addons.max_manifest_bytes == 0 {
            return Err(ConfigError::InvalidManifestLimit);
        }
        if !(1..=128).contains(&self.addons.max_addons_per_request) {
            return Err(ConfigError::InvalidAddonBatchLimit);
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

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
#[serde(deny_unknown_fields)]
pub struct AddonsConfig {
    #[serde(default = "default_addon_cache_path")]
    pub cache_path: std::path::PathBuf,
    #[serde(default = "default_manifest_ttl")]
    pub manifest_ttl_seconds: u64,
    #[serde(default = "default_manifest_limit")]
    pub max_manifest_bytes: usize,
    #[serde(default = "default_addon_batch_limit")]
    pub max_addons_per_request: usize,
}

impl Default for AddonsConfig {
    fn default() -> Self {
        Self {
            cache_path: default_addon_cache_path(),
            manifest_ttl_seconds: DEFAULT_MANIFEST_TTL_SECONDS,
            max_manifest_bytes: DEFAULT_MAX_MANIFEST_BYTES,
            max_addons_per_request: DEFAULT_MAX_ADDONS_PER_REQUEST,
        }
    }
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

fn default_addon_cache_path() -> std::path::PathBuf {
    "data/addon-cache.sqlite3".into()
}

const fn default_manifest_ttl() -> u64 {
    DEFAULT_MANIFEST_TTL_SECONDS
}

const fn default_manifest_limit() -> usize {
    DEFAULT_MAX_MANIFEST_BYTES
}

const fn default_addon_batch_limit() -> usize {
    DEFAULT_MAX_ADDONS_PER_REQUEST
}
