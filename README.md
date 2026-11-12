# PixelSim - Android Simulator

PixelSim is a full phone-OS simulator Android app running inside native Jetpack Compose & Material You.

## Installation on Honor X9d (MagicOS 9)

1. Enable **"Install unknown apps"** for your browser or file manager in system settings.
2. Transfer and install the debug APK.
3. Open PixelSim.

## Fixing Alarm and Notification Delays

MagicOS 9 has aggressive background restrictions. If alarms or notifications are delayed:

1. Open **Settings** inside PixelSim.
2. Go to **MagicOS Reliability Guide**.
3. Follow the 5-step guide to:
   - Set Battery usage to **"Unrestricted"**.
   - Set App launch to **"Manage manually"** (Allow Auto-launch, Secondary launch, and Run in background).
   - **Lock the app in Recents** (Swipe down on the app card in system Recents).
   - Grant **Notification permissions**.
   - Grant **Exact Alarm permission** (Android 12+).

## Device Profiles

You can switch between **Pixel 9 Style** (decorative bezel) and **Honor X9d Native** (fullscreen) in **Settings -> Display -> Device Profile & Frame Mode**.
