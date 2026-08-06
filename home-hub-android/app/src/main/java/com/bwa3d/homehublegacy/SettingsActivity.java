package com.bwa3d.homehublegacy;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity {
    private Prefs prefs;
    private EditText deviceName;
    private EditText dashboardUrl;
    private EditText token;
    private EditText port;
    private CheckBox kiosk;
    private CheckBox motion;
    private CheckBox startOnBoot;
    private SeekBar threshold;
    private TextView thresholdValue;
    private Spinner playerSpinner;
    private final List<PlayerChoice> players = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        setTitle("Home Hub Legacy settings");
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(245, 245, 245));
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(28), dp(20), dp(28), dp(30));
        scroll.addView(form);

        TextView heading = new TextView(this);
        heading.setText("Home Hub Legacy");
        heading.setTextSize(28f);
        heading.setTextColor(Color.rgb(30, 40, 45));
        heading.setPadding(0, 0, 0, dp(14));
        form.addView(heading);

        TextView description = new TextView(this);
        description.setText("A lightweight Android 6 dashboard and Home Assistant display. The local API token below is used by the Home Assistant integration.");
        description.setTextSize(15f);
        description.setTextColor(Color.DKGRAY);
        description.setPadding(0, 0, 0, dp(18));
        form.addView(description);

        deviceName = addTextField(form, "Device name", prefs.getDeviceName(), InputType.TYPE_CLASS_TEXT);
        dashboardUrl = addTextField(form, "Dashboard URL", prefs.getDashboardUrl(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        port = addTextField(form, "Local API port", String.valueOf(prefs.getPort()), InputType.TYPE_CLASS_NUMBER);
        token = addTextField(form, "Local API token", prefs.getToken(), InputType.TYPE_CLASS_TEXT);

        kiosk = addCheckBox(form, "Kiosk / immersive mode", prefs.isKiosk());
        motion = addCheckBox(form, "Wake the screen using front-camera motion", prefs.isMotionEnabled());
        startOnBoot = addCheckBox(form, "Start after device boot", prefs.isStartOnBoot());

        form.addView(label("Motion sensitivity threshold"));
        thresholdValue = new TextView(this);
        thresholdValue.setText(String.valueOf(prefs.getMotionThreshold()));
        thresholdValue.setGravity(Gravity.END);
        form.addView(thresholdValue);
        threshold = new SeekBar(this);
        threshold.setMax(55);
        threshold.setProgress(Math.max(0, prefs.getMotionThreshold() - 5));
        threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thresholdValue.setText(String.valueOf(progress + 5));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        form.addView(threshold);

        form.addView(label("Default player for media sent by Home Assistant"));
        loadPlayers();
        ArrayAdapter<PlayerChoice> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, players);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerSpinner = new Spinner(this);
        playerSpinner.setAdapter(adapter);
        form.addView(playerSpinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        selectCurrentPlayer();

        TextView note = new TextView(this);
        note.setText("Built-in playback is deliberately simple. Selecting VLC or another installed player usually gives better support for HLS and unusual codecs.");
        note.setTextSize(13f);
        note.setTextColor(Color.GRAY);
        note.setPadding(0, dp(6), 0, dp(18));
        form.addView(note);

        Button save = new Button(this);
        save.setText("Save and open dashboard");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        form.addView(save);

        Button connection = new Button(this);
        connection.setText("Show integration connection details");
        connection.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Toast.makeText(SettingsActivity.this,
                        "Port: " + port.getText() + "\nToken: " + token.getText(),
                        Toast.LENGTH_LONG).show();
            }
        });
        form.addView(connection);
        setContentView(scroll);
    }

    private EditText addTextField(LinearLayout form, String title, String value, int inputType) {
        form.addView(label(title));
        EditText editText = new EditText(this);
        editText.setText(value);
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        form.addView(editText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return editText;
    }

    private CheckBox addCheckBox(LinearLayout form, String title, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(title);
        box.setChecked(checked);
        box.setPadding(0, dp(6), 0, dp(6));
        form.addView(box);
        return box;
    }

    private TextView label(String value) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(14f);
        label.setTextColor(Color.DKGRAY);
        label.setPadding(0, dp(12), 0, 0);
        return label;
    }

    private void loadPlayers() {
        players.clear();
        players.add(new PlayerChoice("Built-in player", ""));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("https://example.invalid/video.mp4"), "video/*");
        PackageManager manager = getPackageManager();
        List<ResolveInfo> matches = manager.queryIntentActivities(intent, 0);
        Set<String> packages = new HashSet<>();
        for (ResolveInfo match : matches) {
            String packageName = match.activityInfo.packageName;
            if (packageName.equals(getPackageName()) || !packages.add(packageName)) continue;
            CharSequence appLabel = match.loadLabel(manager);
            players.add(new PlayerChoice(appLabel == null ? packageName : appLabel.toString(), packageName));
        }
    }

    private void selectCurrentPlayer() {
        String selectedPackage = prefs.getPlayerPackage();
        for (int index = 0; index < players.size(); index++) {
            if (players.get(index).packageName.equals(selectedPackage)) {
                playerSpinner.setSelection(index);
                return;
            }
        }
    }

    private void save() {
        String url = dashboardUrl.getText().toString().trim();
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            dashboardUrl.setError("Use a complete http:// or https:// URL");
            return;
        }
        int apiPort;
        try { apiPort = Integer.parseInt(port.getText().toString()); }
        catch (NumberFormatException error) { port.setError("Invalid port"); return; }
        if (apiPort < 1024 || apiPort > 65535) {
            port.setError("Use a port between 1024 and 65535");
            return;
        }
        if (token.getText().toString().trim().length() < 8) {
            token.setError("Use at least 8 characters");
            return;
        }

        prefs.setDeviceName(deviceName.getText().toString());
        prefs.setDashboardUrl(url);
        prefs.setPort(apiPort);
        prefs.setToken(token.getText().toString());
        prefs.setKiosk(kiosk.isChecked());
        prefs.setMotionEnabled(motion.isChecked());
        prefs.setStartOnBoot(startOnBoot.isChecked());
        prefs.setMotionThreshold(threshold.getProgress() + 5);
        PlayerChoice selected = (PlayerChoice) playerSpinner.getSelectedItem();
        prefs.setPlayerPackage(selected == null ? "" : selected.packageName);
        prefs.setConfigured(true);
        setResult(RESULT_OK);
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class PlayerChoice {
        final String label;
        final String packageName;
        PlayerChoice(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
        @Override public String toString() { return label; }
    }
}
