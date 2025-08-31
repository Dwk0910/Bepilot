package org.dslofficial.bepilot;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import android.util.Log;

public class Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_ON -> {
                ComponentName comp = new ComponentName(context, TurnOffNotifyJobService.class);
                JobInfo jobInfo = new JobInfo.Builder(1, comp)
                        .setMinimumLatency(600_000)
                        .setOverrideDeadline(600_000)
                        .build();

                JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                scheduler.schedule(jobInfo);
            }

            case Intent.ACTION_SCREEN_OFF -> {
                JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                scheduler.cancel(1);
            }

            case Intent.ACTION_HEADSET_PLUG -> {
                if (MainActivity.e_warning_earphone) {
                    MainActivity.sendNotification(context, "이어폰이 연결되었습니다.", "이어폰은 청력을 손상시킬 가능성이 존재합니다. 주의하여 사용하십시오.");
                    MainActivity.e_warning_earphone = true;
                }
            }
        }

        try {
            // Other events
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
            if (device != null && device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC &&
                    (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET || device.getBluetoothClass().getDeviceClass() == BluetoothClass.PROFILE_A2DP)
                    && MainActivity.e_warning_earphone) {
                MainActivity.sendNotification(context, "블루투스 이어폰이 연결되었습니다.", "블루투스 이어폰은 청력을 손상시킬 가능성이 존재합니다. 주의하여 사용하십시오.");
                MainActivity.is_earphone_connected = true;
            }
        } catch (SecurityException e) {
            Log.d("Receiver", "Bluetooth permission not granted." + e);
        }
    }
}
