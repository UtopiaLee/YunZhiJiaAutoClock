package com.autoclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private EditText morningTimeEdit, eveningTimeEdit;
    private CheckBox mondayCheck, tuesdayCheck, wednesdayCheck, thursdayCheck, fridayCheck;
    private Button saveButton, testButton, accessibilityButton;
    private TextView statusText, logText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("AutoClockPrefs", MODE_PRIVATE);

        initViews();
        loadSettings();
        updateStatus();
    }

    private void initViews() {
        morningTimeEdit = findViewById(R.id.morningTimeEdit);
        eveningTimeEdit = findViewById(R.id.eveningTimeEdit);
        
        mondayCheck = findViewById(R.id.mondayCheck);
        tuesdayCheck = findViewById(R.id.tuesdayCheck);
        wednesdayCheck = findViewById(R.id.wednesdayCheck);
        thursdayCheck = findViewById(R.id.thursdayCheck);
        fridayCheck = findViewById(R.id.fridayCheck);
        
        saveButton = findViewById(R.id.saveButton);
        testButton = findViewById(R.id.testButton);
        accessibilityButton = findViewById(R.id.accessibilityButton);
        
        statusText = findViewById(R.id.statusText);
        logText = findViewById(R.id.logText);

        saveButton.setOnClickListener(v -> saveSettings());
        testButton.setOnClickListener(v -> testClock());
        accessibilityButton.setOnClickListener(v -> openAccessibilitySettings());
    }

    private void loadSettings() {
        morningTimeEdit.setText(prefs.getString("morningTime", "08:30"));
        eveningTimeEdit.setText(prefs.getString("eveningTime", "18:00"));
        
        mondayCheck.setChecked(prefs.getBoolean("monday", true));
        tuesdayCheck.setChecked(prefs.getBoolean("tuesday", true));
        wednesdayCheck.setChecked(prefs.getBoolean("wednesday", true));
        thursdayCheck.setChecked(prefs.getBoolean("thursday", true));
        fridayCheck.setChecked(prefs.getBoolean("friday", true));
        
        loadLogs();
    }

    private void saveSettings() {
        String morningTime = morningTimeEdit.getText().toString();
        String eveningTime = eveningTimeEdit.getText().toString();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("morningTime", morningTime);
        editor.putString("eveningTime", eveningTime);
        editor.putBoolean("monday", mondayCheck.isChecked());
        editor.putBoolean("tuesday", tuesdayCheck.isChecked());
        editor.putBoolean("wednesday", wednesdayCheck.isChecked());
        editor.putBoolean("thursday", thursdayCheck.isChecked());
        editor.putBoolean("friday", fridayCheck.isChecked());
        editor.apply();

        scheduleAlarms();
        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void scheduleAlarms() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        // 取消旧的闹钟
        cancelAlarms();
        
        // 设置上班打卡
        String morningTime = prefs.getString("morningTime", "08:30");
        scheduleAlarm(alarmManager, morningTime, 1);
        
        // 设置下班打卡
        String eveningTime = prefs.getString("eveningTime", "18:00");
        scheduleAlarm(alarmManager, eveningTime, 2);
    }

    private void scheduleAlarm(AlarmManager alarmManager, String time, int requestCode) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        );
    }

    private void cancelAlarms() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(
            this, 1, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(
            this, 2, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (pendingIntent1 != null) alarmManager.cancel(pendingIntent1);
        if (pendingIntent2 != null) alarmManager.cancel(pendingIntent2);
    }

    private void testClock() {
        if (!isAccessibilityEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show();
            return;
        }
        
        Intent intent = new Intent(this, AlarmReceiver.class);
        sendBroadcast(intent);
        
        addLog("手动测试打卡");
        Toast.makeText(this, "正在测试打卡...", Toast.LENGTH_SHORT).show();
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "请找到并开启"云之家自动打卡"服务", Toast.LENGTH_LONG).show();
    }

    private boolean isAccessibilityEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        
        if (accessibilityEnabled == 1) {
            String services = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (services != null) {
                return services.contains(getPackageName());
            }
        }
        return false;
    }

    private void updateStatus() {
        if (isAccessibilityEnabled()) {
            statusText.setText("✅ 无障碍服务已开启\n✅ 自动打卡已启用");
            statusText.setTextColor(0xFF4CAF50);
        } else {
            statusText.setText("❌ 无障碍服务未开启\n请点击下方按钮开启");
            statusText.setTextColor(0xFFF44336);
        }
    }

    private void addLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("MM-dd HH:mm:ss").format(new java.util.Date());
        String log = timestamp + " - " + message + "\n";
        
        String currentLogs = prefs.getString("logs", "");
        String newLogs = log + currentLogs;
        
        // 只保留最近50条
        String[] lines = newLogs.split("\n");
        if (lines.length > 50) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                sb.append(lines[i]).append("\n");
            }
            newLogs = sb.toString();
        }
        
        prefs.edit().putString("logs", newLogs).apply();
        loadLogs();
    }

    private void loadLogs() {
        String logs = prefs.getString("logs", "暂无打卡记录");
        logText.setText(logs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
