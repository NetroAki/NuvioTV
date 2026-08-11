use std::sync::Arc;

use crate::{capabilities::ServerCapabilities, diagnostics::Diagnostics};

#[derive(Clone)]
pub struct AppState {
    pub capabilities: Arc<ServerCapabilities>,
    pub diagnostics: Arc<Diagnostics>,
}
