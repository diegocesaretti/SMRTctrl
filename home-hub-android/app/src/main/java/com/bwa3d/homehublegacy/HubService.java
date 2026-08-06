package com.bwa3d.homehublegacy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class HubService extends Service {
    private static final int NOTIFICATION_ID = 2323;
    private MiniHttpServer server;

    static void start(Context context) {
        Intent intent = new Intent(context, HubService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
        else context.startService(intent);
    }

    @Override public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        restartServer();
        return START_STICKY;
    }

    private void restartServer() {
        if (server != null) server.stop();
        Prefs prefs = new Prefs(this);
        server = new MiniHttpServer(getApplicationContext(), prefs.getPort(), prefs.getToken());
        server.start();
    }

    private Notification createNotification() {
        String channelId = "home_hub_legacy";
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Home Hub local control", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the local Home Assistant control API available.");
            manager.createNotificationChannel(channel);
        }
        Intent launch = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, channelId) : new Notification.Builder(this);
        return builder.setContentTitle("Home Hub Legacy")
                .setContentText("Dashboard and local Home Assistant control are active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override public void onDestroy() {
        if (server != null) server.stop();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
