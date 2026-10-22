package com.minimal.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.InputStream;

/**
 * Minimal, Apple-TV-style home screen. One row, only the apps listed in APPS.
 *
 * Edit APPS to change what shows up. Each entry:
 *   { label, tile colour (hex), package name(s) to try... }
 *
 * Custom artwork: put a PNG named <package>.png in app/src/main/assets/tiles/
 * (e.g. org.xbmc.kodi.png, ideally 16:9, ~640x360) and it replaces the app's own banner.
 *
 * Press MENU on the remote to open system settings.
 */
public class MainActivity extends Activity {

    private static final String[][] APPS = {
            {"Prime Video", "#00A8E1", "com.amazon.amazonvideo.livingroom"},
            {"Netflix", "#141414", "com.netflix.ninja"},
            {"Kodi", "#17B2E7", "org.xbmc.kodi"},
            {"SmartTube", "#1C1C1E", "org.smarttube.stable", "com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta"},
    };

    private LinearLayout row;
    private float d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        d = getResources().getDisplayMetrics().density;
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(buildRoot());
    }

    @Override
    protected void onResume() {
        super.onResume();
        populate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home pressed while already here: jump back to first tile
        if (row != null && row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    @Override
    public void onBackPressed() {
        // Home screen: swallow back
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            startSafely(new Intent(Settings.ACTION_SETTINGS));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);

        // Background: optional assets/wallpaper.jpg, otherwise a dusk gradient
        Drawable bg = loadAsset("wallpaper.jpg");
        if (bg == null) bg = loadAsset("wallpaper.png");
        if (bg == null) {
            bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF2A2140, 0xFF141226, 0xFF07070C});
        }
        ImageView bgView = new ImageView(this);
        bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgView.setImageDrawable(bg);
        root.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        // Darken the lower part so tiles and labels stay readable
        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00000000, 0x00000000, 0xCC000000}));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        // Clock, top right
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(0xE6FFFFFF);
        clock.setTextSize(22);
        clock.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.setMargins(0, dp(36), dp(56), 0);
        root.addView(clock, cp);

        // The tile row, bottom area
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(dp(56), dp(24), dp(56), dp(24));
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        rp.setMargins(0, 0, 0, dp(64));
        root.setClipChildren(false);
        root.addView(row, rp);

        return root;
    }

    private void populate() {
        row.removeAllViews();
        PackageManager pm = getPackageManager();
        for (String[] app : APPS) {
            for (int i = 2; i < app.length; i++) {
                Intent launch = pm.getLeanbackLaunchIntentForPackage(app[i]);
                if (launch == null) launch = pm.getLaunchIntentForPackage(app[i]);
                if (launch == null) continue;
                row.addView(buildTile(app[0], app[1], app[i], launch, pm));
                break;
            }
        }
        if (row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    private View buildTile(String label, String colorHex, String pkg, final Intent launch, PackageManager pm) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setClipChildren(false);
        LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(dp(290), -2);
        cellLp.setMargins(dp(14), 0, dp(14), 0);
        cell.setLayoutParams(cellLp);

        FrameLayout tile = new FrameLayout(this);
        int base = Color.parseColor(colorHex);
        tile.setBackground(roundRect(base, 0, 0));
        tile.setElevation(dp(6));
        tile.setClipToOutline(true);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(false);
        tile.setClickable(true);

        // Artwork: custom PNG > app banner > app icon
        ImageView art = new ImageView(this);
        Drawable custom = loadAsset("tiles/" + pkg + ".png");
        boolean full = false;
        try {
            if (custom != null) {
                art.setImageDrawable(custom);
                full = true;
            } else {
                Drawable banner = pm.getApplicationBanner(pkg);
                if (banner != null) {
                    art.setImageDrawable(banner);
                    full = true;
                } else {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                    art.setImageDrawable(ai.loadIcon(pm));
                }
            }
        } catch (Exception ignored) {
        }
        art.setScaleType(full ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        int pad = full ? 0 : dp(38);
        art.setPadding(pad, pad, pad, pad);
        tile.addView(art, new FrameLayout.LayoutParams(-1, -1));

        cell.addView(tile, new LinearLayout.LayoutParams(dp(290), dp(163)));

        TextView name = new TextView(this);
        name.setText(label);
        name.setTextColor(0xFFFFFFFF);
        name.setTextSize(15);
        name.setAlpha(0f);
        name.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(-2, -2);
        nlp.topMargin = dp(18);
        cell.addView(name, nlp);

        tile.setOnClickListener(v -> startSafely(launch));
        tile.setOnFocusChangeListener((v, focused) -> {
            tile.animate().scaleX(focused ? 1.12f : 1f).scaleY(focused ? 1.12f : 1f)
                    .setDuration(160).start();
            tile.setElevation(dp(focused ? 18 : 6));
            tile.setBackground(roundRect(base, focused ? 0xFFFFFFFF : 0, focused ? dp(3) : 0));
            name.animate().alpha(focused ? 1f : 0f).setDuration(160).start();
        });
        return cell;
    }

    private GradientDrawable roundRect(int fill, int stroke, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(18));
        if (strokeWidth > 0) g.setStroke(strokeWidth, stroke);
        return g;
    }

    private Drawable loadAsset(String path) {
        try (InputStream in = getAssets().open(path)) {
            return new BitmapDrawable(getResources(), BitmapFactory.decodeStream(in));
        } catch (Exception e) {
            return null;
        }
    }

    private void startSafely(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    private int dp(int v) {
        return Math.round(v * d);
    }
}
