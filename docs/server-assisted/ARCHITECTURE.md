# Server-assisted NuvioTV architecture

## Purpose

This document maps the Android client and records the boundary of the already-started private companion. Phase 1 is feature and correctness work in Kotlin/Compose/Media3. The companion is frozen unless a Phase-1 feature genuinely requires server processing; broader Rust adoption and offload decisions belong to Phase 2 and require measurements from the actual Chromecast.

## Existing Android architecture

```text
MainActivity / Compose navigation
        │
        ├── screen ViewModels (Hilt)
        │       │
        │       ├── domain repository interfaces
        │       ├── sync coordinators
        │       └── player runtime controllers
        │
        ├── repository implementations
        │       ├── Stremio addon HTTP resources
        │       ├── Nuvio/Supabase profile sync
        │       ├── Trakt / Simkl / TMDB
        │       └── local debrid/plugin paths
        │
        ├── local persistence
        │       ├── profile-scoped DataStore
        │       ├── SharedPreferences manifest cache
        │       ├── image/cache directories
        │       └── in-memory screen caches
        │
        └── Media3 / local FFmpeg / optional MPV playback
```

### Application and startup

- `NuvioApplication` initializes Hilt, optional diagnostics, plugin hooks, Android TV channel sync, locale state, and a singleton Coil loader.
- `MainActivity` owns onboarding/profile routing, top-level navigation, global player launchers, and JankStats.
- `StartupSyncService` runs on a supervised IO scope and reconciles profiles, addons, library, watch state, preferences, credentials, collections, and home-catalog settings without blocking the Compose entry point.
- Home Continue Watching restores disk snapshots before live reconciliation. This is already a stale-while-revalidate pattern and should be retained.

### Addons and catalogs

- `AddonRepository` is generic over Stremio addon manifests.
- `AddonRepositoryImpl` preserves ordered configured URLs, enabled state, user display names, manifest metadata, resources, types, and catalogs.
- Manifest fetches are concurrent and backed by memory plus SharedPreferences persistence.
- `CatalogRepositoryImpl` builds Stremio-compatible catalog resource URLs dynamically from manifest declarations and extras.
- The Android client remains authoritative for this fan-out. A persistent companion manifest cache exists from the earlier foundation milestone, but the Android app does not adopt it during Phase 1 merely as an optimization.

### Account state

Nuvio account/Supabase sync remains intact alongside Trakt and Simkl. A separate Stremio account adapter synchronizes the authenticated user's library, resume progress, watched state, and ordered addon collection with existing profile-scoped stores. Library reconciliation remembers the previous Stremio snapshot so later local and remote removals are not confused with first-time imports. Player pause/stop and watched-history writes update Stremio through the existing tracking dispatch path.

Stremio account calls run directly from Android through a dedicated TLS-verifying OkHttp client. They are intentionally not proxied through the companion: these calls are low-volume, while relaying credentials over another process would expand the secret-handling boundary without a measured benefit. The password is never persisted. The returned Stremio auth key and account email are encrypted together with an Android Keystore AES-GCM key and scoped to the active Nuvio profile.

### Playback

- Media3 is the primary player; local FFmpeg decoder support and MPV paths already exist.
- Display refresh-rate/resolution support is detected dynamically.
- Audio output and decoder-related logic exists, but there is no portable client-capability document sent to a server.
- Stream selection resolves direct URLs, debrid streams, torrents, proxy headers, and addon behavior hints.
- There is no server-authored, versioned `PlaybackManifest` contract and no generic direct/remux/audio-transcode/video-transcode planner.

### Audio and subtitles

- Track preferences persist language/name plus track IDs. Semantic fields exist, but persisted IDs remain part of selection and the preference hierarchy is not yet modeled as episode → series → profile → inference → scorer.
- Media3 handles normal subtitle paths. There is no libass client renderer, embedded-font transfer protocol, structural subtitle classifier, ASS merge engine, or collision layout engine.

### Segments

- `SkipIntroRepository` concurrently queries IntroDB, AniSkip, and AnimeSkip and caches results in memory.
- Its model is provider-specific (`SkipInterval`) and lacks confidence, provider interface abstraction, persistent analysis cache, chapter provider, or fingerprint provider.
- This should be adapted behind a generic `SegmentProvider`; existing working providers should not be discarded.

