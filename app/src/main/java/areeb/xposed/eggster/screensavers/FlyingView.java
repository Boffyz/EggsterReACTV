package areeb.xposed.eggster.screensavers;

import android.animation.TimeAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * "Flying Through Space": pictures (or plain dots) fly toward you out of the dark, like the classic
 * Windows starfield / flying-objects screensavers. Used on the settings screen, in the full-screen
 * preview and as the system screensaver.
 */
public class FlyingView extends View {

    /** Pictures are rendered once at this size (longest side) and then scaled while flying. */
    private static final int SPRITE_PX = 256;

    private final FlyModel model = new FlyModel(new Random());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix matrix = new Matrix();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private List<Bitmap> bitmaps = Collections.emptyList();
    private boolean ready = false;
    private int loadToken = 0;

    private float speed = ScreensaverSettings.depthSpeed(ScreensaverSettings.SPEED_DEFAULT);
    private float sizeFactor = 1f;
    private boolean spin = true;

    /** Pixels of screen offset per unit of x / d (half the short side of the view). */
    private float spread = 1f;
    /** Size in pixels of an object at depth 1.0. */
    private float baseSize = 1f;

    private TimeAnimator animator;

    public FlyingView(Context context) {
        this(context, null);
    }

    public FlyingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.BLACK);
        loadParams();
        reloadSprites();
    }

    /** Re-reads speed, number of objects, size and spin from the saved settings. */
    public void loadParams() {
        final ScreensaverPrefs prefs = new ScreensaverPrefs(getContext());
        speed = ScreensaverSettings.depthSpeed(prefs.getSpeed());
        sizeFactor = ScreensaverSettings.sizeFactor(prefs.getSize());
        spin = prefs.isSpin();
        model.setCount(ScreensaverSettings.clampCount(prefs.getCount()));
        updateMetrics();
        invalidate();
    }

    /** Re-reads which picture sets are switched on and loads them in the background. */
    public void reloadSprites() {
        final int token = ++loadToken;
        final Context app = getContext().getApplicationContext();
        final Set<String> sets = new ScreensaverPrefs(app).getSets();
        final boolean dots = sets.contains(SpriteLibrary.DOTS);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Bitmap> loaded = SpriteLibrary.load(app, sets, SPRITE_PX);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (token != loadToken) {
                            return; // a newer reload replaced this one
                        }
                        bitmaps = loaded;
                        model.setSprites(loaded.size(), dots);
                        ready = true;
                        invalidate();
                    }
                });
            }
        }, "screensaver-sprites").start();
    }

    private void updateMetrics() {
        final float shortSide = Math.min(getWidth(), getHeight());
        spread = shortSide * 0.5f;
        baseSize = shortSide * 0.035f * sizeFactor;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateMetrics();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void startAnimation() {
        if (animator != null) {
            return;
        }
        animator = new TimeAnimator();
        animator.setTimeListener(new TimeAnimator.TimeListener() {
            @Override
            public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
                float dt = deltaTime / 1000f;
                if (dt > 0.05f) {
                    dt = 0.05f; // don't jump after a pause
                }
                model.update(dt, speed, spin);
                invalidate();
            }
        });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null) {
            animator.setTimeListener(null);
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!ready) {
            return;
        }
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;

        model.sortByDepth();
        final int[] order = model.order;
        for (int n = 0; n < order.length; n++) {
            final int i = order[n];
            final float depth = model.d[i];
            final float size = baseSize / depth;
            final float sx = cx + model.x[i] / depth * spread;
            final float sy = cy + model.y[i] / depth * spread;

            if (Math.abs(sx - cx) > cx + size || Math.abs(sy - cy) > cy + size) {
                model.respawn(i, false); // flew off the edge of the screen
                continue;
            }
            final float alpha = FlyModel.alpha(depth);
            if (alpha <= 0f) {
                continue;
            }
            final int kind = model.kind[i];
            if (kind < 0 || kind >= bitmaps.size()) {
                paint.setColor(Color.WHITE);
                paint.setAlpha((int) (alpha * 255f));
                canvas.drawCircle(sx, sy, Math.max(1f, size * 0.10f), paint);
            } else {
                final Bitmap bitmap = bitmaps.get(kind);
                final float scale = size / Math.max(bitmap.getWidth(), bitmap.getHeight());
                matrix.reset();
                matrix.postTranslate(-bitmap.getWidth() / 2f, -bitmap.getHeight() / 2f);
                matrix.postScale(scale, scale);
                if (spin) {
                    matrix.postRotate(model.rot[i]);
                }
                matrix.postTranslate(sx, sy);
                paint.setAlpha((int) (alpha * 255f));
                canvas.drawBitmap(bitmap, matrix, paint);
            }
        }
    }
}
