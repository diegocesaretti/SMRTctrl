package com.bwa3d.homehublegacy;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class MiniHttpServer {
    private final Context context;
    private final int port;
    private final String token;
    private final ExecutorService clients = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    MiniHttpServer(Context context, int port, String token) {
        this.context = context.getApplicationContext();
        this.port = port;
        this.token = token == null ? "" : token;
    }

    void start() {
        running = true;
        acceptThread = new Thread(new Runnable() {
            @Override public void run() { acceptLoop(); }
        }, "HomeHubHttp");
        acceptThread.start();
    }

    void stop() {
        running = false;
        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException ignored) { }
        }
        clients.shutdownNow();
    }

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            while (running) {
                final Socket socket = serverSocket.accept();
                socket.setSoTimeout(7000);
                clients.execute(new Runnable() {
                    @Override public void run() { handle(socket); }
                });
            }
        } catch (IOException ignored) { }
    }

    private void handle(Socket socket) {
        try (Socket closeable = socket;
             InputStream input = closeable.getInputStream();
             OutputStream output = closeable.getOutputStream()) {
            String requestLine = readLine(input);
            if (requestLine == null || requestLine.trim().isEmpty()) return;
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                send(output, 400, json("error", "Bad request"));
                return;
            }
            String method = requestParts[0].toUpperCase(Locale.US);
            String target = requestParts[1];
            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = readLine(input)) != null && !line.isEmpty()) {
                int separator = line.indexOf(':');
                if (separator > 0) {
                    headers.put(line.substring(0, separator).trim().toLowerCase(Locale.US),
                            line.substring(separator + 1).trim());
                }
            }
            int contentLength = parseInt(headers.get("content-length"), 0);
            String body = new String(readBytes(input, contentLength), StandardCharsets.UTF_8);
            int queryStart = target.indexOf('?');
            String path = queryStart >= 0 ? target.substring(0, queryStart) : target;
            String query = queryStart >= 0 ? target.substring(queryStart + 1) : "";
            Map<String, String> parameters = parseForm(query);
            parameters.putAll(parseForm(body));
            String suppliedToken = headers.get("x-home-hub-token");
            if (suppliedToken == null) suppliedToken = parameters.get("token");
            if (!token.equals(suppliedToken)) {
                send(output, 401, json("error", "Unauthorized"));
                return;
            }
            route(output, method, path, parameters);
        } catch (IOException ignored) { }
    }

    private void route(OutputStream output, String method, String path,
                       Map<String, String> parameters) throws IOException {
        if ("GET".equals(method) && "/api/status".equals(path)) {
            send(output, 200, statusJson());
            return;
        }
        if (!"POST".equals(method)) {
            send(output, 405, json("error", "Method not allowed"));
            return;
        }
        if ("/api/screen/on".equals(path)) {
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.wakeScreen(); } });
        } else if ("/api/screen/off".equals(path)) {
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.screenOff(); } });
        } else if ("/api/screen/brightness".equals(path)) {
            final int level = clamp(parseInt(parameters.get("level"), 70), 1, 100);
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.setBrightness(level); } });
        } else if ("/api/dashboard/load".equals(path)) {
            final String url = parameters.get("url");
            if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) {
                send(output, 400, json("error", "A complete dashboard URL is required"));
                return;
            }
            new Prefs(context).setDashboardUrl(url);
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.loadDashboard(url); } });
        } else if ("/api/dashboard/reload".equals(path)) {
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.reloadDashboard(); } });
        } else if ("/api/settings/open".equals(path)) {
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.openSettings(); } });
        } else if ("/api/motion/enable".equals(path)) {
            final boolean enabled = "true".equalsIgnoreCase(parameters.get("enabled"))
                    || "1".equals(parameters.get("enabled"));
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.setMotionEnabled(enabled); } });
        } else if ("/api/text".equals(path) || "/api/response".equals(path)) {
            final String text = value(parameters, "text", "");
            final int duration = clamp(parseInt(parameters.get("duration"), 10), 2, 120);
            final boolean speak = "/api/response".equals(path);
            onUi(new UiAction() {
                @Override public void run(MainActivity a) { a.showText(text, duration, speak); }
            });
        } else if ("/api/tts".equals(path)) {
            final String text = value(parameters, "text", "");
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.speak(text); } });
        } else if ("/api/media/play".equals(path)) {
            final String url = parameters.get("url");
            if (url == null || url.isEmpty()) {
                send(output, 400, json("error", "Media URL is required"));
                return;
            }
            final String mime = value(parameters, "mime", "video/*");
            final String title = value(parameters, "title", "");
            onUi(new UiAction() {
                @Override public void run(MainActivity a) { a.playMedia(url, mime, title); }
            });
        } else if ("/api/media/pause".equals(path)) {
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.pauseMedia(); } });
        } else if ("/api/media/resume".equals(path)) {
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.resumeMedia(); } });
        } else if ("/api/media/stop".equals(path)) {
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.stopMedia(); } });
        } else if ("/api/media/volume".equals(path)) {
            final int level = clamp(parseInt(parameters.get("level"), 50), 0, 100);
            onUi(new UiAction() { @Override public void run(MainActivity a) { a.setVolume(level); } });
        } else {
            send(output, 404, json("error", "Not found"));
            return;
        }
        send(output, 200, "{\"ok\":true}");
    }

    private void onUi(final UiAction action) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                MainActivity activity = MainActivity.getCurrent();
                if (activity != null) {
                    action.run(activity);
                    return;
                }
                Intent launch = new Intent(context, MainActivity.class);
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(launch);
                mainHandler.postDelayed(new Runnable() {
                    @Override public void run() {
                        MainActivity started = MainActivity.getCurrent();
                        if (started != null) action.run(started);
                    }
                }, 800);
            }
        });
    }

    private String statusJson() {
        Prefs prefs = new Prefs(context);
        Intent battery = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = -1;
        boolean charging = false;
        if (battery != null) {
            int raw = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            level = scale > 0 ? Math.round(raw * 100f / scale) : raw;
            int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
        }
        return "{"
                + "\"device_id\":\"" + escape(prefs.getDeviceId()) + "\","
                + "\"device_name\":\"" + escape(prefs.getDeviceName()) + "\","
                + "\"manufacturer\":\"" + escape(Build.MANUFACTURER) + "\","
                + "\"model\":\"" + escape(Build.MODEL) + "\","
                + "\"android_api\":" + Build.VERSION.SDK_INT + ","
                + "\"app_version\":\"0.1.0-alpha1\","
                + "\"dashboard_url\":\"" + escape(prefs.getDashboardUrl()) + "\","
                + "\"screen_on\":" + HubState.screenOn + ","
                + "\"brightness\":" + prefs.getBrightness() + ","
                + "\"motion\":" + HubState.motion + ","
                + "\"motion_enabled\":" + prefs.isMotionEnabled() + ","
                + "\"battery_level\":" + level + ","
                + "\"charging\":" + charging + ","
                + "\"media_state\":\"" + escape(HubState.mediaState) + "\","
                + "\"media_url\":\"" + escape(HubState.mediaUrl) + "\","
                + "\"media_title\":\"" + escape(HubState.mediaTitle) + "\","
                + "\"volume\":" + HubState.volume + "}";
    }

    private static String readLine(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = input.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                byte[] bytes = buffer.toByteArray();
                int length = bytes.length;
                if (length > 0 && bytes[length - 1] == '\r') length--;
                return new String(bytes, 0, length, StandardCharsets.UTF_8);
            }
            buffer.write(current);
            previous = current;
            if (buffer.size() > 16384) throw new IOException("Header line too long");
        }
        return buffer.size() == 0 ? null : buffer.toString("UTF-8");
    }

    private static byte[] readBytes(InputStream input, int length) throws IOException {
        if (length <= 0) return new byte[0];
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(data, offset, length - offset);
            if (read < 0) break;
            offset += read;
        }
        if (offset == length) return data;
        byte[] partial = new byte[offset];
        System.arraycopy(data, 0, partial, 0, offset);
        return partial;
    }

    private static Map<String, String> parseForm(String form) {
        Map<String, String> values = new HashMap<>();
        if (form == null || form.isEmpty()) return values;
        for (String pair : form.split("&")) {
            if (pair.isEmpty()) continue;
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            try {
                values.put(URLDecoder.decode(key, "UTF-8"),
                        URLDecoder.decode(value, "UTF-8"));
            } catch (Exception ignored) { }
        }
        return values;
    }

    private static void send(OutputStream output, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String status = code == 200 ? "OK" : code == 400 ? "Bad Request"
                : code == 401 ? "Unauthorized" : code == 404 ? "Not Found"
                : code == 405 ? "Method Not Allowed" : "Error";
        String headers = "HTTP/1.1 " + code + " " + status + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Access-Control-Allow-Origin: *\r\n"
                + "Cache-Control: no-store\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.UTF_8));
        output.write(bytes);
        output.flush();
    }

    private static String json(String key, String value) {
        return "{\"" + escape(key) + "\":\"" + escape(value) + "\"}";
    }
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }
    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
    private static String value(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null ? fallback : value;
    }
    private interface UiAction { void run(MainActivity activity); }
}
