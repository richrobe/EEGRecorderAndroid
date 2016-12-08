package media.mit.edu.macontrol;

import de.fau.sensorlib.dataframe.SensorDataFrame;

/**
 * Created by Robert on 11/28/16.
 */

public interface SensorActivityCallback {

    void onScanResult(boolean sensorFound);

    void onStartStreaming();

    void onStopStreaming();

    void onMessageReceived(Object... message);

    void onSensorConnected();

    void onSensorDisconnected();

    void onSensorConnectionLost();

    void onDataReceived(SensorDataFrame data);

}
