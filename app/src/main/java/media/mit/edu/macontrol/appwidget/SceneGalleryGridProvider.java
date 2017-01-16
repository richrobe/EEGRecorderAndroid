package media.mit.edu.macontrol.appwidget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import media.mit.edu.macontrol.R;
import media.mit.edu.macontrol.SceneItem;

public class SceneGalleryGridProvider implements RemoteViewsFactory {

    private static final String TAG = SceneGalleryGridProvider.class.getSimpleName();

    private ArrayList<SceneItem> sceneItemList = new ArrayList<>();
    private Context mContext = null;

    public SceneGalleryGridProvider(Context context) {
        mContext = context;

        populateListItem();
    }

    private void populateListItem() {
        sceneItemList = new ArrayList<>();
        sceneItemList.add(new SceneItem("Forest", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0000_forest.jpg"));
        sceneItemList.add(new SceneItem("Silverman", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0001_city.jpg"));
        sceneItemList.add(new SceneItem("Rotch", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0002_indoor.jpg"));
        sceneItemList.add(new SceneItem("Colored", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0003_indoor.jpg"));
        sceneItemList.add(new SceneItem("Entrance", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0004_indoor.jpg"));
        sceneItemList.add(new SceneItem("Hyden", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0005_indoor.jpg"));
        sceneItemList.add(new SceneItem("Aquarium", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0006_water.jpg"));
        sceneItemList.add(new SceneItem("Vineyard", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0007_nature.jpg"));
        sceneItemList.add(new SceneItem("ReadingRoom", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0008_indoor.jpg"));
        sceneItemList.add(new SceneItem("Shibuya", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0009_city.jpg"));
        sceneItemList.add(new SceneItem("Kites", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0010_kites.jpg"));
        sceneItemList.add(new SceneItem("Sunset", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0011_sunset.jpg"));
        sceneItemList.add(new SceneItem("Neutral", "http://lightinglab.media.mit.edu:8888/scenes/rep_frame/0012_neutral.jpg"));
    }

    @Override
    public int getCount() {
        return sceneItemList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /*
     * Similar to getView of Adapter where instead of a View we return RemoteViews
     */
    @Override
    public RemoteViews getViewAt(int position) {
        final RemoteViews remoteView = new RemoteViews(
                mContext.getPackageName(), R.layout.layout_appwidget_item);

        SceneItem sceneItem = sceneItemList.get(position);
        remoteView.setTextViewText(R.id.heading, sceneItem.name);
        remoteView.setImageViewBitmap(R.id.imageView, getImageBitmap(sceneItem.imageUrl));

        Intent fillInIntent = new Intent();
        fillInIntent.putExtra(SceneGalleryWidgetProvider.EXTRA_POSITION, position);
        fillInIntent.putExtra(SceneGalleryWidgetProvider.EXTRA_NAME, sceneItemList.get(position).name);
        remoteView.setOnClickFillInIntent(R.id.imageView, fillInIntent);

        return remoteView;
    }


    private Bitmap getImageBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(bis);
            bis.close();
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Error getting bitmap", e);
        }
        return bm;
    }


    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
    }

    @Override
    public void onDestroy() {
    }

}
