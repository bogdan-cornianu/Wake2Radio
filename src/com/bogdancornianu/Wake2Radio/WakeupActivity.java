package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;

public class WakeupActivity extends Activity {
    private MediaPlayer player;
    private ProgressBar bufferBar;
    private String radioUrl = "";
    private TextView radioUrlTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("onCreate", "Waking up...");

        final Window window = getWindow();
        window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.wakeup);

        Button dismissBtn = (Button) findViewById(R.id.dismissBtn);
        Activity currentActivity = this;
        dismissBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopPlaying();
                currentActivity.finish();
            }
        });

        SharedPreferences settings = getSharedPreferences("Wake2RadioPrefs", Context.MODE_PRIVATE);
        radioUrl = settings.getString("streamUrl", "Stream not found.");
        radioUrlTxt = (TextView) findViewById(R.id.textView2);

        Log.d("Reading stream URL", radioUrl);
        radioUrlTxt.setText(radioUrl);

        bufferBar = (ProgressBar) findViewById(R.id.progressBar);

        initializeMediaPlayer();
        startPlaying();
    }

    private void initializeMediaPlayer() {
        if (!isNetworkAvailable(getApplicationContext())) {
            bufferBar.setVisibility(View.GONE);
            radioUrlTxt.setText("No network connection available.");
            Log.d("initializeMediaPlayer", "No network connection available.");
            return;
        }
        player = new MediaPlayer();
        try {
            player.setDataSource(radioUrl);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Player start", "I/O error when playing stream.");
        }
    }

    private void startPlaying() {
        if (player != null) {
            player.prepareAsync();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    player.setOnBufferingUpdateListener(onBufferingUpdateListener);
                    player.setOnErrorListener(onErrorListener);
                    player.start();
                }
            });
        }
    }

    private void stopPlaying() {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.release();
        }
    }

    private void restartMediaStream() {
        Log.d("restartMediaStream", "Restarting stream.");
        player.release();
        initializeMediaPlayer();
        startPlaying();
    }

    MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
            if (!player.isPlaying()) {
                bufferBar.setVisibility(View.VISIBLE);
                if (!isNetworkAvailable(getApplicationContext())) {
                    Log.d("OnBufferingUpdate", "Attempting to restart media stream.");
                    restartMediaStream();
                }
            } else {
                bufferBar.setVisibility(View.GONE);
            }
        }
    };

    MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
            Log.e("Media Player", String.valueOf(what));
            return false;
        }
    };

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
