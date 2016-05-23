package com.bogdancornianu.Wake2Radio.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.bogdancornianu.Wake2Radio.Alarm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

public class Database extends SQLiteOpenHelper {
    static Database instance = null;
    static SQLiteDatabase database = null;

    static final String DATABASE_NAME = "Wak2Radio";
    static final int DATABASE_VERSION = 1;

    public static final String ALARM_TABLE = "alarm";
    public static final String COLUMN_ALARM_ID = "_id";
    public static final String COLUMN_ALARM_ACTIVE = "alarm_active";
    public static final String COLUMN_ALARM_TIME = "alarm_time";
    public static final String COLUMN_ALARM_DAYS = "alarm_days";
    public static final String COLUMN_ALARM_RADIO_URL = "alarm_radio_url";
    public static final String COLUMN_ALARM_RINGTONE_URL = "alarm_ringtone_url";
    public static final String COLUMN_ALARM_VOLUME = "alarm_volume";
    public static final String COLUMN_ALARM_ENABLE_RINGTONE= "alarm_enable_ringtone";
    public static final String COLUMN_ALARM_NAME = "alarm_name";

    public static void init(Context context) {
        if (null == instance) {
            instance = new Database(context);
        }
    }

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    public static SQLiteDatabase getDatabase() {
        if (null == database) {
            database = instance.getWritableDatabase();
        }
        return database;
    }

    public static void deactivate() {
        if (null != database && database.isOpen()) {
            database.close();
        }
        database = null;
        instance = null;
    }

