# Archery Shot Counter

An automatic archery shot counter, built for enthusiasts who train enough to lose count and
don't want to fumble with a manual tally mid-session.

This is the umbrella project — it doesn't contain code itself, just links to the actual
repositories and the overall picture of where things stand.

## Repositories

- **[ArcheryShotCounter-wearos](https://github.com/Vemestael/ArcheryShotCounter)** — the Wear
  OS app. Detects shots from the wrist accelerometer's recoil spike, no buttons needed during
  shooting. Published, currently at v1.4.
- **ArcheryShotCounter-android** — the phone companion app. Not started yet; will sync session
  data from the watch (Wearable Data Layer API) and eventually add data visualization and
  bow-mounted sensor integration.

## Who it's for

1. **Enthusiasts** training regularly who want an honest shot count without breaking focus to
   tap a button.
2. **Competitive/serious athletes**, especially once the companion app adds richer training
   data and (planned) bow-orientation sensor support.
3. **Casual watch owners** who just don't want to hand-count.

## Status

- Wear OS app: feature-complete for solo use, in Google Play publication prep (closed testing).
- Android companion app: architecture/scaffolding stage.

## License

Each repository carries its own license (MIT for the Wear OS app).
