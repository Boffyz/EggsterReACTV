package areeb.xposed.eggster.screensavers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Saves the screensaver's settings (shared by the settings screen, the preview and the system screensaver). */
public final class ScreensaverPrefs {

    private static final String FILE = "screensaver_prefs";
    private static final String KEY_SETS = "sets";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_COUNT = "count";
    private static final String KEY_SIZE = "size";
    private static final String KEY_SPIN = "spin";
    private static final String KEY_ROT_SPEED = "rot_speed";
    private static final String KEY_ROT_DIR = "rot_dir";
    private static final String KEY_TUMBLE = "tumble";

    private final SharedPreferences prefs;

    public ScreensaverPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Which picture sets are switched on (always a fresh copy that is safe to change). */
    public Set<String> getSets() {
        final Set<String> saved = prefs.getStringSet(KEY_SETS, null);
        if (saved == null) {
            return SpriteLibrary.defaultSets();
        }
        return new HashSet<>(saved);
    }

    public void setSets(Set<String> sets) {
        prefs.edit().putStringSet(KEY_SETS, new HashSet<>(sets)).apply();
    }

    public int getSpeed() {
        return ScreensaverSettings.clamp(prefs.getInt(KEY_SPEED, ScreensaverSettings.SPEED_DEFAULT),
                ScreensaverSettings.SPEED_MIN, ScreensaverSettings.SPEED_MAX);
    }

    public void setSpeed(int value) {
        prefs.edit().putInt(KEY_SPEED, value).apply();
    }

    public int getCount() {
        return ScreensaverSettings.clampCount(prefs.getInt(KEY_COUNT, ScreensaverSettings.COUNT_DEFAULT));
    }

    public void setCount(int value) {
        prefs.edit().putInt(KEY_COUNT, value).apply();
    }

    public int getSize() {
        return ScreensaverSettings.clamp(prefs.getInt(KEY_SIZE, ScreensaverSettings.SIZE_DEFAULT),
                ScreensaverSettings.SIZE_MIN, ScreensaverSettings.SIZE_MAX);
    }

    public void setSize(int value) {
        prefs.edit().putInt(KEY_SIZE, value).apply();
    }

    public int getRotSpeed() {
        return ScreensaverSettings.clamp(prefs.getInt(KEY_ROT_SPEED, ScreensaverSettings.ROT_DEFAULT),
                ScreensaverSettings.ROT_MIN, ScreensaverSettings.ROT_MAX);
    }

    public void setRotSpeed(int value) {
        prefs.edit().putInt(KEY_ROT_SPEED, value).apply();
    }

    public int getRotDir() {
        return ScreensaverSettings.clamp(prefs.getInt(KEY_ROT_DIR, ScreensaverSettings.DIR_MIXED),
                ScreensaverSettings.DIR_MIXED, ScreensaverSettings.DIR_CCW);
    }

    public void setRotDir(int value) {
        prefs.edit().putInt(KEY_ROT_DIR, value).apply();
    }

    public boolean isTumble() {
        return prefs.getBoolean(KEY_TUMBLE, false);
    }

    public void setTumble(boolean value) {
        prefs.edit().putBoolean(KEY_TUMBLE, value).apply();
    }

    public boolean isSpin() {
        return prefs.getBoolean(KEY_SPIN, true);
    }

    public void setSpin(boolean value) {
        prefs.edit().putBoolean(KEY_SPIN, value).apply();
    }
}
