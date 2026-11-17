package areeb.xposed.eggster.eggs.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;

/**
 * An analog clock whose hands you can grab and move, used by the Android 12 and 13 eggs.
 *
 * The original eggs build this by extending Android's own AnalogClock and overriding internal
 * methods that apps can't use, so this is a from-scratch version that behaves the same way:
 * touch the dial and drag around it; the minute hand follows your finger, the hour changes when
 * you pass 12, and putting the hands on the target time (and lifting your finger) unlocks the egg.
 *
 * The touch/angle logic is adapted from AOSP's PlatLogoActivity.SettableAnalogClock
 * (Copyright (C) 2021 The Android Open Source Project, Apache License 2.0).
 * Needs Android 12+ (uses the Material You system colors).
 */
public class SettableClockView extends View {

    public interface OnUnlockListener {
        void onUnlock();
    }

    private final int mTargetHourMod12;
    private final OnUnlockListener mListener;

    private int mOverrideHour = -1;
    private int mOverrideMinute = -1;
    private boolean mOverride = false;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int mDialColor;
    private final int mTickColor;
    private final int mHourColor;
    private final int mMinuteColor;
    private final int mCenterColor;

    /**
     * @param targetHourMod12 the hour (0-11) that unlocks the egg when the hands show H:00:
     *                        0 for 12:00, 1 for 1:00 (13:00)
     */
    public SettableClockView(Context context, int targetHourMod12, OnUnlockListener listener) {
        super(context);
        mTargetHourMod12 = targetHourMod12;
        mListener = listener;
        mDialColor = context.getColor(android.R.color.system_neutral2_800);
        mTickColor = context.getColor(android.R.color.system_neutral2_200);
        mHourColor = context.getColor(android.R.color.system_accent1_100);
        mMinuteColor = context.getColor(android.R.color.system_accent1_300);
        mCenterColor = context.getColor(android.R.color.system_accent3_300);
    }

    private static double toPositiveDegrees(double rad) {
        return (Math.toDegrees(rad) + 360 - 90) % 360;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float cx = getWidth() / 2f;
        final float cy = getHeight() / 2f;
        final float r = Math.min(getWidth(), getHeight()) / 2f;

        int hour, minute;
        if (mOverride && mOverrideMinute >= 0) {
            hour = mOverrideHour;
            minute = mOverrideMinute;
        } else {
            final Calendar now = Calendar.getInstance();
            hour = now.get(Calendar.HOUR_OF_DAY);
            minute = now.get(Calendar.MINUTE);
        }

        // dial
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mDialColor);
        canvas.drawCircle(cx, cy, r, mPaint);

        // ticks: a long one every hour, a short one every minute
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(mTickColor);
        for (int i = 0; i < 60; i++) {
            final boolean major = (i % 5) == 0;
            final double a = Math.toRadians(i * 6 - 90);
            final float inner = r * (major ? 0.86f : 0.93f);
            final float outer = r * 0.97f;
            mPaint.setStrokeWidth(r * (major ? 0.03f : 0.012f));
            canvas.drawLine(cx + (float) Math.cos(a) * inner, cy + (float) Math.sin(a) * inner,
                    cx + (float) Math.cos(a) * outer, cy + (float) Math.sin(a) * outer, mPaint);
        }

        // hour hand
        final float hourAngle = ((hour % 12) + minute / 60f) * 30f;
        drawHand(canvas, cx, cy, hourAngle, r * 0.50f, r * 0.075f, mHourColor);
        // minute hand
        drawHand(canvas, cx, cy, minute * 6f, r * 0.78f, r * 0.05f, mMinuteColor);

        // center cap
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mCenterColor);
        canvas.drawCircle(cx, cy, r * 0.06f, mPaint);

        if (!mOverride) {
            postInvalidateDelayed(1000); // keep showing the real time until the user touches it
        }
    }

    private void drawHand(Canvas canvas, float cx, float cy, float degrees, float length,
                          float width, int color) {
        final double a = Math.toRadians(degrees - 90);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(width);
        mPaint.setColor(color);
        canvas.drawLine(cx, cy, cx + (float) Math.cos(a) * length,
                cy + (float) Math.sin(a) * length, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mOverride = true;
                if (mOverrideHour < 0) {
                    mOverrideHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                }
                // pass through
            case MotionEvent.ACTION_MOVE:
                float x = ev.getX();
                float y = ev.getY();
                float cx = getWidth() / 2f;
                float cy = getHeight() / 2f;
                float angle = (float) toPositiveDegrees(Math.atan2(x - cx, y - cy));

                int minutes = (75 - (int) (angle / 6)) % 60;
                int minuteDelta = minutes - mOverrideMinute;
                if (minuteDelta != 0) {
                    if (Math.abs(minuteDelta) > 45 && mOverrideMinute >= 0) {
                        int hourDelta = (minuteDelta < 0) ? 1 : -1;
                        mOverrideHour = (mOverrideHour + 24 + hourDelta) % 24;
                    }
                    mOverrideMinute = minutes;
                    if (mOverrideMinute == 0) {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        if (getScaleX() == 1f) {
                            setScaleX(1.05f);
                            setScaleY(1.05f);
                            animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                        }
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    postInvalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mOverrideMinute == 0 && (mOverrideHour % 12) == mTargetHourMod12) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    if (mListener != null) mListener.onUnlock();
                }
                return true;
        }
        return false;
    }
}
