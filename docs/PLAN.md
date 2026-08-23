# MAURIMAX — Architecture & Delivery Plan

**Repo:** `Maurimax2/Moortv` · **App name:** MAURIMAX · **Package:** `com.maurimax`
**Target:** Android phone/tablet **and** Android TV, from one codebase.

---

## 1. Stack decision

| Layer | Choice | Why |
|---|---|---|
| Language | **Kotlin** | Only first-class language for Android TV. |
| UI | **Jetpack Compose** (mobile) + **Compose for TV** (`androidx.tv:tv-material3`) | One toolkit, one design system, two form factors. |
| Playback | **AndroidX Media3 / ExoPlayer** | HLS + DASH + progressive, Widevine DRM, background/PiP, TV transport controls. |
| DI | **Hilt** | Standard, compile-time, plays well with multi-module. |
| Async | **Coroutines + Flow** | Repository layer exposes cold Flows; ViewModels expose `StateFlow<UiState>`. |
| Network | **Retrofit + OkHttp + kotlinx.serialization** | Predictable, well-understood, easy to mock. |
| Local | **Room** (cache, downloads, continue-watching) + **DataStore** (prefs) | |
| Lists | **Paging 3** | Catalog browsing without loading everything. |
| Images | **Coil** | Compose-native. |
| Nav | **Navigation Compose** (type-safe routes) | |
| Build | **Gradle KTS + version catalog + convention plugins** | Keeps 15+ modules from drifting. |

### Rejected alternatives

- **Flutter / React Native** — Android TV support is second-class: D-pad focus traversal, leanback
  launcher integration, and Media3 bindings all need native escape hatches. For a TV-first product
  that turns into permanent friction.
- **Legacy Leanback (`androidx.leanback`)** — in maintenance mode; `tv-material3` is the current path.
- **Single app module** — mobile and TV need different manifests, launcher intents, icons and
  navigation. Separate thin app modules over shared features is cleaner than runtime branching.

---

## 2. Module layout

```
:app:mobile          — phone/tablet manifest, launcher, nav host, Application class
:app:tv              — leanback manifest + banner, D-pad nav host, Application class

:core:model          — pure Kotlin domain types (no Android deps)
:core:designsystem   — colors, type, spacing; mobile + TV component sets
:core:ui             — shared stateless composables, previews
:core:network        — Retrofit services, DTOs, mappers
:core:database       — Room entities, DAOs, migrations
:core:datastore      — preferences, session
:core:data           — repositories: the ONLY thing features talk to
:core:player         — Media3 wrapper, session, resume/position tracking
:core:testing        — fakes, test rules, fixtures

:feature:home        — shared VM + mobile & TV screens
:feature:catalog
:feature:details
:feature:player
:feature:search
:feature:profile
:feature:downloads
```

**Rule:** features depend on `:core:*` only, never on each other. App modules wire features together.
Both `:app:mobile` and `:app:tv` reuse the same ViewModels; only the composables differ.

### Config

- `compileSdk 36`, `targetSdk 36`, `minSdk 24`
- TV module declares `android.software.leanback` (required) and `android.hardware.touchscreen`
  (`required=false`), plus the `LEANBACK_LAUNCHER` intent filter and a 320×180 banner.

---

## 3. Architecture

Unidirectional data flow, MVVM, single activity per app module.

```
UI (Compose)  →  ViewModel (StateFlow<UiState>)  →  Repository  →  { Network | Room | DataStore }
     ↑                                                    │
     └──────────────── events / actions ──────────────────┘
```

- ViewModels are **form-factor agnostic** and live in `:feature:*`. `HomeScreenMobile` and
  `HomeScreenTv` both render `HomeUiState`.
- Repositories return `Flow<Result<T>>`; Room is the single source of truth where caching applies.
- **`ContentRepository` is an interface** with a swappable implementation. This is deliberate — see §5.

---

## 4. Milestones

