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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends Activity {
    private TimePicker timePicker;
    private EditText radioUrl;
    private CheckBox repeatEveryday;
    private TextView nextAlarmTxt;
    private static final String NEXT_ALARM_TEXT = "Next Alarm: ";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        radioUrl = (EditText)findViewById(R.id.radioUrlTxt);
        repeatEveryday = (CheckBox) findViewById(R.id.repeatAlarmChk);
        nextAlarmTxt = (TextView) findViewById(R.id.nextAlarmTxt);
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);

        try {
            loadSettings();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Button alarmBtn = (Button) findViewById(R.id.setAlarmBtn);
        alarmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Set stream url to", radioUrl.getText().toString());
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
        });
    }

    private void setAlarm(long alarmTime, boolean repeating) {
        if (radioUrl.getText().length() == 0) {
            Toast.makeText(MainActivity.this, "Enter a radio URL", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, WakeupActivity.class);
        PendingIntent sender = PendingIntent.getActivity(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (repeating) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime, AlarmManager.INTERVAL_DAY, sender);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, sender);
        }

        setNextAlarm(alarmTime, repeating);
        Toast.makeText(MainActivity.this, "Alarm set", Toast.LENGTH_SHORT).show();
        Log.d("setAlarm", "Alarm set for " + alarmTime);
    }

    private void setNextAlarm(long nextAlarmTime, boolean repeating) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE HH:mm");
        SimpleDateFormat simpleDateFormatRepeating = new SimpleDateFormat("HH:mm");
        String alarmText = repeating ? NEXT_ALARM_TEXT + "Everyday " + simpleDateFormatRepeating.format(nextAlarmTime) : NEXT_ALARM_TEXT + simpleDateFormat.format(nextAlarmTime);

        nextAlarmTxt.setText(alarmText);

        saveSetting("nextAlarm", simpleDateFormat.format(nextAlarmTime));
    }

    private void saveSetting(String key, String value) {
        SharedPreferences settings = getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(key, value);
        editor.apply();
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
        SharedPreferences settings = getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE HH:mm");
        SimpleDateFormat simpleDateFormatRepeating = new SimpleDateFormat("HH:mm");
        Boolean repeating = Boolean.parseBoolean(settings.getString("repeatEveryday", "false"));
        String nextAlarm = settings.getString("nextAlarm", "");
        long timeNow = System.currentTimeMillis();
        long nextAlarmTime = repeating ? simpleDateFormatRepeating.parse(nextAlarm).getTime() : simpleDateFormat.parse(nextAlarm).getTime();

        timePicker.setCurrentHour(Integer.parseInt(settings.getString("alarmHour", "1")));
        timePicker.setCurrentMinute(Integer.parseInt(settings.getString("alarmMinute", "1")));
        repeatEveryday.setChecked(repeating);
        radioUrl.setText(settings.getString("streamUrl", ""));

        if (nextAlarmTime < timeNow) {
            nextAlarmTxt.setText(NEXT_ALARM_TEXT + "No alarm set.");
        } else {
            nextAlarmTxt.setText(NEXT_ALARM_TEXT + nextAlarm);
        }
    }
}
