package media.mit.edu.macontrol.communication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseIntArray;

import java.util.EnumSet;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.DsSensorManager;
import de.fau.sensorlib.SensorDataProcessor;
import de.fau.sensorlib.SensorFoundCallback;
import de.fau.sensorlib.SensorInfo;
import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.MuseSensor;
import media.mit.edu.macontrol.SensorActivityCallback;

public class BleService extends Service {

    private static final String TAG = BleService.class.getSimpleName();

    private IBinder mBinder = new BleServiceBinder();

    private DsSensor mMuseSensor;
    private MuseDataProcessor mSensorDataProcessor = new MuseDataProcessor();

    private SensorFoundCallback mSensorFoundCallback = new SensorFoundCallback() {
        @Override
        public boolean onKnownSensorFound(SensorInfo sensor) {
            Log.d(TAG, "Known sensor found: " + sensor.getName() + ", " + sensor.getDeviceAddress());
            switch (sensor.getDeviceClass()) {
                case MUSE:
                    mMuseSensor = new MuseSensor(BleService.this, sensor, mSensorDataProcessor);
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


    public void setSensorActivityCallback(SensorActivityCallback callback) {
        mActivityCallback = callback;
    }

    public void startMuse() {
        try {
            DsSensorManager.searchBleDevices(mSensorFoundCallback);
            //mMuseSensor = new MuseSensor(this, DsSensorManager.getFirstConnectableSensor(KnownSensor.MUSE), mSensorDataProcessor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DsSensorManager.cancelRunningScans();
                if (mMuseSensor == null) {
                    mActivityCallback.onScanResult(false);
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

    public MuseDataProcessor getSensorDataProcessor() {
        return mSensorDataProcessor;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service bond!");

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        DsSensorManager.cancelRunningScans();
        mBinder = null;
        mSensorFoundCallback = null;
        return false;
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

    public class MuseDataProcessor extends SensorDataProcessor {

        private int mTotalBlinks = 0;
        private double mBlinkRate = 0.0;

        @Override
        public void onSensorCreated(DsSensor sensor) {
            super.onSensorCreated(sensor);
            mActivityCallback.onScanResult(true);
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
            mActivityCallback.onSensorConnected();
            ((MuseSensor) sensor).shouldUseArtifactSensor(true);
            sensor.startStreaming();
        }

        @Override
        public void onDisconnected(DsSensor sensor) {
            super.onDisconnected(sensor);
            mActivityCallback.onSensorDisconnected();
            mMuseSensor = null;
        }

        @Override
        public void onConnectionLost(DsSensor sensor) {
            super.onConnectionLost(sensor);
            mActivityCallback.onSensorConnectionLost();
        }

        @Override
        public void onStartStreaming(DsSensor sensor) {
            super.onStartStreaming(sensor);
            mActivityCallback.onStartStreaming();
            START_TIME = System.currentTimeMillis();
            mLastTimeStamp = START_TIME;
        }

        @Override
        public void onStopStreaming(DsSensor sensor) {
            super.onStopStreaming(sensor);
            sensor.disconnect();
            mActivityCallback.onStopStreaming();
        }

        @Override
        public void onNotify(DsSensor sensor, Object notification) {
            Log.d(TAG, "onNotify: " + notification);
            super.onNotify(sensor, notification);
        }

        @Override
        public void onNewData(SensorDataFrame data) {
            // TODO
            if (data instanceof MuseSensor.MuseEegDataFrame) {
                MuseSensor.MuseEegDataFrame eegData = (MuseSensor.MuseEegDataFrame) data;
                switch (eegData.getPacketType()) {
                    case ALPHA_RELATIVE:
                        //Log.d(TAG, "alpha: " + Arrays.toString(eegData.getAlphaBand()));
                        break;
                    case BETA_RELATIVE:
                        //Log.d(TAG, "beta: " + Arrays.toString(eegData.getBetaBand()));
                        break;
                    case GAMMA_RELATIVE:
                        //Log.d(TAG, "gamma: " + Arrays.toString(eegData.getGammaBand()));
                        break;
                }
                mActivityCallback.onDataReceived(data);
            } else if (data instanceof MuseSensor.MuseArtifactDataFrame) {
                MuseSensor.MuseArtifactDataFrame museData = (MuseSensor.MuseArtifactDataFrame) data;
                if (museData.getBlink()) {
                    double timeDiff = (museData.getTimestamp() - mLastTimeStamp);
                    if (timeDiff > 150.0) {
                        //Log.d(TAG, "blink: " + timeDiff);
                        int startTimeDiff = (int) (museData.getTimestamp() - START_TIME);
                        mBlinkList.append(startTimeDiff, 1);
                        while ((mBlinkList.keyAt(mBlinkList.size() - 1) - mBlinkList.keyAt(0)) > 10000) {
                            Log.d(TAG, "removing element");
                            mBlinkList.removeAt(0);
                        }

                        if (mBlinkList.size() > 1) {
                            //Log.d(TAG, "SWAP!!! : " + mBlinkList.keyAt(0) + ", " + startTimeDiff);
                            mBlinkRate = (mBlinkList.size() * (1000.0 * 60.0)) / (double) (mBlinkList.keyAt(mBlinkList.size() - 1) - mBlinkList.keyAt(0));
                            Log.d(TAG, "blink rate: " + mBlinkRate + " / min");
                        }
                        Log.d(TAG, "blink list: " + mBlinkList);


                        mTotalBlinks++;
                        mLastTimeStamp = museData.getTimestamp();
                        mActivityCallback.onDataReceived(data);
                    }
                }
            }
        }

        private double START_TIME;
        private double mLastTimeStamp;
        private SparseIntArray mBlinkList = new SparseIntArray();

        public int getTotalBlinks() {
            return mTotalBlinks;
        }

        public double getBlinkRate() {
            return mBlinkRate;
        }
    }
}
