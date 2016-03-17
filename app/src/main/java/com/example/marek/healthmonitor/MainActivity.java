package com.example.marek.healthmonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
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
    LinearLayout metricsLayout;
    RelativeLayout welcomeLayout;
    TextView errorText;
    TextView bodyPartText;
    GridLayout buttonsLayout;
    Button startButton, previousButton;
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

    public void showPreviousMetric(View view) {
        metricIndex--;
        showNextMetric(-1);
        if (metricIndex == 0) previousButton.setVisibility(View.INVISIBLE);
    }

    public void start(View view) {
        showNextMetric(1);
    }

    public void showNextMetric(final int dir) {
        if (metricIndex >= metrics.size()) {
            metricsLayout.animate().translationX(-500f * dir).setDuration(300);
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
        metricsLayout.setVisibility(View.VISIBLE);
        metricsLayout.bringToFront();

        try {
            final int max = metrics.get(metricIndex).getInt("max");

            metricsLayout.animate()
                    .translationX(-500f * dir).setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            regenerateMetricsView(getCurrentMetricName(), max);

                            metricsLayout.setX(500f * dir);
                            metricsLayout.animate()
                                    .translationX(0f).setDuration(200);
                            if (metricIndex > 0) previousButton.setVisibility(View.VISIBLE);
                        }
                    });
        }  catch (JSONException e) {
                e.printStackTrace();
        }
    }

    private void regenerateMetricsView(String name, int max) {
        final int COLS = 4;
        int rows = max / COLS + 1;

        Log.i("Regeneration", Integer.toString(max) + ", rows: " + Integer.toString(rows));

        buttonsLayout.removeAllViews();
        buttonsLayout.setColumnCount(COLS);
        buttonsLayout.setRowCount(rows);

        for (int num = 1; num <= max; num ++) {
            int r = (num - 1) / COLS;
            int c = (num - 1) % COLS;

            Button button = new Button(this);
            GridLayout.LayoutParams param = new GridLayout.LayoutParams();
            param.height = GridLayout.LayoutParams.WRAP_CONTENT;
            param.width = GridLayout.LayoutParams.WRAP_CONTENT;
            param.rightMargin = 2;
            param.topMargin = 2;
            param.setGravity(Gravity.CENTER);
            param.columnSpec = GridLayout.spec(c);
            param.rowSpec = GridLayout.spec(r);
            button.setLayoutParams(param);
            button.setText(Integer.toString(num));
            button.setTag(num);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    postData(v);
                }
            });
            buttonsLayout.addView(button);
        }

        bodyPartText.setText(name);
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
        metricsLayout = (LinearLayout) findViewById(R.id.metricsLayout);
        welcomeLayout = (RelativeLayout) findViewById(R.id.welcomeLayout);
        errorText = (TextView) findViewById(R.id.errorText);
        bodyPartText = (TextView) findViewById(R.id.bodyPartText);
        startButton = (Button) findViewById(R.id.startButton);
        previousButton = (Button) findViewById(R.id.previousButton);

        buttonsLayout = (GridLayout) findViewById(R.id.buttonsLayout);

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
                metricsLayout.setVisibility(View.INVISIBLE);
                errorText.setText(text);
            }
        });
    }

    private String getPrefs(String key) {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(key, "");
    }
}
