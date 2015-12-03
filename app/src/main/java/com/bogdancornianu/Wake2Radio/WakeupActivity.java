package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

public class WakeupActivity extends Activity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener {
    private MediaPlayer player;
    private ProgressBar bufferBar;
    private String radioUrl = "";
    private TextView radioUrlTxt;

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

        radioUrlTxt.setText(radioUrl);

        bufferBar = (ProgressBar) findViewById(R.id.progressBar);

        initializeMediaPlayer();
        startPlaying();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
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
            bufferBar.setVisibility(View.GONE);
            radioUrlTxt.setText("No network connection available.");
            return;
        }

        player = new MediaPlayer();
        player.setOnBufferingUpdateListener(this);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            player.setDataSource(radioUrl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
