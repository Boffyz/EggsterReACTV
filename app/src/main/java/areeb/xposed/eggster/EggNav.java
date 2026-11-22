package areeb.xposed.eggster;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import areeb.xposed.eggster.ui.MainActivity;

/**
 * Decides where the back button goes from inside an egg screen:
 *
 *  - egg opened from Settings > About phone (through the Xposed hook): straight back to About phone
 *  - egg opened from Eggster's own list: back to Eggster's list (even if a later egg stage
 *    cleared the task and the list has to be recreated)
 *  - egg opened some other way (for example from a Neko notification): normal back
 *
 * Back on Eggster's list itself is not handled here: it closes the app as usual.
 */
public final class EggNav {

    /** Added by the Xposed hook when it opens an egg; carried along to every later stage. */
    public static final String EXTRA_FROM_SETTINGS = "areeb.xposed.eggster.FROM_SETTINGS";

    private EggNav() {
    }

    /** Call from onBackPressed() of every egg screen. */
    public static void back(Activity activity) {
        final Intent mine = activity.getIntent();
        if (mine != null && mine.getBooleanExtra(EXTRA_FROM_SETTINGS, false)) {
            // The egg lives in its own task, started on top of Settings: removing that task
            // reveals the Settings task again, with the About phone page on top.
            activity.finishAndRemoveTask();
            return;
        }
        if (openedFromThisApp(activity)) {
            // CLEAR_TOP + SINGLE_TOP: if the list is still underneath, just return to it;
            // if an egg stage cleared the task, a fresh list is created.
            activity.startActivity(new Intent(activity, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            activity.finish();
            return;
        }
        activity.finish();
    }

    /** Copies the "opened from Settings" mark onto the intent for the next egg stage. */
    public static Intent carry(Activity from, Intent next) {
        if (next != null) {
            final Intent mine = from.getIntent();
            if (mine != null && mine.getBooleanExtra(EXTRA_FROM_SETTINGS, false)) {
                next.putExtra(EXTRA_FROM_SETTINGS, true);
            }
        }
        return next;
    }

    private static boolean openedFromThisApp(Activity activity) {
        if (Build.VERSION.SDK_INT < 22) {
            return true; // getReferrer() needs API 22; older phones: assume the list opened it
        }
        final Uri referrer = activity.getReferrer();
        return referrer != null
                && "android-app".equals(referrer.getScheme())
                && activity.getPackageName().equals(referrer.getAuthority());
    }
}
