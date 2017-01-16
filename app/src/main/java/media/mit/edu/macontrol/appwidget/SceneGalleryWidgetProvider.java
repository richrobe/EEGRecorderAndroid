package media.mit.edu.macontrol.appwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

import media.mit.edu.macontrol.R;
import media.mit.edu.macontrol.communication.SceneSpaceWebSocket;

public class SceneGalleryWidgetProvider extends AppWidgetProvider implements SceneSpaceWebSocket.WebSocketListener {

    public static final String ACTION_SCENE = "media.mit.edu.macontrol.ACTION_SCENE";
    public static final String EXTRA_POSITION = "media.mit.edu.macontrol.EXTRA_POSITION";
    public static final String EXTRA_NAME = "media.mit.edu.macontrol.EXTRA_NAME";

    private SceneSpaceWebSocket mWebSocket;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = updateWidgetListView(context,
                    appWidgetId);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.grid_view_widget);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void connectWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://lightinglab.media.mit.edu:55555");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocket = new SceneSpaceWebSocket(uri, this);
        mWebSocket.connect();
    }

    private RemoteViews updateWidgetListView(Context context, int appWidgetId) {

        // define which layout to show in the widget
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_appwidget);

        // RemoteViews Service needed to provide adapter for ListView
        Intent serviceIntent = new Intent(context, SceneGalleryWidgetService.class);

        // passing app widget id to that RemoteViews Service
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        // setting a unique Uri to the intent
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

        // setting an adapter to the widget's GridView
        remoteViews.setRemoteAdapter(R.id.grid_view_widget, serviceIntent);

        // setting an empty view in case of no data
        remoteViews.setEmptyView(R.id.grid_view_widget, R.id.empty_view);

        final Intent clickIntent = new Intent(context, SceneGalleryWidgetProvider.class);
        clickIntent.setAction(ACTION_SCENE);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.grid_view_widget, pendingIntent);

        return remoteViews;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_SCENE.equals(intent.getAction())) {
            connectWebSocket();

            try {
                final int sceneId = intent.getIntExtra(SceneGalleryWidgetProvider.EXTRA_POSITION, -1);
                final String sceneName = intent.getStringExtra(SceneGalleryWidgetProvider.EXTRA_NAME);
                Toast.makeText(context, "Changing to scene [" + sceneId + "] " + sceneName, Toast.LENGTH_SHORT).show();
                if (mWebSocket.getConnection().isOpen()) {
                    mWebSocket.send(buildSceneJson(sceneId, sceneName));
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!mWebSocket.getConnection().isOpen()) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            mWebSocket.send(buildSceneJson(sceneId, sceneName));
                        }
                    }).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String buildSceneJson(int sceneId, String sceneName) {
        JSONObject scene = new JSONObject();
        try {
            scene.put("type", "SCENE")
                    .put("name", sceneName)
                    .put("id", sceneId);
            return scene.toString();

        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onClose(String reason) {

    }
}
