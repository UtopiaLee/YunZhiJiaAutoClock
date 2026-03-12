package com.autoclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.widget.Toast;
import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("AutoClockPrefs", Context.MODE_PRIVATE);
        
        // 检查今天是否需要打卡
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        boolean shouldClock = false;
        switch (dayOfWeek) {
            case Calendar.MONDAY:
                shouldClock = prefs.getBoolean("monday", true);
                break;
            case Calendar.TUESDAY:
                shouldClock = prefs.getBoolean("tuesday", true);
                break;
            case Calendar.WEDNESDAY:
                shouldClock = prefs.getBoolean("wednesday", true);
                break;
            case Calendar.THURSDAY:
                shouldClock = prefs.getBoolean("thursday", true);
                break;
            case Calendar.FRIDAY:
                shouldClock = prefs.getBoolean("friday", true);
                break;
        }
        
        if (!shouldClock) {
            addLog(context, "今天不需要打卡（周末或未选中）");
            return;
        }
        
        // 启动云之家应用
        launchYunZhiJia(context);
    }

    private void launchYunZhiJia(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage("com.yunzhijia.yunzhijiaandroidclient");
            
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                
                addLog(context, "已启动云之家应用");
                
                // 等待3秒后通过无障碍服务点击
                new Handler().postDelayed(() -> {
                    // 无障碍服务会自动处理点击
                }, 3000);
            } else {
                addLog(context, "未找到云之家应用");
            }
        } catch (Exception e) {
            addLog(context, "启动失败: " + e.getMessage());
        }
    }

    private void addLog(Context context, String message) {
        SharedPreferences prefs = context.getSharedPreferences("AutoClockPrefs", Context.MODE_PRIVATE);
        String timestamp = new java.text.SimpleDateFormat("MM-dd HH:mm:ss").format(new java.util.Date());
        String log = timestamp + " - " + message + "\n";
        
        String currentLogs = prefs.getString("logs", "");
        String newLogs = log + currentLogs;
        
        prefs.edit().putString("logs", newLogs).apply();
    }
}
