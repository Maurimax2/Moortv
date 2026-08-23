# MAURIMAX

Android streaming app for **phone, tablet and Android TV** — one Kotlin codebase, two form factors.

> Status: planning. See [`docs/PLAN.md`](docs/PLAN.md) for the architecture and milestone plan.

## Stack

Kotlin · Jetpack Compose + Compose for TV · Media3/ExoPlayer · Hilt · Room · Retrofit · Paging 3

## Layout (planned)

```
app/mobile     phone & tablet
app/tv         Android TV (leanback)
core/*         model, design system, data, network, database, player
feature/*      home, catalog, details, player, search, profile, downloads
```

## Building

Requires JDK 17+ and the Android SDK (`compileSdk 36`). Open in Android Studio, or:

```bash
./gradlew :app:mobile:assembleDebug
./gradlew :app:tv:assembleDebug
```
