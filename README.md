# NoRush Cart 🛒🧘‍♂️

**NoRush Cart** is a native Android mindfulness companion designed to break impulsive online shopping patterns. When you encounter a tempting product link on Amazon or Flipkart, sharing it to NoRush Cart places it into a strict **48-hour cooling-down quarantine**. Only after 48 hours have elapsed can you choose to **Buy Now** or **Discard & Save** (growing your total money saved).

---

## Features

- **Mindful Sanctuary Theme**: Custom high-contrast dark palette (`#0D0630`), Plus Jakarta Sans typography, and iridescent cyan/magenta accents.
- **Parallel Link & Hotspot Capture**: Concurrently scrapes OpenGraph metadata via JSoup and retrieves device GPS with a 2-second timeout.
- **Impulse Hotspots Map**: Visualizes physical locations where spending urges strike, using dark-styled Google Maps and time-of-day cluster pins (Fuchsia for late-night 10 PM–5 AM triggers, Cyan for daytime, Mint for ready items).
- **Custom Place Labeling**: Name your trigger spots (e.g., *"Living Room Couch"*, *"Office Desk"*) via an interactive bottom sheet.
- **Phase 8: "See It in Person" (Nearby Stores)**:
  - Behind an opt-in toggle in Settings (*Find nearby stores*, default OFF).
  - Tapping **"Find in nearby store"** on Ready items checks local retailers within ~5 km using the Google Places API (New) Text Search.
  - Dynamically detects brands (*"Apple store"*, *"Sony store"*, *"Nike store"*) or product categories (*"electronics store"*, *"footwear store"*).
  - Displays stores on the Impulse Map as a distinct layer (`Stores` chip) with amber pins.
  - Interactive Store Bottom Sheet with store name, distance, open/closed status, direct **Call** (`ACTION_DIAL`), **Directions** (Google Maps intent), and a mandatory stock disclaimer: *"May stock this item. Call to confirm."*
  - **Zero Waste & Privacy**: Never uses saved hotspot coordinates (requests a fresh current location only). Results are cached in SQLite for 24 hours (one search per item per day).
  - **Graceful Fallback**: If no API key is provided, or if network/quota errors occur, smoothly falls back to Google Maps app intents (`geo:0,0?q=...`).

---

## Google Maps & Places API Setup

### Where Google Maps & Places APIs Are Used in the App:
1. **Google Maps SDK for Android (`play-services-maps`)**:
   - Used in `ImpulseMapFragment` to render the dark-styled interactive map, display impulse hotspot markers, and render the nearby store layer.
2. **Google Maps Android API Utility Library (`android-maps-utils`)**:
   - Used in `ImpulseMapFragment` for marker clustering (`ClusterManager`, `DefaultClusterRenderer`).
3. **Google Places SDK for Android (New) (`places:3.5.0`)**:
   - Used in `NearbyStoresHelper` for `searchByText()` queries with a 5 km circular radius bias to discover local brick-and-mortar retailers.
4. **Google Maps Intent Fallback**:
   - Used when no API key is set, location is offline, or quota is reached, dispatching `geo:0,0?q=<brand> store` directly to the installed Google Maps app.

---

### Step-by-Step: Adding and Restricting Your Google API Key

#### 1. Enable APIs in Google Cloud Console
1. Navigate to [Google Cloud Console](https://console.cloud.google.com/).
2. Select your project and go to **APIs & Services > Library**.
3. Search for and **Enable** both:
   - **Maps SDK for Android**
   - **Places API (New)**

#### 2. Create and Restrict the API Key
1. Go to **APIs & Services > Credentials** and click **+ Create Credentials > API Key**.
2. Click **Edit API Key** to configure security restrictions:
   - Under **Application restrictions**, choose **Android apps**.
   - Click **+ Add an item**:
     - **Package name**: `com.miniproject.myapplication`
     - **SHA-1 certificate fingerprint**:
       ```
       84:C6:5F:4A:92:CC:9E:B7:E9:2C:60:3E:FF:BC:01:07:E5:FD:58:8E
       ```
       *(This is the SHA-1 of the local debug keystore. For release builds, add the SHA-1 of your release keystore).*
   - Under **API restrictions**, select **Restrict key** and check:
     - **Maps SDK for Android**
     - **Places API (New)**
3. Click **Save**.

#### 3. Add Key to `local.properties`
In your project root directory, add the key to `local.properties` (this file is gitignored and will never be committed):
```properties
MAPS_API_KEY=YourActualGoogleMapsApiKeyHere
```
*(The Google Secrets Gradle Plugin automatically reads this key at compile time and injects it into both `AndroidManifest.xml` and `BuildConfig.MAPS_API_KEY`).*

---

## Architecture & Privacy Guarantee

- **100% On-Device Storage**: SQLiteOpenHelper database (`norush_cart.db`, Version 4) stores items, place labels, and cached store searches entirely on your device.
- **No Third-Party Analytics**: No telemetry, analytics SDKs, or background tracking services.
- **Separation of Hotspot Coordinates vs Store Search**: Impulse coordinates recorded during link sharing are stored locally for hotspot mapping. When searching for nearby stores, the app acquires a single fresh GPS reading strictly for the real-time radius query and never stores this query coordinate into your impulse history.
