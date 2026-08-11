use std::sync::Arc;

use axum::{
    body::Body,
    http::{Request, StatusCode},
};
use http_body_util::BodyExt;
use nuvio_companion::{
    addons::AddonManifestResolver,
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
        addon_manifests: Arc::new(
            AddonManifestResolver::open(":memory:".as_ref(), 3600, 256 * 1024).unwrap(),
        ),
        max_addons_per_request: 4,
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

#[tokio::test]
async fn manifest_endpoint_rejects_private_targets_without_echoing_urls() {
    let app = router(&ServerConfig::default(), test_state()).unwrap();
    let private_url = "http://100.105.18.59/config/private-token";
    let request_body = serde_json::json!({
        "addonUrls": [private_url],
        "allowStale": true
    });
    let response = app
        .oneshot(
            Request::post("/v1/addons/manifests")
                .header("content-type", "application/json")
                .body(Body::from(request_body.to_string()))
                .unwrap(),
        )
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);

    let bytes = response.into_body().collect().await.unwrap().to_bytes();
    let body = std::str::from_utf8(&bytes).unwrap();
    assert!(!body.contains(private_url));
    let json: serde_json::Value = serde_json::from_slice(&bytes).unwrap();
    assert_eq!(json["data"]["entries"][0]["state"], "failed");
    assert_eq!(
        json["data"]["entries"][0]["failure"]["code"],
        "private_address"
    );
}
