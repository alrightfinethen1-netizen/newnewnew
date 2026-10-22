package com.minimal.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.os.Build;
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

    private static final int GLASS = 0x33FFFFFF;
    private static final int GLASS_FOCUS = 0x59FFFFFF;
    private static final int GLASS_EDGE = 0x33FFFFFF;

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
        tile.setBackground(tileBg(GLASS, GLASS_EDGE, 2));
        tile.setClipToOutline(true);
        tile.setElevation(px(8));
        tile.setFocusable(true);
        tile.setClickable(true);

        ImageView art = new ImageView(this);
        Drawable custom = loadAsset("tiles/" + pkg + ".png");
        if (custom != null) {
            art.setImageDrawable(custom);          // your own PNG is shown as-is
        } else {
            Bitmap white = whiteGlyph(pm, pkg);
            if (white != null) art.setImageBitmap(white);
        }
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = th * 22 / 100;
        art.setPadding(pad, pad, pad, pad);
        tile.addView(art, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tw, th);
        lp.setMargins(gap / 2, 0, gap / 2, 0);
        tile.setLayoutParams(lp);

        tile.setOnClickListener(v -> startSafely(launch));
        tile.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.12f : 1f).scaleY(focused ? 1.12f : 1f).setDuration(180).start();
            v.setElevation(px(focused ? 28 : 8));
            v.setBackground(tileBg(focused ? GLASS_FOCUS : GLASS, focused ? 0xFFFFFFFF : GLASS_EDGE, focused ? Math.max(3, px(4)) : 2));
            if (focused) {
                title.setText(label);
                subtitle.setText(tagline);
            }
        });
        return tile;
    }

    /** Turns the app's own installed icon into a white glyph on transparent. */
    private Bitmap whiteGlyph(PackageManager pm, String pkg) {
        try {
            Drawable d = pm.getApplicationIcon(pkg);
            boolean adaptive = false;
            if (Build.VERSION.SDK_INT >= 26 && d instanceof AdaptiveIconDrawable) {
                Drawable fg = ((AdaptiveIconDrawable) d).getForeground();
                if (fg != null) { d = fg; adaptive = true; }
            }
            int n = 432;
            Bitmap src = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(src);
            d.setBounds(0, 0, n, n);
            d.draw(c);
            return whiten(src, adaptive);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap whiten(Bitmap src, boolean forceAlpha) {
        int w = src.getWidth(), h = src.getHeight();
        int[] p = new int[w * h];
        src.getPixels(p, 0, w, 0, 0, w, h);

        int opaque = 0;
        int[] cnt = new int[4096];
        long[] sr = new long[4096], sg = new long[4096], sb = new long[4096];
        for (int c : p) {
            if ((c >>> 24) < 250) continue;
            opaque++;
            int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
            int k = ((r >> 4) << 8) | ((g >> 4) << 4) | (b >> 4);
            cnt[k]++; sr[k] += r; sg[k] += g; sb[k] += b;
        }
        // Solid-background icon (opaque, mostly one colour)? Knock the background out.
        boolean knock = !forceAlpha && opaque > p.length * 0.6;
        int bgR = 0, bgG = 0, bgB = 0;
        if (knock) {
            int best = 0;
            for (int k = 1; k < 4096; k++) if (cnt[k] > cnt[best]) best = k;
            bgR = (int) (sr[best] / cnt[best]);
            bgG = (int) (sg[best] / cnt[best]);
            bgB = (int) (sb[best] / cnt[best]);
        }

        int[] out = new int[p.length];
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int i = 0; i < p.length; i++) {
            int c = p[i];
            int a = c >>> 24;
            int alpha = a;
            if (knock) {
                int dr = Math.abs(((c >> 16) & 255) - bgR);
                int dg = Math.abs(((c >> 8) & 255) - bgG);
                int db = Math.abs((c & 255) - bgB);
                int dist = Math.max(dr, Math.max(dg, db));
                int ramp = Math.max(0, Math.min(255, (dist - 28) * 255 / 72));
                alpha = a * ramp / 255;
            }
            out[i] = (alpha << 24) | 0x00FFFFFF;
            if (alpha > 40) {
                int x = i % w, y = i / w;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
        Bitmap o = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888);
        if (maxX < 0) return o;
        return Bitmap.createBitmap(o, minX, minY, maxX - minX + 1, maxY - minY + 1);
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
