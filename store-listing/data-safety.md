# Play Console — Data Safety Form Reference

Copy-paste source for Play Console → App content → Data safety.

## Does your app collect or share any of the required user data types?
**No.**

Neither the watch app nor the phone companion has the `INTERNET` permission or makes network
requests of its own — verifiable in the manifests and the public source at
github.com/Vemestael/ArcheryShotCounter-wearos and github.com/Vemestael/ArcheryShotCounter-android.
Session/shot data is stored in local databases on the user's devices. The only transfer is
watch ↔ phone sync over the Wear OS Data Layer (Google Play services), between the user's own
paired devices; when they aren't directly connected, Google Play services may relay it
encrypted via Google's servers. The developer never receives any data.

**Re-check this answer for the phone sync:** the original "No" was written when the watch app
was fully local. Confirm against Play's current Data safety guidance whether data relayed by
Google Play services between the user's own devices counts as "collected".

## Standard follow-up questions and how to answer them

- **Is all user data encrypted in transit?** — Yes: watch ↔ phone sync goes over the Wear OS
  Data Layer, which is encrypted.
- **Do you provide a way for users to request data deletion?** — Yes: delete individual
  sessions or clear all data in either app, or uninstall to remove everything.
- **Committed to Play Families Policy / target children?** — No, not designed for children.

## App content section (separate from Data Safety, same "App content" area)

- **Ads**: No ads.
- **Content rating questionnaire**: general-audience utility app, no violence/gambling/user
  communication/etc. — should land on the lowest rating tier (e.g. "Everyone" / "PEGI 3").
- **Target audience**: general audience, not primarily child-directed.
- **News app / COVID-19 app / government app / financial features / health app**: No to all
  — this is a sports training utility, not a regulated health app (it doesn't diagnose,
  treat, or give medical guidance).

## Why this is easy here

No analytics SDK, no crash reporting SDK, no ads SDK, no login/account system, no
`INTERNET` permission in either app. Data stays on the user's own watch and phone.
