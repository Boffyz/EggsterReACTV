package areeb.xposed.eggster.screensavers;

/**
 * The numbers behind the screensaver's settings sliders. Plain Java (no Android classes) so the
 * settings screen and the animation always agree, and so it is easy to test.
 */
final class ScreensaverSettings {

    static final int SPEED_MIN = 1;
    static final int SPEED_MAX = 50;
    static final int SPEED_DEFAULT = 5;

    static final int COUNT_MIN = 10;
    static final int COUNT_STEP = 10;
    static final int COUNT_MAX = 200;
    static final int COUNT_DEFAULT = 60;

    static final int SIZE_MIN = 1;
    static final int SIZE_MAX = 5;
    static final int SIZE_DEFAULT = 3;

    static final int ROT_MIN = 0;
    static final int ROT_MAX = 10;
    static final int ROT_DEFAULT = 5;

    /** Spin direction: each picture picks its own, or all clockwise, or all counter-clockwise. */
    static final int DIR_MIXED = 0;
    static final int DIR_CW = 1;
    static final int DIR_CCW = 2;

    /** The angle slider: 24 steps of 15 degrees (0 to 345). */
    static final int ANGLE_STEP = 15;
    static final int ANGLE_LEVELS = 24;

    private ScreensaverSettings() {
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** How fast the pictures fly, in "depth units" per second (a picture crosses depth 1.0 to 0.06). */
    static float depthSpeed(int speedLevel) {
        return 0.04f + 0.03f * clamp(speedLevel, SPEED_MIN, SPEED_MAX);
    }

    /** How big the pictures are, relative to the normal size. */
    static float sizeFactor(int sizeLevel) {
        switch (clamp(sizeLevel, SIZE_MIN, SIZE_MAX)) {
            case 1:
                return 0.6f;
            case 2:
                return 0.8f;
            case 3:
                return 1.0f;
            case 4:
                return 1.3f;
            default:
                return 1.7f;
        }
    }

    /** Multiplier for how fast pictures spin and tumble (0 = not at all, 2 = twice the normal speed). */
    static float rotationScale(int level) {
        return clamp(level, ROT_MIN, ROT_MAX) / 5f;
    }

    static float angleDegrees(int level) {
        return clamp(level, 0, ANGLE_LEVELS - 1) * ANGLE_STEP;
    }

    static int clampCount(int count) {
        return clamp(count, COUNT_MIN, COUNT_MAX);
    }
}
