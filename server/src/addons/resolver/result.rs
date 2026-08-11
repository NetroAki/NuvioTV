use std::time::SystemTime;

use crate::addons::{
    cache::CachedManifest,
    fetcher::{ManifestFetchError, parse_manifest},
    models::{ManifestEntry, ManifestFailure, ManifestState},
    target::TargetError,
};

pub(super) fn stale_or_failure(
    cached: Option<CachedManifest>,
    error: ManifestFetchError,
) -> ManifestEntry {
    cached
        .and_then(|cached| parse_manifest(&cached.body).ok())
        .map(|manifest| success(ManifestState::StaleCache, manifest))
        .unwrap_or_else(|| failed_fetch(error))
}

pub(super) fn success(state: ManifestState, manifest: serde_json::Value) -> ManifestEntry {
    ManifestEntry {
        state,
        manifest: Some(manifest),
        failure: None,
    }
}

pub(super) fn failed_target(error: TargetError) -> ManifestEntry {
    match error {
        TargetError::InvalidUrl => failed("invalid_url", "The addon URL is invalid"),
        TargetError::UnsupportedScheme => {
            failed("unsupported_scheme", "The addon URL must use HTTP or HTTPS")
        }
        TargetError::PrivateAddress => {
            failed("private_address", "Private addon targets are not permitted")
        }
        TargetError::Dns => failed("dns_error", "The addon host could not be resolved"),
    }
}

pub(super) fn failed_fetch(error: ManifestFetchError) -> ManifestEntry {
    failed(error.code(), error.message())
}

pub(super) fn failed(code: &'static str, message: &'static str) -> ManifestEntry {
    ManifestEntry {
        state: ManifestState::Failed,
        manifest: None,
        failure: Some(ManifestFailure { code, message }),
    }
}

pub(super) fn epoch_seconds() -> i64 {
    SystemTime::UNIX_EPOCH
        .elapsed()
        .map(|duration| duration.as_secs().min(i64::MAX as u64) as i64)
        .unwrap_or_default()
}
