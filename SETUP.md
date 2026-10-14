# UNO Multiplayer — setup & release

## 1. Firebase (needed for sign-in and online play)
Offline play vs the computer works without this.

1. In the Firebase console, add an **Android app** with package `com.satya.uno`
   (or reuse an existing project). Add your debug and release **SHA-1** fingerprints
   (`gradlew signingReport`) — Google sign-in needs them.
2. Download `google-services.json` into `app/`. The build picks it up automatically.
3. **Authentication → Sign-in method**: enable **Email/Password** and **Google**.
4. **Firestore Database**: create it, then paste `firestore.rules` into the Rules tab and publish.
5. Optional cleanup: add a TTL policy on collection `games`, field `createdAt` (e.g. 1 day),
   so finished rooms are deleted automatically.

## 2. Release build
1. Create an upload key: `keytool -genkey -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
2. Create `keystore.properties` in the project root (git-ignored):
   ```
   storeFile=upload.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
3. `gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab` (upload to Play Console).
4. Bump `versionCode` in `app/build.gradle.kts` for every upload.

## 3. Store listing assets
- `store/icon-512.png` — Play Store icon (512×512)
- `store/feature-graphic-1024x500.png` — feature graphic
- Screenshots: take them from a phone/emulator (at least 2, 1080×1920 recommended).

## How online play behaves
- Host taps **Create online game** → gets a 6-character code → **Share code** / **Copy to clipboard**.
- Friends tap **Join with code**. Host can add computer players, then **Start game**.
- The dealer gets **Shuffle & Deal** each round; the dealer seat rotates. Play continues until every
  player has finished, then the next round starts with the same table and running scores.
- 30-second turn limit. A player who loses connection or leaves the app shows as *offline* after
  25 s and the computer plays for them until they return (**Rejoin** on the home screen).
- No human move for 5 minutes (15 minutes in a lobby) closes the game on every phone.
