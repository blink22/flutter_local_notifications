package com.dexterous.flutterlocalnotifications;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

@Keep
public class RingtonePlayingService extends Service {
    private Ringtone ringtone;
    private Timer timer;
    private AudioManager audioManager;
    private int maxVolume;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri ringtoneUri = (Uri) intent.getExtras().get("ringtone-uri");
        boolean mindVolume = intent.getBooleanExtra("mindVolume", false);

        audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(true);
        }
        ringtone.play();

        if (mindVolume) {
            mindVolume();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void mindVolume() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            public void run() {
                //perform your action here
                int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (volume*1.0f/maxVolume < 0.25f) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(maxVolume*0.25f),0);
                }
            }
        };
        timer.schedule(timerTask, 0, 1000);
    }

    @Override
    public void onDestroy() {
        if (ringtone != null) {
            ringtone.stop();
        }
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
}
