package de.fau.lme.widgets;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Robert on 21.12.15.
 */
public class DailyHeartProgressBar extends CardView {

    private OnProgressFinishedListener mListener;

    private CircularProgressBar mProgressBar;
    private ImageView mHeartImageView;
    private TextView mHeartRateTextView;

    private Animation mAnimHeart;

    /**
     * {@link android.animation.ObjectAnimator} for animating the progress of measurement
     */
    private ObjectAnimator mProgressBarAnimator;

    private boolean mProgressBarAnimationEnded;
    /**
     * Boolean indicating if the {@link android.view.animation.Animation} has been cancelled
     */
    private boolean mProgressBarAnimationCancelled;
    private int duration;


    public DailyHeartProgressBar(Context context) {
        this(context, null, -1);
    }

    public DailyHeartProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public DailyHeartProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.widget_dailyheart_progressbar, this);
        mProgressBar = (CircularProgressBar) findViewById(R.id.progressbar_circular);
        mHeartImageView = (ImageView) findViewById(R.id.iv_heart);
        mHeartRateTextView = (TextView) findViewById(R.id.tv_heart_rate);

        // initialize ProgressBarAnimator
        mProgressBarAnimator = ObjectAnimator.ofFloat(mProgressBar, "progress", 1.0f);

        TypedArray attributes = context
                .obtainStyledAttributes(attrs, R.styleable.DailyHeartProgressBar,
                        defStyleAttr, 0);
        if (attributes != null) {
            try {
                setBackgroundColor(ContextCompat.getColor(context, R.color.white));
                mProgressBar.setProgressColor(attributes
                        .getColor(R.styleable.DailyHeartProgressBar_progress_color, Color.CYAN));
                mProgressBar.setProgressBackgroundColor(attributes
                        .getColor(R.styleable.DailyHeartProgressBar_progress_background_color,
                                Color.GREEN));
                mProgressBar.setProgress(
                        attributes.getFloat(R.styleable.DailyHeartProgressBar_progress, 0.0f));
                mProgressBar.setMarkerProgress(
                        attributes.getFloat(R.styleable.DailyHeartProgressBar_marker_progress,
                                0.0f));
                mProgressBar.setWheelSize((int) attributes
                        .getDimension(R.styleable.DailyHeartProgressBar_stroke_width, 20));
                mProgressBar.setThumbEnabled(attributes
                        .getBoolean(R.styleable.DailyHeartProgressBar_thumb_visible, false));
                mProgressBar.setMarkerEnabled(attributes
                        .getBoolean(R.styleable.DailyHeartProgressBar_marker_visible, false));
                mProgressBar.setGravity(attributes
                        .getInt(R.styleable.DailyHeartProgressBar_android_gravity,
                                Gravity.CENTER));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // make sure recycle is always called.
                attributes.recycle();
            }
        }

        // load Animation for pulsating heart
        mAnimHeart = AnimationUtils.loadAnimation(context, R.anim.anim_heart);
        mAnimHeart.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // repeat Animation if is has ended and measurement is still running
                if (!mProgressBarAnimationCancelled && !mProgressBarAnimationEnded) {
                    mHeartImageView.startAnimation(mAnimHeart);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    public void start() {
        mProgressBarAnimationCancelled = false;
        mProgressBarAnimationEnded = false;

        mProgressBarAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // if Animation has ended, automatically stop measurement and disconnect,
                // reverse ProgressBar animation
                if (!mProgressBarAnimationCancelled) {
                    mListener.onProgressBarFinished();
                    mProgressBarAnimationEnded = true;
                    mProgressBar.setProgress(1.0f);
                    mProgressBar.setProgressColor(getResources().getColor(R.color.progress_color_finished));
                    mHeartImageView.clearAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mProgressBar.clearAnimation();
                mHeartImageView.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mProgressBarAnimator.reverse();
        mProgressBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgressBar.setProgress((Float) animation.getAnimatedValue());
            }
        });
        mProgressBar.setMarkerProgress(1.0f);
        mProgressBarAnimator.start();
        // start pulsating heart animation
        mHeartImageView.startAnimation(mAnimHeart);
    }

    public void stop() {
        // stop pulsating heart animation
        mHeartImageView.clearAnimation();
        mProgressBarAnimationCancelled = true;
        // reverse ProgressBar animation and set reverse duration
        mProgressBarAnimator.reverse();
        mProgressBarAnimator.setDuration(1000);
        mProgressBarAnimator = null;
        mProgressBar.setProgress(0.0f);
    }

    public void pause() {
        mProgressBarAnimationCancelled = true;
        if (mProgressBarAnimator != null) {
            mProgressBarAnimator.pause();
            mHeartImageView.clearAnimation();
        }
    }

    public void resume() {
        mProgressBarAnimationCancelled = false;
        if (mProgressBarAnimator != null) {
            mProgressBarAnimator.resume();
            mHeartImageView.startAnimation(mAnimHeart);
        }
    }

    public void setHeartRate(int heartRate) {
        mHeartRateTextView.setText(String.valueOf(heartRate));
    }

    public void setHeartRate(String heartRate) {
        mHeartRateTextView.setText(heartRate);
    }

    public void setOnProgressFinishedListener(OnProgressFinishedListener listener) {
        this.mListener = listener;
    }

    public void setDuration(int duration) {
        this.duration = duration;
        mProgressBarAnimator.setDuration(duration);
    }


    public interface OnProgressFinishedListener {
        void onProgressBarFinished();
    }

    public static class CircularProgressBar extends View {

        /**
         * used to save the super state on configuration change
         */
        private static final String INSTANCE_STATE_SAVEDSTATE = "saved_state";

        /**
         * used to save the progress on configuration changes
         */
        private static final String INSTANCE_STATE_PROGRESS = "progress";

        /**
         * used to save the marker progress on configuration changes
         */
        private static final String INSTANCE_STATE_MARKER_PROGRESS = "marker_progress";

        /**
         * used to save the background color of the progress
         */
        private static final String INSTANCE_STATE_PROGRESS_BACKGROUND_COLOR
                = "progress_background_color";

        /**
         * used to save the color of the progress
         */
        private static final String INSTANCE_STATE_PROGRESS_COLOR = "progress_color";

        /**
         * used to save and restore the visibility of the thumb in this instance
         */
        private static final String INSTANCE_STATE_THUMB_VISIBLE = "thumb_visible";

        /**
         * used to save and restore the visibility of the marker in this instance
         */
        private static final String INSTANCE_STATE_MARKER_VISIBLE = "marker_visible";

        /**
         * The rectangle enclosing the circle.
         */
        private final RectF mCircleBounds = new RectF();

        /**
         * the rect for the thumb square
         */
        private final RectF mSquareRect = new RectF();

        /**
         * the paint for the background.
         */
        private Paint mBackgroundColorPaint = new Paint();

        /**
         * The stroke width used to paint the circle.
         */
        private int mCircleStrokeWidth = 50;

        /**
         * The gravity of the view. Where should the Circle be drawn within the given bounds
         * <p/>
         * {@link #computeInsets(int, int)}
         */
        private int mGravity = Gravity.CENTER;

        /**
         * The Horizontal inset calcualted in {@link #computeInsets(int, int)} depends on {@link
         * #mGravity}.
         */
        private int mHorizontalInset;

        /**
         * true if not all properties are set. then the view isn't drawn and there are no errors in the
         * LayoutEditor
         */
        private boolean mIsInitializing = true;

        /**
         * flag if the marker should be visible
         */
        private boolean mIsMarkerEnabled;

        /**
         * indicates if the thumb is visible
         */
        private boolean mIsThumbEnabled = true;

        /**
         * The Marker color paint.
         */
        private Paint mMarkerColorPaint;

        /**
         * The Marker progress.
         */
        private float mMarkerProgress;

        /**
         * the overdraw is true if the progress is over 1.0.
         */
        private boolean mOverrdraw;

        /**
         * The current progress.
         */
        private float mProgress;

        /**
         * The color of the progress background.
         */
        private int mProgressBackgroundColor;

        /**
         * the color of the progress.
         */
        private int mProgressColor;

        /**
         * paint for the progress.
         */
        private Paint mProgressColorPaint;

        /**
         * Radius of the circle
         * <p/>
         * <p> Note: (Re)calculated in {@link #onMeasure(int, int)}. </p>
         */
        private float mRadius;

        /**
         * The Thumb color paint.
         */
        private Paint mThumbColorPaint = new Paint();

        /**
         * The Thumb pos x.
         * <p/>
         * Care. the position is not the position of the rotated thumb. The position is only calculated
         * in {@link #onMeasure(int, int)}
         */
        private float mThumbPosX;

        /**
         * The Thumb pos y.
         * <p/>
         * Care. the position is not the position of the rotated thumb. The position is only calculated
         * in {@link #onMeasure(int, int)}
         */
        private float mThumbPosY;

        /**
         * The pointer width (in pixels).
         */
        private int mThumbRadius = 10;

        /**
         * The Translation offset x which gives us the ability to use our own coordinates system.
         */
        private float mTranslationOffsetX;

        /**
         * The Translation offset y which gives us the ability to use our own coordinates system.
         */
        private float mTranslationOffsetY;

        /**
         * The Vertical inset calculated in {@link #computeInsets(int, int)} depends on {@link
         * #mGravity}..
         */
        private int mVerticalInset;

        /**
         * Instantiates a new holo circular progress bar.
         *
         * @param context the context
         */
        public CircularProgressBar(Context context) {
            this(context, null);
        }

        /**
         * Instantiates a new holo circular progress bar.
         *
         * @param context the context
         * @param attrs   the attrs
         */
        public CircularProgressBar(Context context, AttributeSet attrs) {
            this(context, attrs, R.attr.DailyHeartProgressBar);
        }

        /**
         * Instantiates a new holo circular progress bar.
         *
         * @param context  the context
         * @param attrs    the attrs
         * @param defStyle the def style
         */
        public CircularProgressBar(Context context, AttributeSet attrs,
                                   int defStyle) {
            super(context, attrs, defStyle);

            mThumbRadius = mCircleStrokeWidth;

            updateBackgroundColor();

            updateMarkerColor();

            updateProgressColor();

            // the view has now all properties and can be drawn
            mIsInitializing = false;

        }

        @Override
        protected void onDraw(Canvas canvas) {

            // All of our positions are using our internal coordinate system.
            // Instead of translating
            // them we let Canvas do the work for us.
            canvas.translate(mTranslationOffsetX, mTranslationOffsetY);

            float progressRotation = getCurrentRotation();

            // draw the background
            if (!mOverrdraw) {
                canvas.drawArc(mCircleBounds, 270, -(360 - progressRotation), false,
                        mBackgroundColorPaint);
            }

            // draw the progress or a full circle if overdraw is true
            canvas.drawArc(mCircleBounds, 270, mOverrdraw ? 360 : progressRotation, false,
                    mProgressColorPaint);

            // draw the marker at the correct rotated position
            if (mIsMarkerEnabled) {
                float markerRotation = getMarkerRotation();

                canvas.save();
                canvas.rotate(markerRotation - 90);
                canvas.drawLine((float) (mThumbPosX + mThumbRadius / 2 * 1.4), mThumbPosY,
                        (float) (mThumbPosX - mThumbRadius / 2 * 1.4), mThumbPosY, mMarkerColorPaint);
                canvas.restore();
            }

            if (isThumbEnabled()) {
                // draw the thumb square at the correct rotated position
                canvas.save();
                canvas.rotate(progressRotation - 90);
                // rotate the square by 45 degrees
                canvas.rotate(45, mThumbPosX, mThumbPosY);
                mSquareRect.left = mThumbPosX - mThumbRadius / 3;
                mSquareRect.right = mThumbPosX + mThumbRadius / 3;
                mSquareRect.top = mThumbPosY - mThumbRadius / 3;
                mSquareRect.bottom = mThumbPosY + mThumbRadius / 3;
                canvas.drawRect(mSquareRect, mThumbColorPaint);
                canvas.restore();
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = getDefaultSize(
                    getSuggestedMinimumHeight() + getPaddingTop() + getPaddingBottom(),
                    heightMeasureSpec);
            int width = getDefaultSize(
                    getSuggestedMinimumWidth() + getPaddingLeft() + getPaddingRight(),
                    widthMeasureSpec);

            int diameter;
            if (heightMeasureSpec == MeasureSpec.UNSPECIFIED) {
                // ScrollView
                diameter = width;
                computeInsets(0, 0);
            } else if (widthMeasureSpec == MeasureSpec.UNSPECIFIED) {
                // HorizontalScrollView
                diameter = height;
                computeInsets(0, 0);
            } else {
                // Default
                diameter = Math.min(width, height);
                computeInsets(width - diameter, height - diameter);
            }

            setMeasuredDimension(diameter, diameter);

            float halfWidth = diameter * 0.5f;

            // width of the drawn circle (+ the drawnThumb)
            float drawnWith;
            if (isThumbEnabled()) {
                drawnWith = mThumbRadius * (5f / 6f);
            } else if (isMarkerEnabled()) {
                drawnWith = mCircleStrokeWidth * 1.4f;
            } else {
                drawnWith = mCircleStrokeWidth / 2f;
            }

            // -0.5f for pixel perfect fit inside the viewbounds
            mRadius = halfWidth - drawnWith - 0.5f;

            mCircleBounds.set(-mRadius, -mRadius, mRadius, mRadius);

            mThumbPosX = (float) (mRadius * Math.cos(0));
            mThumbPosY = (float) (mRadius * Math.sin(0));

            mTranslationOffsetX = halfWidth + mHorizontalInset;
            mTranslationOffsetY = halfWidth + mVerticalInset;

        }

        @Override
        protected void onRestoreInstanceState(Parcelable state) {
            if (state instanceof Bundle) {
                Bundle bundle = (Bundle) state;
                setProgress(bundle.getFloat(INSTANCE_STATE_PROGRESS));
                setMarkerProgress(bundle.getFloat(INSTANCE_STATE_MARKER_PROGRESS));

                int progressColor = bundle.getInt(INSTANCE_STATE_PROGRESS_COLOR);
                if (progressColor != mProgressColor) {
                    mProgressColor = progressColor;
                    updateProgressColor();
                }

                int progressBackgroundColor = bundle
                        .getInt(INSTANCE_STATE_PROGRESS_BACKGROUND_COLOR);
                if (progressBackgroundColor != mProgressBackgroundColor) {
                    mProgressBackgroundColor = progressBackgroundColor;
                    updateBackgroundColor();
                }

                mIsThumbEnabled = bundle.getBoolean(INSTANCE_STATE_THUMB_VISIBLE);

                mIsMarkerEnabled = bundle.getBoolean(INSTANCE_STATE_MARKER_VISIBLE);

                super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE_SAVEDSTATE));
                return;
            }

            super.onRestoreInstanceState(state);
        }

        @Override
        protected Parcelable onSaveInstanceState() {
            Bundle bundle = new Bundle();
            bundle.putParcelable(INSTANCE_STATE_SAVEDSTATE, super.onSaveInstanceState());
            bundle.putFloat(INSTANCE_STATE_PROGRESS, mProgress);
            bundle.putFloat(INSTANCE_STATE_MARKER_PROGRESS, mMarkerProgress);
            bundle.putInt(INSTANCE_STATE_PROGRESS_COLOR, mProgressColor);
            bundle.putInt(INSTANCE_STATE_PROGRESS_BACKGROUND_COLOR, mProgressBackgroundColor);
            bundle.putBoolean(INSTANCE_STATE_THUMB_VISIBLE, mIsThumbEnabled);
            bundle.putBoolean(INSTANCE_STATE_MARKER_VISIBLE, mIsMarkerEnabled);
            return bundle;
        }

        public int getCircleStrokeWidth() {
            return mCircleStrokeWidth;
        }

        /**
         * similar to {@link #getProgress}
         */
        public float getMarkerProgress() {
            return mMarkerProgress;
        }

        /**
         * Sets the marker progress.
         *
         * @param progress the new marker progress
         */
        public void setMarkerProgress(float progress) {
            mIsMarkerEnabled = true;
            mMarkerProgress = progress;
        }

        /**
         * gives the current progress of the ProgressBar. Value between 0..1 if you set the progress to
         * >1 you'll get progress % 1 as return value
         *
         * @return the progress
         */
        public float getProgress() {
            return mProgress;
        }

        /**
         * Sets the progress.
         *
         * @param progress the new progress
         */
        public void setProgress(float progress) {
            if (progress == mProgress) {
                return;
            }

            if (progress == 1) {
                mOverrdraw = false;
                mProgress = 1;
            } else {

                mOverrdraw = progress >= 1;

                mProgress = progress % 1.0f;
            }

            if (!mIsInitializing) {
                invalidate();
            }
        }

        /**
         * Gets the progress color.
         *
         * @return the progress color
         */
        public int getProgressColor() {
            return mProgressColor;
        }

        /**
         * Sets the progress color.
         *
         * @param color the new progress color
         */
        public void setProgressColor(int color) {
            mProgressColor = color;

            updateProgressColor();
        }

        /**
         * @return true if the marker is visible
         */
        public boolean isMarkerEnabled() {
            return mIsMarkerEnabled;
        }

        /**
         * Sets the marker enabled.
         *
         * @param enabled the new marker enabled
         */
        public void setMarkerEnabled(boolean enabled) {
            mIsMarkerEnabled = enabled;
        }

        /**
         * @return true if the marker is visible
         */
        public boolean isThumbEnabled() {
            return mIsThumbEnabled;
        }

        /**
         * shows or hides the thumb of the progress bar
         *
         * @param enabled true to show the thumb
         */
        public void setThumbEnabled(boolean enabled) {
            mIsThumbEnabled = enabled;
        }

        /**
         * Sets the progress background color.
         *
         * @param color the new progress background color
         */
        public void setProgressBackgroundColor(int color) {
            mProgressBackgroundColor = color;

            updateMarkerColor();
            updateBackgroundColor();
        }

        public void setGravity(int gravity) {
            this.mGravity = gravity;
        }

        /**
         * Sets the wheel size.
         *
         * @param dimension the new wheel size
         */
        public void setWheelSize(int dimension) {
            mCircleStrokeWidth = dimension;

            // update the paints
            updateBackgroundColor();
            updateMarkerColor();
            updateProgressColor();
        }

        /**
         * Compute insets.
         * <p/>
         * <pre>
         *  ______________________
         * |_________dx/2_________|
         * |......| /'''''\|......|
         * |-dx/2-|| View ||-dx/2-|
         * |______| \_____/|______|
         * |________ dx/2_________|
         * </pre>
         *
         * @param dx the dx the horizontal unfilled space
         * @param dy the dy the horizontal unfilled space
         */
        @SuppressLint("NewApi")
        private void computeInsets(int dx, int dy) {
            int absoluteGravity = mGravity;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                absoluteGravity = Gravity.getAbsoluteGravity(mGravity, getLayoutDirection());
            }

            switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                case Gravity.START:
                    mHorizontalInset = 0;
                    break;
                case Gravity.END:
                    mHorizontalInset = dx;
                    break;
                case Gravity.CENTER_HORIZONTAL:
                default:
                    mHorizontalInset = dx / 2;
                    break;
            }
            switch (absoluteGravity & Gravity.VERTICAL_GRAVITY_MASK) {
                case Gravity.TOP:
                    mVerticalInset = 0;
                    break;
                case Gravity.BOTTOM:
                    mVerticalInset = dy;
                    break;
                case Gravity.CENTER_VERTICAL:
                default:
                    mVerticalInset = dy / 2;
                    break;
            }
        }

        /**
         * Gets the current rotation.
         *
         * @return the current rotation
         */
        private float getCurrentRotation() {
            return 360 * mProgress;
        }

        /**
         * Gets the marker rotation.
         *
         * @return the marker rotation
         */
        private float getMarkerRotation() {
            return 360 * mMarkerProgress;
        }

        /**
         * updates the paint of the background
         */
        private void updateBackgroundColor() {
            mBackgroundColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBackgroundColorPaint.setColor(mProgressBackgroundColor);
            mBackgroundColorPaint.setStyle(Paint.Style.STROKE);
            mBackgroundColorPaint.setStrokeWidth(mCircleStrokeWidth);

            invalidate();
        }

        /**
         * updates the paint of the marker
         */
        private void updateMarkerColor() {
            mMarkerColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mMarkerColorPaint.setColor(mProgressBackgroundColor);
            mMarkerColorPaint.setStyle(Paint.Style.STROKE);
            mMarkerColorPaint.setStrokeWidth(mCircleStrokeWidth / 2);

            invalidate();
        }

        /**
         * updates the paint of the progress and the thumb to give them a new visual style
         */
        private void updateProgressColor() {
            mProgressColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mProgressColorPaint.setColor(mProgressColor);
            mProgressColorPaint.setStyle(Paint.Style.STROKE);
            mProgressColorPaint.setStrokeWidth(mCircleStrokeWidth);

            mThumbColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mThumbColorPaint.setColor(mProgressColor);
            mThumbColorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mThumbColorPaint.setStrokeWidth(mCircleStrokeWidth);

            invalidate();
        }

    }


}
