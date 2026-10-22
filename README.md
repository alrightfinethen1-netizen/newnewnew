# Minimal TV Home

A one-row, Apple-TV-style home screen for Android TV. Shows only Prime Video, Netflix, Kodi and SmartTube, plus a clock. No rows, no ads, no recommendations.

## Build the APK
**Option A: Android Studio** (easiest)
1. File > Open > select this folder, let it sync.
2. Build > Build Bundle(s) / APK(s) > Build APK(s).
3. APK is at `app/build/outputs/apk/debug/app-debug.apk`.

**Option B: GitHub (no install)**
1. Push this folder to a new GitHub repo.
2. Actions tab > "Build APK" > download the `minimal-tv-home` artifact.

## Install on the TV
```
adb connect <TV_IP>:5555
adb install app-debug.apk
```

## Make it the default home
Press Home and pick "Home" (this app) if the TV asks, choosing "Always". If the stock launcher still wins:
```
adb shell pm disable-user --user 0 com.google.android.tvlauncher
```
(undo: `adb shell pm enable com.google.android.tvlauncher`). Keep the stock launcher
enabled until you've confirmed this app opens.

## Customise
- **Apps / colours:** edit `APPS` at the top of `MainActivity.java`.
- **Icons:** drop a 16:9 PNG (about 640x360) named `<package>.png` into `app/src/main/assets/tiles/`,
  e.g. `org.xbmc.kodi.png`, `com.netflix.ninja.png`.
- **Wallpaper:** put `wallpaper.jpg` in `app/src/main/assets/`.
- **Settings:** press MENU on the remote.
