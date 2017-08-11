package com.start.lewish.selfprogressbar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/**
 * Created by Lewish on 2017/8/10.
 */

public class TextProgressBar extends View {

    private final Paint mLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float mDensity;
    private final float mScaledDensity;
    private int mProgress;
    private int mMax;

    private int mPrimaryColor;
    private int mSecondaryColor;
    private final Rect mTextBounds = new Rect();

    public TextProgressBar(final Context context) {
        this(context, null);
    }

    public TextProgressBar(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextProgressBar(
            final Context context,
            final AttributeSet attrs,
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDensity = getResources().getDisplayMetrics().density;
        mScaledDensity = getResources().getDisplayMetrics().scaledDensity;

        int max = 0;
        int progress = 0;

        float strokeWidth = dp2px(3);

        int primaryColor = 0xFF009688;
        int secondaryColor = 0xFFDADADA;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final TypedValue out = new TypedValue();

            context.getTheme().resolveAttribute(R.attr.colorControlActivated, out, true);
            primaryColor = out.data;
            context.getTheme().resolveAttribute(R.attr.colorControlHighlight, out, true);
            secondaryColor = out.data;
        }

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.TextProgressBar, defStyleAttr, 0);

        if (a != null) {
            max = a.getInt(R.styleable.TextProgressBar_max, max);
            progress = a.getInt(R.styleable.TextProgressBar_progress, progress);
            primaryColor = a.getColor(R.styleable.TextProgressBar_primaryColor, primaryColor);
            secondaryColor = a.getColor(R.styleable.TextProgressBar_secondaryColor, secondaryColor);
            strokeWidth = a.getDimension(R.styleable.TextProgressBar_strokeWidth, strokeWidth);

            a.recycle();
        }

        mPrimaryColor = primaryColor;
        mSecondaryColor = secondaryColor;

        mLinesPaint.setStrokeWidth(strokeWidth);
        mLinesPaint.setStyle(Paint.Style.STROKE);
        mLinesPaint.setStrokeCap(Paint.Cap.SQUARE);


        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(sp2px(18));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(Typeface.create("sans-serif-condensed-light", 0));

        setMax(max);
        setProgress(progress);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        // Recalculate how tall the text needs to be, width is ignored
        final String progress = getBubbleText();
        mTextPaint.getTextBounds(progress, 0, progress.length(), mTextBounds);

        final int bubbleHeight = (int) Math.ceil(getBubbleVerticalDisplacement());

        final float strokeWidth = getStrokeWidth();
        final int dh = (int) Math.ceil(getPaddingTop() + getPaddingBottom() + strokeWidth);

        setMeasuredDimension(
                getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSizeAndState(dh + bubbleHeight, heightMeasureSpec, 0));
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    protected synchronized void onDraw(final Canvas canvas) {

        final float bubbleDisplacement = getBubbleVerticalDisplacement();
        final float top = getPaddingTop() + bubbleDisplacement;
        final float left = getPaddingLeft();
        final float end = getWidth() - getPaddingRight();

        final float max = getMax();
        final float offset = (max == 0) ? 0 : (getProgress() / max);
        final float progressEnd = clamp(lerp(left, end, offset), left, end);

        // Draw the secondary background line
        mLinesPaint.setColor(mSecondaryColor);
        canvas.drawLine(progressEnd, top, end, top, mLinesPaint);

        // Draw the primary progress line
        mLinesPaint.setColor(mPrimaryColor);
        if (progressEnd != left) {
            canvas.drawLine(left, top, progressEnd, top, mLinesPaint);
        }

        final String progress = getBubbleText();
        mTextPaint.getTextBounds(progress, 0, progress.length(), mTextBounds);

        // Draw the bubble text background
        final float bubbleWidth = getBubbleWidth();
        final float bubbleHeight = getBubbleHeight();

        final float bubbleTop = 0;
        final float bubbleLeft = clamp(progressEnd - (bubbleWidth / 2), 0, getWidth() - bubbleWidth);
        final int saveCount = canvas.save();
        canvas.translate(bubbleLeft, bubbleTop);

        // Draw the progress text part of the bubble
        final float textX = bubbleWidth / 2;
        final float textY = bubbleHeight - dp2px(8);

        canvas.drawText(progress, textX, textY, mTextPaint);

        canvas.restoreToCount(saveCount);
    }

    /**
     * 得到Bubble的高度
     *
     * @return
     */
    private float getBubbleVerticalDisplacement() {
        return getBubbleMargin() + getBubbleHeight();
    }

    public float getBubbleMargin() {
        return dp2px(4);
    }

    public float getBubbleWidth() {
        return mTextBounds.width() + /* padding */ dp2px(16);
    }

    public float getBubbleHeight() {
        return mTextBounds.height() + /* padding */ dp2px(16);
    }

    public String getBubbleText() {
        final int progress = (int) (100 * getProgress() / (float) getMax());
        return "已完成" + progress + "%";
    }

    public synchronized void setProgress(int progress) {
        progress = (int) clamp(progress, 0, getMax());
        if (progress == mProgress) {
            return;
        }
        mProgress = progress;
        postInvalidate();
    }

    public void animateProgress(final int progress) {
        // Speed of animation is interpolated from 0 --> MAX in 2s
        // Minimum time duration is 500ms because anything faster than that is waaaay too quick
        final int startProgress = getProgress();
        final int endProgress = (int) clamp(progress, 0, getMax());
        final int diff = Math.abs(getProgress() - endProgress);
        final long duration = Math.max(500L, (long) (2000L * (diff / (float) getMax())));

        final ValueAnimator animator = ValueAnimator.ofInt(getProgress(), endProgress);
        animator.setDuration(duration);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator animation) {
                setProgress((Integer) animation.getAnimatedValue());
            }
        });

        animator.start();
    }


    public int getProgress() {
        return mProgress;
    }

    public void setMax(int max) {
        max = Math.max(0, max);
        if (max != mMax) {
            mMax = max;
            if (mProgress > max) {
                mProgress = max;
            }
            postInvalidate();
        }
    }

    public int getMax() {
        return mMax;
    }

    public void setPrimaryColor(final int color) {
        mPrimaryColor = color;

        invalidate();
    }

    public int getPrimaryColor() {
        return mPrimaryColor;
    }

    public void setSecondaryColor(final int color) {
        mSecondaryColor = color;

        invalidate();
    }

    public int getSecondaryColor() {
        return mSecondaryColor;
    }

    public void setStrokeWidth(final float width) {
        mLinesPaint.setStrokeWidth(width);

        requestLayout();
        invalidate();
    }

    public float getStrokeWidth() {
        return mLinesPaint.getStrokeWidth();
    }

    public void setTypeface(final Typeface typeface) {
        mTextPaint.setTypeface(typeface);

        requestLayout();
        invalidate();
    }

    public void setTextPaint(final Paint paint) {
        mTextPaint.set(paint);

        requestLayout();
        invalidate();
    }

    /**
     * Return a copy so that fields can only be modified through {@link #setTextPaint}
     */
    public Paint getTextPaint() {
        return new Paint(mTextPaint);
    }

    private float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float lerp(float v0, float v1, float t) {
        return (t == 1) ? v1 : (v0 + t * (v1 - v0));
    }

    private float dp2px(final float dips) {
        return dips * mDensity;
    }

    public int px2dip(float pxValue) {
        return (int) (pxValue / mDensity + 0.5f);
    }

    private float sp2px(final int sp) {
        return sp * mScaledDensity;
    }
}
