# PocketRate

A free Android currency converter with integrated travel expense tracking.

## Features

- **Currency Converter**: Convert 160+ currencies using cached rates.
- **Trip Expense Mode**: Track spending across multiple currencies per trip.
- **Cost Splitting**: Add companions and calculate minimum settlement transactions.
- **Charts**: Historical rate line charts and category pie charts.
- **Export**: Share trips as CSV or PDF reports.
- **Ad-Supported**: Banner, interstitial, and rewarded ads via AdMob. Watch a rewarded ad to remove banners for 24 hours.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Repository + Clean Architecture
- **DI**: Hilt
- **Local Storage**: Room + DataStore
- **Networking**: Retrofit (ExchangeRate-API + Frankfurter fallback)
- **Background Sync**: WorkManager
- **Charts**: Vico
- **PDF**: iText7
- **Logging**: Timber

## Project Structure

```
com.reganye.pocketrate
├── data          # Entities, DAOs, database, remote APIs, repositories
├── di            # Hilt modules
├── domain        # Models and use cases
├── presentation  # UI screens, ViewModels, navigation, theme
└── worker        # Background sync worker
```

## Setup

1. Open the project in Android Studio (Ladybug or newer).
2. Sync Gradle.
3. Build and run on an emulator or device.

## Monetization Notes

- AdMob test IDs are configured for debug builds in `app/build.gradle.kts`.
- Before release, replace the empty `ADMOB_*` BuildConfig fields and `admob_app_id` `resValue` in the `release` build type with your production IDs.
- Release builds with blank IDs will skip ad initialization to avoid crashes.
- No subscriptions are implemented in v1.0.

## License

Copyright 2026 Regan Ye. All rights reserved.
