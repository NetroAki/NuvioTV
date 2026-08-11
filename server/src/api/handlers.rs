use axum::{Json, extract::State};

use crate::{capabilities::ServerCapabilities, diagnostics::DiagnosticsSnapshot};

use super::{
    AppState,
    models::{ApiEnvelope, HealthResponse},
};

pub async fn health() -> Json<ApiEnvelope<HealthResponse>> {
    Json(ApiEnvelope::new(HealthResponse {
        status: "ok",
        server_version: env!("CARGO_PKG_VERSION"),
    }))
}

pub async fn capabilities(State(state): State<AppState>) -> Json<ApiEnvelope<ServerCapabilities>> {
    Json(ApiEnvelope::new(state.capabilities.as_ref().clone()))
}

pub async fn diagnostics(State(state): State<AppState>) -> Json<ApiEnvelope<DiagnosticsSnapshot>> {
    Json(ApiEnvelope::new(state.diagnostics.snapshot()))
}
