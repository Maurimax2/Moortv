# MAURIMAX

Android streaming app for **phone, tablet and Android TV** — one Kotlin codebase, two form factors.

> Status: M0/M1 — both apps build and CI is green. The catalog is still
> placeholder data. See [`docs/PLAN.md`](docs/PLAN.md) for the architecture and milestone plan.

## Getting an APK

No local Android SDK needed. Every push builds both apps in GitHub Actions and
attaches the APKs to the run:

1. Open the newest **Android** run under the repo's **Actions** tab.
2. Download `maurimax-mobile-debug` or `maurimax-tv-debug` from **Artifacts**.
3. Unzip, then install — tap the file on a phone, or
   `adb connect <tv-ip> && adb install -r maurimax-tv-debug.apk` for a TV.

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
