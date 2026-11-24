package areeb.xposed.eggster.screensavers;

import java.util.Random;

/**
 * The "flying through space" simulation: a cloud of pictures that start far away and fly toward
 * you. Each one has a spot in the sky (x, y, in -1..1) and a depth d: 1.0 is far away, small
 * numbers are close. The screen position is x / d (so things drift outward and speed up as they
 * get closer, like a real starfield) and the size is proportional to 1 / d.
 *
 * Plain Java (no Android classes) so it can be tested on its own.
 */
final class FlyModel {

    /** Depth where pictures appear (they fade in around here). */
    static final float D_FAR = 1.0f;
    /** Pictures that get this close are recycled to the far end. */
    static final float D_NEAR = 0.06f;

    /** When plain dots are mixed with pictures, this share of the objects are dots. */
    static final float DOT_SHARE = 0.35f;

    private static final float TWO_PI = (float) (2.0 * Math.PI);

    private final Random random;

    int count = 0;
    float[] x = new float[0];
    float[] y = new float[0];
    float[] d = new float[0];
    float[] rot = new float[0];
    float[] rotSpeed = new float[0];
    /** Which picture each object shows: an index into the picture list, or -1 for a plain dot. */
    int[] kind = new int[0];
    /** Object indices sorted from far to near: the order to draw them in. */
    int[] order = new int[0];

    private int pictureCount = 0;
    private boolean dots = true;

    FlyModel(Random random) {
        this.random = random;
    }

    /**
     * Tells the model what is available to show.
     *
     * @param pictureCount how many pictures are loaded
     * @param dots         whether plain dots take part in the mix; with no pictures at all it is
     *                     always dots
     */
    void setSprites(int pictureCount, boolean dots) {
        this.pictureCount = Math.max(0, pictureCount);
        this.dots = dots || this.pictureCount == 0;
        for (int i = 0; i < count; i++) {
            kind[i] = pickKind();
        }
    }

    /** Changes the number of objects; existing ones keep flying, new ones start at random depths. */
    void setCount(int newCount) {
        newCount = Math.max(0, newCount);
        if (newCount == count) {
            return;
        }
        final int old = count;
        x = copy(x, newCount);
        y = copy(y, newCount);
        d = copy(d, newCount);
        rot = copy(rot, newCount);
        rotSpeed = copy(rotSpeed, newCount);
        kind = copy(kind, newCount);
        order = new int[newCount];
        count = newCount;
        for (int i = old; i < newCount; i++) {
            respawn(i, true);
        }
        for (int i = 0; i < newCount; i++) {
            order[i] = i;
        }
    }

    /** Moves everything toward the viewer. */
    void update(float dt, float speed, boolean spin) {
        for (int i = 0; i < count; i++) {
            d[i] -= speed * dt;
            if (spin) {
                rot[i] += rotSpeed[i] * dt;
                if (rot[i] > 360f) {
                    rot[i] -= 360f;
                } else if (rot[i] < -360f) {
                    rot[i] += 360f;
                }
            }
            if (d[i] <= D_NEAR) {
                respawn(i, false);
            }
        }
    }

    /** Puts object i at a new random place: anywhere in the sky when first created, else at the far end. */
    void respawn(int i, boolean initial) {
        final float angle = random.nextFloat() * TWO_PI;
        final float radius = 0.03f + 0.97f * random.nextFloat();
        x[i] = (float) Math.cos(angle) * radius;
        y[i] = (float) Math.sin(angle) * radius;
        d[i] = initial ? 0.15f + 0.85f * random.nextFloat() : D_FAR + 0.15f * random.nextFloat();
        rot[i] = random.nextFloat() * 360f;
        rotSpeed[i] = (random.nextBoolean() ? 1f : -1f) * (15f + 60f * random.nextFloat());
        kind[i] = pickKind();
    }

    /** Sorts {@link #order} from far to near (insertion sort: the order barely changes between frames). */
    void sortByDepth() {
        for (int a = 1; a < count; a++) {
            final int idx = order[a];
            final float depth = d[idx];
            int b = a - 1;
            while (b >= 0 && d[order[b]] < depth) {
                order[b + 1] = order[b];
                b--;
            }
            order[b + 1] = idx;
        }
    }

    /** 0 when far away (just appearing) up to 1 when well inside the picture. */
    static float alpha(float depth) {
        final float a = (D_FAR + 0.15f - depth) / 0.25f;
        return a < 0f ? 0f : (a > 1f ? 1f : a);
    }

    private int pickKind() {
        if (pictureCount == 0) {
            return -1;
        }
        if (dots && random.nextFloat() < DOT_SHARE) {
            return -1;
        }
        return random.nextInt(pictureCount);
    }

    private static float[] copy(float[] source, int length) {
        final float[] result = new float[length];
        System.arraycopy(source, 0, result, 0, Math.min(source.length, length));
        return result;
    }

    private static int[] copy(int[] source, int length) {
        final int[] result = new int[length];
        System.arraycopy(source, 0, result, 0, Math.min(source.length, length));
        return result;
    }
}
