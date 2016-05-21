package com.bogdancornianu.Wake2Radio;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;
import com.google.android.exoplayer.util.Util;

public class WakeupActivity extends Activity {
    private MediaPlayer ringtonePlayer;
    private ProgressBar bufferBar;
    private String radioUrl = "";
    private TextView radioUrlTxt;
    private Integer alarmVolume;
    private ExoPlayer exoPlayer;
    private static final int BUFFER_SEGMENT_SIZE = 64 * 1024;
    private static final int BUFFER_SEGMENT_COUNT = 256;

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

        exoPlayer = ExoPlayer.Factory.newInstance(1);
        Uri radioUri = Uri.parse(radioUrl);

        Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
        String userAgent = Util.getUserAgent(getApplicationContext(), "ExoPlayerDemo");
        DataSource dataSource = new DefaultUriDataSource(getApplicationContext(), null, userAgent);
        ExtractorSampleSource sampleSource = new ExtractorSampleSource(
                radioUri, dataSource, allocator, BUFFER_SEGMENT_SIZE * BUFFER_SEGMENT_COUNT);
        MediaCodecAudioTrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource, MediaCodecSelector.DEFAULT);

        exoPlayer.prepare(audioRenderer);
        startPlaying();
    }

    private void playRingtone() {
        if (exoPlayer != null) {
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
        if (exoPlayer != null) {
            exoPlayer.addListener(new ExoPlayer.Listener() {
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    if (playbackState == ExoPlayer.STATE_READY) {
                        bufferBar.setVisibility(View.GONE);
                    } else if (playbackState == ExoPlayer.STATE_BUFFERING) {
                        bufferBar.setVisibility(View.VISIBLE);
                        if (!isNetworkAvailable(getApplicationContext())) {
                            restartMediaStream();
                        }
                    }
                }

                @Override
                public void onPlayWhenReadyCommitted() {

                }

                @Override
                public void onPlayerError(ExoPlaybackException error) {
                    Log.e("Wake2Radio-ExoPlayer", error.getMessage());
                }
            });

            exoPlayer.setPlayWhenReady(true);

            new java.util.Timer().schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    if (exoPlayer != null && exoPlayer.getPlaybackState() != ExoPlayer.STATE_READY) {
                        playRingtone();
                    }
                }
            }, 60 * 1000);
        }
    }

    private void stopPlaying() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
    }

    private void restartMediaStream() {
        exoPlayer.release();
        initializeMediaPlayer();
        startPlaying();
    }

    public boolean isNetworkAvailable(final Context context) {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
