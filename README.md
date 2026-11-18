# किराना कैलकुलेटर — Kirana Calculator (Kotlin + XML)

A single-screen, fully offline kirana (grocery) calculator built with **Kotlin** and
classic **XML layouts** (no Compose). The UI is Hindi, matching the reference design.

## The three modes

| Tab | Inputs | Result shown |
|---|---|---|
| पैसा निकालें (money) | वजन in grams + रेट ₹/kg | ₹ amount |
| ग्राम निकालें (grams) | रकम ₹ + रेट ₹/kg | weight in grams |
| बिल जोड़ें (bill) | one रकम at a time | running कुल बिल + item count |

Everything is calculated live as you type; `→` commits an entry (in bill mode it adds
the amount to the total, in the other modes it clears the amount for the next
customer while keeping the rate). `C` clears the active field; on an already-empty
amount in bill mode it resets the whole bill. All maths is on-device — no internet
permission is declared.

## Project layout

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/kiranacalculator/MainActivity.kt   # all UI logic
└── res/
    ├── layout/activity_main.xml                        # header, tabs, result card, 2 input cards, keypad, footer
    ├── drawable/                                       # rounded card / key / tab ripples, backspace & arrow icons
    └── values/                                         # colors.xml, strings.xml (Hindi), themes.xml
```

## Design tokens

- Background `#F1F5F3`, cards `#FFFFFF`, brand green `#14874C`, primary text `#1C2B23`,
  secondary `#6B7B72`, placeholder `#B3C0B9`.
- Cards: 18dp corner radius; active input card gets a 2dp green stroke.
- Keys: 16dp radius, 5dp gaps, 4×4 grid with weighted rows so it fits any screen height.
- Type scale: 24sp title, 40sp result, 30sp inputs, 26sp keys, 12sp card labels.

## Build

Open the folder in Android Studio and sync, or from the command line:

```
./gradlew assembleRelease
```

Release signing is read from Gradle properties so no key material lives in git:

```
./gradlew assembleRelease \
  -PKIRANA_STORE_FILE=../kirana-release.keystore \
  -PKIRANA_STORE_PASSWORD=*** -PKIRANA_KEY_ALIAS=kirana -PKIRANA_KEY_PASSWORD=***
```

The app uses only the Android framework, so it has no third-party dependencies.

## Changing things

- Colours → `res/values/colors.xml`; all wording → `res/values/strings.xml`.
- To translate the UI, copy `strings.xml` into `res/values-en/` and replace the Hindi text.
- The rate is ₹ per kilo; to support other units change the `/ 1000.0` factors in
  `compute()` inside `MainActivity.kt`.
