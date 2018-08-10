package edu.mit.media.eegmonitor;

import de.fau.sensorlib.dataframe.SensorDataFrame;
import de.fau.sensorlib.sensors.AbstractSensor;

/**
 * Created by Robert on 11/28/16.
 */

public interface SensorActivityCallback {

    void onScanResult(AbstractSensor sensor, boolean sensorFound);

    void onStartStreaming(AbstractSensor sensor);

    void onStopStreaming(AbstractSensor sensor);

    void onMessageReceived(AbstractSensor sensor, Object... message);

    void onSensorConnected(AbstractSensor sensor);

    void onSensorDisconnected(AbstractSensor sensor);

    void onSensorConnectionLost(AbstractSensor sensor);

    void onDataReceived(AbstractSensor sensor, SensorDataFrame data);

}
