# Tyre Inventory

A local-first stock management app for a tyre shop, written in Kotlin with Jetpack Compose (Material 3).

Built for Yassine's tyre shop: track tyres by **rim size → size spec → brand**, run an invoice builder that
generates PDF invoices (with shop identity and TVA/VAT), and follow up on sales, client credit and monthly profit —
all offline, all in the user's language (English / Français / العربية).

## Features

- **Stock**
  - Drill-down catalogue: rim inches (13"–22") → size spec (`205/55 R16`) → brand
  - Full tyre fields: brand, model, season, load/speed index, width, profile, rim, SKU, qty, low-stock threshold,
    selling price, **cost price**, supplier, notes
  - Sell-price × qty value, low-stock badge, **stock-ageing badge** (tyres sitting longer than 120 days)
  - Per-item photo (camera or gallery)
  - Sort by size / brand / price / quantity, text search, barcode scanning
- **Invoice builder**
  - Pick items from stock, edit quantities and unit prices, discount %, **client picker** (saved across sales),
    payment status **Cash / Partial / Credit** with paid amount
  - **TVA 20%** (configurable) applied after discount, DH totals
  - Generates an A4 PDF with your **shop name / address / phone / RC / ICE header** and VAT line, then shares it
  - **Custom invoice design**: shop logo (top-left), accent color, standard **HT / TVA / TTC** lines,
    the total **spelled out in French** ("Arrêtée la présente facture à la somme de … dirhams et … centimes"),
    a red **outstanding-balance** line on partial/credit invoices, and a thank-you footer
  - Completing a sale decrements stock (with a safety clamp) and logs a SALE movement
- **Sales history**
  - Dashboard: today's revenue & invoice count, this month's revenue and **profit** (from cost price)
  - Expand an invoice to see the full discount + VAT breakdown, **reprint the PDF**, or **record a partial payment**
- **Stock movements**
  - Journal of every in/out: SALE, RESTOCK, ADJUST, DELETE with quantity deltas, date and notes
- **Shop settings**
  - Shop profile (name, address, phone, RC, ICE) — printed on the invoice header
  - TVA rate (default 20%)
- **Invoice design**
  - Logo upload (preview + remove), accent color picker, toggles for amount-in-letters, legal strip,
    HT/TVA/TTC lines, outstanding balance and thank-you footer — applied instantly to new PDFs and reprints
- **Tools**
  - CSV import / export, database backup / restore, barcode scan
  - In-app language switch EN / FR / AR (persisted, survives restart)
  - **Daily auto-backup** of the database to the app's External Files area (7-day retention)
- Local Room database (`inventory.db`), no accounts, works fully offline

## Download

Grab the latest APK from the [**Releases**](https://github.com/beav0team/tyre-inventory/releases) page, or install
straight from a build:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Tech stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose + Material 3, Compose BOM 2024.12.01 |
| Language | Kotlin 2.1.0 |
| Persistence | Room 2.6.1 (`room-runtime`/`room-ktx`, KSP `2.1.0-1.0.29`) |
| PDF | manual A4 canvas drawing (no extra PDF lib), shared via FileProvider |
| Scanning | `com.journeyapps:zxing-android-embedded` |
| Background | WorkManager (low-stock alerts, daily auto-backup) |
| minSdk / target / compileSdk | 26 / 35 / 35 |

## Project structure

```
app/src/main/java/com/yassine/inventory/
├── App.kt / MainActivity.kt       # locale attachBaseContext, WorkManager scheduling
├── AppLocale.kt                   # shared-prefs language helper (EN/FR/AR)
├── Invoice.kt / InvoicePdf.kt     # invoice model + A4 PDF renderer
├── AmountWords.kt                 # French amount-in-letters (dirhams / centimes)
├── InvoiceDesign.kt               # invoice design prefs (logo, accent, options)
├── CsvImporter.kt / CsvExporter.kt
├── DbBackup.kt                    # manual backup/restore via SAF
├── LowStockWorker.kt / AutoBackupWorker.kt
├── ShopSettings.kt                # shop profile + TVA prefs
├── InventoryViewModel.kt / InventoryRepository.kt
├── data/                          # AppDatabase (v4), entities, DAOs, Item
└── ui/                            # screens (InventoryScreen, InvoicePane,
                                   # SalesHistoryScreen, MovementsScreen,
                                   # ShopSettingsScreen, InvoiceDesignScreen,
                                   # ItemEditorDialog)
```

## Building

```bash
# prerequisites: JDK 17+, Android SDK 35
export JAVA_HOME=/path/to/jdk          # e.g. ~/.local/jdk
./gradlew :app:assembleDebug           # APK → app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest       # JVM unit tests (totals, CSV, item model)
```

Install on a device/emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Versioning

See [CHANGELOG.md](CHANGELOG.md). Current version: **1.5.0** (build 5).
Pre-built APKs are published on the [Releases](https://github.com/beav0team/tyre-inventory/releases) page.

## License

Private project — all rights reserved.