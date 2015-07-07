package com.pandamonium.chargealert;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class MainService extends Service {
    private static final String TAG = MainService.class.getName();
    private int mChargeNotificationId = 0;
    private int mDataNotificationId = 1;
    private boolean mChargeAlreadyNotified = false;
    private boolean mLastNetwork = true;
    private NotificationManager mNotificationManager;
//    private int mDebugNotificationId = 1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "creating service");
        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
//        registerReceivers();
    }

    public void onDestroy() {
        Log.d(TAG, "destroying service");
        unregisterReceiver(mChargeReceiver);
        unregisterReceiver(mDataReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        registerReceivers();
        return super.onStartCommand(intent, flags, startId);
    }

    private void registerReceivers() {
        SharedPreferences preferences = getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
        if (preferences.getBoolean(MainActivity.PREFERENCE_KEY_CHARGE_ENABLED, false)) {
            registerReceiver(mChargeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } else {
            unregisterReceiver(mChargeReceiver);
        }

        if (preferences.getBoolean(MainActivity.PREFERENCE_KEY_DATA_ENABLED, false)) {
            registerReceiver(mDataReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        } else {
            unregisterReceiver(mDataReceiver);
        }
    }

    public static void startIfEnabled(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
        boolean isEnabled = preferences.getBoolean(MainActivity.PREFERENCE_KEY_CHARGE_ENABLED, false) ||
                preferences.getBoolean(MainActivity.PREFERENCE_KEY_DATA_ENABLED, false);
        Intent intent = new Intent(context, MainService.class);
        if (isEnabled) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    private static int getNotificationDefaults(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
        int defaults = Notification.DEFAULT_LIGHTS;
        if (preferences.getBoolean(MainActivity.PREFERENCE_KEY_VIBRATE, false)) {
            defaults |= Notification.DEFAULT_VIBRATE;
        }
        if (preferences.getBoolean(MainActivity.PREFERENCE_KEY_SOUND, false)) {
            defaults |= Notification.DEFAULT_SOUND;
        }

        return defaults;
    }

    BroadcastReceiver mChargeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();
                if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, Integer.MIN_VALUE);

                    if (status == BatteryManager.BATTERY_STATUS_FULL) {
                        if (!mChargeAlreadyNotified) {
                            mChargeAlreadyNotified = true;

                            Notification notification = new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_action_battery)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setContentText(getString(R.string.full))
                                    .setOnlyAlertOnce(true)
                                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 0))
                                    .setDefaults(getNotificationDefaults(context))
                                    .build();
                            mNotificationManager.notify(mChargeNotificationId, notification);
                        }
                    } else {
                        mChargeAlreadyNotified = false;
                        mNotificationManager.cancel(mChargeNotificationId);
                    }

                    Log.d(TAG, String.format("battery status: %d", status));
//                    Notification notification = new NotificationCompat.Builder(context)
//                            .setSmallIcon(R.drawable.ic_launcher)
//                            .setContentTitle(getString(R.string.app_name))
//                            .setContentText(String.format("battery status: %d", status))
//                            .build();
//                    mNotificationManager.notify(mDebugNotificationId++, notification);
                }
            }
        }
    };

    BroadcastReceiver mDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    if (activeNetwork.isConnected() && activeNetwork.getSubtype() == TelephonyManager.NETWORK_TYPE_LTE) {
                        Log.d(TAG, "connected to 4G");
                        if (!mLastNetwork) {
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_action_network_cell)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setContentText(getString(R.string.data_connected))
                                    .setOnlyAlertOnce(true)
                                    .setDefaults(getNotificationDefaults(context));

                            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS), 0);
                            if (pendingIntent != null) {
                                builder.setContentIntent(pendingIntent);
                            }

                            Notification notification = builder.build();

                            mNotificationManager.notify(mDataNotificationId, notification);

                            mLastNetwork = true;
                        }
                    } else {
                        // lost 4g
                        Log.d(TAG, "lost 4G");
                        mLastNetwork = false;
                        mNotificationManager.cancel(mDataNotificationId);
                    }
                } else {
                    // connected to a different network
                    Log.d(TAG, "connected to non-mobile");
                    mLastNetwork = true;
                }
            }
        }
    };
}
