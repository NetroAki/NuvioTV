use axum::{Json, extract::State, http::StatusCode};

use crate::{
    addons::{ManifestBatchRequest, ManifestRequestError},
    capabilities::ServerCapabilities,
    diagnostics::DiagnosticsSnapshot,
};

use super::{
    AppState,
    models::{ApiEnvelope, ErrorResponse, HealthResponse},
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

pub async fn addon_manifests(
    State(state): State<AppState>,
    Json(request): Json<ManifestBatchRequest>,
) -> Result<
    Json<ApiEnvelope<crate::addons::ManifestBatchResponse>>,
    (StatusCode, Json<ErrorResponse>),
> {
    if let Err(error) = request.validate(state.max_addons_per_request) {
        let response = match error {
            ManifestRequestError::Empty => ErrorResponse {
                code: "empty_addon_batch",
                message: "At least one addon URL is required",
            },
            ManifestRequestError::TooMany => ErrorResponse {
                code: "addon_batch_too_large",
                message: "The addon batch exceeds the configured limit",
            },
        };
        return Err((StatusCode::BAD_REQUEST, Json(response)));
    }
    let response = state
        .addon_manifests
        .resolve_batch(request.addon_urls, request.allow_stale)
        .await;
    Ok(Json(ApiEnvelope::new(response)))
}
