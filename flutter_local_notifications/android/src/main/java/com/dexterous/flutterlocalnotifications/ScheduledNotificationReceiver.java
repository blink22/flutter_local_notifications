package com.dexterous.flutterlocalnotifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;

import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/** Created by michaelbui on 24/3/18. */
@Keep
public class ScheduledNotificationReceiver extends BroadcastReceiver {
  String beforeFajrOptionKey = "beforeFajrOption";

  @Override
  public void onReceive(final Context context, Intent intent) {
    String notificationDetailsJson =
        intent.getStringExtra(FlutterLocalNotificationsPlugin.NOTIFICATION_DETAILS);
    if (StringUtils.isNullOrEmpty(notificationDetailsJson)) {
      // This logic is needed for apps that used the plugin prior to 0.3.4
      Notification notification = intent.getParcelableExtra("notification");
      notification.when = System.currentTimeMillis();
      String callStatus = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
      if (callStatus != null && callStatus.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
        // in a call, disable sound
        notification.sound = null;
      }
      else if (notification.sound != null) {
        playSound(notification, context, intent);
        notification.sound = null;
      }
      int notificationId = intent.getIntExtra("notification_id", 0);
      NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
      notificationManager.notify(notificationId, notification);
      boolean repeat = intent.getBooleanExtra("repeat", false);
      if (!repeat) {
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(context, notificationId);
      }
    } else {
      Gson gson = FlutterLocalNotificationsPlugin.buildGson();
      Type type = new TypeToken<NotificationDetails>() {}.getType();
      NotificationDetails notificationDetails = gson.fromJson(notificationDetailsJson, type);
      String callStatus = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
      if (callStatus != null && callStatus.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
        // in a call, disable sound
        notificationDetails.channelId = "default";
        notificationDetails.playSound = false;
      }
      else if (notificationDetails.playSound) {
        playSound(notificationDetails, context, intent);
        notificationDetails.channelId = "default";
        notificationDetails.playSound = false;
      }
      FlutterLocalNotificationsPlugin.showNotification(context, notificationDetails);
      if (notificationDetails.scheduledNotificationRepeatFrequency != null) {
        FlutterLocalNotificationsPlugin.zonedScheduleNextNotification(context, notificationDetails);
      } else if (notificationDetails.matchDateTimeComponents != null) {
        FlutterLocalNotificationsPlugin.zonedScheduleNextNotificationMatchingDateComponents(
            context, notificationDetails);
      } else if (notificationDetails.repeatInterval != null) {
        FlutterLocalNotificationsPlugin.scheduleNextRepeatingNotification(
            context, notificationDetails);
      } else {
        FlutterLocalNotificationsPlugin.removeNotificationFromCache(
            context, notificationDetails.id);
      }
    }
  }

  private void playSound(Notification notification, Context context, Intent intent) {
    AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    int volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
    audioManager.setStreamVolume(
            AudioManager.STREAM_RING,
            volume,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
    );

    Intent startIntent = new Intent(context, RingtonePlayingService.class);
    startIntent.putExtra("ringtone-uri", notification.sound);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (notification.getChannelId().equals(beforeFajrOptionKey)) {
        startIntent.putExtra("mindVolume", true);
      }
    }
    context.startService(startIntent);
  }

  private void playSound(NotificationDetails notificationDetails, Context context, Intent intent) {
    AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
    int volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
    audioManager.setStreamVolume(
            AudioManager.STREAM_RING,
            volume,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
    );

    Uri uri = FlutterLocalNotificationsPlugin.retrieveSoundResourceUri(
                    context, notificationDetails.sound, notificationDetails.soundSource);
    Intent startIntent = new Intent(context, RingtonePlayingService.class);
    startIntent.putExtra("ringtone-uri", uri);
    if (notificationDetails.channelId.equals(beforeFajrOptionKey)) {
      startIntent.putExtra("mindVolume", true);
    }
    context.startService(startIntent);
  }
}
