use std::sync::Arc;

use axum::{body::Body, http::Request};
use http_body_util::BodyExt;
use nuvio_companion::{
    api::{AppState, router},
    capabilities::{
        MediaCapabilities, ServerCapabilities, SystemCapabilities, TransportCapabilities,
    },
    config::ServerConfig,
    diagnostics::Diagnostics,
};
use tower::ServiceExt;

fn test_state() -> AppState {
    AppState {
        capabilities: Arc::new(ServerCapabilities {
            api_versions: vec![1],
            transport: TransportCapabilities {
                interface: "tailscale0".to_owned(),
                address_family: "ipv4".to_owned(),
                tailnet_only: true,
            },
            system: SystemCapabilities { logical_cpus: 4 },
            media: MediaCapabilities {
                ffmpeg_available: true,
                ffprobe_available: true,
                hardware_accelerators: vec!["vaapi".to_owned()],
            },
        }),
        diagnostics: Arc::new(Diagnostics::new()),
    }
}

#[tokio::test]
async fn health_and_capabilities_are_versioned() {
    let app = router(&ServerConfig::default(), test_state()).unwrap();
    let health = app
        .clone()
        .oneshot(Request::get("/v1/health").body(Body::empty()).unwrap())
        .await
        .unwrap();
    assert!(health.status().is_success());
    let health_json: serde_json::Value =
        serde_json::from_slice(&health.into_body().collect().await.unwrap().to_bytes()).unwrap();
    assert_eq!(health_json["apiVersion"], 1);
    assert_eq!(health_json["data"]["status"], "ok");

    let capabilities = app
        .oneshot(
            Request::get("/v1/capabilities")
                .body(Body::empty())
                .unwrap(),
        )
        .await
        .unwrap();
    let capabilities_json: serde_json::Value =
        serde_json::from_slice(&capabilities.into_body().collect().await.unwrap().to_bytes())
            .unwrap();
    assert_eq!(capabilities_json["apiVersion"], 1);
    assert_eq!(capabilities_json["data"]["transport"]["tailnetOnly"], true);
    assert_eq!(
        capabilities_json["data"]["media"]["hardwareAccelerators"][0],
        "vaapi"
    );
}
