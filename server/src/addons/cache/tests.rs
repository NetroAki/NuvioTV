use std::time::{SystemTime, UNIX_EPOCH};

use super::ManifestCache;

#[test]
fn persists_manifests_and_tracks_freshness() {
    let nonce = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    let directory = std::env::temp_dir().join(format!(
        "nuvio-manifest-cache-{}-{nonce}",
        std::process::id()
    ));
    let path = directory.join("cache.sqlite3");

    {
        let cache = ManifestCache::open(&path).unwrap();
        cache
            .put("hashed-key", br#"{"id":"one"}"#, 100, 200)
            .unwrap();
        let fresh = cache.get("hashed-key", 150).unwrap().unwrap();
        assert!(fresh.fresh);
        assert_eq!(fresh.fetched_at, 100);
    }

    let reopened = ManifestCache::open(&path).unwrap();
    let stale = reopened.get("hashed-key", 201).unwrap().unwrap();
    assert!(!stale.fresh);
    assert_eq!(stale.body, br#"{"id":"one"}"#);

    drop(reopened);
    std::fs::remove_dir_all(directory).unwrap();
}
