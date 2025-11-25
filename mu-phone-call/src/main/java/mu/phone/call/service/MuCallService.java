package mu.phone.call.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * 前台服务
 * @author LiYejun
 * @date 2023/7/10
 */
public class MuCallService extends Service {

    private static final String CHANNEL_ID = "com.mu.call";
    private static final String TAG = "zzmm";

    private Notification initNotification() {
        String channelId = getPackageName();
        Notification.Builder builder = new Notification.Builder(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, "Channel Call", NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(1);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(notificationChannel);
            builder.setChannelId(channelId);
        }
        Notification build = builder.setTicker("已开启拨号服务")
                .setContentTitle("拨号服务")
                .setContentText("服务正在运行，请勿关闭")
                .setStyle(new Notification.BigTextStyle()
                        .bigText("详细的通知内容"))
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOngoing(true)
                .build();
        build.flags |= 32;
        return build;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"service bind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"service onCreate");
        startForeground(1, initNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
    }
}
