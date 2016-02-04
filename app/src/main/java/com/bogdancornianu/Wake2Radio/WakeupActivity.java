package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

public class WakeupActivity extends Activity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {
    private MediaPlayer player;
    private MediaPlayer ringtonePlayer;
    private ProgressBar bufferBar;
    private String radioUrl = "";
    private TextView radioUrlTxt;
    private Integer alarmVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.wakeup);

        Button dismissBtn = (Button) findViewById(R.id.dismissBtn);
        final Activity currentActivity = this;
        dismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopPlaying();
                currentActivity.finish();
            }
        });

        SharedPreferences settings = getApplicationContext().getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        radioUrl = settings.getString("streamUrl", "Stream not found.");
        radioUrlTxt = (TextView) findViewById(R.id.textView2);

        alarmVolume = Integer.parseInt(settings.getString("alarmVolume", "50"));

        radioUrlTxt.setText(radioUrl);

        bufferBar = (ProgressBar) findViewById(R.id.progressBar);

        initializeMediaPlayer();
    }

    @Override
    public void onDestroy() {
        stopPlaying();
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (player != null && !player.isPlaying()) {
                    playRingtone();
                }
            }
        }, 60 * 1000);
        player.start();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        if (!player.isPlaying()) {
            bufferBar.setVisibility(View.VISIBLE);
            if (!isNetworkAvailable(getApplicationContext())) {
                restartMediaStream();
            }
        } else {
            bufferBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        return false;
    }

    private void initializeMediaPlayer() {
        if (!isNetworkAvailable(getApplicationContext())) {
            playRingtone();
            bufferBar.setVisibility(View.GONE);
            radioUrlTxt.setText("No network connection available.");
            return;
        }

        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, alarmVolume / maxVolume, 0);

        player = new MediaPlayer();
        player.setOnBufferingUpdateListener(this);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        try {
            player.setDataSource(radioUrl);
            startPlaying();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playRingtone() {
        if (player != null) {
            stopPlaying();
        }
        if (ringtonePlayer == null) {
            SharedPreferences settings = getApplicationContext().getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
            String ringtoneUri = settings.getString("ringtoneUri", null);

            if (ringtoneUri != null) {
                Uri notification = Uri.parse(ringtoneUri);
                ringtonePlayer = MediaPlayer.create(getApplicationContext(), notification);
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
            }
        }
    }

    private void startPlaying() {
        if (player != null) {
            player.prepareAsync();
        }
    }

    private void stopPlaying() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
    }

    private void restartMediaStream() {
        player.release();
        initializeMediaPlayer();
        startPlaying();
    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
