package com.example.marek.healthmonitor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;


public class MainActivity extends ActionBarActivity {
    ArrayList<JSONObject> metrics = new ArrayList<JSONObject>();
    FrameLayout errorLayout;
    GridLayout buttonsLayout;
    TextView errorText;
    TextView bodyPartText;
    int metricIndex = 0;
    int userId = -1;

    private class UnauthorizedException extends Exception {}

    public class UploadTask extends AsyncTask<JSONObject, Void, String> {

        private Exception exception = null;

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
                if(response.getStatusLine().getStatusCode() == 200)
                    return null;
                else if (response.getStatusLine().getStatusCode() == 403)
                    throw new UnauthorizedException();
            } catch (JSONException|IOException|UnauthorizedException ex) {
                this.exception = ex;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (this.exception == null) {
                Toast.makeText(getApplicationContext(), "Thanks for responding!", Toast.LENGTH_SHORT).show();
                metricIndex ++;
                showNextMetric();
            } else {
                Log.e("PostData", this.exception.getMessage());
                if (this.exception.getClass().equals(UnauthorizedException.class)) {
                    Toast.makeText(getApplicationContext(), "Unauthorized - Please check your username and password settings!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Error uploading data!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        private Exception exception = null;

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
                errorLayout.setVisibility(View.VISIBLE);
                errorText.setText("Internet connection required :(");
                this.exception = ex;
                return null;
            } catch (FileNotFoundException ex) {
                errorLayout.setVisibility(View.VISIBLE);
                if (response_code == 403)
                    errorText.setText("Failed to log into server.\nPlease verify your username and password.");
                else
                    errorText.setText("Weird error: server responded with " + response_code);

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
                    errorLayout.setVisibility(View.INVISIBLE);
                    JSONObject jObject = new JSONObject(result);
                    userId = jObject.getInt("user");
                    JSONArray array = jObject.getJSONArray("metrics");
                    for (int i = 0; i < array.length(); i++) {
                        metrics.add(array.getJSONObject(i));
                    }
                    showNextMetric();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getCurrentMetricName() {
        try {
            return metrics.get(metricIndex).getString("name");
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void postData(View view) {
        Log.i("PostData", "starting...");
        try {
            String tag = view.getTag().toString();
            String metric_name = getCurrentMetricName();

            UploadTask task = new UploadTask();

            JSONObject data = new JSONObject();
            JSONObject sessionData = new JSONObject();
            JSONObject valuesData = new JSONObject();
            sessionData.put("name", getPrefs("username"));
            sessionData.put("password", getPrefs("password"));
            valuesData.put(metric_name, tag);
            data.put("session", sessionData);
            data.put("values", valuesData);

            task.execute(data).get();
        } catch(InterruptedException|ExecutionException|JSONException e) {
            e.printStackTrace();
        }
    }

    private String getPrefs(String key) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        errorLayout = (FrameLayout) findViewById(R.id.errorLayout);
        buttonsLayout = (GridLayout) findViewById(R.id.buttonsLayout);
        errorText = (TextView) findViewById(R.id.errorText);
        bodyPartText = (TextView) findViewById(R.id.bodyPartText);

        downloadMetrics();
    }

    private void downloadMetrics() {
        DownloadTask task = new DownloadTask();
        try {
            task.execute(getPrefs("username"), getPrefs("password")).get();
        } catch(InterruptedException|ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void showNextMetric() {
        if (metricIndex >= metrics.size()) {
            Toast.makeText(this, "That's all, thanks!", Toast.LENGTH_LONG).show();
            return;
        }

        buttonsLayout.setVisibility(View.VISIBLE);
        buttonsLayout.bringToFront();

        bodyPartText.setText(getCurrentMetricName());
    }

    public void retryDownload(View view) {
        downloadMetrics();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // TODO sprawdzić jak to działa

        //noinspection SimplifiableIfStatement
        if (id == R.id.settings) {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
