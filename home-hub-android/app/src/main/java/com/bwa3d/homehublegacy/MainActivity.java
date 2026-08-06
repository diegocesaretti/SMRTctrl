package com.bwa3d.homehublegacy;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.MediaController;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class MainActivity extends Activity implements MotionDetector.Listener {
    private static final int REQUEST_SETTINGS = 31;
    private static final int REQUEST_CAMERA = 32;
    private static WeakReference<MainActivity> current = new WeakReference<>(null);

    private Prefs prefs;
    private WebView webView;
    private View sleepOverlay;
    private TextView responseOverlay;
    private VideoView videoView;
    private Button settingsButton;
    private MotionDetector motionDetector;
    private TextToSpeech textToSpeech;
    private final Handler handler = new Handler();
    private final Runnable hideOverlay = new Runnable() {
        @Override
        public void run() {
            responseOverlay.setVisibility(View.GONE);
        }
    };
    private final Runnable clearMotion = new Runnable() {
        @Override
        public void run() {
            HubState.motion = false;
        }
    };
    private float previousBrightness = -1f;

    static MainActivity getCurrent() {
        return current.get();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = new WeakReference<>(this);
        prefs = new Prefs(this);
        buildUi();
        configureWebView();
        applyKiosk();
        loadDashboard(prefs.getDashboardUrl());
        HubService.start(this);
        initializeTts();

        if (!prefs.isConfigured()) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openSettings();
                }
            }, 500);
        }
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        videoView = new VideoView(this);
        videoView.setVisibility(View.GONE);
        videoView.setBackgroundColor(Color.BLACK);
        root.addView(videoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopMedia();
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                HubState.mediaState = "idle";
                Toast.makeText(MainActivity.this, "The built-in player could not open this media.", Toast.LENGTH_LONG).show();
                videoView.setVisibility(View.GONE);
                return true;
            }
        });

        responseOverlay = new TextView(this);
        responseOverlay.setVisibility(View.GONE);
        responseOverlay.setTextColor(Color.WHITE);
        responseOverlay.setBackgroundColor(0xE6000000);
        responseOverlay.setGravity(Gravity.CENTER);
        responseOverlay.setPadding(48, 32, 48, 32);
        root.addView(responseOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        sleepOverlay = new View(this);
        sleepOverlay.setBackgroundColor(Color.BLACK);
        sleepOverlay.setVisibility(View.GONE);
        sleepOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wakeScreen();
            }
        });
        root.addView(sleepOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        settingsButton = new Button(this);
        settingsButton.setText("⋮");
        settingsButton.setTextSize(22f);
        settingsButton.setTextColor(Color.WHITE);
        settingsButton.setBackgroundColor(0x55000000);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
        FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(dp(52), dp(52));
        buttonParams.gravity = Gravity.TOP | Gravity.END;
        buttonParams.setMargins(0, dp(8), dp(8), 0);
        root.addView(settingsButton, buttonParams);

        setContentView(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUserAgentString(settings.getUserAgentString() + " HomeHubLegacy/0.1");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternalUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleExternalUrl(request.getUrl().toString());
            }
        });
    }

    private boolean handleExternalUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return false;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException ignored) {
        }
        return true;
    }

    private void initializeTts() {
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });
    }

    void loadDashboard(final String url) {
        if (url == null || url.trim().isEmpty()) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    void reloadDashboard() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.reload();
            }
        });
    }

    void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
    }

    void screenOff() {
        HubState.screenOn = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams attributes = getWindow().getAttributes();
                previousBrightness = attributes.screenBrightness;
                attributes.screenBrightness = 0.01f;
                getWindow().setAttributes(attributes);
                sleepOverlay.setVisibility(View.VISIBLE);
                sleepOverlay.bringToFront();
                settingsButton.setVisibility(View.GONE);
            }
        });
    }

    void wakeScreen() {
        HubState.screenOn = true;
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "HomeHubLegacy:MotionWake"
            );
            wakeLock.acquire(3000);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams attributes = getWindow().getAttributes();
                if (previousBrightness >= 0f) {
                    attributes.screenBrightness = previousBrightness;
                } else {
                    attributes.screenBrightness = prefs.getBrightness() / 100f;
                }
                getWindow().setAttributes(attributes);
                sleepOverlay.setVisibility(View.GONE);
                settingsButton.setVisibility(View.VISIBLE);
                settingsButton.bringToFront();
                applyKiosk();
            }
        });
    }

    void setBrightness(int percent) {
        prefs.setBrightness(percent);
        if (!HubState.screenOn) return;
        final float level = prefs.getBrightness() / 100f;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WindowManager.LayoutParams attributes = getWindow().getAttributes();
                attributes.screenBrightness = level;
                getWindow().setAttributes(attributes);
            }
        });
    }

    void showText(final String text, final int seconds, final boolean speak) {
        wakeScreen();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String safeText = text == null ? "" : text.trim();
                int length = safeText.length();
                float size = length < 60 ? 46f : (length < 150 ? 34f : 26f);
                responseOverlay.setTextSize(size);
                responseOverlay.setText(safeText);
                responseOverlay.setVisibility(View.VISIBLE);
                responseOverlay.bringToFront();
                settingsButton.bringToFront();
                handler.removeCallbacks(hideOverlay);
                handler.postDelayed(hideOverlay, Math.max(2, seconds) * 1000L);
                if (speak) speak(safeText);
            }
        });
    }

    void speak(String text) {
        if (textToSpeech == null || text == null || text.trim().isEmpty()) return;
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "home_hub_response");
    }

    void playMedia(final String url, final String mime, final String title) {
        wakeScreen();
        HubState.mediaUrl = url == null ? "" : url;
        HubState.mediaTitle = title == null ? "" : title;
        HubState.mediaState = "playing";

        final String selectedPackage = prefs.getPlayerPackage();
        if (selectedPackage != null && !selectedPackage.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(url), mime == null || mime.isEmpty() ? "video/*" : mime);
                    intent.setPackage(selectedPackage);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException error) {
                        Toast.makeText(MainActivity.this, "Selected media player is not available. Using the built-in player.", Toast.LENGTH_LONG).show();
                        playBuiltIn(url);
                    }
                }
            });
            return;
        }
        playBuiltIn(url);
    }

    private void playBuiltIn(final String url) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                videoView.setVisibility(View.VISIBLE);
                videoView.bringToFront();
                settingsButton.bringToFront();
                videoView.setVideoURI(Uri.parse(url));
                videoView.start();
            }
        });
    }

    void pauseMedia() {
        HubState.mediaState = "paused";
        if (videoView.getVisibility() == View.VISIBLE) {
            videoView.pause();
        } else {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
        }
    }

    void resumeMedia() {
        HubState.mediaState = "playing";
        if (videoView.getVisibility() == View.VISIBLE) {
            videoView.start();
        } else {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
        }
    }

    void stopMedia() {
        HubState.mediaState = "idle";
        HubState.mediaUrl = "";
        HubState.mediaTitle = "";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (videoView.getVisibility() == View.VISIBLE) {
                    videoView.stopPlayback();
                    videoView.setVisibility(View.GONE);
                } else {
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_STOP);
                }
            }
        });
    }

    void setVolume(int percent) {
        int value = Math.max(0, Math.min(100, percent));
        HubState.volume = value;
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager == null) return;
        int maximum = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(maximum * (value / 100f)), 0);
    }

    private void sendMediaKey(int keyCode) {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager == null) return;
        KeyEvent down = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent up = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        manager.dispatchMediaKeyEvent(down);
        manager.dispatchMediaKeyEvent(up);
    }

    void setMotionEnabled(boolean enabled) {
        prefs.setMotionEnabled(enabled);
        configureMotion();
    }

    private void configureMotion() {
        if (!prefs.isMotionEnabled()) {
            stopMotion();
            return;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }
        stopMotion();
        motionDetector = new MotionDetector(prefs.getMotionThreshold(), this);
        motionDetector.start();
    }

    private void stopMotion() {
        if (motionDetector != null) {
            motionDetector.stop();
            motionDetector = null;
        }
        HubState.motion = false;
    }

    @Override
    public void onMotionDetected() {
        HubState.motion = true;
        wakeScreen();
        handler.removeCallbacks(clearMotion);
        handler.postDelayed(clearMotion, 5000L);
    }

    private void applyKiosk() {
        if (!prefs.isKiosk()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        current = new WeakReference<>(this);
        applyKiosk();
        configureMotion();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMotion();
    }

    @Override
    protected void onDestroy() {
        stopMotion();
        handler.removeCallbacksAndMessages(null);
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (current.get() == this) current.clear();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applyKiosk();
    }

    @Override
    public void onBackPressed() {
        if (responseOverlay.getVisibility() == View.VISIBLE) {
            responseOverlay.setVisibility(View.GONE);
            return;
        }
        if (videoView.getVisibility() == View.VISIBLE) {
            stopMedia();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        if (!prefs.isKiosk()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            applyKiosk();
            loadDashboard(prefs.getDashboardUrl());
            configureMotion();
            HubService.start(this);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            configureMotion();
        } else if (requestCode == REQUEST_CAMERA) {
            prefs.setMotionEnabled(false);
            Toast.makeText(this, "Camera permission is required for motion sensing.", Toast.LENGTH_LONG).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
