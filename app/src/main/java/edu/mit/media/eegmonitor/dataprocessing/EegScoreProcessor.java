package edu.mit.media.eegmonitor.dataprocessing;

import android.util.Log;

import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.google.common.math.DoubleMath;
import com.google.common.math.Stats;

/**
 * Created by Robert on 2/22/17.
 */
public class EegScoreProcessor {


    /**
     *
     */
    public interface EegScoreListener {

        /**
         * Callback when new current Scores are available
         *
         * @param values Current Scores
         */
        void onNewCurrentScores(ScoreType type, double[] values, double timestamp);

        /**
         * Callback when new average Scores are available
         *
         * @param values Average Scores
         */
        void onNewAverageScores(ScoreType type, double[] values, double timestamp);

        void onNewClassification(ScoreType type, boolean stateReached, double timestamp);
    }


    public enum ScoreType {
        FOCUS,
        RELAX
    }

    /**
     * Score Measures computed from the EEG signal
     */
    public enum ScoreMeasure {
        NAIVE,
        SHANNON,
        RENYI,
        TSALLIS
    }

    private static final String TAG = EegScoreProcessor.class.getSimpleName();
    private static final int LEARNING_TIME = 3 * 60 * 1000;
    private static final int WINDOW_SIZE = 20;
    private static final int ENTROPIC_INDEX = 3;

    private static double MAX_SHANNON = 0.0;
    private static double MAX_RENYI = 0.0;


    private EegScoreListener mListener;

    /**
     * Score type for this processor instance
     */
    private ScoreType mScoreType;

    private double mStartTimestamp;
    private double mCurrentTimestamp;
    private boolean mLearning;

    /**
     * EEG channels used for score computation
     */
    private Eeg[] mChannels;

    /**
     * EEG bands used for score computation
     */
    private MuseDataPacketType[] mEegBands;

    /**
     * Temporary buffer storing the new incoming EEG samples
     * <p>
     * 1st dimension: # of bands
     * 2nd dimension: # of channels
     */
    private double[][] mTempValues;
    /**
     * Row counter for ringbuffer
     */
    private int rowCounter = 0;
    /**
     * EEG sample ringbuffer for Score computation
     * <p>
     * 1st dimension: # of bands
     * 2nd dimension: # of channels
     * 3rd dimension: ring buffer size
     */
    private double[][][] mEegRingbuffer;

    /**
     * Quantile Processor for minimum/maxiumum Outlier detection
     * <p>
     * 1st dimension: # of bands
     * 2nd dimension: # of channels
     * 3rd dimension: upper, lower quantile
     */
    private QuantileProcessor[][][] mEegQuantiles;


    private QuantileProcessor[] mScoreQuantiles;


    /**
     * Current Score values
     * 1st dimension: # of measures
     */
    private double[] mCurrScores;
    /**
     * Moving-average filtered Score values
     * 1st dimension: # of measures
     */
    private double[] mAvgScores;

    /**
     * Indicator for new EEG samples
     * 1st dimension: # of bands
     */
    private boolean[] mNewVals;

    /**
     * Score ringbuffer for moving-average computation
     * 1st dimension: # of score measures
     * 2nd dimension: history size
     */
    private double[][] mScoreRingbuffer;


