pub mod api;
pub mod capabilities;
pub mod config;
pub mod diagnostics;
pub mod network;

use std::sync::Arc;

use api::AppState;
use capabilities::ServerCapabilities;
use config::ServerConfig;
use diagnostics::Diagnostics;

pub async fn build_app(
    config: &ServerConfig,
    bind: &network::ResolvedBind,
) -> Result<axum::Router, config::ConfigError> {
    let capabilities = ServerCapabilities::discover(bind).await;
    let state = AppState {
        capabilities: Arc::new(capabilities),
        diagnostics: Arc::new(Diagnostics::new()),
    };
    api::router(config, state)
}
