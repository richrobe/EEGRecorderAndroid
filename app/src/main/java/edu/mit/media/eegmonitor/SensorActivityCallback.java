package edu.mit.media.eegmonitor;

import de.fau.sensorlib.DsSensor;
import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Created by Robert on 11/28/16.
 */

public interface SensorActivityCallback {

    void onScanResult(DsSensor sensor, boolean sensorFound);

    void onStartStreaming(DsSensor sensor);

    void onStopStreaming(DsSensor sensor);

    void onMessageReceived(DsSensor sensor, Object... message);

    void onSensorConnected(DsSensor sensor);

    void onSensorDisconnected(DsSensor sensor);

    void onSensorConnectionLost(DsSensor sensor);

    void onDataReceived(DsSensor sensor, SensorDataFrame data);

}
