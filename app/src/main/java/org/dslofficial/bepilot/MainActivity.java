package org.dslofficial.bepilot;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {
    public static volatile boolean e_warning_turnoff, e_warning_earphone, e_warning_volume;
    public static volatile boolean is_earphone_connected = false;

    public static String NOTIFY_CHANNEL_ID = "bepilot_notification_channel";
    public Button grantButton;
    public TextView statusTextView;
    public SwitchMaterial warning_turnoff, warning_earphone, warning_volume;

    public static void sendNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_bepilot)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, builder.build());
    }

    private void saveSwitchState(String key, boolean isChecked) {
        SharedPreferences prefs = getSharedPreferences("Bepilot", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, isChecked);
        editor.apply();
    }

    private void loadSwitchStates() {
        SharedPreferences prefs = getSharedPreferences("Bepilot", MODE_PRIVATE);
        warning_turnoff.setChecked(prefs.getBoolean("warning_turnoff", false));
        warning_earphone.setChecked(prefs.getBoolean("warning_earphone", false));
        warning_volume.setChecked(prefs.getBoolean("warning_volume", false));

        e_warning_turnoff = warning_turnoff.isChecked();
        e_warning_earphone = warning_earphone.isChecked();
        e_warning_volume = warning_volume.isChecked();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize your app components here
        warning_turnoff = findViewById(R.id.warning_turnoff);
        warning_earphone = findViewById(R.id.warning_earphone);
        warning_volume = findViewById(R.id.warning_volume);

        statusTextView = findViewById(R.id.textView_status);

        grantButton = findViewById(R.id.button_grant);

        // Load saved switch states
        loadSwitchStates();

        // Set listeners for global variables
        warning_turnoff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSwitchState("warning_turnoff", isChecked);
            e_warning_turnoff = isChecked;
        });
        warning_volume.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSwitchState("warning_volume", isChecked);
            e_warning_volume = isChecked;
        });
        warning_earphone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            } else {
                saveSwitchState("warning_earphone", isChecked);
                e_warning_earphone = isChecked;
            }
        });

        // Check notification permission
        changeNotificationPermissionStatus(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);

        // Create Notification Channel
        CharSequence name = "Bepilot Notifications";
        String description = "Notifications from Bepilot";
        int importance = android.app.NotificationManager.IMPORTANCE_MAX;
        android.app.NotificationChannel channel = new android.app.NotificationChannel(NOTIFY_CHANNEL_ID, name, importance);
        channel.setDescription(description);
        android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        // Register receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        // Observer for handle volume changes
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI,
                true,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        if (e_warning_volume && is_earphone_connected) {
                            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            if (maxVolume != 0 && ((float) currentVolume / maxVolume) >= 0.6f) {
                                sendNotification(getApplicationContext(), "이어폰 음량이 안전범위를 초과하였습니다.", "높은 볼륨은 청력을 손상시킬 수 존재합니다. 즉시 음량을 줄이십시오.");
                            }
                        }
                    }
                }
        );

        // registerReceiver(new Receiver() { @Override... }, filter); // Anonymous class example
        registerReceiver(new Receiver(), filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check notification permission when the activity resumes
        changeNotificationPermissionStatus(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            warning_earphone.setChecked(false);
        }
        loadSwitchStates();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo device : devices) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                is_earphone_connected = true;
                break;
            }
        }
    }

    /**
     * This method is intended to change the notification permission status.
     * However, in Android, notification permissions are managed by the system and cannot be changed programmatically.
     * You can only check the status or direct users to the settings to change it manually.
     * This method is a placeholder and does not perform any action.
     *
     * @param granted The desired status of the notification permission (true for granted, false for denied).
     * @author Dwk0910
     */
    private void changeNotificationPermissionStatus(boolean granted) {
        if (granted) {
            statusTextView.setText(R.string.notification_access_granted);
            Drawable checkIcon = ContextCompat.getDrawable(this, R.drawable.ic_check_circle);
            statusTextView.setCompoundDrawablesWithIntrinsicBounds(checkIcon, null, null, null);

            grantButton.setVisibility(Button.INVISIBLE);
        } else {
            statusTextView.setText(R.string.notification_access_not_granted);
            Drawable errorIcon = ContextCompat.getDrawable(this, R.drawable.ic_err_circle);
            statusTextView.setCompoundDrawablesWithIntrinsicBounds(errorIcon, null, null, null);

            warning_turnoff.setChecked(false);
            warning_earphone.setChecked(false);
            warning_volume.setChecked(false);
            warning_turnoff.setEnabled(false);
            warning_earphone.setEnabled(false);
            warning_volume.setEnabled(false);

            grantButton.setVisibility(Button.VISIBLE);
            grantButton.setOnClickListener(v -> {
                // Direct user to app settings to manually enable notification permission
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            });
        }
    }
}