## Compose compiler baseline

The full benchmark Compose report contains 651 composable functions:

- 565 restartable
- 565 skippable
- 0 restartable-but-non-skippable
- 20 readonly

This means blanket stability annotation work is not evidence-backed. Some parameters remain unstable (ViewModels, navigation controllers, image requests, player objects), but strong skipping already keeps their call sites skippable.

Potentially expensive visual/runtime features exist and require device profiling before removal:

- 48 `AnimatedVisibility` call sites
- 38 `animateFloatAsState` call sites
- 23 `animateDpAsState` call sites
- 10 infinite-repeat animations
- 7 crossfades
- blur/graphics-layer usage
- 60 lifecycle-unaware `collectAsState` calls versus 67 lifecycle-aware calls

These counts identify profiling targets, not proven bottlenecks.

## Deferred Phase-2 server boundary

If profiling later justifies broader server use, the Android app should depend on one narrow interface rather than Rust internals:

```text
ServerGateway
    health()
    capabilities()
    syncSnapshot(...)
    catalogs(...)
    metadata(...)
    preparePlayback(...)
    artwork(...)
    subtitles(...)
    segments(...)
    diagnostics(...)
```

Every response must carry an API version. Client fallback remains the existing direct Nuvio path when the server is unavailable or a feature is unsupported.

## Deferred Rust candidates

```text
server/src/
    api/            versioned Axum routes and wire models
    config/         versioned configuration and validation
    network/        tailscale0/explicit tailnet bind discovery
    diagnostics/    local metrics and decision reasons
    stremio/        Stremio account sync provider
    addons/         manifest/resource orchestration and secret redaction
    catalogs/       normalized catalogs and slow-provider isolation
    metadata/       normalized metadata
    cache/          memory + SQLite persistent cache
    prefetch/       cancellable adaptive work
    images/         dimension-aware artwork variants
    media/          ffprobe/libav discovery
    playback/       capability-driven planner and manifests
    subtitles/      classification, ASS merge, fonts, collision analysis
    segments/       provider interface, chapters, AniSkip, fingerprints
    preferences/    semantic preference hierarchy
```

The foundation and manifest-cache slices already exist and remain isolated. No additional module is implemented from this list until Phase 1 is feature-complete and target-device profiling identifies a concrete bottleneck or a feature requires server-only processing.

## Network and trust boundary

- Server bind defaults must resolve the configured Tailscale interface; wildcard bind is rejected.
- Explicit Tailscale IP and MagicDNS client endpoints are configuration, never compile-time constants.
- HTTP is acceptable inside Tailscale because WireGuard supplies transport encryption. HTTPS remains optional defense in depth.
- Tailscale ACLs are the primary authorization boundary. Optional application tokens may be enabled without becoming mandatory account infrastructure.
- Every request is size-limited and validated.
- Addon configuration URLs are secrets: normal logs must show only redacted origin/addon identity.

### Existing issues to remove during integration

- The shared Android OkHttp client currently trusts every TLS certificate and hostname. Neither the server gateway nor Stremio account adapter inherits this behavior; the global client still needs focused regression coverage before it can be repaired safely.
- Configured addon URLs are treated as secrets. Manifest, catalog, metadata, stream, and subtitle repository logs omit complete URLs so configured paths and query tokens are not exposed.

## Data freshness model

During Phase 1, existing Android memory/disk caches remain in place and upstream requests continue through the current repositories. A server cache or prefetch layer is introduced in Phase 2 only when server execution plus network cost beats local execution in target-device measurements. No universal TTL is planned.

## Performance gates

A UI optimization is retained only with target-device before/after evidence. Server paths have independent measurable gates:

- cache hit latency
- server RTT
- provider fan-out latency and timeout isolation
- manifest/playback preparation latency
- image output size/decode dimensions
- time to first useful cached payload

Until a representative Android TV target is attached, work can proceed on server correctness, deterministic planner tests, cache benchmarks, and instrumentation, but no claim of improved Chromecast frame performance will be made.
