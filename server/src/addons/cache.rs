use std::{
    fs,
    path::Path,
    sync::{Arc, Mutex},
};

use rusqlite::{Connection, OptionalExtension, params};
use thiserror::Error;

#[derive(Clone)]
pub struct ManifestCache {
    connection: Arc<Mutex<Connection>>,
}

impl ManifestCache {
    pub fn open(path: &Path) -> Result<Self, ManifestCacheError> {
        if let Some(parent) = path
            .parent()
            .filter(|parent| !parent.as_os_str().is_empty())
        {
            fs::create_dir_all(parent).map_err(ManifestCacheError::CreateDirectory)?;
        }
        let connection = Connection::open(path)?;
        connection.execute_batch(
            "PRAGMA journal_mode = WAL;
             PRAGMA synchronous = NORMAL;
             CREATE TABLE IF NOT EXISTS addon_manifests (
                 cache_key TEXT PRIMARY KEY NOT NULL,
                 body BLOB NOT NULL,
                 fetched_at INTEGER NOT NULL,
                 expires_at INTEGER NOT NULL
             );",
        )?;
        Ok(Self {
            connection: Arc::new(Mutex::new(connection)),
        })
    }

    pub fn get(
        &self,
        cache_key: &str,
        now_epoch_seconds: i64,
    ) -> Result<Option<CachedManifest>, ManifestCacheError> {
        let connection = self
            .connection
            .lock()
            .map_err(|_| ManifestCacheError::Poisoned)?;
        connection
            .query_row(
                "SELECT body, fetched_at, expires_at FROM addon_manifests WHERE cache_key = ?1",
                [cache_key],
                |row| {
                    let expires_at: i64 = row.get(2)?;
                    Ok(CachedManifest {
                        body: row.get(0)?,
                        fetched_at: row.get(1)?,
                        fresh: expires_at > now_epoch_seconds,
                    })
                },
            )
            .optional()
            .map_err(ManifestCacheError::from)
    }

    pub fn put(
        &self,
        cache_key: &str,
        body: &[u8],
        fetched_at: i64,
        expires_at: i64,
    ) -> Result<(), ManifestCacheError> {
        let connection = self
            .connection
            .lock()
            .map_err(|_| ManifestCacheError::Poisoned)?;
        connection.execute(
            "INSERT INTO addon_manifests (cache_key, body, fetched_at, expires_at)
             VALUES (?1, ?2, ?3, ?4)
             ON CONFLICT(cache_key) DO UPDATE SET
                 body = excluded.body,
                 fetched_at = excluded.fetched_at,
                 expires_at = excluded.expires_at",
            params![cache_key, body, fetched_at, expires_at],
        )?;
        Ok(())
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct CachedManifest {
    pub body: Vec<u8>,
    pub fetched_at: i64,
    pub fresh: bool,
}

#[derive(Debug, Error)]
pub enum ManifestCacheError {
    #[error("failed to create manifest cache directory: {0}")]
    CreateDirectory(std::io::Error),
    #[error("manifest cache database error: {0}")]
    Sql(#[from] rusqlite::Error),
    #[error("manifest cache lock is poisoned")]
    Poisoned,
}

#[cfg(test)]
mod tests;
