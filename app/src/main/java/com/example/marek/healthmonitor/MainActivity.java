package com.example.marek.healthmonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    ArrayList<JSONObject> metrics = new ArrayList<JSONObject>();
    FrameLayout errorLayout;
    GridLayout buttonsLayout;
    RelativeLayout welcomeLayout;
    TextView errorText;
    TextView bodyPartText;
    Button startButton;
    int metricIndex = 0;
    int userId = -1;

    private String getCurrentMetricName() {
        try {
            return metrics.get(metricIndex).getString("name");
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void downloadMetrics() {
        DownloadTask task = new DownloadTask(this);
        task.execute(getPrefs("username"), getPrefs("password"));
    }

    public void showNextMetric(View view) {
        if (metricIndex >= metrics.size()) {
            buttonsLayout.animate().translationX(-500f).setDuration(300);
            welcomeLayout.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.HelloMessage)).setText("That's all, thanks!");

            Runnable endTask = new Runnable() {
                public void run() {
                    finishAffinity();
                }
            };

            ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(); //delay
            worker.schedule(endTask, 3, TimeUnit.SECONDS);

            return;
        }

        startButton.setVisibility(View.GONE);
        buttonsLayout.setVisibility(View.VISIBLE);
        buttonsLayout.bringToFront();

        if (metricIndex != 0) {
            buttonsLayout.animate()
                    .translationX(-500f).setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            bodyPartText.setText(getCurrentMetricName());
                            buttonsLayout.setX(500f);
                            buttonsLayout.animate()
                                    .translationX(0f).setDuration(200);
                        }
                    });
        }
    }

    public void retryDownload(View view) {
        errorLayout.setVisibility(View.INVISIBLE);
        welcomeLayout.setVisibility(View.VISIBLE);

        downloadMetrics();
    }

    public void postData(View view) {
        Log.i("PostData", "starting...");
        try {
            String tag = view.getTag().toString();
            String metric_name = getCurrentMetricName();

            UploadTask task = new UploadTask(this);

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

    public void metricsReady() {
        startButton.setVisibility(View.VISIBLE);
        findViewById(R.id.spinner).setVisibility(View.GONE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        errorLayout = (FrameLayout) findViewById(R.id.errorLayout);
        buttonsLayout = (GridLayout) findViewById(R.id.buttonsLayout);
        welcomeLayout = (RelativeLayout) findViewById(R.id.welcomeLayout);
        errorText = (TextView) findViewById(R.id.errorText);
        bodyPartText = (TextView) findViewById(R.id.bodyPartText);
        startButton = (Button) findViewById(R.id.startButton);

        welcomeLayout.bringToFront();
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

    public void showError(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                errorLayout.setVisibility(View.VISIBLE);
                welcomeLayout.setVisibility(View.INVISIBLE);
                buttonsLayout.setVisibility(View.INVISIBLE);
                errorText.setText(text);
            }
        });
    }

    private String getPrefs(String key) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, "");
    }
}
