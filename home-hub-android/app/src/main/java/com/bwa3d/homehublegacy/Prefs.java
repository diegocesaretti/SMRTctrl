package com.bwa3d.homehublegacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.UUID;

final class Prefs {
    private static final String NAME = "home_hub_legacy";
    private final SharedPreferences prefs;

    Prefs(Context context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE);
        ensureIdentity();
    }

    private void ensureIdentity() {
        SharedPreferences.Editor editor = null;
        if (!prefs.contains("device_id")) {
            editor = prefs.edit();
            editor.putString("device_id", UUID.randomUUID().toString());
        }
        if (!prefs.contains("token")) {
            if (editor == null) editor = prefs.edit();
            editor.putString("token", UUID.randomUUID().toString().replace("-", ""));
        }
        if (editor != null) editor.apply();
    }

    String getDeviceId() { return prefs.getString("device_id", "unknown"); }
    String getDeviceName() { return prefs.getString("device_name", Build.MANUFACTURER + " " + Build.MODEL); }
    void setDeviceName(String value) { prefs.edit().putString("device_name", value.trim()).apply(); }
    String getDashboardUrl() { return prefs.getString("dashboard_url", "http://homeassistant.local:8123/"); }
    void setDashboardUrl(String value) { prefs.edit().putString("dashboard_url", value.trim()).apply(); }
    String getToken() { return prefs.getString("token", ""); }
    void setToken(String value) { prefs.edit().putString("token", value.trim()).apply(); }
    int getPort() { return prefs.getInt("port", 2323); }
    void setPort(int value) { prefs.edit().putInt("port", value).apply(); }
    boolean isKiosk() { return prefs.getBoolean("kiosk", true); }
    void setKiosk(boolean value) { prefs.edit().putBoolean("kiosk", value).apply(); }
    boolean isMotionEnabled() { return prefs.getBoolean("motion_enabled", false); }
    void setMotionEnabled(boolean value) { prefs.edit().putBoolean("motion_enabled", value).apply(); }
    int getMotionThreshold() { return prefs.getInt("motion_threshold", 18); }
    void setMotionThreshold(int value) { prefs.edit().putInt("motion_threshold", value).apply(); }
    boolean isStartOnBoot() { return prefs.getBoolean("start_on_boot", true); }
    void setStartOnBoot(boolean value) { prefs.edit().putBoolean("start_on_boot", value).apply(); }
    String getPlayerPackage() { return prefs.getString("player_package", ""); }
    void setPlayerPackage(String value) { prefs.edit().putString("player_package", value == null ? "" : value).apply(); }
    int getBrightness() { return prefs.getInt("brightness", 70); }
    void setBrightness(int value) { prefs.edit().putInt("brightness", Math.max(1, Math.min(100, value))).apply(); }
    boolean isConfigured() { return prefs.getBoolean("configured", false); }
    void setConfigured(boolean value) { prefs.edit().putBoolean("configured", value).apply(); }
}
