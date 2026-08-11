# Server-assisted NuvioTV architecture

## Purpose

This document maps the current Android client at upstream revision `f6e5048` and defines the integration seams for a private Rust companion server. It is deliberately incremental: existing Android behavior stays in place until a measured server-assisted path is proven faster and reliable.

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
- The Android client currently performs this fan-out itself; slow-addon isolation exists in parts of the ViewModel layer but there is no shared server-side circuit breaker/cache.

### Account state

Current sync is Nuvio account/Supabase oriented, with separate Trakt and Simkl integrations. There is no `StremioSyncProvider` abstraction for importing an existing Stremio account as specified. The new provider must coexist with current providers rather than replacing them.

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

## Server integration boundary

The Android app should depend on one narrow interface rather than Rust internals:

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

## Planned Rust modules

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

The first server milestone will implement only configuration, Tailscale-only binding, health/capability discovery, diagnostics, and tested wire contracts. Feature modules will be added as vertical slices rather than empty placeholder layers.

## Network and trust boundary

- Server bind defaults must resolve the configured Tailscale interface; wildcard bind is rejected.
- Explicit Tailscale IP and MagicDNS client endpoints are configuration, never compile-time constants.
- HTTP is acceptable inside Tailscale because WireGuard supplies transport encryption. HTTPS remains optional defense in depth.
- Tailscale ACLs are the primary authorization boundary. Optional application tokens may be enabled without becoming mandatory account infrastructure.
- Every request is size-limited and validated.
- Addon configuration URLs are secrets: normal logs must show only redacted origin/addon identity.

### Existing issues to remove during integration

- The shared Android OkHttp client currently trusts every TLS certificate and hostname. The server gateway must not inherit this behavior, and the global client should be repaired under focused regression coverage.
- Addon and catalog logging currently includes complete configured URLs, which may reveal embedded credentials. Logging must use a centralized redactor.

## Data freshness model

```text
Android memory cache
        ↓
Android disk snapshot
        ↓
Rust memory cache
        ↓
Rust SQLite/content cache
        ↓
upstream provider
```

Cache policy is per data class. Home/library/catalog/metadata use stale-while-revalidate; playback plans and probes use media identity/capability hashes; subtitle/font/segment results use content hashes. No universal TTL is planned.

## Performance gates

A UI optimization is retained only with target-device before/after evidence. Server paths have independent measurable gates:

- cache hit latency
- server RTT
- provider fan-out latency and timeout isolation
- manifest/playback preparation latency
- image output size/decode dimensions
- time to first useful cached payload

Until a representative Android TV target is attached, work can proceed on server correctness, deterministic planner tests, cache benchmarks, and instrumentation, but no claim of improved Chromecast frame performance will be made.
