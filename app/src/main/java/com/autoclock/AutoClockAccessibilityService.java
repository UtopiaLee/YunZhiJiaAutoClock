package com.autoclock;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class AutoClockAccessibilityService extends AccessibilityService {

    private static final String YUNZHIJIA_PACKAGE = "com.yunzhijia.yunzhijiaandroidclient";
    private Handler handler = new Handler();
    private boolean isProcessing = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null || !event.getPackageName().toString().equals(YUNZHIJIA_PACKAGE)) {
            return;
        }

        if (isProcessing) {
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            handler.postDelayed(() -> {
                performClockIn();
            }, 2000);
        }
    }

    private void performClockIn() {
        if (isProcessing) return;
        isProcessing = true;

        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) {
                addLog("无法获取窗口信息");
                isProcessing = false;
                return;
            }

            // 查找打卡按钮的多种可能文本
            String[] clockTexts = {"打卡", "签到", "上班打卡", "下班打卡", "考勤打卡"};
            boolean found = false;

            for (String text : clockTexts) {
                List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
                if (nodes != null && !nodes.isEmpty()) {
                    for (AccessibilityNodeInfo node : nodes) {
                        if (node.isClickable()) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            addLog("已点击打卡按钮: " + text);
                            found = true;
                            break;
                        } else if (node.getParent() != null && node.getParent().isClickable()) {
                            node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            addLog("已点击打卡按钮(父节点): " + text);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
            }

            if (!found) {
                addLog("未找到打卡按钮");
            }

            rootNode.recycle();
        } catch (Exception e) {
            addLog("打卡失败: " + e.getMessage());
        } finally {
            handler.postDelayed(() -> {
                isProcessing = false;
            }, 5000);
        }
    }

    private void addLog(String message) {
        SharedPreferences prefs = getSharedPreferences("AutoClockPrefs", MODE_PRIVATE);
        String timestamp = new java.text.SimpleDateFormat("MM-dd HH:mm:ss").format(new java.util.Date());
        String log = timestamp + " - " + message + "\n";
        
        String currentLogs = prefs.getString("logs", "");
        String newLogs = log + currentLogs;
        
        prefs.edit().putString("logs", newLogs).apply();
    }

    @Override
    public void onInterrupt() {
        // 服务中断时的处理
    }
}
