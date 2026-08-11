use nuvio_companion::config::{ApiConfig, AuthConfig, CONFIG_VERSION, ServerConfig};

#[test]
fn defaults_are_private_and_valid() {
    let config = ServerConfig::default();
    assert_eq!(config.version, CONFIG_VERSION);
    assert_eq!(config.network.interface, "tailscale0");
    assert!(config.network.bind_ip.is_none());
    assert!(config.auth.token_env.is_none());
    config.validate().unwrap();
}

#[test]
fn deserialization_rejects_unknown_and_future_fields() {
    let error = toml::from_str::<ServerConfig>(
        r#"
            version = 1
            unexpected = true
        "#,
    )
    .unwrap_err();
    assert!(error.to_string().contains("unknown field"));
}

#[test]
fn validates_configuration_version_and_request_limits() {
    let unsupported = ServerConfig {
        version: CONFIG_VERSION + 1,
        ..ServerConfig::default()
    };
    assert!(unsupported.validate().is_err());

    let invalid_limit = ServerConfig {
        api: ApiConfig {
            max_request_body_bytes: 0,
            ..ApiConfig::default()
        },
        ..ServerConfig::default()
    };
    assert!(invalid_limit.validate().is_err());

    let blank_token_env = ServerConfig {
        auth: AuthConfig {
            token_env: Some("   ".to_owned()),
        },
        ..ServerConfig::default()
    };
    assert!(blank_token_env.validate().is_err());
}
