package de.fau.lme.dailyheartwidgets;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Original version by Robert Richer, Digital Sports Group, Pattern Recognition Lab, Department of Computer Science.
 * <p/>
 * FAU Erlangen-Nürnberg
 * <p/>
 * (c) 2014
 * <p/>
 * A StatusBar
 *
 * @author Robert Richer
 */
public class StatusBar extends LinearLayout {

    public static final String ACTION_STATUS = "de.lme.statusbar.ACTION_STATUS";
    public static final String BROADCAST_KEY_STATUS = "de.lme.statusbar.BROADCAST_STATUS";

    public static final int RQS_STATUS_DISCONNECTED = 0x34;
    public static final int RQS_STATUS_CONNECTED = 0x44;
    public static final int RQS_STATUS_CONNECTING = 0x54;
    public static final int RQS_STATUS_SIMULATING = 0x64;

    /**
     * ECG-BLE device disconnected / no device available
     */
    public static final int STATUS_DISCONNECTED = 0;
    /**
     * Connecting to ECG-BLE device
     */
    public static final int STATUS_CONNECTING = 1;
    /**
     * Connected to ECG-BLE device
     */
    public static final int STATUS_CONNECTED = 2;
    /**
     * Simulator connected
     */
    public static final int STATUS_SIMULATING = 3;

    public static final int STATUS_UNKNOWN = 4;

    private int mStatus;
    private Context mContext;
    private TextView mTextViewStatus;

    public StatusBar(Context context) {
        this(context, null, -1);
    }

    public StatusBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public StatusBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_status_bar, this);

        mContext = context;
        mTextViewStatus = (TextView) findViewById(R.id.tv_status);

        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.StatusBar);
        if (attributes != null) {
            try {
                setStatus(attributes.getInteger(R.styleable.StatusBar_status, 0));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                attributes.recycle();
            }
        }

    }

    /**
     * Returns the current connection state.
     *
     * @return The current connection state
     */
    public int getState() {
        return mStatus;
    }

    /**
     * Sets the current connections state of the Status Bar.
     *
     * @param status The current connection state
     */
    public void setStatus(int status) {
        Intent intent = new Intent();
        intent.setAction(ACTION_STATUS);
        switch (status) {
            case STATUS_DISCONNECTED:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_disconnected));
                mTextViewStatus.setText(getResources().getString(R.string.status_bar_disconnected).toUpperCase());
                intent.putExtra(BROADCAST_KEY_STATUS, RQS_STATUS_DISCONNECTED);
                break;
            case STATUS_CONNECTING:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_connecting));
                mTextViewStatus.setText(getResources().getString(R.string.status_bar_connecting).toUpperCase());
                intent.putExtra(BROADCAST_KEY_STATUS, RQS_STATUS_CONNECTING);
                break;
            case STATUS_CONNECTED:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_connected));
                mTextViewStatus.setText(getResources().getString(R.string.status_bar_connected).toUpperCase());
                intent.putExtra(BROADCAST_KEY_STATUS, RQS_STATUS_CONNECTED);
                break;
            case STATUS_SIMULATING:
                setBackgroundColor(ContextCompat.getColor(mContext, R.color.status_bar_simulating));
                mTextViewStatus.setText(getResources().getString(R.string.status_bar_simulating).toUpperCase());
                intent.putExtra(BROADCAST_KEY_STATUS, RQS_STATUS_SIMULATING);
                break;
            case STATUS_UNKNOWN:
            default:
                setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent));
                mTextViewStatus.setText("");
                break;
        }
        mContext.sendBroadcast(intent);
        mStatus = status;
    }
}
