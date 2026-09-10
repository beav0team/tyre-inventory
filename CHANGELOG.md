# Changelog

All notable changes to **Tyre Inventory** are documented here.

## [1.4.0] - 2026-09-10

### Added
- **Sales history** — daily/monthly revenue dashboard, invoice list with full discount + TVA breakdown,
  PDF reprint, and "record payment" for partial/credit invoices.
- **Shop settings** — shop name, address, phone, RC/ICE and TVA rate (default 20%).
  Printed on the PDF invoice header (shop name + legal strip) and used for TVA calculation.
- **Stock movements** — journal of every change: SALE, RESTOCK, ADJUST, DELETE with quantity deltas,
  timestamps and notes; filterable chips. Completed sales and manual +/- restocking are logged automatically.
- **Cost price & profit** — new `costPrice` and `supplier` fields; per-item margin shown on the stock list
  and monthly profit computed in Sales history.
- **Client picker** — clients are remembered from previous invoices and offered in the invoice builder.
- **Payment status** — Cash / Partial / Credit with paid amount; due amounts tracked on each invoice.
- **Stock ageing** — an "OLD" badge with age in days for tyres stocked longer than 120 days.
- **Item photos** — attach a photo per tyre via camera or gallery (local cache, shown in the editor).
- **Daily auto-backup** — automated database copy to the app's external-files backups folder
  (7-day retention) via WorkManager.
- **Unit tests** — JVM tests for invoice totals (subtotal/discount/TVA), CSV import and the item model.
- **GitHub Actions** — build & unit test pipeline on `main` and pull requests.
- CSV import now also reads cost price and supplier columns.

### Changed
- Database upgraded to **schema v4** (new tables: invoices, invoice_lines, clients, movements; new
  item columns) with full in-place migration from v1–v3.
- Invoice PDF totals now include TVA and the shop header; share intent grants its content URI via ClipData.
- Invoice numbering is assigned once per sale and stored with the invoice (matches the PDF filename).
- Restocking via the `+` stepper logs a RESTOCK movement; `-` logs ADJUST.

### Fixed
- Language switch (EN/FR/AR) not persisting / no-op on WayDroid API 33 — replaced AppCompatDelegate /
  LocaleManager with a shared-preferences locale applied in `attachBaseContext`.
- Invoice PDF share crashing / SecurityException in the share sheet — added `invoices/` cache path and
  a granted content URI (`ClipData`).
- Record-payment entry now **adds** to the already-paid amount instead of overwriting it.

## [1.3.0] - 2026

- Rebuilt stock screen with M3 theming, gradient stats hero, pill quantity steppers and filter chips.
- Added invoice builder with A4 PDF generation (client/phone/discount, editable unit prices, DH totals).
- Export/import CSV, manual database backup/restore, barcode scanning, low-stock notifications.
- In-app language switch (EN / FR / AR).

## [1.2.0] - 2026

- Advanced filtering (rim → size → brand) and search, SKU field, sort options.

## [1.1.0] - 2026

- Initial room-based inventory (categories, quantities, thresholds).

## [1.0.0] - 2026

- First functional version: add/edit/delete tyres, low-stock tracking.