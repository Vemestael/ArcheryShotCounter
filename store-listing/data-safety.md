# Play Console — Data Safety Form Reference

Copy-paste source for Play Console → App content → Data safety.

## Does your app collect or share any of the required user data types?
**No.**

The app has no `INTERNET` permission and makes no network requests — verifiable in the
manifest and the public source at github.com/Vemestael/ArcheryShotCounter. All session/shot
data is stored in a local database on the device only. Play's Data Safety definition of
"collection" is about data transmitted off the device — local-only storage doesn't count,
so "No data collected" is the accurate answer here, not a simplification.

## Standard follow-up questions and how to answer them

- **Is all user data encrypted in transit?** — N/A, no data leaves the device.
- **Do you provide a way for users to request data deletion?** — Yes: delete individual
  sessions in-app, or uninstall the app to remove everything.
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
`INTERNET` permission at all. There is genuinely nothing to declare beyond "we store data
locally and don't send it anywhere."
