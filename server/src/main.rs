use std::{env, path::PathBuf};

use nuvio_companion::{build_app, config::ServerConfig, network::resolve_bind};
use tokio::{net::TcpListener, signal};
use tracing::info;
use tracing_subscriber::EnvFilter;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .with_target(false)
        .compact()
        .init();

    let config_path = env::var_os("NUVIO_SERVER_CONFIG").map(PathBuf::from);
    let config = ServerConfig::load(config_path.as_deref())?;
    let bind = resolve_bind(&config.network)?;
    let app = build_app(&config, &bind).await?;
    let listener = TcpListener::bind(bind.socket_addr).await?;

    info!(
        interface = %bind.interface,
        address = %bind.socket_addr,
        "Nuvio companion listening on the tailnet"
    );
    axum::serve(listener, app)
        .with_graceful_shutdown(shutdown_signal())
        .await?;
    Ok(())
}

async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C signal handler");
    };
    #[cfg(unix)]
    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("failed to install SIGTERM signal handler")
            .recv()
            .await;
    };
    #[cfg(not(unix))]
    let terminate = std::future::pending::<()>();

    tokio::select! {
        () = ctrl_c => {},
        () = terminate => {},
    }
}
