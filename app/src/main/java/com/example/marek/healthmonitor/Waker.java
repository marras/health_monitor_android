package com.example.marek.healthmonitor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by marek on 05/03/16.
 */

// TODO We will need BOOT_COMPLETED and WAKE_LOCK permission

public class Waker extends BroadcastReceiver {
    private void createNotification(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, 0);

        Notification notification = new Notification.Builder(context)
                .setContentTitle("Fill in your questionaire")
                .setContentText("It's time to respond")
                .setSmallIcon(R.drawable.heart)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, notification);
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {
     //   PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
     //   PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
     //   wl.acquire();

        Log.w("Waker", "RECEIVED ALARM!");// Put here YOUR code.
        createNotification(context);

//        wl.release();
    }

    public void SetAlarm(Context context, int hour, int minute)
    {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Waker.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
        Log.i("Waker", "Alarm set.");
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, Waker.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