    public static long create(Alarm alarm) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ALARM_ACTIVE, alarm.getAlarmActive());
        contentValues.put(COLUMN_ALARM_TIME, alarm.getAlarmTimeString());

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(alarm.getDays());
            byte[] buff = byteArrayOutputStream.toByteArray();

            contentValues.put(COLUMN_ALARM_DAYS, buff);

        } catch (Exception e) {
            Log.e("Database", e.getMessage());
        }

        contentValues.put(COLUMN_ALARM_RADIO_URL, alarm.getRadioUrl());
        contentValues.put(COLUMN_ALARM_RINGTONE_URL, alarm.getRingtoneUrl());
        contentValues.put(COLUMN_ALARM_VOLUME, alarm.getAlarmVolume());
        contentValues.put(COLUMN_ALARM_ENABLE_RINGTONE, alarm.getEnableRingtone());
        contentValues.put(COLUMN_ALARM_NAME, alarm.getAlarmName());

        return getDatabase().insert(ALARM_TABLE, null, contentValues);
    }

    public static int update(Alarm alarm) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_ALARM_ACTIVE, alarm.getAlarmActive());
        contentValues.put(COLUMN_ALARM_TIME, alarm.getAlarmTimeString());

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(alarm.getDays());
            byte[] buff = byteArrayOutputStream.toByteArray();

            contentValues.put(COLUMN_ALARM_DAYS, buff);

        } catch (Exception e) {
            Log.e("Database", e.getMessage());
        }

        contentValues.put(COLUMN_ALARM_RADIO_URL, alarm.getRadioUrl());
        contentValues.put(COLUMN_ALARM_RINGTONE_URL, alarm.getRingtoneUrl());
        contentValues.put(COLUMN_ALARM_VOLUME, alarm.getAlarmVolume());
        contentValues.put(COLUMN_ALARM_ENABLE_RINGTONE, alarm.getEnableRingtone());
        contentValues.put(COLUMN_ALARM_NAME, alarm.getAlarmName());

        return getDatabase().update(ALARM_TABLE, contentValues, "_id=" + alarm.getId(), null);
    }

    public static Alarm getAlarm(int id) {
        String[] columns = new String[] {
                COLUMN_ALARM_ID,
                COLUMN_ALARM_ACTIVE,
                COLUMN_ALARM_TIME,
                COLUMN_ALARM_DAYS,
                COLUMN_ALARM_RADIO_URL,
                COLUMN_ALARM_RINGTONE_URL,
                COLUMN_ALARM_VOLUME,
                COLUMN_ALARM_ENABLE_RINGTONE,
                COLUMN_ALARM_NAME
        };
        Cursor c = getDatabase().query(ALARM_TABLE, columns, COLUMN_ALARM_ID + "=" + id, null, null, null,
                null);
        Alarm alarm = null;

        if(c.moveToFirst()){

            alarm =  new Alarm();
            alarm.setId(c.getInt(1));
            alarm.setAlarmActive(c.getInt(2) == 1);
            alarm.setAlarmTime(c.getString(3));
            byte[] repeatDaysBytes = c.getBlob(4);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(repeatDaysBytes);
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                Alarm.Day[] repeatDays;
                Object object = objectInputStream.readObject();
                if(object instanceof Alarm.Day[]){
                    repeatDays = (Alarm.Day[]) object;
                    alarm.setDays(repeatDays);
                }
            } catch (StreamCorruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            alarm.setRadioUrl(c.getString(5));
            alarm.setRingtoneUrl(c.getString(6));
            alarm.setAlarmVolume(Integer.parseInt(c.getString(7)));
            alarm.setEnableRingtone(c.getInt(8) == 1);
            alarm.setAlarmName(c.getString(9));
        }
        c.close();
        return alarm;
    }

    public static Cursor getCursor() {
        String[] columns = new String[] {
                COLUMN_ALARM_ID,
                COLUMN_ALARM_ACTIVE,
                COLUMN_ALARM_TIME,
                COLUMN_ALARM_DAYS,
                COLUMN_ALARM_RADIO_URL,
                COLUMN_ALARM_RINGTONE_URL,
                COLUMN_ALARM_VOLUME,
                COLUMN_ALARM_ENABLE_RINGTONE,
                COLUMN_ALARM_NAME
        };

        return getDatabase().query(ALARM_TABLE, columns, null, null, null, null, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + ALARM_TABLE + " ( "
                + COLUMN_ALARM_ID + " INTEGER primary key autoincrement, "
                + COLUMN_ALARM_ACTIVE + " INTEGER NOT NULL, "
                + COLUMN_ALARM_TIME + " TEXT NOT NULL, "
                + COLUMN_ALARM_DAYS + " BLOB NOT NULL, "
                + COLUMN_ALARM_RADIO_URL + " TEXT NOT NULL, "
                + COLUMN_ALARM_RINGTONE_URL + " TEXT NOT NULL, "
                + COLUMN_ALARM_VOLUME + " INTEGER NOT NULL, "
                + COLUMN_ALARM_ENABLE_RINGTONE + " INTEGER NOT NULL, "
                + COLUMN_ALARM_NAME + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ALARM_TABLE);
        onCreate(db);
    }

    public static List<Alarm> getAll() {
        List<Alarm> alarms = new ArrayList<Alarm>();
        Cursor cursor = Database.getCursor();

        if (cursor.moveToFirst()) {
            do {
                Alarm alarm = new Alarm();
                alarm.setId(cursor.getInt(0));
                alarm.setAlarmActive(cursor.getInt(1) == 1);
                alarm.setAlarmTime(cursor.getString(2));
                byte[] repeatDaysBytes = cursor.getBlob(3);

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                        repeatDaysBytes);
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(
                            byteArrayInputStream);
                    Alarm.Day[] repeatDays;
                    Object object = objectInputStream.readObject();
                    if (object instanceof Alarm.Day[]) {
                        repeatDays = (Alarm.Day[]) object;
                        alarm.setDays(repeatDays);
                    }
                } catch (StreamCorruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                alarm.setRadioUrl(cursor.getString(4));
                alarm.setRingtoneUrl(cursor.getString(5));
                alarm.setAlarmVolume(cursor.getInt(6));
                alarm.setEnableRingtone(cursor.getInt(7) == 1);
                alarm.setAlarmName(cursor.getString(8));

                alarms.add(alarm);

            } while (cursor.moveToNext());
        }
        cursor.close();
        return alarms;
    }

    public static int deleteEntry(Alarm alarm){
        return deleteEntry(alarm.getId());
    }

    public static int deleteEntry(int id){
        return getDatabase().delete(ALARM_TABLE, COLUMN_ALARM_ID + "=" + id, null);
    }

    public static int deleteAll(){
        return getDatabase().delete(ALARM_TABLE, "1", null);
    }
}
