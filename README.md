# MADevelopments RoofHood Remote Control

A personal Android app that replaces a dead infrared (IR) remote for a Golden MOD 3004 90 BL range hood (90 cm, black, made in Turkey). It sends IR commands from the phone's IR blaster and was built for a Poco X7 Pro.

## Features
- Remote-style screen: ON/OFF, Light, AUTO, Fan −, Fan +, Timer
- Built-in codes for the buttons found on this hood
- Learn mode: scans codes one by one (like Mi Remote's test button) until the hood reacts, then saves the code to a button
- Auto-scan with adjustable delay, plus Prev / Send again controls
- Protocols scanned: NEC, NEC-Extended, Samsung, Sony 12-bit, RC5
- Long-press any button to re-learn it; "Reset to defaults" restores the built-in codes

## Codes found for this hood
All codes use the NEC protocol with address 0x00.

| Button | Command |
|---|---|
| ON/OFF | 0x40 |         [As Default]
| Light | 0x47 |          [As Default]
| Fan − | 0x07 |          [As Default]
| Fan + | 0x09 |          [As Default]
| AUTO | 0x45 |           [As Default]
| Timer | not found yet | [As Default]

## Requirements
- An Android phone with an IR blaster (Android 11 or newer)
- A hood that uses an infrared remote. RF (radio) remotes will not work

## Build the APK
No Android Studio is needed. GitHub builds it for you:
1. Open the **Actions** tab and select **Build APK**
2. Click **Run workflow** (it also runs on every commit)
3. When the run is green, download **HoodRemote-apk** from the Artifacts section
4. Unzip it and install `app-debug.apk` on the phone

## Project layout
- `app/src/main/java/com/hood/remote/Ir.kt` – IR protocol encoders and the code scanner list
- `app/src/main/java/com/hood/remote/MainActivity.kt` – remote screen and learn screen
- `.github/workflows/build.yml` – cloud build

## Notes
- Each command is sent as a single frame, because the hood treats repeated frames as extra button presses.
- Point the top edge of the phone at the hood when pressing buttons.
