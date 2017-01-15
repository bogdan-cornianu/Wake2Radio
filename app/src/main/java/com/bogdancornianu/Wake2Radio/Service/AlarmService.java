package com.bogdancornianu.Wake2Radio.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.bogdancornianu.Wake2Radio.AlarmDetailActivity;
import com.bogdancornianu.Wake2Radio.WakeupActivity;

import java.util.Calendar;

public class AlarmService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        Boolean repeating = Boolean.parseBoolean(settings.getString("repeatEveryday", "false"));
        long nextAlarmTime = Long.parseLong(settings.getString("nextAlarm", "0"));
        long timeNow = Calendar.getInstance().getTimeInMillis();

        Log.d("wake2radio_Service", "service on command");

        if (nextAlarmTime > timeNow) {
            Intent wakeupIntent = new Intent(this, WakeupActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, AlarmDetailActivity.PENDING_INTENT_ID, wakeupIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (repeating) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, nextAlarmTime, AlarmManager.INTERVAL_DAY, pendingIntent);
                Log.d("wake2radio_service", "alarm set repeating");
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmTime, pendingIntent);
                Log.d("wake2radio_service", "alarm set");
            }
        }

        return START_NOT_STICKY;
    }
}
