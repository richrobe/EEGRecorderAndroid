package media.mit.edu.macontrol.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import media.mit.edu.macontrol.communication.DownloadFileTask;
import media.mit.edu.macontrol.R;
import media.mit.edu.macontrol.communication.SceneSpaceWebSocket;

public abstract class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, SceneSpaceWebSocket.WebSocketListener, DownloadFileTask.AsyncDownloadResponse, View.OnClickListener {

    private static final String TAG = BaseActivity.class.getSimpleName();

    protected CoordinatorLayout mCoordinatorLayout;
    protected Toolbar mToolbar;
    protected DrawerLayout mDrawerLayout;
    protected NavigationView mNavigationView;

    protected ImageView mCurrentSceneImageView;
    protected TextView mCurrentSceneTextView;
    protected FloatingActionButton mFabOnOff;
    protected boolean mFabOnOffToggle = false;

    protected static final String BASE_URL = "http://lightinglab.media.mit.edu:8888/scenes/";
    protected SceneSpaceWebSocket mWebSocket;
    protected JSONObject mPingJson = new JSONObject();
    protected JSONObject mOffJson = new JSONObject();
    protected JSONObject mOnJson = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        mCurrentSceneImageView = (ImageView) findViewById(R.id.iv_current_scene);
        mCurrentSceneTextView = (TextView) findViewById(R.id.tv_current_scene);
        if (mCurrentSceneTextView != null) {
            mCurrentSceneTextView.setText(Html.fromHtml(getString(R.string.placeholder_current_scene, "loading...")));
        }
        mFabOnOff = (FloatingActionButton) findViewById(R.id.fab_on_off);
        if (mFabOnOff != null) {
            mFabOnOff.setOnClickListener(this);
        }

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator);

        try {
            mPingJson.put("type", "PING");
            mOffJson.put("type", "OFF");
            mOnJson.put("type", "SCENE").put("name", "Neutral");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebSocket != null && mWebSocket.getConnection().isOpen()) {
            mWebSocket.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_base, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        Intent intent;
        switch (item.getItemId()) {
            case R.id.nav_main:
                intent = new Intent(this, MainActivity.class);
                break;
            case R.id.nav_scene_space:
                intent = new Intent(this, SceneSpaceActivity.class);
                break;
            case R.id.nav_scene_gallery:
                intent = new Intent(this, SceneGalleryActivity.class);
                break;
            case R.id.nav_muse_recording:
                intent = new Intent(this, MuseRecordingActivity.class);
                break;
            case R.id.nav_settings:
                intent = new Intent(this, SettingsActivity.class);
                break;
            default:
                intent = null;
                Snackbar.make(mCoordinatorLayout, "Not implemented yet :)", Snackbar.LENGTH_SHORT).show();
        }

        if (intent != null) {
            startActivity(intent);
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void connectWebSocket() {
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

    @Override
    public void onOpen() {
        mWebSocket.send(mPingJson.toString());
    }

    @Override
    public void onMessage(String message) {
        final JSONObject object;
        final String name;
        final int sceneId;
        try {
            object = new JSONObject(message);
            name = object.getString("descriptive_name");
            sceneId = object.getInt("scene_id");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        if (sceneId == -1) {
            // off
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mCurrentSceneImageView.setImageResource(0);
                    mFabOnOffToggle = false;
                    mFabOnOff.setImageResource(R.drawable.ic_off);
                    //mFabOnOff.setBackgroundColor(ContextCompat.getColor(R.color.grey_800));
                }
            });
        } else {
            // on
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFabOnOffToggle = true;
                    mFabOnOff.setImageResource(R.drawable.ic_on);
                }
            });
            String url = BASE_URL + String.format(Locale.getDefault(), "%04d", sceneId) + ".json";
            Log.d(TAG, url);
            new DownloadFileTask(new DownloadFileTask.AsyncDownloadResponse() {
                @Override
                public void publishFinish(String response) {
                    JSONObject object;
                    try {
                        object = new JSONObject(response);
                        JSONObject feature = object.getJSONObject("feature");
                        final String path = feature.getString("rep_frame_path");
                        Picasso.with(BaseActivity.this).load(BASE_URL + path).into(mCurrentSceneImageView);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).execute(url);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCurrentSceneTextView.setText(Html.fromHtml(getString(R.string.placeholder_current_scene, name)));
            }
        });
    }

    @Override
    public void onClose(String reason) {

    }

    @Override
    public void publishFinish(String response) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_on_off:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!mWebSocket.getConnection().isOpen()) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        if (mFabOnOffToggle) {
                            mWebSocket.send(mOffJson.toString());
                        } else {
                            mWebSocket.send(mOnJson.toString());
                        }
                    }
                }).start();

                break;
        }
    }
}
