package areeb.xposed.eggster.screensavers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The user's own pictures for the screensaver. Chosen images are copied into the app (scaled down to
 * at most 512 px) so they keep working even if the original files are moved or deleted.
 */
public final class CustomPictures {

    public static final int MAX_PICTURES = 60;

    private static final String DIR = "screensaver_pictures";
    private static final int STORE_PX = 512;
    private static final String TAG = "CustomPictures";
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private CustomPictures() {
    }

    private static File dir(Context context) {
        final File dir = new File(context.getFilesDir(), DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    /** The saved pictures, in the order they were added. */
    public static List<File> list(Context context) {
        final File[] files = dir(context).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File directory, String name) {
                return name.endsWith(".png");
            }
        });
        final List<File> result = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files);
            result.addAll(Arrays.asList(files));
        }
        return result;
    }

    public static int count(Context context) {
        return list(context).size();
    }

    /** Copies the chosen images into the app. Returns how many were added. Call off the main thread. */
    public static int importAll(Context context, List<Uri> uris) {
        int added = 0;
        int total = count(context);
        for (Uri uri : uris) {
            if (total >= MAX_PICTURES) {
                break;
            }
            final Bitmap bitmap = decode(context, uri, STORE_PX);
            if (bitmap == null) {
                continue;
            }
            final File out = new File(dir(context), String.format(Locale.US, "pic_%013d_%03d.png",
                    System.currentTimeMillis(), COUNTER.incrementAndGet()));
            if (save(bitmap, out)) {
                added++;
                total++;
            }
            bitmap.recycle();
        }
        return added;
    }

    /** Turns a saved picture by 90 degrees (use 90 or -90). Call off the main thread. */
    public static boolean rotate(File file, int degrees) {
        final Bitmap source = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (source == null) {
            return false;
        }
        try {
            final Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            final Bitmap rotated = Bitmap.createBitmap(source, 0, 0, source.getWidth(),
                    source.getHeight(), matrix, true);
            final boolean ok = save(rotated, file);
            if (rotated != source) {
                rotated.recycle();
            }
            return ok;
        } catch (Throwable t) {
            Log.w(TAG, "Could not rotate " + file.getName(), t);
            return false;
        } finally {
            source.recycle();
        }
    }

    public static boolean delete(File file) {
        return file.delete();
    }

    /** Loads a saved picture scaled so its longest side is at most maxPx, or null if it can't be read. */
    static Bitmap load(File file, int maxPx) {
        try {
            final Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap == null) {
                return null;
            }
            return fit(bitmap, maxPx);
        } catch (Throwable t) {
            Log.w(TAG, "Could not load " + file.getName(), t);
            return null;
        }
    }

    static Bitmap decode(Context context, Uri uri, int maxPx) {
        try {
            final ContentResolver resolver = context.getContentResolver();
            final BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                BitmapFactory.decodeStream(in, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) {
                sample *= 2;
            }
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap bitmap;
            try (InputStream in = resolver.openInputStream(uri)) {
                if (in == null) {
                    return null;
                }
                bitmap = BitmapFactory.decodeStream(in, null, options);
            }
            return bitmap == null ? null : fit(bitmap, maxPx);
        } catch (Throwable t) {
            Log.w(TAG, "Could not read the chosen picture", t);
            return null;
        }
    }

    private static Bitmap fit(Bitmap bitmap, int maxPx) {
        final int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longest <= maxPx) {
            return bitmap;
        }
        final float scale = (float) maxPx / longest;
        final Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                Math.max(1, Math.round(bitmap.getWidth() * scale)),
                Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
        if (scaled != bitmap) {
            bitmap.recycle();
        }
        return scaled;
    }

    static boolean save(Bitmap bitmap, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            return bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Throwable t) {
            Log.w(TAG, "Could not save " + file.getName(), t);
            return false;
        }
    }
}
