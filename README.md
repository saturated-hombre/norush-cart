# NoRush Cart 🛒🧘‍♂️

[![Android](https://img.shields.io/badge/Platform-Android_8.0+_(API_26--34)-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java_17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Architecture](https://img.shields.io/badge/Storage-100%25_On--Device_SQLite-003B57?style=flat&logo=sqlite&logoColor=white)](https://www.sqlite.org)
[![Maps & Places](https://img.shields.io/badge/Google_Maps-SDK_%26_Places_v3.5-4285F4?style=flat&logo=googlemaps&logoColor=white)](https://developers.google.com/maps)
[![Design](https://img.shields.io/badge/Design_System-Nordic_Slate_%26_Teal-4FB8A3?style=flat)](https://material.io)

**NoRush Cart** is a native Android mindfulness companion designed to break impulsive online shopping patterns. 

By intercepting shared product links from major e-commerce platforms (such as Amazon, Flipkart, Blinkit, BigBasket, Zepto, and Myntra), NoRush Cart places desired items into a mandatory **48-hour cooling-down quarantine**. Only after the timer expires can you choose between **Buy Now** or **Discard & Save**—turning impulsive spending into conscious savings.

---

## 📸 Core Highlights

| Cooldown Quarantine | Impulse Hotspots Map | Savings Vault |
|:---:|:---:|:---:|
| Real-time down-to-the-second countdown timers locking impulsive purchases | Dark-styled Google Map clustering late-night & daytime spending triggers | Visual savings accumulator celebrating every impulsive purchase avoided |

---

## ✨ Features

### ⏳ 1. 48-Hour Impulsive Quarantine
- **Strict Anti-Impulse Buffer**: Newly captured items enter a strict 48-hour cooling-down lock.
- **Live Countdown Timers**: High-precision tickers display exact hours, minutes, and seconds remaining.
- **Decisive Review State**: Once unlocked, items move to the **Ready for Review** tab where you either execute the purchase or discard it to grow your **Total Savings Vault**.

### 🛍️ 2. Multi-Platform Smart Scraper
- **Universal Indian & Global E-Commerce Support**:
  - **Amazon India** (`amazon.in`, `amzn.to`, `amzn.in`)
  - **Flipkart** (`flipkart.com`, `fkrt.it`)
  - **Blinkit** (`blinkit.com`)
  - **BigBasket** (`bigbasket.com`)
  - **Zepto** (`zeptonow.com`)
  - **Myntra** (`myntra.com`)
  - Generic web pages with OpenGraph or JSON-LD metadata.
- **Robust Price Parser**:
  - Handles Indian numbering system formatting (`₹1,20,000` / Lakhs / Crores) without dropping digit groups.
  - Multi-tier extraction: JSON-LD `offers.price` schema $\rightarrow$ Meta tags (`og:price:amount`, `product:price:amount`) $\rightarrow$ Regex pattern matching on DOM containers.
- **In-App Price Editing**: One-tap pencil icon opens a sleek dark dialog to adjust or correct scraped amounts instantly.
- **Direct Link Dialog**: Add product links manually inside the app via the `+` action button without relying solely on system share intents.

### 📍 3. Impulse Hotspots & Trigger Analytics
- **Geotagged Desires**: Concurrently captures device GPS coordinates with a 2-second timeout when an item is shared.
- **Dark-Themed Google Maps**: Custom map styling matching the app's Nordic Slate aesthetic (`#0F1216`).
- **Time-of-Day Visual Clustering**:
  - 🟣 **Magenta Pins**: Late-night impulse triggers (10:00 PM – 5:00 AM).
  - 🔵 **Cyan Pins**: Daytime purchases.
  - 🟢 **Mint Pins**: Unlocked items ready for review.
- **Custom Place Tagging**: Name personal impulse zones (*"Living Room Couch"*, *"Office Desk"*, *"Bed"*) with automatic coordinate aggregation.

### 🏬 4. "See It in Person" (Nearby Store Finder)
- **Physical Retail Discovery**: Optional feature that searches for brick-and-mortar stores within a 5 km circular radius using the **Google Places API (New)**.
- **Brand & Category Querying**: Automatically identifies brands (*"Apple store"*, *"Sony store"*, *"Nike store"*) or product categories (*"electronics store"*, *"footwear store"*).
- **Interactive Store Sheet**: Displays store address, distance, operating status, direct phone dialer (`ACTION_DIAL`), and Google Maps directions.
- **Zero Privacy Leakage**: Searches acquire a single fresh location reading and never correlate with or expose historical impulse coordinates.

### 💰 5. Savings Vault & Impact Analytics
- **Financial Consciousness**: Tracks total accumulated rupees saved from avoided impulse purchases.
- **Weekly Savings Canvas Chart**: Custom vector graph rendering savings distribution across the week.

### 🎨 6. Premium Nordic Slate Design System
- **Color Palette**: Deep Obsidian Slate (`#0F1216`), Elevated Surface (`#1B222C`), Frosted Pine Teal (`#4FB8A3`), Amber Accent (`#F59E0B`), and Crimson Cancel (`#F97066`).
- **Adaptive Launcher Icon**: Custom vector icon artwork with multi-density mipmaps (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) and 512x512 store graphic.
- **Android 12+ Splash Screen**: Native system splash screen integration via `windowSplashScreenAnimatedIcon` and `windowSplashScreenBackground`.

---

## 🔒 Privacy & Architecture

- **100% On-Device Database**: All products, coordinates, place labels, and cached queries reside exclusively in a local SQLite database (`norush_cart.db`, Version 4).
- **No Cloud Sync / No Telemetry**: No third-party trackers, analytics, or behavioral surveillance.
- **Offline Capable**: Works fully offline; network access is only used for metadata scraping and Google Places queries.

---

## 🏗️ Tech Stack

- **Platform**: Android (Min SDK 26 / Android 8.0 Oreo, Target SDK 34 / Android 14)
- **Language**: Java 17
- **UI Framework**: Android Views, Material Design 3, ViewBinding, CoordinatorLayout
- **Networking & Scraping**: JSoup, OkHttp3
- **Image Loading**: Glide 4
- **Asynchronous Execution**: Java `ExecutorService` (Cached & Single thread pools)
- **Background Tasks**: AndroidX WorkManager
- **Location & Maps**: Google Play Services Location, Google Maps SDK, Android Maps Utils, Google Places SDK (New)

---

## 📁 Repository Structure

```
No rush Kart/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── com/miniproject/myapplication/
│   │   │   │   │   ├── database/DatabaseHelper.java      # SQLite schema, migrations (V1 -> V4)
│   │   │   │   │   └── MainActivity.java                 # Core coordinator, tab views, FAB
│   │   │   │   └── com/project/norushcart/
│   │   │   │       ├── data/                             # ProductItem, NearbyStore models
│   │   │   │       ├── location/                         # LocationHelper, NearbyStoresHelper
│   │   │   │       ├── network/                          # UrlScraperHelper multi-platform parser
│   │   │   │       ├── service/                          # CooldownWorker, SchedulerHelper
│   │   │   │       └── ui/                               # Adapters, Fragments, BottomSheets, Views
│   │   │   └── res/
│   │   │       ├── drawable/                             # Vector drawables, custom dark backgrounds
│   │   │       ├── layout/                               # Clean XML layouts
│   │   │       ├── values/                               # Colors, styles, dimens, strings
│   │   │       └── values-v31/                           # Android 12+ SplashScreen theme
│   │   └── test/
│   │       └── java/com/project/norushcart/network/      # Scraper & Price unit tests
├── stitch_design/                                        # UX Design System, screen tokens & specs
├── build.gradle.kts                                      # Root Gradle configuration
├── settings.gradle.kts                                   # Subproject inclusions
├── local.defaults.properties                             # API key template
└── .gitignore                                            # Production-grade Android ignore rules
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Iguana (2023.2.1) or newer
- **JDK**: Java Development Kit 17
- **Android SDK**: API Level 34

### 1. Clone the Repository
```bash
git clone https://github.com/saturated-hombre/norush-cart.git
cd norush-cart
```

### 2. Configure Google Maps & Places API Key
1. Obtain an API key from the [Google Cloud Console](https://console.cloud.google.com/).
2. Enable **Maps SDK for Android** and **Places API (New)**.
3. Add your key to `local.properties` in the project root:
   ```properties
   MAPS_API_KEY=AIzaSyYourActualKeyHere
   ```
   *(Note: `local.properties` is strictly ignored by Git and will never be committed).*

### 3. Build & Run
- **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Run Unit Tests**:
  ```bash
  ./gradlew test
  ```
- **Install on Device via ADB**:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

---

## 🧪 Testing

Unit tests for scraper logic, regexes, and price extraction are located in:
`app/src/test/java/com/project/norushcart/network/UrlScraperHelperTest.java`

Run the test suite with:
```bash
./gradlew test
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
