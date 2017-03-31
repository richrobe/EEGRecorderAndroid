package edu.mit.media.eegmonitor.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.choosemuse.libmuse.Eeg;

import java.text.DecimalFormat;

import de.fau.lme.plotview.Plot;
import de.fau.lme.plotview.PlotView;
import de.fau.lme.plotview.SamplingPlot;
import de.fau.lme.widgets.StatusBar;
import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.MuseSensor;
import edu.mit.media.eegmonitor.R;
import edu.mit.media.eegmonitor.SensorActivityCallback;
import edu.mit.media.eegmonitor.communication.BleService;

import static edu.mit.media.eegmonitor.dataprocessing.EegScoreProcessor.EegScoreCallback;
import static edu.mit.media.eegmonitor.dataprocessing.EegScoreProcessor.ScoreMeasure;
import static edu.mit.media.eegmonitor.dataprocessing.EegScoreProcessor.ScoreType;

public class MuseRecordingActivity extends BaseActivity implements View.OnClickListener,
        ServiceConnection, SensorActivityCallback, EegScoreCallback {

    private static final String TAG = MuseRecordingActivity.class.getSimpleName();

    private static final int SAMPLING_RATE_EEG_BANDS = 10;

    private StatusBar mStatusBar;
    private FloatingActionButton mFab;
    private boolean mFabToggle;
    private Button mPauseButton;
    private boolean mPauseButtonPressed;
    private Button mStopButton;
    private PlotView mEegBandsPlotView;
    private Plot mAlphaBandPlot;
    private Plot mBetaBandPlot;
    private Plot mGammaBandPlot;
    private Plot mThetaBandPlot;
    private PlotView mEegScoresPlotView;
    private Plot mFocusScorePlot;
    private Plot mRelaxScorePlot;
    private TextView mTotalBlinksTextView;
    private TextView mBlinkRateTextView;


    private Animation mAnimLeftClose;
    private Animation mAnimRightClose;
    private Animation mAnimFABPressed;

    /**
     * Boolean indicating if Muse device is streaming and if the Pause Button is clicked.
     */
    private boolean mStreaming;
    private boolean mConnected;

    protected BleService mService;
    protected Intent mServiceIntent;
    private BleService.MuseDataProcessor mSensorDataProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_muse_recording);
        super.onCreate(savedInstanceState);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(this);
        mStatusBar = (StatusBar) findViewById(R.id.status_bar);
        mStatusBar.setStatus(StatusBar.STATUS_DISCONNECTED);
        mPauseButton = (Button) findViewById(R.id.button_pause);
        mPauseButton.setOnClickListener(this);
        mStopButton = (Button) findViewById(R.id.button_stop);
        mStopButton.setOnClickListener(this);
        mTotalBlinksTextView = (TextView) findViewById(R.id.tv_total_blinks);
        mTotalBlinksTextView.setText(getString(R.string.placeholder_total_blinks, 0));
        mBlinkRateTextView = (TextView) findViewById(R.id.tv_blink_rate);
        mBlinkRateTextView.setText(getString(R.string.placeholder_blink_rate, new DecimalFormat("00.00").format(0)));

        mEegBandsPlotView = (PlotView) findViewById(R.id.pv_eeg_bands);
        mAlphaBandPlot = new SamplingPlot("",
                Plot.generatePlotPaint(5f, ContextCompat.getColor(this, android.R.color.holo_blue_light)),
                Plot.PlotStyle.LINE, 250000);
        ((SamplingPlot) mAlphaBandPlot).setViewport(SAMPLING_RATE_EEG_BANDS, 1);
        mAlphaBandPlot.hideAxis(true);
        mBetaBandPlot = new SamplingPlot("",
                Plot.generatePlotPaint(5f, ContextCompat.getColor(this, android.R.color.holo_orange_light)),
                Plot.PlotStyle.LINE, 250000);
        mBetaBandPlot.hideAxis(true);
        ((SamplingPlot) mBetaBandPlot).setViewport(SAMPLING_RATE_EEG_BANDS, 1);
        mGammaBandPlot = new SamplingPlot("",
                Plot.generatePlotPaint(5f, ContextCompat.getColor(this, android.R.color.holo_purple)),
                Plot.PlotStyle.LINE, 250000);
        mGammaBandPlot.hideAxis(true);
        ((SamplingPlot) mGammaBandPlot).setViewport(SAMPLING_RATE_EEG_BANDS, 1);
        mThetaBandPlot = new SamplingPlot("",
                Plot.generatePlotPaint(5f, ContextCompat.getColor(this, android.R.color.holo_red_light)),
                Plot.PlotStyle.LINE, 250000);
        mThetaBandPlot.hideAxis(true);
        ((SamplingPlot) mThetaBandPlot).setViewport(SAMPLING_RATE_EEG_BANDS, 1);
        mEegBandsPlotView.setVisibility(View.VISIBLE);
        mEegBandsPlotView.attachPlot(mAlphaBandPlot);
        mEegBandsPlotView.attachPlot(mBetaBandPlot);
        mEegBandsPlotView.attachPlot(mGammaBandPlot);
        mEegBandsPlotView.attachPlot(mThetaBandPlot);
        mEegBandsPlotView.addFlag(PlotView.Flags.USE_THREAD_SAFE_UI_CALLS);

        mEegScoresPlotView = (PlotView) findViewById(R.id.pv_eeg_scores);
        mFocusScorePlot = new SamplingPlot("Focus / Relax",
                Plot.generatePlotPaint(5f, ContextCompat.getColor(this, android.R.color.holo_red_light)),
                Plot.PlotStyle.LINE, 250000);
        mFocusScorePlot.hideAxis(true);
        ((SamplingPlot) mFocusScorePlot).setViewport(SAMPLING_RATE_EEG_BANDS, 10);
        mRelaxScorePlot = new SamplingPlot("",
                Plot.generatePlotPaint(5f, ContextCompat.getColor(this, android.R.color.holo_green_light)),
                Plot.PlotStyle.LINE, 250000);
        mRelaxScorePlot.hideAxis(true);
        ((SamplingPlot) mRelaxScorePlot).setViewport(SAMPLING_RATE_EEG_BANDS, 10);
        mEegScoresPlotView.attachPlot(mFocusScorePlot);
        mEegScoresPlotView.attachPlot(mRelaxScorePlot);
        mEegScoresPlotView.addFlag(PlotView.Flags.USE_THREAD_SAFE_UI_CALLS);

        // load animations for closing Pause and Stop Buttons and animate FAB
        mAnimLeftClose = AnimationUtils.loadAnimation(this, R.anim.view_pause_close);
        mAnimRightClose = AnimationUtils.loadAnimation(this, R.anim.view_stop_close);
        mAnimFABPressed = AnimationUtils.loadAnimation(this, R.anim.fab_pressed);

        mServiceIntent = new Intent(this, BleService.class);

        checkPreferenceUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPreferenceUpdates();
        try {
            DsSensorManager.enableBluetooth(this);
            DsSensorManager.checkBtLePermissions(this, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.button_stop:
                onStopButtonClick();
                break;
            case R.id.button_pause:
                onPauseButtonClick();
                break;
            case R.id.fab:
                if (!mFabToggle && !mStreaming) {
                    startMuse();
                }
                animateFAB();
        }
    }

    public void clearUi() {
        mAlphaBandPlot.clear();
    }

    private void onPauseButtonClick() {
        if (!mPauseButton.isEnabled()) {
            return;
        }

        if (!mPauseButtonPressed) {
            // Pause DailyHeart
            mPauseButton.setText(R.string.button_resume);
        } else {
            // Resume DailyHeart
            mPauseButton.setText(R.string.button_pause);
        }
        mPauseButtonPressed = !mPauseButtonPressed;
    }

    private void onStopButtonClick() {
        if (!mStopButton.isEnabled()) {
            return;
        }
        // start animations
        mStreaming = false;
        mPauseButton.startAnimation(mAnimLeftClose);
        mStopButton.startAnimation(mAnimRightClose);
        mFab.performClick();
        mFab.setImageResource(R.drawable.ic_play);
        stopMuse();
    }

    void animateFAB() {
        if (!mFabToggle) {
            Animation animLeft = AnimationUtils.loadAnimation(this, R.anim.view_pause_open);
            Animation animRight = AnimationUtils.loadAnimation(this, R.anim.view_stop_open);
            Animation animFAB = AnimationUtils.loadAnimation(this, R.anim.fab_not_pressed);
            mPauseButton.setVisibility(View.VISIBLE);
            mPauseButton.setEnabled(true);
            mStopButton.setVisibility(View.VISIBLE);
            mStopButton.setEnabled(true);
            mPauseButton.startAnimation(animLeft);
            mStopButton.startAnimation(animRight);
            mFab.startAnimation(animFAB);
            mFab.setImageResource(R.drawable.ic_stop);
        } else {
            mAnimLeftClose.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mPauseButton.setVisibility(View.INVISIBLE);
                    mPauseButton.setEnabled(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mAnimRightClose.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mStopButton.setVisibility(View.INVISIBLE);
                    mStopButton.setEnabled(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            mPauseButton.startAnimation(mAnimLeftClose);
            mStopButton.startAnimation(mAnimRightClose);
            mFab.startAnimation(mAnimFABPressed);
        }
        mFabToggle = !mFabToggle;
    }

    private void checkPreferenceUpdates() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String ipAddress = sp.getString(getString(R.string.pref_address), "127.0.0.1");
        int port = Integer.parseInt(sp.getString(getString(R.string.pref_port), "5000"));
        Log.d(TAG, "address: " + ipAddress + ", port: " + port);
        if (mService != null) {
            mService.setStreamingEnabled(sp.getBoolean(getString(R.string.pref_enable_streaming), true), sp.getBoolean(getString(R.string.pref_streaming_type), false));
            mService.setDestinationAddress(ipAddress, port);
        }
    }

    private void startMuse() {
        Log.d(TAG, "startMuse");
        bindService(mServiceIntent, this, Context.BIND_AUTO_CREATE);
    }

    private void stopMuse() {
        mService.stopMuse();

        // unbind Service
        unbindService(this);
        mService = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = ((BleService.BleServiceBinder) service).getService();
        mService.setSensorActivityCallback(this);
        mService.setScoreCallback(this);
        mService.startMuse();
        mSensorDataProcessor = mService.getMuseDataProcessor();
        checkPreferenceUpdates();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    public void onScanResult(DsSensor sensor, boolean sensorFound) {
        if (!sensorFound) {
            Log.d(TAG, "No sensors found...");
            mStopButton.performClick();
        } else {
            mStatusBar.setStatus(StatusBar.STATUS_CONNECTING);
        }
    }

    @Override
    public void onStartStreaming(DsSensor sensor) {
        mFab.performClick();
        mStreaming = true;
        clearUi();
    }

    @Override
    public void onStopStreaming(DsSensor sensor) {
        mStreaming = false;
    }

    @Override
    public void onMessageReceived(DsSensor sensor, Object... message) {

    }

    @Override
    public void onSensorConnected(DsSensor sensor) {
        mConnected = true;
        mStatusBar.setStatus(StatusBar.STATUS_CONNECTED);
    }

    @Override
    public void onSensorDisconnected(DsSensor sensor) {
        mConnected = false;
        mStatusBar.setStatus(StatusBar.STATUS_DISCONNECTED);
    }

    @Override
    public void onSensorConnectionLost(DsSensor sensor) {
        mConnected = false;
        mStopButton.performClick();
        mFab.performClick();
        mStatusBar.setStatus(StatusBar.STATUS_DISCONNECTED);
        Snackbar.make(mCoordinatorLayout, "Connection to sensor lost.", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDataReceived(DsSensor sensor, SensorDataFrame data) {
        if (data instanceof MuseSensor.MuseEegDataFrame) {
            MuseSensor.MuseEegDataFrame eegData = (MuseSensor.MuseEegDataFrame) data;
            long timestamp = (long) eegData.getTimestamp();
            double[] eegValues = eegData.getEegBand();
            float eegValue = (float) (eegValues[Eeg.EEG1.ordinal()] + eegValues[Eeg.EEG4.ordinal()]) / 2.0f;
            Plot plot;
            switch (eegData.getPacketType()) {
                case ALPHA_RELATIVE:
                    plot = mAlphaBandPlot;
                    break;
                case BETA_RELATIVE:
                    plot = mBetaBandPlot;
                    break;
                case GAMMA_RELATIVE:
                    plot = mGammaBandPlot;
                    break;
                case THETA_RELATIVE:
                    plot = mThetaBandPlot;
                    break;
                default:
                    return;
            }

            if (!Float.isNaN(eegValue)) {
                ((SamplingPlot) plot).addValue(eegValue, timestamp);
                mEegBandsPlotView.requestRedraw(true);
            }
        } else if (data instanceof MuseSensor.MuseArtifactDataFrame) {
            if (((MuseSensor.MuseArtifactDataFrame) data).getBlink()) {
                float blinkRate = (float) mSensorDataProcessor.getBlinkRate();
                mTotalBlinksTextView.setText(getString(R.string.placeholder_total_blinks, mSensorDataProcessor.getTotalBlinks()));
                mBlinkRateTextView.setText(getString(R.string.placeholder_blink_rate, new DecimalFormat("00.00").format(blinkRate)));
                mCoordinatorLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCoordinatorLayout.setBackgroundColor(ContextCompat.getColor(MuseRecordingActivity.this, R.color.grey_50));
                    }
                }, 200);
            }
        }
    }

    @Override
    public void onNewCurrentScores(ScoreType type, double[] values, double timestamp) {

    }

    @Override
    public void onNewAverageScores(ScoreType type, double[] values, double timestamp) {
        double scoreVal = values[ScoreMeasure.RENYI.ordinal()];
        if (!Double.isNaN(scoreVal)) {
            switch (type) {
                case FOCUS:
                    ((SamplingPlot) mFocusScorePlot).addValue((float) scoreVal, (long) timestamp);
                    mEegScoresPlotView.requestRedraw(true);
                    break;
                case RELAX:
                    ((SamplingPlot) mRelaxScorePlot).addValue((float) scoreVal, (long) timestamp);
                    mEegScoresPlotView.requestRedraw(true);
                    break;
            }
        }
    }

    @Override
    public void onNewClassification(ScoreType type, boolean stateReached, double timestamp) {

    }
}
