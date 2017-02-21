package de.fau.lme.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Original version by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
 * <p/>
 * FAU Erlangen-Nürnberg
 * <p/>
 * (c) 2014
 * <p/>
 * Modified {@link CardView} customized for showing the heart status.
 *
 * @author Robert Richer
 */
public class HeartStatusCardView extends CardView {

    public enum HeartStatus {
        HEART_STATUS_UNKNOWN,
        HEART_STATUS_OK,
        HEART_STATUS_TOO_LOW,
        HEART_STATUS_TOO_HIGH,
        HEART_STATUS_ARRHYTHMIA
    }

    private HeartStatus mStatus;
    private TextView mTextView;

    public HeartStatusCardView(Context context) {
        this(context, null, -1);
    }

    public HeartStatusCardView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public HeartStatusCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_heart_status_card, this);
        mTextView = (TextView) findViewById(R.id.tv_heart_status);
        TypedArray attributes = context
                .obtainStyledAttributes(attrs, R.styleable.HeartStatusCardView, defStyleAttr, 0);
        if (attributes != null) {
            try {
                setHeartStatus(HeartStatus.values()[attributes.getInteger(R.styleable.HeartStatusCardView_heart_status, 0)]);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                attributes.recycle();
            }
        }
    }

    /**
     * Returns the current heart status of this {@link CardView}.
     *
     * @return The current heart status
     */
    public HeartStatus getStatus() {
        return mStatus;
    }

    /**
     * Sets the current heart status to display in this {@link CardView}
     *
     * @param status The current heart status
     */
    public void setHeartStatus(HeartStatus status) {
        mStatus = status;
        String heartStatus = "";
        int backgroundColor;
        int textColor;
        switch (mStatus) {
            case HEART_STATUS_OK:
                heartStatus = getContext().getString(R.string.heart_status_ok);
                textColor = ContextCompat.getColor(getContext(), R.color.heart_status_text_color_ok);
                backgroundColor = ContextCompat.getColor(getContext(), R.color.heart_status_background_color_ok);
                break;
            case HEART_STATUS_TOO_HIGH:
                heartStatus = getContext().getString(R.string.heart_status_high_long);
                textColor = ContextCompat.getColor(getContext(), R.color.heart_status_text_color_too_high);
                backgroundColor = ContextCompat.getColor(getContext(), R.color.heart_status_background_color_too_high);
                break;
            case HEART_STATUS_TOO_LOW:
                heartStatus = getContext().getString(R.string.heart_status_low_long);
                textColor = ContextCompat.getColor(getContext(), R.color.heart_status_text_color_too_low);
                backgroundColor = ContextCompat.getColor(getContext(), R.color.heart_status_background_color_too_low);
                break;
            case HEART_STATUS_ARRHYTHMIA:
                heartStatus = getContext().getString(R.string.heart_status_arr_long);
                textColor = ContextCompat.getColor(getContext(), R.color.heart_status_text_color_arrhythmia);
                backgroundColor = ContextCompat.getColor(getContext(), R.color.heart_status_background_color_arrhythmia);
                break;
            case HEART_STATUS_UNKNOWN:
            default:
                textColor = ContextCompat.getColor(getContext(), R.color.white);
                backgroundColor = ContextCompat.getColor(getContext(), R.color.white);
                break;
        }

        mTextView.setText(heartStatus);
        mTextView.setTextColor(textColor);
        setBackgroundColor(backgroundColor);
    }
}
