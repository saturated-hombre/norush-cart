# NoRush Cart 🛒🧘‍♂️

> **Break impulse shopping loops. Reclaim your money and peace of mind through mindful 48-hour cooling-down quarantine.**

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Java](https://img.shields.io/badge/Language-Java%2017-ED8B00.svg?style=flat&logo=openjdk)](https://openjdk.org/)
[![Material 3](https://img.shields.io/badge/Design-Material%203%20Mindful%20Sanctuary-7952B3.svg?style=flat)](https://m3.material.io/)
[![Google Maps](https://img.shields.io/badge/Google%20Maps-Platform%20SDK-4285F4.svg?style=flat&logo=googlemaps)](https://developers.google.com/maps)
[![Zero Telemetry](https://img.shields.io/badge/Privacy-100%25%20On--Device-10B981.svg?style=flat)]()

---

## 🌟 Overview

Modern e-commerce apps are engineered with 1-click checkout, artificial scarcity countdowns, and algorithmic recommendations designed to trigger instantaneous gratification. 

**NoRush Cart** acts as an intentional friction buffer. When browsing Amazon or Flipkart and tempted by a spontaneous purchase, you simply share the product link to NoRush Cart instead of adding it to your checkout cart. 

The item is immediately placed into a strict **48-hour cooling-down quarantine**:
- **Locked Phase**: No impulse buying permitted. The app tracks remaining time with real-time countdown tickers.
- **Decision Phase**: Once the 48 hours lapse, dopamine clears. You make a clear-headed choice:
  - **Discard & Save**: Resist the urge, remove the item, and watch your total savings counter and weekly savings chart climb.
  - **See It In Person**: Find nearby local brick-and-mortar stores stocking the item to inspect it physically before spending.
  - **Proceed to Store**: If you genuinely still need it, proceed to the merchant page with full intention.

---

## ✨ Key Features

### 1. 🧘 Mindful Sanctuary Design System
- **OLED Dark Palette**: Root canvas `#0D0630` paired with frosted glass card containers and ambient glow borders.
- **Edge-to-Edge Experience**: Fully immersive edge-to-edge system window handling with dynamic status and navigation bar insets.
- **Typography & Precision**: Clean Plus Jakarta Sans typography with tabular numbers (`tnum`) for jitter-free countdown rendering.
- **Tactile Haptic Feedback**: Context-aware haptics (`HapticHelper`) delivering light ticks on navigation and filters, and affirming feedback upon resisting purchases.

### 2. ⏳ 48-Hour Quarantine & Real-Time Countdowns
- Native OpenGraph scraper (`LinkScraperHelper`) parses product images, titles, and prices concurrently in background threads.
- Targeted countdown updates via `tickVisibleCountdowns()` ensuring smooth 60fps scrolling without full view re-renders.

### 3. ⏱️ Mindful Economics ("Hours of Life" Cost)
- Configure your estimated hourly wage in **Settings > Mindful Economics**.
- Automatically converts product price tags into equivalent hours of work needed to earn that amount (e.g., `· 4.2h of work`).
- Displayed on the Interception Share Sheet, Cooldown Cards, and Ready Cards to reframe abstract currency into real human effort.

### 4. 🎯 Dream Savings Goal Tracker
- Set a personal dream milestone (e.g., *"Trip to Japan"*, *"Custom Gaming PC"*) with a target amount.
- Every resisted impulse automatically funds your dream goal progress bar, transforming fleeting cravings into tangible milestones.

### 5. 🏷️ Emotional Urge Tagging
- Interception share sheet features quick emotional tags (🥱 *Bored*, 😫 *Stressed*, 🌙 *Late Night*, 🏷️ *FOMO*, 🎁 *Reward*).
- Helps you build self-awareness around emotional spending triggers.

### 6. 🗺️ Impulse Hotspots Map & Analytics
- **Geotagged Interceptions**: Captures device GPS coordinates upon link sharing with a 2-second timeout to protect battery and speed.
- **Custom Theme Markers (`MapMarkerHelper`)**: Replaces generic map pins with high-contrast glowing vector discs:
  - 🌙 **Electric Fuchsia**: Late-night impulses (10 PM – 5 AM).
  - ⚡ **Electric Cyan**: Daytime trigger locations.
  - ✨ **Mint Green**: Unlocked items ready for decision.
  - 🏬 **Amber Solar**: Nearby physical retail stores.
- **Hotspot Clustering**: Smooth map clustering using Google Maps Android Utility library with glowing disc counters.
- **Behavioral Insights**: Highlights what percentage of your impulses happen during late-night vulnerability hours.
- **Demo Mode**: Built-in "Load Demo Hotspots" button to explore map visualizations immediately.

### 7. 🏬 "See It in Person" Nearby Retailers
- Opt-in local store search powered by the **Google Places SDK for Android (New)**.
- Intelligently queries brands (*"Apple store"*, *"Sony store"*, *"Nike store"*) or product categories within a 5 km circular radius.
- Interactive store bottom sheet displaying distance, open/closed status, direct call dialer (`ACTION_DIAL`), and Google Maps directions intent.
- **Zero-Waste Privacy**: Always uses a single fresh GPS location strictly for the search; never writes your query coordinates to your impulse history. 24-hour local SQLite caching prevents redundant API calls.

---

## 🔒 Security & API Key Management

NoRush Cart strictly adheres to Android security best practices. **No API keys or sensitive credentials are committed to version control.**

### Gradle Secrets Architecture
The project utilizes the [Google Secrets Gradle Plugin](https://github.com/google/secrets-gradle-plugin):
- `local.properties` holds your private API keys locally and is completely ignored by Git.
- `local.defaults.properties` provides safe fallback placeholders so the project builds out-of-the-box on fresh clones or CI pipelines.

### Configuring Google Maps & Places API

To enable live Google Maps satellite/street tiles and nearby store discovery:

#### 1. Enable Cloud APIs
1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Select your project and navigate to **APIs & Services > Library**.
3. Enable both:
   - **Maps SDK for Android**
   - **Places API (New)**

#### 2. Create and Restrict Your API Key
1. Go to **APIs & Services > Credentials** and click **Create Credentials > API Key**.
2. Select **Edit API Key**:
   - Under **Application restrictions**, choose **Android apps**.
   - Add your application identity:
     - **Package Name**: `com.miniproject.myapplication`
     - **Debug SHA-1 Fingerprint**:
       ```text
       84:C6:5F:4A:92:CC:9E:B7:E9:2C:60:3E:FF:BC:01:07:E5:FD:58:8E
       ```
       *(Obtain your custom debug fingerprint anytime via `./gradlew signingReport`)*.
   - Under **API restrictions**, select **Restrict key** and check:
     - **Maps SDK for Android**
     - **Places API (New)**
3. Click **Save**.

#### 3. Add to `local.properties`
Add the key to your root `local.properties` file:
```properties
MAPS_API_KEY=AIzaSyYourActualGoogleMapsApiKeyHere
```
*(The secrets plugin injects this key into `AndroidManifest.xml` and `BuildConfig.MAPS_API_KEY` at build time).*

> **Note**: Even without an API key, the app functions smoothly! Impulse tracking, timers, analytics, and haptics work completely offline. Map links gracefully fall back to native Google Maps navigation intents.

---

## 🏗️ Project Architecture

```
No rush Kart/
├── app/
│   ├── build.gradle.kts           # Dependencies, Secrets Gradle Plugin config
│   └── src/main/
│       ├── AndroidManifest.xml    # Activities, permissions, intent filters
│       ├── java/
│       │   ├── com/miniproject/myapplication/
│       │   │   ├── MainActivity.java               # Core bottom navigation & dashboard
│       │   │   └── database/DatabaseHelper.java     # SQLiteOpenHelper (Schema v4)
│       │   └── com/project/norushcart/
│       │       ├── data/ProductItem.java           # Model for quarantined products
│       │       ├── location/                       # GPS & Places API integration
│       │       │   ├── LocationHelper.java         # FusedLocationProviderClient wrapper
│       │       │   ├── NearbyStoresHelper.java     # Places SDK text search & caching
│       │       │   └── StoreItem.java              # Retailer model with phone & hours
│       │       ├── network/LinkScraperHelper.java  # OpenGraph scraper via JSoup
│       │       ├── service/SchedulerHelper.java    # AlarmManager & notification manager
│       │       └── ui/                             # Mindful Sanctuary UI components
│       │           ├── CooldownAdapter.java        # Quarantined items recycler adapter
│       │           ├── ReviewAdapter.java          # Ready-to-decide items recycler adapter
│       │           ├── ImpulseMapFragment.java     # Google Maps & cluster visualization
│       │           ├── MapMarkerHelper.java        # Canvas-rendered theme vector pins
│       │           ├── HapticHelper.java           # Tactile feedback utility
│       │           ├── SettingsActivity.java       # Mindful economics & feature toggles
│       │           ├── ShareReceiverActivity.java  # System share sheet receiver
│       │           └── StoreDetailsBottomSheet.java# Store preview dialog
│       └── res/
│           ├── layout/                             # M3 XML layouts
│           ├── values/                             # Color palette, themes, strings
│           └── font/                               # Plus Jakarta Sans typefaces
├── stitch_design/                                  # Design tokens & screen specifications
├── local.defaults.properties                       # Safe fallback keys for CI/clones
└── README.md
```

---

## 🛡️ Privacy Principles

- **100% On-Device**: All link metadata, timestamps, price tags, and custom labels are stored in a local SQLite database (`norush_cart.db`).
- **Zero Third-Party Telemetry**: No analytics SDKs, advertising trackers, or background sync daemons.
- **Isolated Location Queries**: Impulse hotspot coordinates captured during share-sheet receipt are stored locally. Nearby store searches issue a fresh real-time query that is never linked to your hotspot history.
- **Local Cache**: Places search results are cached in SQLite for 24 hours per product to eliminate redundant network transmissions.

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **JDK**: Java 17
- **Android SDK**: Compile SDK 35, Min SDK 26 (Android 8.0+)

### Building and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/saturated-hombre/norush-cart.git
   cd norush-cart
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run on a connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
