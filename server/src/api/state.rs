use std::sync::Arc;

use crate::{
    addons::AddonManifestResolver, capabilities::ServerCapabilities, diagnostics::Diagnostics,
};

#[derive(Clone)]
pub struct AppState {
    pub capabilities: Arc<ServerCapabilities>,
    pub diagnostics: Arc<Diagnostics>,
    pub addon_manifests: Arc<AddonManifestResolver>,
    pub max_addons_per_request: usize,
}
