package com.minimal.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.InputStream;

/**
 * Minimal Apple-TV-style home screen. Everything is sized relative to the real
 * screen (not dp), so it fits whatever resolution/density the TV reports.
 *
 * Edit APPS to change tiles: { label, tagline, accent colour, package names to try... }
 * Custom artwork: assets/tiles/<package>.png (16:9) replaces the app's own banner.
 * Wallpaper: assets/wallpaper.jpg. Press MENU on the remote for system settings.
 */
public class MainActivity extends Activity {

    private static final String[][] APPS = {
            {"Prime Video", "Movies and series", "#00A8E1", "com.amazon.amazonvideo.livingroom"},
            {"Netflix", "Watch now", "#E50914", "com.netflix.ninja"},
            {"Kodi", "Your media library", "#17B2E7", "org.xbmc.kodi"},
            {"SmartTube", "Videos without ads", "#FF3B30", "org.smarttube.stable", "com.teamsmart.videomanager.tv", "com.liskovsoft.smarttubetv.beta"},
    };

    private LinearLayout row;
    private TextView title, subtitle;
    private float s;          // scale: 1.0 == 1080p
    private int W, H;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        W = dm.widthPixels;
        H = dm.heightPixels;
        s = Math.min(W / 1920f, H / 1080f);
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
        if (row != null && row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    @Override
    public void onBackPressed() { }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            startSafely(new Intent(Settings.ACTION_SETTINGS));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private int px(float v) { return Math.round(v * s); }

    private void size(TextView t, float v) { t.setTextSize(TypedValue.COMPLEX_UNIT_PX, v * s); }

    private View buildRoot() {
        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);

        Drawable bg = loadAsset("wallpaper.jpg");
        if (bg == null) {
            bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF2A2140, 0xFF141226, 0xFF07070C});
        }
        ImageView bgView = new ImageView(this);
        bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgView.setImageDrawable(bg);
        root.addView(bgView, new FrameLayout.LayoutParams(-1, -1));

        // soft scrims for legibility: top (clock) and bottom (tiles)
        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x66000000, 0x00000000, 0x00000000, 0xB0000000}));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        // Brand label, top left
        TextView brand = new TextView(this);
        brand.setText("Home");
        brand.setTextColor(0xCCFFFFFF);
        size(brand, 34);
        brand.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        brand.setLetterSpacing(0.04f);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        bp.setMargins(px(110), px(70), 0, 0);
        root.addView(brand, bp);

        // Big clock + date, top right
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.END);
        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(0xFFFFFFFF);
        size(clock, 84);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setIncludeFontPadding(false);
        clockBox.addView(clock);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        date.setTextColor(0xCCFFFFFF);
        size(date, 30);
        date.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        clockBox.addView(date);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        cp.setMargins(0, px(52), px(110), 0);
        root.addView(clockBox, cp);

        // Focused-app headline, like the reference
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        title = new TextView(this);
        title.setTextColor(0xFFFFFFFF);
        size(title, 96);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setShadowLayer(px(14), 0, px(3), 0x88000000);
        hero.addView(title);
        subtitle = new TextView(this);
        subtitle.setTextColor(0xDDFFFFFF);
        size(subtitle, 34);
        subtitle.setShadowLayer(px(10), 0, px(2), 0x88000000);
        hero.addView(subtitle);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.BOTTOM);
        hp.setMargins(px(110), 0, 0, px(430));
        root.addView(hero, hp);

        // Tile row
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(px(110) - px(14), px(30), px(110), px(30));
        FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        rp.setMargins(0, 0, 0, px(110));
        root.addView(row, rp);

        return root;
    }

    private void populate() {
        row.removeAllViews();
        PackageManager pm = getPackageManager();
        int n = 0;
        for (String[] app : APPS) if (findPkg(pm, app) != null) n++;
        if (n == 0) n = 1;
        int gap = px(28);
        int avail = W - px(110) * 2;
        int tw = Math.min(px(400), (avail - gap * (n - 1)) / n);
        int th = tw * 9 / 16;

        for (String[] app : APPS) {
            String pkg = findPkg(pm, app);
            if (pkg == null) continue;
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch == null) launch = pm.getLaunchIntentForPackage(pkg);
            row.addView(buildTile(app[0], app[1], app[2], pkg, launch, pm, tw, th, gap));
        }
        if (row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    private String findPkg(PackageManager pm, String[] app) {
        for (int i = 3; i < app.length; i++) {
            if (pm.getLeanbackLaunchIntentForPackage(app[i]) != null
                    || pm.getLaunchIntentForPackage(app[i]) != null) return app[i];
        }
        return null;
    }

    private View buildTile(final String label, final String tagline, String accentHex, String pkg,
                           final Intent launch, PackageManager pm, int tw, int th, int gap) {
        final int accent = Color.parseColor(accentHex);

        FrameLayout tile = new FrameLayout(this);
        tile.setBackground(tileBg(0xFF1C1C22, 0, 0));
        tile.setClipToOutline(true);
        tile.setElevation(px(8));
        tile.setFocusable(true);
        tile.setClickable(true);

        ImageView art = new ImageView(this);
        boolean full = false;
        try {
            Drawable custom = loadAsset("tiles/" + pkg + ".png");
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
        } catch (Exception ignored) { }
        art.setScaleType(full ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        if (!full) {
            int p = th / 5;
            art.setPadding(p, p, p, p);
            tile.setBackground(tileBg(0xFF24242C, 0, 0));
        }
        tile.addView(art, new FrameLayout.LayoutParams(-1, -1));

        // subtle top sheen for a glassy feel
        View sheen = new View(this);
        sheen.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x26FFFFFF, 0x00FFFFFF}));
        tile.addView(sheen, new FrameLayout.LayoutParams(-1, th / 2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tw, th);
        lp.setMargins(gap / 2, 0, gap / 2, 0);
        tile.setLayoutParams(lp);

        tile.setOnClickListener(v -> startSafely(launch));
        tile.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.12f : 1f).scaleY(focused ? 1.12f : 1f).setDuration(180).start();
            v.setElevation(px(focused ? 28 : 8));
            v.setBackground(tileBg(0xFF1C1C22, focused ? 0xFFFFFFFF : 0, focused ? Math.max(3, px(4)) : 0));
            if (focused) {
                title.setText(label);
                subtitle.setText(tagline);
            }
        });
        return tile;
    }

    private GradientDrawable tileBg(int fill, int stroke, int strokeW) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(px(22));
        if (strokeW > 0) g.setStroke(strokeW, stroke);
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
        } catch (Exception ignored) { }
    }
}
