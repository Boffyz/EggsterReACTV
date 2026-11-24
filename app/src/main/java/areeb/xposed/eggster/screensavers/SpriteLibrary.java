package areeb.xposed.eggster.screensavers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import areeb.xposed.eggster.R;

/** The pictures the screensaver can fly: sets of images that already live inside the app. */
public final class SpriteLibrary {

    public static final String DOTS = "dots";
    public static final String DESSERTS = "desserts";
    public static final String NYANDROID = "nyandroid";
    public static final String BEANS = "beans";
    public static final String STARS = "stars";
    public static final String EGG_LOGOS = "egglogos";
    public static final String PLATFORM_LOGOS = "platlogos";

    /** Every set, in the order they are listed on the settings screen. */
    public static final String[] SET_IDS = {
            DOTS, DESSERTS, NYANDROID, BEANS, STARS, EGG_LOGOS, PLATFORM_LOGOS
    };

    private static final String[] DESSERT_NAMES = {
            "dessert_android", "dessert_cupcake", "dessert_dandroid", "dessert_donut",
            "dessert_donutburger", "dessert_eclair", "dessert_flan", "dessert_froyo",
            "dessert_gingerbread", "dessert_honeycomb", "dessert_ics", "dessert_jandycane",
            "dessert_jellybean", "dessert_keylimepie", "dessert_kitkat", "dessert_petitfour",
            "dessert_zombiegingerbread", "jandycane"
    };

    private static final String[] BEAN_NAMES = {
            "redbean0", "redbean1", "redbean2", "redbeandroid"
    };

    private static final String[] EGG_LOGO_NAMES = {
            "logo_gb", "logo_hc", "logo_ics", "logo_jb", "logo_kk", "logo_l", "logo_lp", "logo_mm",
            "logo_n", "logo_nm", "logo_neko", "logo_oreo", "logo_oreo81", "logo_pie", "logo_ten",
            "logo_eleven", "logo_twelve", "logo_thirteen", "logo_fourteen", "logo_fifteen",
            "logo_sixteen", "logo_seventeen", "logo_lineage"
    };

    private static final String[] PLATFORM_LOGO_NAMES = {
            "platlogo_alt", "platlogo_gb", "platlogo_hc", "platlogo_ics", "platlogo_jb", "platlogo_kk"
    };

    private SpriteLibrary() {
    }

    /** What is switched on the first time the screensaver is used. */
    public static Set<String> defaultSets() {
        return new HashSet<>(Arrays.asList(DESSERTS, EGG_LOGOS));
    }

    /** The text shown next to a set's checkbox. */
    public static int labelRes(String setId) {
        switch (setId) {
            case DOTS:
                return R.string.ss_set_dots;
            case DESSERTS:
                return R.string.ss_set_desserts;
            case NYANDROID:
                return R.string.ss_set_nyandroid;
            case BEANS:
                return R.string.ss_set_beans;
            case STARS:
                return R.string.ss_set_stars;
            case EGG_LOGOS:
                return R.string.ss_set_egglogos;
            default:
                return R.string.ss_set_platlogos;
        }
    }

    /** The names of the drawables in a set (DOTS has none: it is drawn, not loaded). */
    static String[] drawableNames(String setId) {
        switch (setId) {
            case DESSERTS:
                return DESSERT_NAMES;
            case NYANDROID:
                return numbered("nyandroid", 0, 11, 2);
            case BEANS:
                return BEAN_NAMES;
            case STARS:
                return numbered("star", 0, 5, 1);
            case EGG_LOGOS:
                return EGG_LOGO_NAMES;
            case PLATFORM_LOGOS:
                return PLATFORM_LOGO_NAMES;
            default:
                return new String[0];
        }
    }

    private static String[] numbered(String prefix, int from, int to, int width) {
        final String[] names = new String[to - from + 1];
        for (int i = from; i <= to; i++) {
            names[i - from] = prefix + String.format(Locale.US, "%0" + width + "d", i);
        }
        return names;
    }

    /**
     * Loads the pictures of the chosen sets as bitmaps, each scaled so its longest side is maxPx.
     * Anything that can't be loaded is skipped. Call this off the main thread.
     */
    public static List<Bitmap> load(Context context, Set<String> sets, int maxPx) {
        final List<Bitmap> result = new ArrayList<>();
        for (String setId : SET_IDS) {
            if (!sets.contains(setId)) {
                continue;
            }
            for (String name : drawableNames(setId)) {
                final int resId = context.getResources()
                        .getIdentifier(name, "drawable", context.getPackageName());
                if (resId == 0) {
                    continue;
                }
                final Bitmap bitmap = render(context, resId, maxPx);
                if (bitmap != null) {
                    result.add(bitmap);
                }
            }
        }
        return result;
    }

    private static Bitmap render(Context context, int resId, int maxPx) {
        try {
            final Drawable drawable = ContextCompat.getDrawable(context, resId);
            if (drawable == null) {
                return null;
            }
            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            if (w <= 0 || h <= 0) {
                w = maxPx;
                h = maxPx;
            }
            final float scale = (float) maxPx / Math.max(w, h);
            final int bw = Math.max(1, Math.round(w * scale));
            final int bh = Math.max(1, Math.round(h * scale));
            final Bitmap bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, bw, bh);
            drawable.draw(canvas);
            return bitmap;
        } catch (Throwable t) {
            Log.w("SpriteLibrary", "Skipping a picture that failed to load", t);
            return null;
        }
    }
}
