mod result;

use std::path::Path;

use futures_util::{StreamExt, stream};
use thiserror::Error;

use super::{
    cache::{ManifestCache, ManifestCacheError},
    fetcher::{ManifestFetcher, parse_manifest},
    models::{ManifestBatchResponse, ManifestEntry, ManifestState},
    target::prepare_manifest_target,
};
use result::{epoch_seconds, failed, failed_fetch, failed_target, stale_or_failure, success};

const MAX_CONCURRENT_FETCHES: usize = 8;

#[derive(Clone)]
pub struct AddonManifestResolver {
    cache: ManifestCache,
    fetcher: ManifestFetcher,
    ttl_seconds: u64,
}

impl AddonManifestResolver {
    pub fn open(
        cache_path: &Path,
        ttl_seconds: u64,
        max_manifest_bytes: usize,
    ) -> Result<Self, AddonManifestResolverError> {
        Ok(Self {
            cache: ManifestCache::open(cache_path)?,
            fetcher: ManifestFetcher::new(max_manifest_bytes),
            ttl_seconds,
        })
    }

    pub async fn resolve_batch(
        &self,
        addon_urls: Vec<String>,
        allow_stale: bool,
    ) -> ManifestBatchResponse {
        let mut entries = stream::iter(addon_urls.into_iter().enumerate())
            .map(|(index, url)| {
                let resolver = self.clone();
                async move { (index, resolver.resolve_one(&url, allow_stale).await) }
            })
            .buffer_unordered(MAX_CONCURRENT_FETCHES)
            .collect::<Vec<_>>()
            .await;
        entries.sort_unstable_by_key(|(index, _)| *index);
        ManifestBatchResponse {
            entries: entries.into_iter().map(|(_, entry)| entry).collect(),
        }
    }

    async fn resolve_one(&self, raw_url: &str, allow_stale: bool) -> ManifestEntry {
        let target = match prepare_manifest_target(raw_url).await {
            Ok(target) => target,
            Err(error) => return failed_target(error),
        };
        let now = epoch_seconds();
        let cached = match self.cache.get(&target.cache_key, now) {
            Ok(cached) => cached,
            Err(_) => return failed("cache_error", "The manifest cache is unavailable"),
        };
        if let Some(entry) = cached.as_ref().filter(|entry| entry.fresh)
            && let Ok(manifest) = parse_manifest(&entry.body)
        {
            return success(ManifestState::FreshCache, manifest);
        }

        match self.fetcher.fetch(&target).await {
            Ok(body) => {
                let manifest = match parse_manifest(&body) {
                    Ok(manifest) => manifest,
                    Err(error) => return failed_fetch(error),
                };
                let expires_at = now.saturating_add(self.ttl_seconds.min(i64::MAX as u64) as i64);
                if self
                    .cache
                    .put(&target.cache_key, &body, now, expires_at)
                    .is_err()
                {
                    return failed("cache_error", "The manifest cache is unavailable");
                }
                success(ManifestState::Refreshed, manifest)
            }
            Err(error) if allow_stale => stale_or_failure(cached, error),
            Err(error) => failed_fetch(error),
        }
    }
}

#[derive(Debug, Error)]
pub enum AddonManifestResolverError {
    #[error(transparent)]
    Cache(#[from] ManifestCacheError),
}
