# Play Console — Store Listing Reference

Copy-paste source for Play Console → Grow → Store presence → Main store listing.
One listing covers both the Wear OS app and the phone companion (same package name).

## App name
Archery Shot Counter

## Short description (max 80 chars)
```
Count archery shots hands-free on Wear OS; review every session on your phone
```
(77 chars)

## Full description (max 4000 chars)
Plain text — Play doesn't render Markdown. (2518 chars)

```
Archery Shot Counter counts your shots automatically while you train — no buttons, no hand tally, no breaking focus between arrows. Your Wear OS watch does the counting; the companion app on your phone keeps your full training history.

Built by an archer who kept losing count mid-session and wanted real numbers on their training.

HOW IT WORKS
When the string is released, the recoil produces a sharp spike in wrist acceleration. The watch detects it, vibrates and adds the shot — whether you shoot compound, Olympic recurve, barebow or traditional. Detection depends on your bow, which arm wears the watch and your technique, so sensitivity and the cooldown between shots are adjustable, and you can always correct the count by hand.

ON YOUR WATCH
- Automatic shot detection from the wrist accelerometer
- Manual −/+ correction
- Start, pause, resume and end sessions
- Ends (series): set shots per end and see your progress through the current end
- Optional auto-pause after each end, so walking to the target isn't counted
- Sensitivity presets (High, Medium, Low) or your own threshold, plus a configurable shot cooldown
- Session history with date, time, duration and shot count
- Per-shot detail: the time and impact strength of every shot
- Edit a session's times, shot count or shots per end
- Energy efficiency: keeps the screen on but dimmed while counting, and can hand over to Always On Display while paused

ON YOUR PHONE
- Every session synced from your watch, grouped by year as your history grows
- A session in progress appears live, with its shot count updating in real time
- Per-shot detail for every session
- Edit start and end time, shot count and shots per end — changes sync back to the watch
- Two-way sync, deletions included
- Back up and restore your full history as JSON or CSV

The watch app works on its own. The phone app is a companion: it needs the watch app to sync sessions from.

MAKE IT YOURS
- Six colour themes, the same on watch and phone
- Three counter sizes on the watch
- 12 languages: English, Russian, Spanish, French, German, Portuguese, Chinese, Japanese, Korean, Arabic, Turkish, Hindi

PRIVACY
No accounts, no ads, no analytics, and no internet permission in either app. Your sessions stay on your own watch and phone.

REQUIREMENTS
Watch: Wear OS 3.0 or later (all Wear OS watches have the accelerometer it uses). Developed and tested on a Samsung Galaxy Watch 6 Classic.
Phone (optional): Android 8.0 or later with Google Play services, paired with the watch.
```

## What's new in 1.6 (max 500 chars)
Release notes for both the watch and phone builds. (455 chars)

```
- Redesigned watch and phone apps with a new look
- Six colour themes, the same on both devices
- Edit a session's start and end time, shot count and shots per end on the watch or phone
- Edits and deletions sync both ways
- Watch history refreshes live when you change a session on the phone
- More reliable shot detection with the screen dimmed: counting no longer hands off to Always On Display
- No more battery and Always On Display prompts at launch
```

## Category
Suggest: Health & Fitness (or Sports, depending on what's available at listing time)

## Contact details
- Email: vemestael@gmail.com
- Privacy policy: https://vemestael.github.io/ArcheryShotCounter/privacy-policy.html
  (served by GitHub Pages from `docs/` in this repo; the old `ArcheryShotCounter-wearos` URL redirects here)

## Graphics
Rendered by `Design/store-assets/build.py` (outside the repo) from raw v1.6 emulator captures.

- `store-listing/feature-graphic.png` — 1024×500, required for the listing
- `store-listing/screenshots/phone/` — 01_hero … 05_themes, 1080×1920 with captions (phone screenshots may carry captions/frames)
- `store-listing/screenshots/wear/` — 01_detecting … 07_themes, 480×480, raw UI only, no frame or text (Wear OS screenshot rules; min 384×384)

All are 24-bit PNG without alpha, as Play requires.
