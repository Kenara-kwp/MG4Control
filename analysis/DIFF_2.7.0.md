# T2 — Diff MG4Control (2.6.2) vs 2.7.0 fork

> Source of truth: `MG4Control/` (git). `2.7.0/` = manual divergent fork (no git).
> Full machine diff (CRLF-normalized): [`DIFF_2.7.0.full.diff`](DIFF_2.7.0.full.diff) (~4.3k lines).
> Note: every `2.7.0` file uses CRLF (Windows) — line-ending noise was stripped (`diff --strip-trailing-cr -w`) for this analysis.

## Headline
**2.7.0 is NOT a superset of 2.6.2.** It is a *parallel divergent fork* branched from an older base:
it **removes** several features that later landed in MG4Control 2.6.x (brightness, power-off, diagnostic unlock, QR, auto-update) **and adds** a new audio subsystem. A merge must reconcile both directions, not fast-forward.

## Manifest / build changes
| Area | MG4Control 2.6.2 | 2.7.0 |
|---|---|---|
| `versionCode` / `versionName` | 14 / 2.6.2 | 15 / 2.7.1 |
| `INTERNET`, `ACCESS_NETWORK_STATE`, `INSTALL_PACKAGES`, `WRITE_SETTINGS` perms | present | **removed** |
| `signingConfigs.platform` (platform.keystore) | absent | **added** (debug+release signed with platform key) |
| APK output rename `MG4Control-${versionName}.apk` | absent | **added** |
| zxing `com.google.zxing:core` dep | present | **removed** (QR gone) |
| `update/` package | full impl | **gutted to stubs** (UpdateChecker/ApkDownloader/… return no-op) |

## Features ADDED in 2.7.0 (candidates to port INTO MG4Control)
| Feature | Files | Notes / risk |
|---|---|---|
| **Audio control** (Bose sound, loudness, balance, fader, 3D effect, tone, speed-volume) | `ui/AudioFragment.kt` (new), `ui/MainActivity` nav btn, `nav_graph.xml`, `res/layout/fragment_audio.xml`, `hardware/MG4Hardware.kt` (audio helper) | Talks to a vendor `CarAdapterService` via IBinder `transact` (`HELPER_AUDIO_CODE`, `TX_QUERY_AUDIO_CLIENT`). **Writes to car** (`setLoudnessState`, `setBoseSoundType`, …) → must be gated behind security review (T4). Audio const IDs reverse-engineered. |

## Features REMOVED / stubbed in 2.7.0 (do NOT lose when porting)
| Removed in 2.7.0 | Where it lives in MG4Control |
|---|---|
| Auto-update (GitHub+GitLab fetch, APK download/install, update dialog, skip-version) | `update/*` (full), `SettingsFragment` (-178), `MG4Hardware.fetchFromGitHub/GitLab` |
| Screen brightness control (A9 + old-SDK paths) | `MG4Hardware.getBrightnessA9/OldSdk/setBrightness…`, `getScreenBrightnessPercent` |
| Vehicle power-off | `MG4Hardware.vehiclePowerOff*`, `hasVehiclePowerOff` |
| Diagnostic unlock | `SettingsFragment.setupDiagnosticUnlock` |
| QR generation (info dialog) | `generateQrBitmap` + zxing dep |
| ADAS verify/retry, ELK verify | `ProfileApplier.verifyAdasWithRetry/verifyElk/verifyOneAlert` (ProfileApplier -180) |

## Per-file change magnitude (real, CRLF-stripped)
Net-negative almost everywhere (stripping fork). Biggest:
```
hardware/MG4Hardware.kt        +292/-698   (audio added, brightness/update/power-off removed)
update/UpdateDialogManager.kt    +2/-263   (stub)
update/UpdateChecker.kt          +3/-219   (stub)
profile/ProfileApplier.kt       +19/-180   (ADAS/ELK verify removed)
ui/SettingsFragment.kt          +11/-178   (update + diagnostic UI removed)
ui/DashboardFragment.kt         +63/-176
service/MG4ControlService.kt    +69/-127
service/ProfilePickerOverlay.kt  +8/-119
```
Small/cosmetic: ProfileAdapter, DriveRegenFragment, ClimateFragment, ProfileManager, BootReceiver, AppLogger, FirmwareHelper, ConsoleFragment.

## SOH (relevant to T5)
No `SOH` / battery-health / capacity signal in **either** codebase, and none in `MG4Control_SWI_Reference.md`.
Closest energy signal: `PROP_ENERGY_SAVING = 0x5030007` (energy-saving toggle, not health). → T5 starts from zero; needs vendor-property probing.

## Recommendation for the offline build (T3)
2.7.0 already embodies the "offline + hardened" intent (no network, platform-signed, no self-update). But it also drops wanted features. So the offline flavor should be built **from current MG4Control** by *disabling* network/update at flavor level — **not** by importing 2.7.0 wholesale. Port only the **audio feature** from 2.7.0 separately.