    public EegScoreProcessor(Eeg[] channels, MuseDataPacketType[] eegBands, ScoreType type, EegScoreListener listener) {

        mListener = listener;
        mChannels = channels;
        mEegBands = eegBands;
        mScoreType = type;

        mTempValues = new double[eegBands.length][channels.length];
        mNewVals = new boolean[eegBands.length];
        mEegRingbuffer = new double[eegBands.length][channels.length][WINDOW_SIZE];
        mEegQuantiles = new QuantileProcessor[eegBands.length][channels.length][2];
        for (int i = 0; i < mEegQuantiles.length; i++) {
            for (int j = 0; j < mEegQuantiles[i].length; j++) {
                mEegQuantiles[i][j][0] = new QuantileProcessor(0.1);
                mEegQuantiles[i][j][1] = new QuantileProcessor(0.9);
            }
        }
        mScoreRingbuffer = new double[ScoreMeasure.values().length][WINDOW_SIZE];
        mCurrScores = new double[ScoreMeasure.values().length];
        mAvgScores = new double[ScoreMeasure.values().length];
        mScoreQuantiles = new QuantileProcessor[ScoreMeasure.values().length];
        if (mScoreType == ScoreType.FOCUS) {
            mScoreQuantiles[ScoreMeasure.NAIVE.ordinal()] = new QuantileProcessor(0.55);
            mScoreQuantiles[ScoreMeasure.SHANNON.ordinal()] = new QuantileProcessor(0.55);
            mScoreQuantiles[ScoreMeasure.RENYI.ordinal()] = new QuantileProcessor(0.65);
            mScoreQuantiles[ScoreMeasure.TSALLIS.ordinal()] = new QuantileProcessor(0.65);
        } else {
            mScoreQuantiles[ScoreMeasure.NAIVE.ordinal()] = new QuantileProcessor(0.50);
            mScoreQuantiles[ScoreMeasure.SHANNON.ordinal()] = new QuantileProcessor(0.50);
            mScoreQuantiles[ScoreMeasure.RENYI.ordinal()] = new QuantileProcessor(0.55);
            mScoreQuantiles[ScoreMeasure.TSALLIS.ordinal()] = new QuantileProcessor(0.55);
        }

        MAX_SHANNON = 0.5 * DoubleMath.log2(0.25 * Math.PI * Math.E) * mEegBands.length;
        MAX_RENYI = (1 / (1 - ENTROPIC_INDEX)) * DoubleMath.log2(Math.pow(0.001, ENTROPIC_INDEX)) /
                (mEegBands.length + 1);

        mStartTimestamp = System.currentTimeMillis();
    }


