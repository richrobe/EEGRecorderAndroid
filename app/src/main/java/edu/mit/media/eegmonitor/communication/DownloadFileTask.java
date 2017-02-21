package edu.mit.media.eegmonitor.communication;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Robert on 11/17/16.
 */

public class DownloadFileTask extends AsyncTask<String, String, String> {

    private static final String TAG = DownloadFileTask.class.getSimpleName();

    public interface AsyncDownloadResponse {
        void publishFinish(String response);
    }

    private AsyncDownloadResponse response;

    public DownloadFileTask(AsyncDownloadResponse response) {
        this.response = response;
    }

    @Override
    protected String doInBackground(String... urls) {
        int count;
        try {
            URL url = new URL(urls[0]);
            URLConnection connection = url.openConnection();
            connection.connect();
            InputStream stream = new BufferedInputStream(url.openStream(), 8192);

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while ((count = stream.read(buffer)) != -1) {
                result.write(buffer, 0, count);
            }

            stream.close();
            return result.toString("UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);

        if (response != null && s != null) {
            response.publishFinish(s);
        }
    }
}
