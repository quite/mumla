# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## About

Mumla is an Android Mumble voice chat client, forked from [Plumble](https://github.com/acomminos/Plumble). It uses [Humla](https://gitlab.com/quite/humla) (a fork of Jumble) as its protocol/audio implementation, included as a git submodule at `libraries/humla`.

## Build Setup

**Prerequisites:**
- JDK 21 (`export JAVA_HOME=/usr/lib/jvm/java-21-openjdk`)
- Android SDK (set `ANDROID_SDK_ROOT` or write to `local.properties`: `sdk.dir=/path/to/Android/Sdk`)
- NDK 25.1.8937393 (bundled with AGP 8.x; install via SDK Manager under "NDK (Side by side)")

**First-time setup** — initialize submodules and build the spongycastle dependency:
```
git submodule update --init --recursive
pushd libraries/humla/libs/humla-spongycastle
../../gradlew jar
popd
```

**Build:**
```
./gradlew assembleDebug          # builds all debug variants
./gradlew assembleFossDebug      # builds the FOSS flavor only
```

If you hit Java heap space errors, increase `-Xmx` in `gradle.properties` (currently `2048m`).

**Lint:**
```
./gradlew :app:lintDebug
```
Lint is configured in `app/build.gradle` with `abortOnError = true`; `InvalidPackage` and `MissingTranslation` are suppressed.

**Tests** (instrumented only; no unit test suite is active):
```
./gradlew :app:testDebugUnitTest   # runs unit tests if any exist
```

## Product Flavors

Four flavors exist under the `release` dimension:

| Flavor | App ID | Notes |
|--------|--------|-------|
| `foss` | `se.lublin.mumla` | F-Droid release; no billing |
| `goog` | `se.lublin.mumla` | Google Play release; includes `billing` dependency |
| `donation` | `se.lublin.mumla.donation` | Separate donation variant |
| `beta` | `se.lublin.mumla.beta` | versionCode auto-set to minutes since epoch |

Flavor-specific sources live under `app/src/{foss,goog,donation,beta}/`.

## Architecture

### Layer overview

```
MumlaActivity  (single Activity, drawer-based navigation)
     │
     ├── binds to ──▶  MumlaService  (foreground Service)
     │                      │
     │                      └── extends ──▶  HumlaService  (libraries/humla)
     │                                          │
     │                                          └── Mumble protocol + audio (native NDK)
     │
     └── hosts Fragments that implement HumlaServiceFragment
```

### Key classes

- **`MumlaActivity`** (`app/`) — the sole Activity. Manages the navigation drawer, fragment back-stack, and service binding lifecycle. Implements `HumlaServiceProvider` so all fragments can retrieve the bound service.
- **`MumlaService`** (`service/`) — extends `HumlaService` and adds Mumla-specific features: PTT broadcast receiver, TTS readback, proximity wake-lock, chat message log, connection/reconnect/message notifications, and the floating channel overlay.
- **`IMumlaService`** — extends `IHumlaService` with the Mumla-only surface (overlay, notifications, PTT key events, message log).
- **`HumlaServiceFragment`** / **`HumlaServiceProvider`** (`util/`) — base Fragment class and Activity interface that manage the bind/unbind lifecycle and observer registration pattern used by all UI fragments.
- **`Settings`** — singleton wrapper around `SharedPreferences` for all app preferences. Preference keys are `PREF_*` constants; defaults are `DEFAULT_*` constants.
- **`MumlaDatabase`** / **`MumlaSQLiteDatabase`** (`db/`) — interface + SQLite implementation for persistent storage: server list, pinned channels, access tokens, local mute/ignore lists, and certificates (PKCS12).

### Fragment layout

- `channel/` — channel browser (`ChannelListFragment`), channel detail (`ChannelFragment`), in-channel chat (`ChannelChatFragment`), channel/user context menus, ACL permission pop-ups.
- `servers/` — favourite server list, public server directory (fetched from the Mumble server list), server edit dialog.
- `preference/` — all settings screens; certificate management (generate, import, export, select).
- `service/` — `MumlaOverlay` (floating PTT/user overlay), `MumlaHotCorner`, notification helpers.

### Observer pattern

Fragments subscribe to Humla events by returning an `IHumlaObserver` from `getServiceObserver()`. `HumlaServiceFragment` registers/unregisters it automatically on bind/unbind.

### Humla submodule

`libraries/humla` contains the Mumble protocol implementation and native audio engine (Opus codec via NDK). Its `HumlaService` is the true Android Service; `MumlaService` subclasses it. `IHumlaService` / `IHumlaObserver` are the public API surface consumed by Mumla's UI layer.
