package edu.mit.media.eegmonitor.communication;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Created by Robert on 11/26/16.
 */

public class SceneSpaceWebSocket extends WebSocketClient {

    private static final String TAG = SceneSpaceWebSocket.class.getSimpleName();

    public interface WebSocketListener {
        void onOpen();

        void onMessage(String message);

        void onClose(String reason);
    }

    private WebSocketListener mListener;


    public SceneSpaceWebSocket(URI serverURI, WebSocketListener listener) {
        super(serverURI, new Draft_6455());
        mListener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d(TAG, "Websocket opened");
        mListener.onOpen();
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, "Websocket message received: " + message);
        mListener.onMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d(TAG, "Websocket closed: " + reason);
        mListener.onClose(reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.d(TAG, "Websocket exception: " + ex.getMessage());
    }
}
