package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import com.bogdancornianu.Wake2Radio.database.Database;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TimePicker timePicker;
    private Alarm defaultAlarm;
    private EditText radioUrl;
    private CheckBox repeatEveryday;
    private TextView nextAlarmTxt;
    private Button alarmBtn;
    private CheckBox ringtoneChk;
    private Button radioListBtn;
    private Color alarmBtnBackgroundColor;
    private SeekBar volumeSeek;
    private TextView alarmVolumeText;
    private boolean isAlarmSet = false;
    public static final int PENDING_INTENT_ID = 17031988;
    private static final int PICK_RINGTONE_REQUEST = 9991;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Database.init(getApplicationContext());
        try {
            setDefaultAlarm(Database.getAll().get(0));
        } catch (Exception e) {
            setDefaultAlarm(new Alarm());
        }

        radioUrl = (EditText)findViewById(R.id.radioUrlTxt);
        alarmBtn = (Button) findViewById(R.id.setAlarmBtn);
        radioListBtn = (Button) findViewById(R.id.radioListBtn);
        repeatEveryday = (CheckBox) findViewById(R.id.repeatAlarmChk);
        nextAlarmTxt = (TextView) findViewById(R.id.nextAlarmTxt);
        ringtoneChk = (CheckBox) findViewById(R.id.ringtoneChk);
        timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        volumeSeek = (SeekBar) findViewById(R.id.volumeSeek);
        volumeSeek.setMax(100);
        alarmVolumeText = (TextView) findViewById(R.id.alarmVolume);

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
                    cancelAlarm(true, true);
                    alarmBtn.setBackgroundResource(android.R.drawable.btn_default);
                } else {
                    timePicker.clearFocus();
                    defaultAlarm.setRadioUrl(radioUrl.getText().toString());
                    defaultAlarm.setAlarmTime(getCalendarFromTimePicker(timePicker));
//                    saveSetting("alarmHour", String.valueOf(timePicker.getCurrentHour()));
//                    saveSetting("alarmMinute", String.valueOf(timePicker.getCurrentMinute()));
//                    saveSetting("repeatEveryday", String.valueOf(repeatEveryday.isChecked()));

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
                    cancelAlarm(false, true);
                }
            }
        });

        repeatEveryday.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    nextAlarmTxt.setText("Everyday");
                }
                if (isAlarmSet) {
                    cancelAlarm(false, true);
                    if (isChecked) {
                        buttonView.setChecked(true);
                    }
                }
            }
        });

        ringtoneChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent pickRingtoneIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    pickRingtoneIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone");
                    pickRingtoneIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                    pickRingtoneIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    pickRingtoneIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);

                    startActivityForResult(pickRingtoneIntent, PICK_RINGTONE_REQUEST);
                } else {
//                    saveSetting("ringtoneUri", null);
                    defaultAlarm.setRingtoneUrl(null);
                }
            }
        });

        radioUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isAlarmSet) {
                    cancelAlarm(false, true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        radioListBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), RadioActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alarmVolumeText.setText(progress + "%");
//                saveSetting("alarmVolume", String.valueOf(progress));
                defaultAlarm.setAlarmVolume(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

//    private void setDefaultTestData() {
//        defaultAlarm.setEnableRingtone(false);
//        defaultAlarm.setAlarmVolume(50);
//        defaultAlarm.setRingtoneUrl("");
//        defaultAlarm.setRadioUrl("");
//        defaultAlarm.setAlarmName("DefaultAlarm");
//        defaultAlarm.setAlarmActive(true);
//
//        Calendar newAlarmTime = Calendar.getInstance();
//        newAlarmTime.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
//        newAlarmTime.set(Calendar.MINUTE, timePicker.getCurrentMinute());
//        newAlarmTime.set(Calendar.SECOND, 0);
//        defaultAlarm.setAlarmTime(newAlarmTime);
//
//        defaultAlarm.addDay(Alarm.Day.values()[0]);
//    }

    private Alarm getDefaultAlarm() {
        return defaultAlarm;
    }

    public void setDefaultAlarm(Alarm alarm) {
        this.defaultAlarm = alarm;
    }

    private void saveAlarm() {
        if (getDefaultAlarm().getId() < 1) {
            Database.create(getDefaultAlarm());
        } else {
            Database.update(getDefaultAlarm());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("alarm", getDefaultAlarm());
    };

    @Override
    public void onPause() {
        super.onPause();
//        saveSetting("ringtoneChk", String.valueOf(ringtoneChk.isChecked()));
        defaultAlarm.setEnableRingtone(ringtoneChk.isChecked());
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_RINGTONE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
//                saveSetting("ringtoneUri", ringtoneUri.toString());
                defaultAlarm.setRingtoneUrl(ringtoneUri.toString());
            } else {
                ringtoneChk.setChecked(false);
//                saveSetting("ringtoneUri", null);
                defaultAlarm.setRingtoneUrl(null);
                defaultAlarm.setEnableRingtone(false);
            }
        } else if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                radioUrl.setText(data.getStringExtra("radioUrl"));
            }
        }
    }

    private void cancelAlarm(boolean resetTimePicker, boolean showToast) {
        Intent intent = new Intent(this, WakeupActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, PENDING_INTENT_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        pendingIntent.cancel();
        alarmManager.cancel(pendingIntent);

        alarmBtn.setText(R.string.alarm_text);
        alarmBtn.setBackgroundResource(android.R.drawable.btn_default);
        nextAlarmTxt.setText(R.string.no_alarm);
        repeatEveryday.setChecked(false);

        isAlarmSet = false;

        if (resetTimePicker) {
            timePicker.setCurrentHour(0);
            timePicker.setCurrentMinute(0);
        }

//        saveSetting("alarmHour", String.valueOf(timePicker.getCurrentHour()));
//        saveSetting("alarmMinute", String.valueOf(timePicker.getCurrentMinute()));
//        saveSetting("repeatEveryday", String.valueOf(repeatEveryday.isChecked()));
//        saveSetting("repeatEveryday", String.valueOf(repeatEveryday.isChecked()));
//        saveSetting("isAlarmSet", String.valueOf(isAlarmSet));
        defaultAlarm.setAlarmActive(isAlarmSet);
        defaultAlarm.setAlarmTime(getCalendarFromTimePicker(timePicker));
        saveSetting("nextAlarm", "0");

        if (showToast) {
            Toast.makeText(MainActivity.this, "Alarm canceled", Toast.LENGTH_SHORT).show();
        }
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
        saveAlarm();
    }

    private void setNextAlarm(long nextAlarmTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE HH:mm", Locale.US);
        String alarmText = simpleDateFormat.format(nextAlarmTime);

        if (repeatEveryday.isChecked()) {
            nextAlarmTxt.setText("Everyday");
        } else {
            nextAlarmTxt.setText(alarmText);
        }
        alarmBtn.setText("Cancel Alarm");
        alarmBtn.setBackgroundColor(Color.parseColor("#C20F00"));
        isAlarmSet = true;

        saveSetting("nextAlarm", String.valueOf(nextAlarmTime));
        defaultAlarm.setAlarmActive(isAlarmSet);
//        saveSetting("isAlarmSet", "true");
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
        Boolean repeating = Boolean.parseBoolean(settings.getString("repeatEveryday", "false"));
        long nextAlarm = Long.parseLong(settings.getString("nextAlarm", "0"));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE HH:mm", Locale.US);
        String nextAlarmText = nextAlarm == 0 ? "No Alarm set." : simpleDateFormat.format(new Date(nextAlarm));

        nextAlarmTxt.setText(nextAlarmText);
//        timePicker.setCurrentHour(Integer.parseInt(settings.getString("alarmHour", "1")));
//        timePicker.setCurrentMinute(Integer.parseInt(settings.getString("alarmMinute", "1")));
        repeatEveryday.setChecked(repeating);
//        radioUrl.setText(settings.getString("streamUrl", ""));
        radioUrl.setText(defaultAlarm.getRadioUrl());
        timePicker.setCurrentHour(defaultAlarm.getAlarmTime().getTime().getHours());
        timePicker.setCurrentMinute(defaultAlarm.getAlarmTime().getTime().getMinutes());
//        isAlarmSet = Boolean.parseBoolean(settings.getString("isAlarmSet", "false"));
        isAlarmSet = defaultAlarm.getAlarmActive();
        ringtoneChk.setChecked(Boolean.parseBoolean(settings.getString("ringtoneChk", "false")));
        volumeSeek.setProgress(Integer.parseInt(settings.getString("alarmVolume", "50")));
        alarmVolumeText.setText(volumeSeek.getProgress() + "%");

        if (nextAlarmText.length() > 0) {
            Date nextAlarmTime = new Date(nextAlarm);

            if (Calendar.getInstance().getTime().getTime() > nextAlarmTime.getTime()) {
                cancelAlarm(true, false);
            } else {
                if (repeating) {
                    nextAlarmTxt.setText("Everyday");
                } else {
                    nextAlarmTxt.setText(nextAlarmText);
                }
            }
        }
    }
}
