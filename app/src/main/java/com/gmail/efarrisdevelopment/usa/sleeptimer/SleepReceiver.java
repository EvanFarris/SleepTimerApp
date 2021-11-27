package com.gmail.efarrisdevelopment.usa.sleeptimer;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

public class SleepReceiver extends DeviceAdminReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("goToSleep")) {
                DevicePolicyManager dpm = (DevicePolicyManager) context.getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
                dpm.lockNow();

                AudioAttributes aAttr = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
                AudioFocusRequest aRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(aAttr).build();
                ((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(aRequest);
                ((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).abandonAudioFocusRequest(aRequest);
        }

    }

}
