package media.mit.edu.macontrol;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SceneGalleryActivity extends BaseActivity implements DownloadFileTask.AsyncDownloadResponse, View.OnTouchListener {

    private static final String TAG = SceneGalleryActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private GridLayoutManager mLayoutManager;
    private SceneAdapter mAdapter;


    private ScaleGestureDetector mScaleDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_scene_gallery);
        super.onCreate(savedInstanceState);

        mScaleDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                Log.d(TAG, "scale: " + scaleFactor);
                int spanCount = mLayoutManager.getSpanCount();
                if (0.67 <= scaleFactor && scaleFactor < 1.0) {
                    spanCount++;
                } else if (0.33 <= scaleFactor && scaleFactor < 0.67) {
                    spanCount += 2;
                } else if (1.0 < scaleFactor && scaleFactor <= 3.0) {
                    spanCount--;
                } else if (3.0 < scaleFactor) {
                    spanCount -= 2;
                }

                if (spanCount <= 0) {
                    spanCount = 1;
                }
                mLayoutManager.setSpanCount(spanCount);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.invalidate();
                super.onScaleEnd(detector);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setOnTouchListener(this);
        mLayoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SceneAdapter();
        mRecyclerView.setAdapter(mAdapter);

        connectWebSocket();

        new DownloadFileTask(new DownloadFileTask.AsyncDownloadResponse() {
            @Override
            public void publishFinish(String response) {
                try {
                    JSONArray sceneSpace = new JSONArray(response);
                    Log.d(TAG, sceneSpace.toString());
                    for (int i = 0; i < sceneSpace.length(); i++) {
                        JSONObject scene = sceneSpace.getJSONObject(i);
                        new DownloadFileTask(SceneGalleryActivity.this).execute(BASE_URL + scene.getString("file_name"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).execute(BASE_URL + "scene_space.json");
    }

    @Override
    public void publishFinish(String response) {
        try {
            JSONObject scene = new JSONObject(response);
            mAdapter.add(scene);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == mRecyclerView.getId()) {
            mScaleDetector.onTouchEvent(event);
        }
        return false;
    }


    public class SceneAdapter extends RecyclerView.Adapter<SceneViewHolder> implements SceneViewHolder.ItemClickListener {

        // Underlying dataset
        private List<JSONObject> mDataset;

        @Override
        public void onItemClick(View view, int position) {
            Log.d(TAG, "onItemClick: " + position);
            JSONObject scene = mDataset.get(position);
            try {
                final int sceneId = scene.getInt("scene_id");
                final String sceneName = scene.getString("descriptive_name");
                Snackbar.make(mCoordinatorLayout, "Changing to scene [" + sceneId + "] " + sceneName, Snackbar.LENGTH_SHORT).show();
                if (mWebSocket.getConnection().isOpen()) {
                    mWebSocket.send(buildSceneJson(sceneId, sceneName));
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!mWebSocket.getConnection().isOpen()) {
                                try {
                                    Log.d(TAG, "sleep");
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            mWebSocket.send(buildSceneJson(sceneId, sceneName));
                        }
                    }).start();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SceneAdapter() {
            mDataset = new ArrayList<>(0);
        }

        @Override
        public SceneViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SceneViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.widget_scene_gallery, parent, false), this);
        }

        @Override
        public void onBindViewHolder(final SceneViewHolder holder, int position) {
            String sceneName;
            String sceneImagePath;
            try {
                sceneName = mDataset.get(position).getString("descriptive_name");
                sceneImagePath = BASE_URL + mDataset.get(position).getJSONObject("feature").getString("rep_frame_path");
                Log.d(TAG, sceneImagePath);
                holder.mSceneNameTextView.setText(sceneName);
                Picasso.with(getApplicationContext()).load(sceneImagePath).into(holder.mSceneImageView);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        @Override
        public int getItemCount() {
            return (mDataset == null) ? 0 : mDataset.size();
        }

        /**
         * Adds element at the specified position and
         * notifies the {@link SceneAdapter} that the underlying list has changed.
         *
         * @param position Insert position
         * @param element  Sensor element as {@link Bundle}
         */
        public void addAt(int position, JSONObject element) {
            if (!mDataset.contains(element)) {
                mDataset.add(position, element);
                notifyItemInserted(position);
                notifyItemRangeChanged(position, mDataset.size() - position - 1);
            }
        }

        /**
         * Adds element to the end of the list and
         * notifies the {@link SceneAdapter} that the underlying list has changed.
         *
         * @param element Sensor element as {@link Bundle}
         */
        public void add(JSONObject element) {
            addAt(mDataset.size(), element);
        }


        /**
         * Removes element at the specified position and
         * notifies the {@link SceneAdapter} that the underlying list has changed.
         *
         * @param position Position to remove
         */
        public void removeAt(int position) {
            mDataset.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, mDataset.size() - position);
        }

        /**
         * Removes element of the end of the list and
         * notifies the {@link SceneAdapter} that the underlying list has changed.
         */
        public void remove() {
            removeAt(mDataset.size() - 1);
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

    public static class SceneViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView mSceneNameTextView;
        private ImageView mSceneImageView;
        private ItemClickListener mItemClickListener;

        SceneViewHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            mSceneNameTextView = (TextView) itemView.findViewById(R.id.tv_scene);
            mSceneImageView = (ImageView) itemView.findViewById(R.id.iv_scene);
            mItemClickListener = listener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mItemClickListener.onItemClick(v, getAdapterPosition());
        }

        interface ItemClickListener {
            void onItemClick(View view, int position);
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
    }
}
