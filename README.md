# Archery Shot Counter

An automatic archery shot counter, built for enthusiasts who train enough to lose count and
don't want to fumble with a manual tally mid-session.

This is the umbrella project — it doesn't contain code itself, just links to the actual
repositories and the overall picture of where things stand.

## Repositories

- **[ArcheryShotCounter-wearos](https://github.com/Vemestael/ArcheryShotCounter-wearos)** — the Wear
  OS app. Detects shots from the wrist accelerometer's recoil spike, no buttons needed during
  shooting. Published, currently at v1.4.
- **[ArcheryShotCounter-android](https://github.com/Vemestael/ArcheryShotCounter-android)** —
  the phone companion app. Two-way syncs sessions with the watch (Wearable Data Layer API),
  editable history, JSON/CSV export-import. Data visualization and bow-mounted sensor support
  are planned next.

## Who it's for

1. **Enthusiasts** training regularly who want an honest shot count without breaking focus to
   tap a button.
2. **Competitive/serious athletes**, especially once the companion app adds richer training
   data and (planned) bow-orientation sensor support.
3. **Casual watch owners** who just don't want to hand-count.

## Status

- Wear OS app: feature-complete for solo use, in Google Play publication prep (closed testing).
- Android companion app: core sync + history + editing + export/import working, in field
  testing with a second user before wider release.

## License

Each repository carries its own license (MIT for the Wear OS app).
