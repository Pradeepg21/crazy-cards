# Project Rules

- `SPEC.md` is the source of truth for how this app is versioned and released. Read it before every change.
- **Every code change bumps the version** in `app/build.gradle.kts`: `appVersionCode` +1 (always) and `appVersionName` per semver (PATCH fix, MINOR feature, MAJOR breaking). Also update the header table in `SPEC.md` and add a row to the Change Log in `SPEC.md` §4. One bump per task, not per edit.
- `appVersionCode` must be higher than `lastUploadedVersionCode` in `play-upload.properties` — Play rejects reused codes, and release builds fail otherwise.
- After the user uploads a build to Play Console, set `lastUploadedVersionCode` in `play-upload.properties` to that build's code.
- Firestore rules in `firestore.rules` are shared with the NCERT app (same Firebase project). Never drop the NCERT section.
