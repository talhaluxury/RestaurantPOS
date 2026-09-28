# Talha POS — Restaurant Billing & Receipt App

A complete Android Restaurant POS/Billing app built with Kotlin, Jetpack Compose, Material 3,
Hilt, Room (offline-first), Firebase (Auth/Firestore/Storage), WorkManager, and a custom
"liquid glass" design system.

## What's included

- Full offline-first billing: bills can be created with zero internet, sync automatically and
  safely (no duplicate order numbers) once connectivity returns.
- Auth (email/password + Google Sign-In scaffold) and multi-tenant restaurant setup.
- Fast New Bill screen: product grid, variants/add-ons bottom sheet, live cart, discounts,
  dine-in table selection, takeaway/delivery, payment, PDF receipt (A4 / 80mm / 58mm) with
  share/print, Bluetooth ESC/POS thermal printing.
- Menu management (categories, products, variants, add-ons, image upload), Orders (with
  tabs/status/refund/duplicate), Reports (revenue, AOV, payment breakdown), Staff, Inventory
  (low-stock alerts), Tables, Kitchen Display Mode, Settings (theme, tax, printer).
- Light/Dark glass theme system with a reusable component library
  (`presentation/components/Glass*.kt`).

## Required setup before building

This project ships with a **placeholder** `app/google-services.json` so it opens and compiles
out of the box. Before you run it against real data:

1. Create a Firebase project at https://console.firebase.google.com
2. Add an Android app with package name `com.talha.restaurantpos`
3. Download the real `google-services.json` and replace `app/google-services.json`
4. Enable **Authentication** → Email/Password and Google sign-in methods
5. Enable **Cloud Firestore** and **Cloud Storage**
6. Deploy the security rules documented in
   `app/src/main/java/com/talha/restaurantpos/data/remote/FirestorePaths.kt` (bottom of file) via
   the Firebase Console → Firestore → Rules
7. For Google Sign-In, make sure the OAuth **Web client ID** is present in your
   `google-services.json` (Firebase enables this automatically once you add a Web app or
   enable Google sign-in) — the app reads it as `R.string.default_web_client_id` and the
   Google button self-disables gracefully until it's available.

## Building

Open the project root (`RestaurantPOS/`) in Android Studio (Koala or newer recommended),
let Gradle sync, and run on a device/emulator with API 24+. Debug builds seed a sample
restaurant menu automatically the first time you create a restaurant
(`BuildConfig.SEED_SAMPLE_DATA`).

## Architecture

```
data/        Room entities/DAOs, Firestore-backed repositories (offline-first)
domain/      Models + repository interfaces
presentation/
  theme/       Glass design tokens, light/dark theme
  components/  GlassCard, GlassButton, GlassTextField, GlassProductCard, etc.
  navigation/  Nav graph + routes
  screens/     auth, setup, dashboard, billing, payment, orders, menu, reports, settings, kitchen
receipt/     PDF receipt generator (A4 / 80mm / 58mm)
printer/     Bluetooth ESC/POS thermal printer abstraction
util/        Session/network/currency/order-number helpers, sync WorkManager worker
```

## Notes on scope

- Bluetooth printing targets classic SPP (RFCOMM) printers — the vast majority of budget/mid
  thermal printers sold for POS use. Network (Wi-Fi ESC/POS over TCP:9100) or USB printers can
  be added later as another `ThermalPrinter` implementation without touching billing code.
- Google Sign-In requires a configured Firebase project (see above); the button is real code,
  not a stub, but stays inert until `default_web_client_id` exists.
