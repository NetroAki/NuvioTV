# Stock NuvioTV baseline

Date: 2026-08-11

## Source

- Upstream: `NuvioMedia/NuvioTV`
- Upstream branch: `dev`
- Baseline revision: `f6e504868765aa6e5657c383723bdd2a9e6ebf7b`
- Fork: `NetroAki/NuvioTV`
- Implementation branch: `feat/server-assisted-foundation`
- App version: `0.8.3-beta` (`versionCode` 1044)

The application source was not changed before collecting this baseline. Local build setup used Android SDK `/opt/android-sdk`, JDK 17, and a standard Android debug keystore because upstream config signs the debug variant through its release signing block.

## Build environment

- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Kotlin 2.3.0
- compile/target SDK 36
- min SDK 24
- Host default JDK 26.0.1 is incompatible with this Gradle/AGP combination; JDK 17 works.
- No Android TV or Chromecast target was attached through ADB.

## Stock build results

### Debug

`./gradlew :app:assembleFullDebug`

Source compilation and packaging succeeded. Generated ARM64 APK:

- `app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk`
- size: 90,593,109 bytes
- package: `com.nuviodebug.com`
- signed with a local Android debug certificate

The universal debug APK is 237,490,721 bytes.

### Benchmark/profile build

`./gradlew :app:assembleFullBenchmark`

Succeeded with R8/resource shrinking. Generated ARM64 APK:

- `app/build/outputs/apk/full/benchmark/app-full-arm64-v8a-benchmark.apk`
- size: 70,388,287 bytes

The project already contains an Android Macrobenchmark/Baseline Profile module. Its scenario covers launch, onboarding, horizontal/vertical D-pad navigation, detail opening, and back navigation.

### Baseline profile warning

D8 reports thousands of missing startup-profile classes/methods against the current dependency graph and cannot keep all requested startup classes in the primary dex. The profile should be regenerated on a representative device before treating it as current performance evidence.

## Stock test results

The untouched unit suite initially did not compile because test fixtures had drifted behind production APIs:

- four fixtures returned `Flow<TmdbSettings>` where production now requires `StateFlow<TmdbSettings>`
- `SearchViewModelConcurrencyTest` omitted the newer `MetaRepository` dependency

Those fixture-only compile breaks were repaired after the untouched baseline was recorded. The full suite then ran:

- 807 tests executed
- 794 passed
- 12 failed
- 1 skipped

Existing failures at the baseline revision:

1. `LocalhostZeroCopyDataSourceTest.testHttpError404`
2. two `DolbyVisionBaseLayerPolicyTest` cases
3. `FrameRateUtilsMkvSparseTracksTest`
4. `MatroskaAfrProbeTest`
5. `CollectionsDataStoreSourceMigrationTest`
6. `TraktAuthServiceTest`
7. `SimklMutationReconciliationTest`
8. `ContinueWatchingAiringRulesTest`
9. three `NuvioExoPlayerPerformanceHelperTest` cases

Report: `app/build/reports/tests/testFullDebugUnitTest/index.html`

These are baseline defects, not regressions introduced by the server-assisted work. Relevant failures must be repaired or explicitly isolated before modifying their behavior.

## Static architecture observations

- Single large Android app module plus `baselineprofile` and a local FFmpeg decoder module.
- Kotlin/Compose UI remains the product UI; 670 `@Composable` declarations were counted.
- Main package distribution: 280 UI files, 145 data files, 144 core files, and 48 domain files.
- Hilt binds repository interfaces to singleton implementations.
- OkHttp/Retrofit handles addon and third-party APIs; DataStore is the main local persistence mechanism.
- `StartupSyncService` already performs concurrent, asynchronous account/profile/addon/library/watch-state reconciliation.
- Home Continue Watching already restores a disk snapshot before live reconciliation.
- Coil already has memory/disk caching and disables crossfade.
- `MainActivity` already enables JankStats, but only logs janky frames; no baseline aggregation/export exists.
- Several major files are very large. This is a maintainability and recomposition-risk signal, not proof of runtime cost.

## Measurement limits

No Android TV/Chromecast was available over ADB, so the following are **not measured yet**:

- launch-to-interactive
- D-pad-to-visible-response latency
- frame/jank distributions
- Compose recomposition counts
- image decode cost on target hardware
- Play-to-first-frame
- seek latency, dropped frames, or decoder behavior

A phone is visible on the tailnet, but it is not an acceptable substitute for weak Android TV performance evidence and was not modified.

No UI performance optimization will be called successful until before/after metrics are collected on a representative Android TV target.
