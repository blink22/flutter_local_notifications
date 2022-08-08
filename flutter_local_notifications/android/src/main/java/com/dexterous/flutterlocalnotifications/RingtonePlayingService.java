package com.dexterous.flutterlocalnotifications;

import android.app.Service;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

@Keep
public class RingtonePlayingService extends Service {
    private Ringtone ringtone;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri ringtoneUri = (Uri) intent.getExtras().get("ringtone-uri");

        ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone.setLooping(true);
        }
        ringtone.play();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (ringtone != null) {
            ringtone.stop();
        }
        super.onDestroy();
    }
}
