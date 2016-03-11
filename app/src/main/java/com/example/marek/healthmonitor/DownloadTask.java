package com.example.marek.healthmonitor;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

/**
* Created by marek on 11/03/16.
*/
public class DownloadTask extends AsyncTask<String, Void, String> {

    private MainActivity main;
    private Exception exception = null;

    public DownloadTask(MainActivity main) {
        this.main = main;
    }

    // (name, password)
    @Override
    protected String doInBackground(String... params) {
        String result = "";
        URL url;
        HttpURLConnection connection = null;
        int response_code = -1;

        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("health-monit.herokuapp.com")
                    .appendPath("metrics")
                    .appendQueryParameter("session[name]", params[0])
                    .appendQueryParameter("session[password]", params[1]);
            url = new URL(builder.build().toString());

            connection = (HttpURLConnection) url.openConnection();
            response_code = connection.getResponseCode();
            InputStream is = connection.getInputStream();
            InputStreamReader reader = new InputStreamReader(is);

            int data = reader.read();

            while (data != -1) {
                char current = (char) data;
                result += current;
                data = reader.read();
            }

            Log.i("Download", "Result: '" + result + "' ");
        } catch (UnknownHostException ex) {
            main.showError("Internet connection required :(");
            this.exception = ex;
            return null;
        } catch (FileNotFoundException ex) {
            if (response_code == 403)
                main.showError("Failed to log into server.\nPlease verify your username and password.");
            else
                main.showError("Weird error: server responded with " + response_code);

            this.exception = ex;
            return null;
        } catch (IOException ex) {
            this.exception = ex;
            return null;
        }

        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (this.exception != null) {
            this.exception.printStackTrace();
        } else {
            try {
                Log.i("Download SUCCESS:", result);
                main.errorLayout.setVisibility(View.INVISIBLE);
                JSONObject jObject = new JSONObject(result);
                main.userId = jObject.getInt("user");
                JSONArray array = jObject.getJSONArray("metrics");
                for (int i = 0; i < array.length(); i++) {
                    main.metrics.add(array.getJSONObject(i));
                }
                main.metricsReady();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
