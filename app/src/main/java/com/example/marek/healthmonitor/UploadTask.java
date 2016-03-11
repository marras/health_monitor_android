package com.example.marek.healthmonitor;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by marek on 11/03/16.
 */
public class UploadTask extends AsyncTask<JSONObject, Void, String> {

    private final MainActivity main;
    private Exception exception = null;

    private class UnauthorizedException extends Exception {}

    public UploadTask(MainActivity activity) { this.main = activity; }

    @Override
    protected String doInBackground(JSONObject... params) {
        try {
            String postMessage = params[0].toString();
            Log.i("PostData", "Post message: '" + postMessage + "'");

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority("health-monit.herokuapp.com")
                    .appendPath("data")
                    .appendQueryParameter("session[name]", params[0].getJSONObject("session").getString("name"))
                    .appendQueryParameter("session[password]", params[0].getJSONObject("session").getString("password"));
            String url = builder.build().toString();
            Log.i("URL:", url);

            HttpPost post = new HttpPost(url);
            post.setHeader(HTTP.CONTENT_TYPE, "application/json; charset=UTF-8");
            post.setEntity(new ByteArrayEntity(postMessage.getBytes("UTF8")));

            Log.i("PostData", "Executing request...");
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(post);
            Log.i("PostData - Response", response.getStatusLine().toString());
            if (response.getStatusLine().getStatusCode() == 200)
                return null;
            else if (response.getStatusLine().getStatusCode() == 403)
                throw new UnauthorizedException();
        } catch (JSONException | IOException | UnauthorizedException ex) {
            this.exception = ex;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (this.exception == null) {
            Toast.makeText(main.getApplicationContext(), "Thanks for responding!", Toast.LENGTH_SHORT).show();
            main.metricIndex++;
            main.showNextMetric(null);
        } else {
            Log.e("PostData", this.exception.getMessage());
            if (this.exception.getClass().equals(UnauthorizedException.class)) {
                Toast.makeText(main.getApplicationContext(), "Unauthorized - Please check your username and password settings!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(main.getApplicationContext(), "Error uploading data!", Toast.LENGTH_LONG).show();
            }
        }
    }
}