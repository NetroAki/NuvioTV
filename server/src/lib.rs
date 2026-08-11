pub mod addons;
pub mod api;
pub mod capabilities;
pub mod config;
pub mod diagnostics;
pub mod network;

use std::sync::Arc;

use addons::{AddonManifestResolver, AddonManifestResolverError};
use api::AppState;
use capabilities::ServerCapabilities;
use config::ServerConfig;
use diagnostics::Diagnostics;

pub async fn build_app(
    config: &ServerConfig,
    bind: &network::ResolvedBind,
) -> Result<axum::Router, BuildAppError> {
    let capabilities = ServerCapabilities::discover(bind).await;
    let addon_manifests = AddonManifestResolver::open(
        &config.addons.cache_path,
        config.addons.manifest_ttl_seconds,
        config.addons.max_manifest_bytes,
    )?;
    let state = AppState {
        capabilities: Arc::new(capabilities),
        diagnostics: Arc::new(Diagnostics::new()),
        addon_manifests: Arc::new(addon_manifests),
        max_addons_per_request: config.addons.max_addons_per_request,
    };
    Ok(api::router(config, state)?)
}

#[derive(Debug, thiserror::Error)]
pub enum BuildAppError {
    #[error(transparent)]
    Config(#[from] config::ConfigError),
    #[error(transparent)]
    AddonManifest(#[from] AddonManifestResolverError),
}
