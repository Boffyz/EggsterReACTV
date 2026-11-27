package areeb.xposed.eggster.screensavers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;

import java.io.File;
import java.util.Random;

/** The backgrounds behind the flying pictures: black, a few vivid starry spaces, or the user's own image. */
final class SpaceBackground {

    static final int BLACK = 0;
    static final int DEEP_SPACE = 1;
    static final int PINK_NEBULA = 2;
    static final int TEAL_NEBULA = 3;
    static final int CUSTOM = 4;
    static final int STYLE_COUNT = 5;

    private static final String CUSTOM_FILE = "screensaver_background.png";
    private static final int CUSTOM_MAX_PX = 1600;

    private SpaceBackground() {
    }

    static File customFile(Context context) {
        return new File(context.getFilesDir(), CUSTOM_FILE);
    }

    /** Copies the chosen image into the app as the custom background. Call off the main thread. */
    static boolean importCustom(Context context, Uri uri) {
        final Bitmap bitmap = CustomPictures.decode(context, uri, CUSTOM_MAX_PX);
        if (bitmap == null) {
            return false;
        }
        final boolean ok = CustomPictures.save(bitmap, customFile(context));
        bitmap.recycle();
        return ok;
    }

    /**
     * Draws the background at the given size. Returns null for plain black (nothing to draw) or if the
     * custom image is missing. The same picture comes out every time. Call off the main thread.
     */
    static Bitmap render(Context context, int style, int w, int h, float density) {
        try {
            switch (style) {
                case DEEP_SPACE:
                    return space(w, h, density, 1, 0xFF02030A, 0xFF0B0B2A,
                            new int[]{0xFF4B2A9A, 0xFF1E5FD6, 0xFF7A1FA2, 0xFF0B7C9A});
                case PINK_NEBULA:
                    return space(w, h, density, 2, 0xFF12020F, 0xFF2A0618,
                            new int[]{0xFFE91E63, 0xFFFF6F00, 0xFF8E24AA, 0xFFFFD54F});
                case TEAL_NEBULA:
                    return space(w, h, density, 3, 0xFF01100F, 0xFF02222B,
                            new int[]{0xFF00BFA5, 0xFF00ACC1, 0xFF1DE9B6, 0xFF3949AB});
                case CUSTOM:
                    return fromImage(context, w, h);
                default:
                    return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap space(int w, int h, float density, long seed, int top, int bottom,
                                int[] palette) {
        final Random random = new Random(seed * 7919L);
        final Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // dark sky, a little lighter toward the bottom
        paint.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // soft colored clouds, painted small and stretched up so they come out smooth
        final int cw = Math.max(8, w / 4);
        final int ch = Math.max(8, h / 4);
        final Bitmap clouds = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888);
        final Canvas cloudCanvas = new Canvas(clouds);
        for (int i = 0; i < 7; i++) {
            final float cx = random.nextFloat() * cw;
            final float cy = random.nextFloat() * ch;
            final float radius = (0.25f + 0.45f * random.nextFloat()) * Math.max(cw, ch);
            final int base = palette[i % palette.length] & 0x00FFFFFF;
            final int alpha = 70 + random.nextInt(70);
            paint.setShader(new RadialGradient(cx, cy, radius, base | (alpha << 24), base,
                    Shader.TileMode.CLAMP));
            cloudCanvas.drawCircle(cx, cy, radius, paint);
        }
        paint.setShader(null);
        canvas.drawBitmap(clouds, null, new Rect(0, 0, w, h), new Paint(Paint.FILTER_BITMAP_FLAG));
        clouds.recycle();

        // lots of small stars, some warm, some cool
        final int stars = Math.max(60, (int) ((long) w * h / 5000L));
        for (int i = 0; i < stars; i++) {
            final float x = random.nextFloat() * w;
            final float y = random.nextFloat() * h;
            final float size = density * (0.35f + 0.9f * random.nextFloat() * random.nextFloat());
            final int a = 90 + random.nextInt(166);
            final int tint = random.nextInt(8);
            final int rgb = tint == 0 ? 0xFFD0A0 : (tint == 1 ? 0xA0C8FF : 0xFFFFFF);
            paint.setColor((a << 24) | rgb);
            canvas.drawCircle(x, y, Math.max(0.6f, size), paint);
        }

        // a few bright stars with a soft glow
        for (int i = 0; i < 9; i++) {
            final float x = random.nextFloat() * w;
            final float y = random.nextFloat() * h;
            final float glow = density * (7f + 7f * random.nextFloat());
            paint.setShader(new RadialGradient(x, y, glow, 0xE6FFFFFF, 0x00FFFFFF,
                    Shader.TileMode.CLAMP));
            canvas.drawCircle(x, y, glow, paint);
            paint.setShader(null);
            paint.setColor(0xFFFFFFFF);
            canvas.drawCircle(x, y, density * 1.2f, paint);
        }
        return bitmap;
    }

    private static Bitmap fromImage(Context context, int w, int h) {
        final File file = customFile(context);
        if (!file.exists()) {
            return null;
        }
        final Bitmap source = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (source == null) {
            return null;
        }
        final Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(out);
        final float scale = Math.max((float) w / source.getWidth(), (float) h / source.getHeight());
        final Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postTranslate((w - source.getWidth() * scale) / 2f, (h - source.getHeight() * scale) / 2f);
        canvas.drawBitmap(source, matrix, new Paint(Paint.FILTER_BITMAP_FLAG));
        canvas.drawColor(0x55000000); // a little darker, so the flying pictures stand out
        source.recycle();
        return out;
    }
}
