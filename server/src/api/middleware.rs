use std::sync::Arc;

use axum::{
    Json,
    extract::{Request, State},
    http::{StatusCode, header::AUTHORIZATION},
    middleware::Next,
    response::{IntoResponse, Response},
};

use crate::{config::AuthToken, diagnostics::Diagnostics};

use super::models::ErrorResponse;

pub async fn authorize(State(token): State<AuthToken>, request: Request, next: Next) -> Response {
    let candidate = request
        .headers()
        .get(AUTHORIZATION)
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.strip_prefix("Bearer "));
    if candidate.is_some_and(|candidate| token.matches(candidate)) {
        return next.run(request).await;
    }
    (
        StatusCode::UNAUTHORIZED,
        Json(ErrorResponse {
            code: "unauthorized",
            message: "a valid bearer token is required",
        }),
    )
        .into_response()
}

pub async fn observe(
    State(diagnostics): State<Arc<Diagnostics>>,
    request: Request,
    next: Next,
) -> Response {
    let started = diagnostics.begin_request();
    let response = next.run(request).await;
    diagnostics.finish_request(started, response.status().is_success());
    response
}
