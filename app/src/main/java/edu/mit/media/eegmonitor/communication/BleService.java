package edu.mit.media.eegmonitor.communication;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.MuseSensor;
import edu.mit.media.eegmonitor.SensorActivityCallback;
import edu.mit.media.eegmonitor.dataprocessing.BlinkRateProcessor;
import edu.mit.media.eegmonitor.dataprocessing.EegScoreProcessor;
import edu.mit.media.eegmonitor.dataprocessing.EegScoreProcessor.ScoreMeasure;
import edu.mit.media.eegmonitor.dataprocessing.EegScoreProcessor.ScoreType;

public class BleService extends Service {

    private static final String TAG = BleService.class.getSimpleName();

    private IBinder mBinder = new BleServiceBinder();

    private static int PORT;
    private static String IP_ADDRESS;
    private static final String ADDRESS_BLINK = "/muse/elements/blink";
    private static final String ADDRESS_BLINK_RATE = "/muse/elements/blink_rate";
    private static final String ADDRESS_EEG = "/muse/elements/";
    private static final String ADDRESS_RELAX = "/eeg/relax_score";
    private static final String ADDRESS_FOCUS = "/eeg_focus_score";
    private OSCPortOut mOscPortOut;
    private boolean mStreamingEnabled;
    private boolean mStreamScoreValues;

    private final BlockingQueue<Runnable> mSendQueue = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor mThreadPoolExecutor;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;
    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private SensorFoundCallback mSensorFoundCallback = new SensorFoundCallback() {
        @Override
        public boolean onKnownSensorFound(SensorInfo sensor) {
            Log.d(TAG, "Known sensor found: " + sensor.getName() + ", " + sensor.getDeviceAddress());
            switch (sensor.getDeviceClass()) {
                case MUSE:
                    mMuseSensor = new MuseSensor(BleService.this, sensor, mMuseDataProcessor);
                    break;
                case EMPATICA:
                    break;
            }
            return true;
        }

        @Override
        public boolean onUnknownSensorFound(String name, String address) {
            return super.onUnknownSensorFound(name, address);
        }
    };

    private SensorActivityCallback mActivityCallback;


    private DsSensor mMuseSensor;
    private MuseDataProcessor mMuseDataProcessor = new MuseDataProcessor();
    private EegScoreProcessor.EegScoreListener mScoreCallback;

    public void setSensorActivityCallback(SensorActivityCallback callback) {
        mActivityCallback = callback;
    }

    public void setScoreCallback(EegScoreProcessor.EegScoreListener callback) {
        mScoreCallback = callback;
    }

