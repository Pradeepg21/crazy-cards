# Privacy Policy — UNO Multiplayer

**Effective date:** 14 October 2026
**App:** UNO Multiplayer (Android, package `com.satya.uno`)
**Developer:** Satya Digital
**Contact:** support.satyadigital@gmail.com

This policy explains what information UNO Multiplayer ("the app", "we") collects, why, and the
choices you have. The app has no ads, no analytics or tracking tools, and we never sell your data.

## 1. Playing offline

You can play against computer players without an account. In this mode nothing you do leaves your
phone. The app saves the following only on your device:

- the player name you type in
- an unfinished offline game, so you can continue it later
- your settings: number and level of computer players, and the language chosen on the Rules screen
- the code of an online game you can rejoin

This data is removed when you clear the app's storage or uninstall the app.

## 2. Your account (online play)

Online play needs an account. Sign-in is provided by Google Firebase Authentication.

| You sign in with | We receive and store |
|---|---|
| Email and password | Your email address and the display name you enter. Your password is handled by Firebase and stored only in a securely hashed form — we never see it. |
| Google | Your Google account's name, email address and profile photo link, as shared by Google when you choose to sign in. |

Each account also gets a random user ID. We use this information only to sign you in, show your
name at the game table, and let you rejoin your games.

## 3. Online game data

When you create or join an online game, the following is stored in Google Cloud Firestore:

- the game code, the players' user IDs and display names, and who is hosting
- the game itself: cards dealt, moves, turns, scores and timestamps
- a "last seen" time, updated about every 10 seconds while the app is open, so other players can
  see who is connected and the computer can play for someone who drops out

Your display name and your moves are visible to the other players in the same game. Your email
address is never shown to other players.

## 4. Device and network information

The app uses the Internet and network-state permissions only to connect to online games and to
tell you when your connection drops. Google's Firebase and sign-in services may process technical
data such as your IP address and device information to provide and secure their services. We do
not collect your location, contacts, photos, files, microphone or camera.

## 5. Service providers

We use Google services to run online play and sign-in:

- **Firebase Authentication** and **Cloud Firestore** (Google LLC)
- **Google Sign-In / Android Credential Manager** (Google LLC)

These providers process data on our behalf under their own terms. See the
[Google Privacy Policy](https://policies.google.com/privacy) and
[Firebase Privacy and Security](https://firebase.google.com/support/privacy).
We do not share your data with anyone else, except where required by law.

## 6. How long we keep data

- **Account information** is kept until you ask us to delete your account.
- **Online game records** are kept after a game ends. Unused lobbies close after 15 minutes and
  games with no moves close after 5 minutes, but their records remain stored until deleted on
  request.
- **On-device data** stays on your phone until you clear the app's storage or uninstall it.

## 7. Deleting your account and data

To delete your account and the game data linked to it, email **support.satyadigital@gmail.com**
from the address you signed in with (or tell us the email of your account), with the subject
**"Delete my UNO account"**. We will delete your account and remove or anonymise game records
linked to it within 30 days, and confirm by email.

## 8. Security

Data travels between the app and Google's servers over encrypted connections (HTTPS/TLS). Access
to game data is restricted by Firestore security rules so that only signed-in players can read
games, and players can only change games they belong to. No method of storage or transmission is
100% secure, but we take reasonable steps to protect your information.

## 9. Children

The app is not directed to children under 13, and we do not knowingly collect personal
information from them. If you believe a child has created an account, contact us and we will
delete it.

## 10. Your rights

Depending on where you live, you may have the right to access, correct or delete your personal
data, or to object to how it is used. To make a request, contact us at
support.satyadigital@gmail.com.

## 11. Changes to this policy

If we change this policy, we will update the effective date above and publish the new version at
this same address. Significant changes will also be noted in the app's release notes.

## 12. Contact

Questions about this policy or your data: **support.satyadigital@gmail.com**
