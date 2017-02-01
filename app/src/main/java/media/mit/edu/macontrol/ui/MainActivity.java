package media.mit.edu.macontrol.ui;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;

import media.mit.edu.macontrol.R;
import media.mit.edu.macontrol.ui.MainMenuCardView.MainMenuWrapper;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener,
        View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private MainMenuAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        // initialize UI elements
        mAdapter = new MainMenuAdapter(this);
        GridView grid = (GridView) findViewById(R.id.grid_view);
        grid.setAdapter(mAdapter);
        grid.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(this, mAdapter.mMenuItems[position].className),
                ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
    }

    public class MainMenuAdapter extends BaseAdapter {

        private Context mContext;

        MainMenuWrapper[] mMenuItems =
                new MainMenuWrapper[]{
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
