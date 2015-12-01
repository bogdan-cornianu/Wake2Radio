package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.util.Calendar;

public class MainActivity extends Activity {
    private TimePicker timePicker;
    private EditText radioUrl;
    private CheckBox repeatEveryday;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        radioUrl = (EditText)findViewById(R.id.radioUrlTxt);
        repeatEveryday = (CheckBox) findViewById(R.id.repeatAlarmChk);
        timePicker = (TimePicker) findViewById(R.id.timePicker);

        loadRadioUrl();

        Button alarmBtn = (Button) findViewById(R.id.setAlarmBtn);
        alarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
                calendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                Log.d("Set stream url to", radioUrl.getText().toString());
                saveSettings();

                setAlarm(calendar.getTimeInMillis(), repeatEveryday.isChecked());
            }
        });
    }

    private void setAlarm(long alarmTime, boolean repeating) {
        Intent intent = new Intent(this, WakeupActivity.class);
        PendingIntent sender = PendingIntent.getActivity(
                this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (repeating) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime, AlarmManager.INTERVAL_DAY, sender);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);
        }
        Log.d("setAlarm", "Alarm set for " + alarmTime);
    }

    private void saveSettings() {
        SharedPreferences settings = getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString("streamUrl", radioUrl.getText().toString());
        editor.putString("alarmHour", String.valueOf(timePicker.getCurrentHour()));
        editor.putString("alarmMinute", String.valueOf(timePicker.getCurrentMinute()));
        editor.putString("repeatEveryday", String.valueOf(repeatEveryday));
        editor.apply();
    }

    private void loadRadioUrl() {
        SharedPreferences settings = getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);

        timePicker.setCurrentHour(Integer.parseInt(settings.getString("alarmHour", "1")));
        timePicker.setCurrentMinute(Integer.parseInt(settings.getString("alarmMinute", "1")));
        repeatEveryday.setChecked(Boolean.parseBoolean(settings.getString("repeatEveryday", "false")));
        radioUrl.setText(settings.getString("streamUrl", ""));
    }
}
