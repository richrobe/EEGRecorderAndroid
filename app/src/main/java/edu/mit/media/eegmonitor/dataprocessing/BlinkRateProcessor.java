package edu.mit.media.eegmonitor.dataprocessing;

import android.util.Log;
import android.util.SparseIntArray;

import com.google.common.math.DoubleMath;
import com.google.common.math.Stats;

import java.util.Arrays;

/**
 * Created by Robert on 2/22/17.
 */

public class BlinkRateProcessor {


    private static final String TAG = BlinkRateProcessor.class.getSimpleName();

    /**
     * Conversion of one minute into miliseconds
     */
    private static final double MINUTE_TO_MS = 1000.0 * 60.0;
    /**
     * Interval over which blink rate is being computed
     */
    private static final int BLINK_RATE_INTERVAL = 10000;

    /**
     * Minimum time difference in ms between two blink events
     */
    private static final double BLINK_DIFF_MIN = 150.0;

    /**
     * Start time of blink rate measurement, used as key for {@link SparseIntArray}
     * containing blink events (mBlinkList)
     */
    private final double mStartTime;
    /**
     * Timestamp of the last blink event
     */
    private double mLastTimestamp = 0.0;
    /**
     * Total blinks since start
     */
    private int mTotalBlinks = 0;
    private double mCurrentBlinkRate = 0.0;
    private double[] mBlinkRateList = new double[10];
    private double mAverageBlinkRate = 0.0;
    /**
     * Key-Value list containing blink events within the BLINK_RATE_INTERVAL with
     * their corresponding timestamps
     */
    private SparseIntArray mBlinkList = new SparseIntArray();


    public BlinkRateProcessor(double startTime) {
        mStartTime = startTime;
    }

    public void nextBlink(double timestamp) {
        double timeDiff = timestamp - mLastTimestamp;

        if (timeDiff <= BLINK_DIFF_MIN) {
            return;
        }

        mBlinkList.append((int) (timestamp - mStartTime), 1);
        while ((mBlinkList.keyAt(mBlinkList.size() - 1) - mBlinkList.keyAt(0)) > BLINK_RATE_INTERVAL) {
            mBlinkList.removeAt(0);
        }

        if (mBlinkList.size() > 1) {
            mCurrentBlinkRate = (mBlinkList.size() * MINUTE_TO_MS) /
                    (double) (mBlinkList.keyAt(mBlinkList.size() - 1) - mBlinkList.keyAt(0));
            mBlinkRateList[mTotalBlinks % mBlinkRateList.length] = mCurrentBlinkRate;
            mAverageBlinkRate = Stats.meanOf(mBlinkRateList);
        }

        mTotalBlinks++;
        mLastTimestamp = timestamp;
    }

    public double getBlinkRate() {
        return mCurrentBlinkRate;
    }

    public double getAverageBlinkRate() {
        return mAverageBlinkRate;
    }

    public int getTotalBlinks() {
        return mTotalBlinks;
    }

}
