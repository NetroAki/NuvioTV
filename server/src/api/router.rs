use std::time::Duration;

use axum::{Router, http::StatusCode, middleware as axum_middleware, routing::get};
use tower_http::{limit::RequestBodyLimitLayer, timeout::TimeoutLayer};

use crate::config::{ConfigError, ServerConfig};

use super::{AppState, handlers, middleware};

pub fn router(config: &ServerConfig, state: AppState) -> Result<Router, ConfigError> {
    let mut app = Router::new()
        .route("/v1/health", get(handlers::health))
        .route("/v1/capabilities", get(handlers::capabilities))
        .route("/v1/diagnostics", get(handlers::diagnostics))
        .with_state(state.clone())
        .layer(RequestBodyLimitLayer::new(
            config.api.max_request_body_bytes,
        ))
        .layer(TimeoutLayer::with_status_code(
            StatusCode::REQUEST_TIMEOUT,
            Duration::from_secs(config.api.request_timeout_seconds),
        ));

    if let Some(token) = config.auth_token()? {
        app = app.layer(axum_middleware::from_fn_with_state(
            token,
            middleware::authorize,
        ));
    }

    Ok(app.layer(axum_middleware::from_fn_with_state(
        state.diagnostics,
        middleware::observe,
    )))
}
