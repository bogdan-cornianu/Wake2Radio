package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private TimePicker timePicker;
    private EditText radioUrl;
    private CheckBox repeatEveryday;
    private TextView nextAlarmTxt;
    private Button alarmBtn;
    private boolean isAlarmSet = false;
    private static final int PENDING_INTENT_ID = 17031988;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        radioUrl = (EditText)findViewById(R.id.radioUrlTxt);
        alarmBtn = (Button) findViewById(R.id.setAlarmBtn);
        repeatEveryday = (CheckBox) findViewById(R.id.repeatAlarmChk);
        nextAlarmTxt = (TextView) findViewById(R.id.nextAlarmTxt);
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        try {
            loadSettings();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        alarmBtn.setText(isAlarmSet ? "Cancel Alarm" : "Set Alarm");
        alarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAlarmSet) {
                    cancelAlarm(true);
                } else {
                    saveSetting("streamUrl", radioUrl.getText().toString());
                    saveSetting("alarmHour", String.valueOf(timePicker.getCurrentHour()));
                    saveSetting("alarmMinute", String.valueOf(timePicker.getCurrentMinute()));
                    saveSetting("repeatEveryday", String.valueOf(repeatEveryday.isChecked()));

                    Calendar calendar = getCalendarFromTimePicker(timePicker);

                    if (isAlarmBeforeCurrentTime()) {
                        calendar.add(Calendar.DAY_OF_WEEK, 1);
                    }

                    setAlarm(calendar.getTimeInMillis(), repeatEveryday.isChecked());
                }
            }
        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                if (isAlarmSet) {
                    cancelAlarm(false);
                }
            }
        });
    }

    private void cancelAlarm(boolean resetTimePicker) {
        Intent intent = new Intent(this, WakeupActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        pendingIntent.cancel();
        alarmManager.cancel(pendingIntent);

        alarmBtn.setText(R.string.AlarmText);
        nextAlarmTxt.setText(R.string.NoAlarm);
        repeatEveryday.setChecked(false);
        isAlarmSet = false;

        if (resetTimePicker) {
            timePicker.setCurrentHour(0);
            timePicker.setCurrentMinute(0);
        }

        saveSetting("alarmHour", String.valueOf(timePicker.getCurrentHour()));
        saveSetting("alarmMinute", String.valueOf(timePicker.getCurrentMinute()));
        saveSetting("repeatEveryday", String.valueOf(repeatEveryday.isChecked()));
        saveSetting("repeatEveryday", String.valueOf(repeatEveryday.isChecked()));
        saveSetting("isAlarmSet", String.valueOf(isAlarmSet));
        saveSetting("nextAlarm", null);

        Toast.makeText(MainActivity.this, "Alarm canceled", Toast.LENGTH_SHORT).show();
    }

    private void setAlarm(long alarmTime, boolean repeating) {
        if (radioUrl.getText().length() == 0) {
            Toast.makeText(MainActivity.this, "Enter a radio URL", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, WakeupActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (repeating) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime, AlarmManager.INTERVAL_DAY, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
        }

        setNextAlarm(alarmTime);
        Toast.makeText(MainActivity.this, "Alarm set", Toast.LENGTH_SHORT).show();
    }

    private void setNextAlarm(long nextAlarmTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE HH:mm", Locale.US);
        String alarmText = simpleDateFormat.format(nextAlarmTime);

        nextAlarmTxt.setText(alarmText);
        alarmBtn.setText("Cancel Alarm");
        isAlarmSet = true;

        saveSetting("nextAlarm", simpleDateFormat.format(nextAlarmTime));
        saveSetting("isAlarmSet", "true");
    }

    private void saveSetting(String key, String value) {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(key, value).apply();
    }

    private boolean isAlarmBeforeCurrentTime() {
        return getCalendarFromTimePicker(timePicker).getTimeInMillis() < System.currentTimeMillis();
    }

    private Calendar getCalendarFromTimePicker(TimePicker timePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
        calendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private void loadSettings() throws ParseException {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE HH:mm", Locale.US);
        Boolean repeating = Boolean.parseBoolean(settings.getString("repeatEveryday", "false"));
        String nextAlarm = settings.getString("nextAlarm", "No Alarm set.");

        nextAlarmTxt.setText(nextAlarm);
        timePicker.setCurrentHour(Integer.parseInt(settings.getString("alarmHour", "1")));
        timePicker.setCurrentMinute(Integer.parseInt(settings.getString("alarmMinute", "1")));
        repeatEveryday.setChecked(repeating);
        radioUrl.setText(settings.getString("streamUrl", ""));
        isAlarmSet = Boolean.parseBoolean(settings.getString("isAlarmSet", "false"));

        if (nextAlarm.length() > 0) {
            Date nextAlarmTime = simpleDateFormat.parse(nextAlarm);
            if (Calendar.getInstance().after(nextAlarmTime)) {
                nextAlarmTxt.setText(R.string.NoAlarm);
            } else {
                nextAlarmTxt.setText(nextAlarm);
            }
        }
    }
}
