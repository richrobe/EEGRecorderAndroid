package media.mit.edu.macontrol;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import media.mit.edu.macontrol.MainMenuCardView.MainMenuWrapper;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener,
        DownloadFileTask.AsyncDownloadResponse, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MainMenuAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        connectWebSocket();

        // initialize UI elements
        mAdapter = new MainMenuAdapter(this);
        GridView grid = (GridView) findViewById(R.id.grid_layout);
        grid.setAdapter(mAdapter);
        grid.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(this, mAdapter.mMenuItems[position].className),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
    }

    public class MainMenuAdapter extends BaseAdapter {

        private Context mContext;

        MainMenuWrapper[] mMenuItems =
                new MainMenuWrapper[]{
                        new MainMenuWrapper(SceneSpaceActivity.class, R.drawable.ic_scene_space, R.string.string_scene_space),
                        new MainMenuWrapper(SceneGalleryActivity.class, R.drawable.ic_scene_gallery, R.string.string_scene_gallery),
                        new MainMenuWrapper(MuseRecordingActivity.class, R.drawable.ic_muse, R.string.string_muse)
                };

        MainMenuAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return mMenuItems.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MainMenuCardView cv = new MainMenuCardView(mContext);
            cv.setImage(mMenuItems[position].imageId);
            cv.setTitle(mMenuItems[position].titleId);
            return cv;
        }

    }
}
