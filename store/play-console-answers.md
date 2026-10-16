# Play Console answers — Crazy Cards

Answers for every form needed to publish to **Closed testing**. They are based on the actual app:
Firebase Auth and Firestore only, no ads, no analytics, no in-app purchases, no ad ID, and only the
INTERNET and ACCESS_NETWORK_STATE permissions declared by the app.

Policy URLs:

- Privacy policy: `https://github.com/Pradeepg21/crazy-cards/blob/main/PRIVACY.md`
- Account deletion: `https://github.com/Pradeepg21/crazy-cards/blob/main/ACCOUNT_DELETION.md`

The reviewer login is **not** stored here (the repo is public). It is in the chat where it was
created: `support.satyadigital+playreview@gmail.com` plus the password you saved.

---

## 1. Policy and programs → App content

### Privacy policy
- URL: the privacy policy URL above.

### Ads
- **Does your app contain ads?** No, my app does not contain ads.

### App access
- **All or some functionality in my app is restricted.** Add instructions:
  - Name: `Crazy Cards reviewer login`
  - Username: `support.satyadigital+playreview@gmail.com`
  - Password: (the saved reviewer password)
  - Any other information required to access your app:
    ```
    Offline play needs no login: tap "Play vs Computer" on the home screen.

    Online multiplayer needs sign-in:
    1. Tap "Sign in for online play".
    2. Enter the email and password above and tap "Sign in" (use the email form, not "Continue with Google").
    3. Tap "Create online game", add computer players with "+ Add bot", then tap "Start game".
    To test joining, open the app on a second device signed in with any account, tap "Join with code" and enter the 6-character game code shown in the lobby.
    ```
  - Leave "No other information is required" unticked and "Allow Android to use the credentials for performance and app compatibility testing" ticked.

### Content rating (IARC questionnaire)
- Email: `support.satyadigital@gmail.com`
- Category: **Game**. (If asked for a sub-type: casual/card game. Not a gambling app.)
- Answers:

| Question | Answer |
|---|---|
| Violence (any kind, including cartoon or fantasy) | No |
| Fear / horror | No |
| Sexuality or nudity | No |
| Language (profanity, crude humour) | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Gambling: real-money gambling | No |
| Gambling: simulated gambling (casino games, betting with virtual chips or coins) | No. It's a colour/number matching card game with points and no betting. |
| Miscellaneous: do users interact or exchange info (e.g. chat, user-generated names)? | **Yes**. Players see each other's display names in online games. There is no chat. |
| Shares the user's current physical location with other users | No |
| Allows users to buy digital goods | No |
| Unrestricted internet access (browser) | No |
| Is this a web browser or search engine? | No |
| Promotes or sells age-restricted products | No |
| Contains "loot boxes" or randomised paid items | No |

- Expected result: **Everyone / PEGI 3 / IARC 3+**, with the interactive element "Users Interact".

### Target audience and content
- **Target age groups:** tick **13–15**, **16–17** and **18 and over**. Don't tick any under-13 group,
  or the app falls under the Families policy and its stricter requirements (the privacy policy says
  the app isn't directed to children under 13).
- **Could your store listing unintentionally appeal to children?** No.
- Store presence/appeal to children: No.

### News apps
- **Is your app a news app?** No.

### COVID-19 contact tracing and status apps (if shown)
- My app is not a publicly available COVID-19 contact tracing or status app.

### Data safety
**Overview**
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Which of the following methods of account creation does your app support? | **Username and password** and **OAuth** (Sign in with Google) |
| Delete account URL | the account deletion URL above |
| Do you provide a way for users to request that some or all of their data is deleted, without requiring them to delete their account? | No |

**Data types — tick only these:**
| Category | Data type |
|---|---|
| Personal info | **Name** |
| Personal info | **Email address** |
| Personal info | **User IDs** |
| App activity | **App interactions** (game moves and the "last seen" connection time) |

Leave every other category unticked: location, financial info, health and fitness, messages,
photos and videos, audio, files and docs, calendar, contacts, web browsing, app info and
performance, and device or other IDs.

**For each of the 4 ticked types, answer the same way:**
| Question | Answer |
|---|---|
| Is this data collected, shared, or both? | **Collected** (not shared; Google acting as our service provider doesn't count as sharing) |
| Is this data processed ephemerally? | No |
| Is this data required for your app, or can users choose whether it's collected? | **Users can choose** (offline play needs no account) |
| Why is this user data collected? | Name, Email address, User IDs: **App functionality** and **Account management**. App interactions: **App functionality** only. |

### Government apps
- No.

### Financial features
- My app doesn't provide any financial features.

### Health apps
- My app does not have any health features.

### Advertising ID
- **Does your app use advertising ID?** No.

### Photo and video permissions / other sensitive permissions
- Not shown. The app requests none of these permissions.

---

## 2. Grow users → Store presence → Main store listing
Copy from `store/listing.md`: app name, short and full descriptions, icon, feature graphic, and
phone and tablet screenshots.

## 3. Grow users → Store presence → Store settings
| Field | Value |
|---|---|
| App or game | Game |
| Category | Card |
| Tags | Card, Casual, Multiplayer, Offline, Family, Party (choose the closest available) |
| Email address | support.satyadigital@gmail.com |
| Phone / Website | Leave blank (optional) |
| External marketing | Your choice (default on is fine) |

---

## 4. Test and release → Testing → Closed testing
1. **Create track** (or use the default "Closed testing - Alpha").
2. **Countries/regions:** add **India**. You can add more, or tick all.
3. **Testers:** create an email list (e.g. "Crazy Cards testers") with at least **12 Gmail
   addresses**. Save, then copy the **opt-in link** and send it to them.
   - New personal developer accounts need **12+ testers opted in for 14 days in a row** before
     applying for production access.
   - Feedback URL or email: `support.satyadigital@gmail.com`
4. **Create new release:**
   - Upload a **signed** 1.3.1 (code 7) bundle. Sign it with the upload key Play already knows
     (SHA-1 `FB:12:D1:7B…A0:65`): Android Studio → Build → Generate Signed App Bundle, or
     `gradlew bundleRelease` with `keystore.properties` in the project root. Play rejects
     bundles signed with any other key.
   - Release name: `1.3.1 (7)`
   - Release notes:
     ```
     <en-US>
     First test release of Crazy Cards.
     • Play offline against 1–9 computer players
     • Online multiplayer with a 6-character game code (up to 10 players)
     • Rules in 31 languages
     Please report any problems to support.satyadigital@gmail.com
     </en-US>
     ```
5. **Next → Save → Go to Publishing overview → Send changes for review.**

### Play App Signing
On the first upload, accept **Play App Signing** (Google manages the app signing key, and your
`.jks` becomes the upload key).

**After that first upload, add Play's SHA-1 to Firebase, or Google sign-in will fail on Play installs:**
Play Console → Test and release → App integrity → **App signing key certificate** → copy the
SHA-1 → Firebase Console → Project settings → Android app `com.satya.uno` → **Add fingerprint**.
Email sign-in works without this step.