    public synchronized void nextEegSample(MuseDataPacketType band, double[] values,
                                           double timestamp, double[] quality) {
        mCurrentTimestamp = timestamp;
        // if quality indicator available: check if signal quality in required channels sufficient
        if (quality != null) {
            for (Eeg eeg : Eeg.values()) {
                if (quality[eeg.ordinal()] != 1) {
                    return;
                }
            }
        }

        // add new EEG samples to temporary buffer
        for (int i = 0; i < mEegBands.length; i++) {
            if (mEegBands[i].equals(band)) {
                for (int j = 0; j < mChannels.length; j++) {
                    mTempValues[i][j] = values[mChannels[j].ordinal()];
                    mNewVals[i] = true;
                }
            }
        }


        // check if new EEG data arrived for all frequency bands
        for (boolean b : mNewVals) {
            if (!b) {
                return;
            }
        }

        if (mLearning) {
            mLearning = (mCurrentTimestamp - mStartTimestamp) < LEARNING_TIME;
        }

        // update quantiles and add temporary values to ringbuffer
        for (int i = 0; i < mEegBands.length; i++) {
            for (int j = 0; j < mChannels.length; j++) {
                double val = mTempValues[i][j];
                mEegQuantiles[i][j][0].next(val);
                mEegQuantiles[i][j][1].next(val);
                mEegRingbuffer[i][j][rowCounter % WINDOW_SIZE] = val;
            }
        }
        rowCounter++;

        if (rowCounter % WINDOW_SIZE == 0) {
            // compute Scores if ring buffer is full
            try {
                computeScores();
                if (!mLearning) {
                    classify();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
            if (mListener != null) {
                mListener.onNewCurrentScores(mScoreType, mCurrScores, mCurrentTimestamp);
                mListener.onNewAverageScores(mScoreType, mAvgScores, mCurrentTimestamp);
            }
        }
    }

    private void classify() {
        if (mAvgScores[ScoreMeasure.RENYI.ordinal()] >= mScoreQuantiles[ScoreMeasure.RENYI.ordinal()].getP()) {
            mListener.onNewClassification(mScoreType, true, mCurrentTimestamp);
        } else {
            mListener.onNewClassification(mScoreType, false, mCurrentTimestamp);
        }
    }

    private synchronized void computeScores() {
        mNewVals = new boolean[mChannels.length];

        double[][] normVals = new double[mEegBands.length][mChannels.length];
        for (int i = 0; i < mEegBands.length; i++) {
            for (int j = 0; j < mChannels.length; j++) {
                double meanVal = Stats.meanOf(mEegRingbuffer[i][j]);
                double normVal = normalize(meanVal, mEegQuantiles[i][j][0].getP(), mEegQuantiles[i][j][1].getP());
                if (normVal < 0.0 || normVal > 1.0) {
                    Log.i(TAG, mScoreType + " out of range");
                    return;
                }
                normVals[i][j] = normVal;
            }
        }

        computeNaiveScore(normVals);
        computeEntropyScores(normVals);
    }

    private void computeEntropyScores(double[][] vals) {
        double[] shannon = new double[mChannels.length];
        double[] renyi = new double[mChannels.length];
        double[] tsallis = new double[mChannels.length];

        for (int j = 0; j < mChannels.length; j++) {
            for (int i = 0; i < mEegBands.length; i++) {
                shannon[j] += (vals[i][j] * DoubleMath.log2(vals[i][j]));
                renyi[j] += Math.pow(vals[i][j], ENTROPIC_INDEX);
                tsallis[j] += (vals[i][j] - Math.pow(vals[i][j], ENTROPIC_INDEX));
            }
            renyi[j] = DoubleMath.log2(renyi[j]);
        }

        double shannonVal = 1.0 + normalize(Stats.meanOf(shannon), 0, MAX_SHANNON);
        shannonVal = Math.max(Math.min(shannonVal, 1.0), 0.0);
        double renyiVal = 1.0 - (1.0 / (1.0 - ENTROPIC_INDEX)) * Stats.meanOf(renyi);
        renyiVal = Math.max(Math.min(0.5 * (1.0 - normalize(renyiVal, 0, MAX_RENYI)), 1.0), 0.0);
        double tsallisVal = 1.0 - (1.0 / (ENTROPIC_INDEX - 1.0)) * Stats.meanOf(tsallis);
        tsallisVal = Math.max(Math.min(tsallisVal, 1.0), 0.0);

        mScoreRingbuffer[ScoreMeasure.SHANNON.ordinal()][(rowCounter / WINDOW_SIZE) % WINDOW_SIZE] = shannonVal;
        mScoreRingbuffer[ScoreMeasure.RENYI.ordinal()][(rowCounter / WINDOW_SIZE) % WINDOW_SIZE] = renyiVal;
        mScoreRingbuffer[ScoreMeasure.TSALLIS.ordinal()][(rowCounter / WINDOW_SIZE) % WINDOW_SIZE] = tsallisVal;

        mCurrScores[ScoreMeasure.SHANNON.ordinal()] = shannonVal;
        mCurrScores[ScoreMeasure.RENYI.ordinal()] = renyiVal;
        mCurrScores[ScoreMeasure.TSALLIS.ordinal()] = tsallisVal;

        for (int i = ScoreMeasure.NAIVE.ordinal(); i < ScoreMeasure.values().length; i++) {
            mAvgScores[i] = Stats.meanOf(mScoreRingbuffer[i]);
            mScoreQuantiles[i].next(mAvgScores[i]);
        }
    }

    private void computeNaiveScore(double[][] vals) {
        for (int i = 0; i < mEegBands.length; i++) {
            if (mEegBands[i] == MuseDataPacketType.ALPHA_RELATIVE || mEegBands[i] == MuseDataPacketType.GAMMA_RELATIVE) {
                double score = Stats.meanOf(vals[i]);
                mScoreRingbuffer[ScoreMeasure.NAIVE.ordinal()][(rowCounter / WINDOW_SIZE) % WINDOW_SIZE] = score;
                mCurrScores[ScoreMeasure.NAIVE.ordinal()] = score;
                mAvgScores[ScoreMeasure.NAIVE.ordinal()] = Stats.meanOf(mScoreRingbuffer[ScoreMeasure.NAIVE.ordinal()]);
            }
        }
    }

    private double normalize(double val, double min, double max) {
        return (Math.abs(max - min) <= 10e-5) ? val : (val - min) / (max - min);
    }

    /**
     * Returns the Score type computed by this processor.
     *
     * @return Score type (RELAX or FOCUS)
     */
    public ScoreType getScoreType() {
        return mScoreType;
    }

    /**
     * Returns the current Score.
     *
     * @param measure {@link ScoreMeasure} that should be returned
     * @return Current Score for the specified measure
     */
    public double getCurrScore(ScoreMeasure measure) {
        return mCurrScores[measure.ordinal()];
    }

    /**
     * Returns the current Score.
     *
     * @return Current Score for the Default Score measure (Renyi entropy-based score)
     */
    public double getCurrScore() {
        return mCurrScores[ScoreMeasure.RENYI.ordinal()];
    }

    /**
     * Returns the moving-average filtered Score.
     *
     * @param measure {@link ScoreMeasure} that should be returned
     * @return Moving-average filtered Score for the specified measure
     */
    public double getAvgScore(ScoreMeasure measure) {
        return mAvgScores[measure.ordinal()];
    }

    /**
     * Returns the moving-average filtered Score.
     *
     * @return Moving-average filtered Score for the Default Score measure (Renyi entropy-based score)
     */
    public double getAvgScore() {
        return mAvgScores[ScoreMeasure.RENYI.ordinal()];
    }
}