| # | Milestone | Ships |
|---|---|---|
| **M0** | Foundation | Gradle multi-module skeleton, version catalog, convention plugins, ktlint + detekt, GitHub Actions CI, README. Builds green, no features. |
| **M1** | Design system | MAURIMAX theme (colors/type/shape) for both Material3 and tv-material3, shared components, screenshot tests, `FakeContentRepository`. |
| **M2** | Mobile shell | Bottom nav; Home / Catalog / Search / Profile against fake data. Clickable end to end. |
| **M3** | TV shell | D-pad focus traversal, hero carousel + content rows, immersive detail screen. Tested on the TV emulator profile. |
| **M4** | Player | Media3 integration, HLS/DASH, mobile touch controls + TV D-pad controls, resume position, PiP (mobile), background audio behavior. |
| **M5** | Real data | ✅ Xtream client, sign-in, live categories and channels on the home screen. Still to come: Room caching, Paging 3, offline states. |
| **M6** | Catalog depth | Films and series browsing, EPG from XMLTV, search, favourites. |
| **M7** | Offline + DRM | Media3 downloads (mobile), Widevine if the catalog needs it. |
| **M8** | Release | R8/ProGuard, signing config, Play Console listing for **both** phone and TV, internal testing track. |

M0–M4 run entirely on fake data, so none of them are blocked by §5.

---

## 5. Content source: Xtream Codes, with the host baked in

**Decided.** MAURIMAX is an Xtream Codes client for one specific portal — yours.

The difference from a generic IPTV player is the login. A generic player asks for
host, username and password. MAURIMAX asks for **username and password only**:
the portal URL is compiled into the build, so the app is bound to your server and
a customer never sees, types or changes a server address.

```
gradle.properties
  maurimax.portalUrl=http://your-server:8080   ← the only place the host appears
        │
        └─> BuildConfig.PORTAL_URL  ─>  XtreamClient / XtreamUrls
```

### The panel API

| Call | Purpose |
|---|---|
| `player_api.php?username=&password=` | Sign-in — `user_info.auth == 1` means valid |
| `…&action=get_live_categories` / `get_live_streams` | Live TV |
| `…&action=get_vod_categories` / `get_vod_streams` | Films |
| `…&action=get_series_categories` / `get_series` | Series |
| `xmltv.php?username=&password=` | Full EPG |

Playback URLs are path-encoded rather than API calls, which is why `XtreamUrls`
is unit tested:

```
{portal}/live/{user}/{pass}/{stream_id}.m3u8
{portal}/movie/{user}/{pass}/{stream_id}.{container_extension}
{portal}/series/{user}/{pass}/{episode_id}.{container_extension}
```

### Panels lie about JSON types

The same field comes back as `"1"` on one server, `1` on another and `null` on a
third. Every DTO field goes through a lenient serializer so a panel quirk cannot
crash the app; `DtoParsingTest` pins the shapes seen in the wild.

### Sign-in outcomes are distinguished

"Wrong password", "account expired" and "server unreachable" are three different
failures with three different customer-facing messages. A customer cannot fix an
outage, so that message says to check their connection rather than showing an
exception.

## 6. Build & verification constraint

This cloud session has **JDK 21 and Gradle 8.14**, but **no Android SDK**, and the environment's
network policy blocks `dl.google.com` (and `maven.google.com`, which redirects there). Consequences:

- Android/AndroidX dependencies **cannot be resolved here**, so `assembleDebug` cannot run in-session.
- Code is authored here; **CI is the compiler**. GitHub Actions runners ship the Android SDK, so
  every push gets built and tested there — that's the M0 deliverable that makes everything after it
  verifiable.
- Local verification with Android Studio on your machine stays the fastest inner loop, especially
  for the TV emulator work in M3.

---

## 7. Definition of done, per milestone

1. Compiles for both `:app:mobile` and `:app:tv` in CI.
2. ktlint + detekt clean.
3. Unit tests for every new ViewModel and repository.
4. Screenshot tests for new design-system components.
5. Manually exercised on a phone emulator **and** a TV (1080p, D-pad only) emulator.
