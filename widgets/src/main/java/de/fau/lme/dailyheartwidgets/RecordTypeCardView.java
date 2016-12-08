package de.fau.lme.dailyheartwidgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Robert on 08.09.16.
 */
public class RecordTypeCardView extends CardView {

    /**
     * Enum representing different record types.
     */
    public enum RecordType {

        RECORD_TYPE_UNKNOWN,
        /**
         * ECG data acquired with CurrentHR
         */
        RECORD_TYPE_CURRENT_HR,
        /**
         * ECG data acquired with AnalyzeECG
         */
        RECORD_TYPE_ANALYZE_ECG,
        /**
         * ECG data acquired with DailyMonitor
         */
        RECORD_TYPE_MONITOR_ECG,
        /**
         * ECG data acquired with DailyTrain
         */
        RECORD_TYPE_TRACK_TRAINING,
        /**
         * ECG data acquired with AnalyzeHRV
         */
        RECORD_TYPE_ANALYZE_HRV
    }


    private Context mContext;

    private ImageView mRecordTypeImageView;
    private TextView mRecordTypeTextView;

    private RecordType mRecordType;

    public RecordTypeCardView(Context context) {
        this(context, null, -1);
    }

    public RecordTypeCardView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public RecordTypeCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.widget_record_type_card, this);

        mContext = context;
        mRecordTypeImageView = (ImageView) findViewById(R.id.ic_record_type);
        mRecordTypeTextView = (TextView) findViewById(R.id.tv_record_type);
        //setRadius(getResources().getDimensionPixelSize(R.dimen.cardview_default_radius));
        //setElevation(getResources().getDimensionPixelSize(R.dimen.cardview_default_elevation));

        TypedArray attributes = mContext.obtainStyledAttributes(attrs, R.styleable.RecordTypeCardView);
        if (attributes != null) {
            try {
                mRecordType = RecordType.values()[attributes.getInteger(R.styleable.RecordTypeCardView_record_type, 0)];
                setRecordType(mRecordType);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                attributes.recycle();
            }
        }
    }

    private void setRecordType(RecordType recordType) {
        setBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
        switch (recordType) {
            case RECORD_TYPE_CURRENT_HR:
                mRecordTypeImageView.setImageResource(R.drawable.img_current_hr);
                mRecordTypeTextView.setText(R.string.record_type_current_hr);
                break;
            case RECORD_TYPE_ANALYZE_ECG:
                mRecordTypeImageView.setImageResource(R.drawable.img_analyze_ecg);
                mRecordTypeTextView.setText(R.string.record_type_analyze_ecg);
                break;
            case RECORD_TYPE_MONITOR_ECG:
                mRecordTypeImageView.setImageResource(R.drawable.img_monitor_ecg);
                mRecordTypeTextView.setText(R.string.record_type_monitor_ecg);
                break;
            case RECORD_TYPE_TRACK_TRAINING:
                mRecordTypeImageView.setImageResource(R.drawable.img_track_training);
                mRecordTypeTextView.setText(R.string.record_type_track_training);
                break;
            case RECORD_TYPE_ANALYZE_HRV:
                mRecordTypeImageView.setImageResource(R.drawable.img_analyze_hrv);
                mRecordTypeTextView.setText(R.string.record_type_analyze_hrv);
                break;
            case RECORD_TYPE_UNKNOWN:
            default:
                mRecordTypeImageView.setImageResource(android.R.color.transparent);
                mRecordTypeTextView.setText("");
                break;
        }
    }

    private RecordType getRecordType() {
        return mRecordType;
    }
}
