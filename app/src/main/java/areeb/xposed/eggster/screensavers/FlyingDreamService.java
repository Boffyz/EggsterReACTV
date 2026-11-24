package areeb.xposed.eggster.screensavers;

import android.service.dreams.DreamService;

/** Lets "Flying Through Space" be chosen as the phone's screensaver (Settings > Display > Screen saver). */
public class FlyingDreamService extends DreamService {

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false); // a touch wakes the phone up
        setFullscreen(true);
        setContentView(new FlyingView(this));
    }
}