    public void startMuse() {
        try {
            DsSensorManager.searchBleDevices(mSensorFoundCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DsSensorManager.cancelRunningScans();
                if (mMuseSensor == null) {
                    mActivityCallback.onScanResult(null, false);
                }
            }
        }, 10000);
    }

    public void stopMuse() {
        if (mMuseSensor != null) {
            mMuseSensor.stopStreaming();
            mMuseSensor.disconnect();
            mMuseSensor = null;
        }
    }

    public MuseDataProcessor getMuseDataProcessor() {
        return mMuseDataProcessor;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bond!");

        try {
            mOscPortOut = new AsyncTask<Void, Void, OSCPortOut>() {

                @Override
                protected OSCPortOut doInBackground(Void... params) {
                    try {
                        return mOscPortOut = new OSCPortOut(InetAddress.getByName(IP_ADDRESS), PORT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mThreadPoolExecutor = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES, KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, mSendQueue);

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DsSensorManager.cancelRunningScans();
        mBinder = null;
        mSensorFoundCallback = null;
        return false;
    }

    public void setStreamingEnabled(boolean enabled, boolean streamScoreValues) {
        mStreamingEnabled = enabled;
        mStreamScoreValues = streamScoreValues;

    }

    public void setDestinationAddress(String ipAddress, int port) {
        IP_ADDRESS = ipAddress;
        PORT = port;
    }

    /**
     * Inner class representing the Binder between {@link BleService}
     * and {@link android.app.Activity}
     */
    public class BleServiceBinder extends Binder {

        public BleService getService() {
            return BleService.this;
        }
    }

    public class MuseDataProcessor extends SensorDataProcessor implements EegScoreProcessor.EegScoreListener {

        private BlinkRateProcessor mBlinkRateProcessor;
        private EegScoreProcessor mFocusScoreProcessor;
        private EegScoreProcessor mRelaxScoreProcessor;

        @Override
        public void onSensorCreated(DsSensor sensor) {
            super.onSensorCreated(sensor);
            mActivityCallback.onScanResult(sensor, true);
            try {
                sensor.useHardwareSensors(EnumSet.of(DsSensor.HardwareSensor.EEG_FREQ_BANDS));
                sensor.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConnected(DsSensor sensor) {
            super.onConnected(sensor);
            mActivityCallback.onSensorConnected(sensor);
            ((MuseSensor) sensor).shouldUseArtifactSensor(true);
            sensor.startStreaming();
        }

        @Override
        public void onDisconnected(DsSensor sensor) {
            super.onDisconnected(sensor);
            mActivityCallback.onSensorDisconnected(sensor);
            mMuseSensor = null;
        }

        @Override
        public void onConnectionLost(DsSensor sensor) {
            super.onConnectionLost(sensor);
            mActivityCallback.onSensorConnectionLost(sensor);
        }

        @Override
        public void onStartStreaming(DsSensor sensor) {
            super.onStartStreaming(sensor);
            mActivityCallback.onStartStreaming(sensor);
            mBlinkRateProcessor = new BlinkRateProcessor(System.currentTimeMillis());
            mFocusScoreProcessor = new EegScoreProcessor(new Eeg[]{Eeg.EEG1, Eeg.EEG4},
                    new MuseDataPacketType[]{MuseDataPacketType.GAMMA_RELATIVE, MuseDataPacketType.BETA_RELATIVE}, ScoreType.FOCUS, this);
            mRelaxScoreProcessor = new EegScoreProcessor(new Eeg[]{Eeg.EEG1, Eeg.EEG4},
                    new MuseDataPacketType[]{MuseDataPacketType.ALPHA_RELATIVE, MuseDataPacketType.THETA_RELATIVE}, ScoreType.RELAX, this);
        }

        @Override
        public void onStopStreaming(DsSensor sensor) {
            super.onStopStreaming(sensor);
            sensor.disconnect();
            mActivityCallback.onStopStreaming(sensor);
        }

        @Override
        public void onNotify(DsSensor sensor, Object notification) {
            Log.d(TAG, "onNotify: " + notification);
            super.onNotify(sensor, notification);
        }

        @Override
        public void onNewData(SensorDataFrame data) {
            if (data instanceof MuseSensor.MuseEegDataFrame) {
                MuseSensor.MuseEegDataFrame eegData = (MuseSensor.MuseEegDataFrame) data;
                mActivityCallback.onDataReceived(mMuseSensor, data);
                switch (eegData.getPacketType()) {
                    case ALPHA_RELATIVE:
                    case THETA_RELATIVE:
                        mRelaxScoreProcessor.nextEegSample(eegData.getPacketType(),
                                eegData.getEegBand(), eegData.getTimestamp(), null);
                        break;
                    case GAMMA_RELATIVE:
                    case BETA_RELATIVE:
                        mFocusScoreProcessor.nextEegSample(eegData.getPacketType(),
                                eegData.getEegBand(), eegData.getTimestamp(), null);
                        break;
                }

                if (mStreamingEnabled && !mStreamScoreValues) {
                    sendOscDataEeg(eegData.getEegBand(), eegData.getPacketType());
                }
            } else if (data instanceof MuseSensor.MuseArtifactDataFrame) {
                MuseSensor.MuseArtifactDataFrame museData = (MuseSensor.MuseArtifactDataFrame) data;
                if (museData.getBlink()) {
                    mBlinkRateProcessor.nextBlink(museData.getTimestamp());
                    mActivityCallback.onDataReceived(mMuseSensor, data);
                    if (mStreamingEnabled) {
                        sendOscDataBlink(mBlinkRateProcessor.getBlinkRate());
                    }
                }
            }
        }

        public int getTotalBlinks() {
            return mBlinkRateProcessor.getTotalBlinks();
        }

        public double getBlinkRate() {
            return mBlinkRateProcessor.getBlinkRate();
        }

        @Override
        public void onNewCurrentScores(ScoreType type, double[] values, double timestamp) {
            mScoreCallback.onNewCurrentScores(type, values, timestamp);
        }

        @Override
        public void onNewAverageScores(ScoreType type, double[] values, double timestamp) {
            Log.d(TAG, "New " + type + " score: " + values[ScoreMeasure.RENYI.ordinal()]);
            //Log.d(TAG, "New " + type + " score: " + Arrays.toString(values));
            mScoreCallback.onNewAverageScores(type, values, timestamp);
            if (mStreamingEnabled && mStreamScoreValues) {
                sendOscDataScores(type, values[ScoreMeasure.RENYI.ordinal()]);
            }
        }

        @Override
        public void onNewClassification(ScoreType type, boolean stateReached, double timestamp) {
            mScoreCallback.onNewClassification(type, stateReached, timestamp);
        }
    }


    private synchronized void sendOscDataEeg(final double[] eegValues, final MuseDataPacketType packetType) {
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                OSCMessage msg = new OSCMessage(ADDRESS_EEG + packetType.name().toLowerCase());
                try {
                    for (int i = 0; i < 4; i++) {
                        if (Float.isNaN((float) eegValues[i])) {
                            return;
                        }
                        msg.addArgument((float) eegValues[i]);
                    }
                    mOscPortOut.send(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private synchronized void sendOscDataScores(final ScoreType type, final double scoreValue) {
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                OSCMessage msg;
                if (type ==ScoreType.FOCUS) {
                    msg = new OSCMessage(ADDRESS_FOCUS);
                } else {
                    msg = new OSCMessage(ADDRESS_RELAX);
                }
                try {
                    if (Float.isNaN((float) scoreValue)) {
                        return;
                    }
                    msg.addArgument(scoreValue);
                    mOscPortOut.send(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private synchronized void sendOscDataBlink(final double blinkrate) {
        mThreadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    OSCMessage msgBlink = new OSCMessage(ADDRESS_BLINK);
                    msgBlink.addArgument(1);
                    OSCMessage msgBlinkRate = new OSCMessage(ADDRESS_BLINK_RATE);
                    msgBlinkRate.addArgument(blinkrate);
                    mOscPortOut.send(msgBlink);
                    mOscPortOut.send(msgBlinkRate);
                    Log.e(TAG, String.valueOf(msgBlink.getArguments()));
                    Log.e(TAG, String.valueOf(msgBlinkRate.getArguments()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
