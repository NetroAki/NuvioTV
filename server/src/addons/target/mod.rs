mod policy;

use std::net::{IpAddr, SocketAddr};

use reqwest::Url;
use sha2::{Digest, Sha256};
use thiserror::Error;
use tokio::net::lookup_host;

use policy::is_public_ip;

pub struct ManifestTarget {
    pub url: Url,
    pub host: String,
    pub address: SocketAddr,
    pub cache_key: String,
}

pub async fn prepare_manifest_target(raw: &str) -> Result<ManifestTarget, TargetError> {
    let mut url = Url::parse(raw.trim()).map_err(|_| TargetError::InvalidUrl)?;
    if url.scheme() != "http" && url.scheme() != "https" {
        return Err(TargetError::UnsupportedScheme);
    }
    if !url.username().is_empty() || url.password().is_some() || url.fragment().is_some() {
        return Err(TargetError::InvalidUrl);
    }
    let host = url.host_str().ok_or(TargetError::InvalidUrl)?.to_owned();
    let port = url.port_or_known_default().ok_or(TargetError::InvalidUrl)?;

    if !url.path().to_ascii_lowercase().ends_with("/manifest.json") {
        let path = format!("{}/manifest.json", url.path().trim_end_matches('/'));
        url.set_path(&path);
    }

    let address = resolve_public_address(&host, port).await?;
    let cache_key = format!("{:x}", Sha256::digest(url.as_str().as_bytes()));
    Ok(ManifestTarget {
        url,
        host,
        address,
        cache_key,
    })
}

async fn resolve_public_address(host: &str, port: u16) -> Result<SocketAddr, TargetError> {
    if let Ok(ip) = host.parse::<IpAddr>() {
        return is_public_ip(ip)
            .then_some(SocketAddr::new(ip, port))
            .ok_or(TargetError::PrivateAddress);
    }
    lookup_host((host, port))
        .await
        .map_err(|_| TargetError::Dns)?
        .find(|address| is_public_ip(address.ip()))
        .ok_or(TargetError::PrivateAddress)
}

#[derive(Debug, Error)]
pub enum TargetError {
    #[error("invalid addon URL")]
    InvalidUrl,
    #[error("addon URL must use HTTP or HTTPS")]
    UnsupportedScheme,
    #[error("addon host resolves to a non-public address")]
    PrivateAddress,
    #[error("addon host could not be resolved")]
    Dns,
}
