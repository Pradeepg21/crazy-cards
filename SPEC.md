# Crazy Cards — Spec

| | |
|---|---|
| Package | `com.satya.uno` |
| Current version | **1.3.0** (versionCode **6**) |
| Last uploaded to Play | versionCode **1** |
| Firebase project | `satya-digital-7248e` (shared with the NCERT app) |

## 1. Purpose
Crazy Cards — a color-and-number matching card game for Android (formerly "UNO Multiplayer"; renamed to avoid Mattel's UNO trademark): offline vs computer players, online multiplayer by game code,
official rules, Google and email/password sign-in. Setup and release steps are in `SETUP.md`.

## 2. Versioning & Change Process (MANDATORY)
Google Play only accepts a build whose `versionCode` is higher than every build uploaded before.

1. **Every code change bumps the version** in `app/build.gradle.kts`:
   - `appVersionCode` **+1, always** — even for one-line fixes, UI tweaks or dependency updates.
   - `appVersionName` as `MAJOR.MINOR.PATCH`:
     - PATCH (1.0.1 → 1.0.2) for bug fixes and small tweaks
     - MINOR (1.0.2 → 1.1.0) for new features
     - MAJOR (1.1.0 → 2.0.0) for breaking or flow-changing releases
   - Bump once per change/task, not once per edited file.
2. Update the header table above and add a row to the Change Log (§4).
3. `appVersionCode` must be **higher than `lastUploadedVersionCode`** in `play-upload.properties`.
   `gradlew bundleRelease` / `assembleRelease` fail with a clear message otherwise, so a reused
   code can never reach Play Console.
4. After uploading to Play Console, set `lastUploadedVersionCode` in `play-upload.properties`
   to the uploaded code and update "Last uploaded to Play" above.
5. Changes that don't touch the app (docs, store graphics, Firestore rules) don't need a bump.

## 3. Release checklist
1. Version bumped (§2) and Change Log updated.
2. `gradlew testDebugUnitTest` passes.
3. `gradlew bundleRelease` (needs `keystore.properties` with the upload key, SHA-1 `FB:12:D1:7B…A0:65`).
4. Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console.
5. Update `play-upload.properties` (§2.4).

## 4. Change Log
| Version | Code | Date | Changes |
|---|---|---|---|
| 1.3.0 | 6 | 2026-10-15 | Renamed to **Crazy Cards** (trademark). "UNO!" call is now "Last Card!" everywhere incl. all 31 rules files; rules title "How to play". New card art: upright diamond instead of tilted oval, four-color diamond wilds, Crazy Cards card back. New launcher/splash icon (three fanned cards), Play icon and feature graphic. |
| 1.2.0 | 5 | 2026-10-14 | Rules in 31 languages (English, 12 Indian, 18 foreign) with a dropdown picker and right-to-left layout for Urdu, Arabic and Persian. Each language is a small asset file loaded only when chosen. |
| 1.1.0 | 4 | 2026-10-14 | Rules screen in English, Hindi and Bengali with a language picker (defaults to the phone language, remembers the choice). |
| 1.0.2 | 3 | 2026-10-14 | Fix Google sign-in saying "isn't configured" in release builds: keep `default_web_client_id` from resource shrinking. |
| 1.0.1 | 2 | 2026-10-14 | Firebase connected (Google + email sign-in, online multiplayer). Light status-bar icons, tilted card ovals, solid dimming of unplayable cards, readable gold buttons, larger table on tablets. Release builds check the version against Play. |
| 1.0.0 | 1 | 2026-10-13 | First upload: offline vs computer, online rooms with share code, official rules, rejoin, idle close. |
