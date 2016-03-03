package com.example.marek.healthmonitor;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


public class MainActivity extends ActionBarActivity {
    HttpClient client = new DefaultHttpClient();
    HttpPost post = new HttpPost("http://health-monit.herokuapp.com/data");

    public class UploadTask extends AsyncTask<String, Void, String> {

        private Exception exception = null;

        @Override
        protected String doInBackground(String... params) {
            try {
                String postMessage = "{\"values\": { \"kupa\": " + params[0] + "} }";
                Log.i("PostData", "Post message: '"+postMessage+"'");
                post.setHeader(HTTP.CONTENT_TYPE, "application/json; charset=UTF-8");
                post.setEntity(new ByteArrayEntity(postMessage.getBytes("UTF8")));
             } catch (UnsupportedEncodingException ex) {
                this.exception = ex;
                return null;
            }

            Log.i("PostData", "Executing request...");
            try {
                HttpResponse response = client.execute(post);
            } catch (IOException ex) {
                this.exception = ex;
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("PostData", "Post-execute - fuck this shit");
            if (this.exception != null) Log.e("PostData", this.exception.getMessage());
        }
    }

    public void postData(View view) {
        Log.i("PostData", "starting...");
        UploadTask task = new UploadTask();
        try {
            task.execute(view.getTag().toString()).get();
        } catch(InterruptedException e) {
            e.printStackTrace();
        } catch(ExecutionException e) {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), "I/O failed, fuck you!", Toast.LENGTH_LONG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}