@echo off
REM Philips 55PUT7906 debloat + launcher setup.
REM Put this file and app-debug.apk in your platform-tools folder, then run:
REM    debloat.bat          (apply)
REM    debloat.bat undo     (re-enable everything that was disabled)
setlocal

set LAUNCHER=com.minimal.tvhome
set STOCK=com.google.android.tvlauncher

REM Disabled (reversible). Freeview UK apps, other-region apps, Google/Philips junk.
set BLOAT=uk.co.freeview.mdsclient uk.co.freeview.amc_catchup uk.co.freeview.onnow uk.co.freeview.uktv uk.co.freeview.bbc_sounds uk.co.freeview.explore uk.co.freeview.bbc uk.co.freeview.ch5 uk.co.freeview.itv uk.co.freeview.stv uk.co.freeview.amc_horror uk.co.freeview.ch4_vod de.swr.avp.ard.tv com.zdf.android.mediathek ru.ivi.client tv.wuaki.apptv fusion.android.tv.demo com.google.android.videos com.google.android.play.games com.google.android.tvrecommendations com.google.android.backdrop com.google.android.feedback com.google.android.partnersetup org.droidtv.nettv.market org.droidtv.nettvregistration org.droidtv.candeebug com.tpv.xmic.ots.cms.client com.mediatek.android.leanbacklauncher.partnercustomizer

REM Uninstalled for this user: apps you said you don't use.
set REMOVE=ch.protonvpn.android com.FDGEntertainment.redball4.gp com.streamflixreborn.streamflix me.zhanghai.android.files

set /p TV=TV IP address:
adb connect %TV%:5555
adb devices

if /i "%1"=="undo" goto undo

REM ---- apply ----
adb shell pm list packages %LAUNCHER% | find "%LAUNCHER%" >nul
if errorlevel 1 (
  echo Launcher not installed, installing app-debug.apk ...
  adb install -r app-debug.apk
  adb shell pm list packages %LAUNCHER% | find "%LAUNCHER%" >nul
  if errorlevel 1 (
    echo Launcher install failed. Stopping before touching anything.
    exit /b 1
  )
)

for %%P in (%BLOAT%) do (
  echo Disabling %%P
  adb shell pm disable-user --user 0 %%P
)
for %%P in (%REMOVE%) do (
  echo Removing %%P
  adb shell pm uninstall --user 0 %%P
)

adb shell cmd package set-home-activity %LAUNCHER%/.MainActivity

echo.
echo Now press Home on the remote. If you see your new launcher, run this to finish:
echo    adb shell pm disable-user --user 0 %STOCK%
echo (undo with: debloat.bat undo)
exit /b 0

:undo
for %%P in (%BLOAT% %STOCK%) do (
  echo Enabling %%P
  adb shell pm enable %%P
)
echo Done. Removed user apps (Proton, Redball, StreamFlix, Material Files) must be reinstalled manually.
exit /b 0
