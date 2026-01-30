package com.salesdialer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.salesdialer.R;
import com.salesdialer.activity.AutoDialerActivity;
import com.salesdialer.model.Customer;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动拨号服务
 * 实现自动拨号队列、间隔控制、状态管理
 */
public class AutoDialerService extends Service {
    
    private static final String TAG = "AutoDialerService";
    private static final String CHANNEL_ID = "auto_dialer_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private final IBinder binder = new LocalBinder();
    private Handler handler;
    
    // 拨号队列
    private List<Customer> dialQueue = new ArrayList<>();
    private int currentIndex = 0;
    
    // 拨号状态
    private boolean isDialing = false;
    private boolean isPaused = false;
    private Customer currentCustomer;
    
    // 拨号配置
    private int dialInterval = 5000; // 默认5秒间隔
    private String dialStrategy = "priority"; // priority, time, random
    
    // 拨号统计
    private int totalDialed = 0;
    private int successCount = 0;
    private int failedCount = 0;
    private long startTime = 0;
    
    // 回调监听
    private DialerListener listener;
    
    public interface DialerListener {
        void onDialStart(Customer customer);
        void onDialComplete(Customer customer, boolean success);
        void onQueueComplete();
        void onError(String error);
        void onStatusUpdate(DialerStatus status);
    }
    
    public static class DialerStatus {
        public int totalCount;
        public int currentIndex;
        public int successCount;
        public int failedCount;
        public boolean isPaused;
        public boolean isRunning;
        public long elapsedTime;
        
        public DialerStatus(int total, int current, int success, int failed, 
                          boolean paused, boolean running, long elapsed) {
            this.totalCount = total;
            this.currentIndex = current;
            this.successCount = success;
            this.failedCount = failed;
            this.isPaused = paused;
            this.isRunning = running;
            this.elapsedTime = elapsed;
        }
    }
    
    public class LocalBinder extends Binder {
        public AutoDialerService getService() {
            return AutoDialerService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        Log.d(TAG, "Service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    /**
     * 开始自动拨号
     */
    public void startDialing(List<Customer> customers, String strategy, int interval) {
        if (isDialing) {
            Log.w(TAG, "Dialing already in progress");
            return;
        }
        
        this.dialQueue = new ArrayList<>(customers);
        this.dialStrategy = strategy;
        this.dialInterval = interval;
        this.currentIndex = 0;
        this.totalDialed = 0;
        this.successCount = 0;
        this.failedCount = 0;
        this.startTime = System.currentTimeMillis();
        this.isDialing = true;
        this.isPaused = false;
        
        // 根据策略排序队列
        sortQueue();
        
        Log.d(TAG, "Starting auto dialer with " + dialQueue.size() + " customers");
        
        // 更新通知
        updateNotification("自动拨号进行中...");
        
        // 开始拨号
        dialNext();
    }
    
    /**
     * 暂停拨号
     */
    public void pauseDialing() {
        isPaused = true;
        handler.removeCallbacks(dialRunnable);
        updateNotification("自动拨号已暂停");
        notifyStatusUpdate();
        Log.d(TAG, "Dialing paused");
    }
    
    /**
     * 恢复拨号
     */
    public void resumeDialing() {
        if (!isDialing) return;
        isPaused = false;
        updateNotification("自动拨号进行中...");
        notifyStatusUpdate();
        dialNext();
        Log.d(TAG, "Dialing resumed");
    }
    
    /**
     * 停止拨号
     */
    public void stopDialing() {
        isDialing = false;
        isPaused = false;
        handler.removeCallbacks(dialRunnable);
        currentCustomer = null;
        updateNotification("自动拨号已停止");
        notifyStatusUpdate();
        Log.d(TAG, "Dialing stopped");
    }
    
    /**
     * 跳过当前客户
     */
    public void skipCurrent() {
        if (currentCustomer != null) {
            failedCount++;
            if (listener != null) {
                listener.onDialComplete(currentCustomer, false);
            }
        }
        dialNext();
    }
    
    /**
     * 拨打下一个客户
     */
    private void dialNext() {
        if (!isDialing || isPaused) {
            return;
        }
        
        // 检查是否完成
        if (currentIndex >= dialQueue.size()) {
            completeDialing();
            return;
        }
        
        // 获取下一个客户
        currentCustomer = dialQueue.get(currentIndex);
        currentIndex++;
        totalDialed++;
        
        // 验证手机号
        if (!currentCustomer.isValidPhone()) {
            Log.w(TAG, "Invalid phone number for customer: " + currentCustomer.getName());
            failedCount++;
            if (listener != null) {
                listener.onDialComplete(currentCustomer, false);
            }
            // 继续下一个
            handler.postDelayed(this::dialNext, 1000);
            return;
        }
        
        // 通知开始拨号
        if (listener != null) {
            listener.onDialStart(currentCustomer);
        }
        
        // 更新通知
        updateNotification("正在拨打: " + currentCustomer.getName());
        notifyStatusUpdate();
        
        // 执行拨号
        makeCall(currentCustomer.getPhone());
        
        // 记录拨号时间
        currentCustomer.setLastCallTime(System.currentTimeMillis());
        currentCustomer.setCallCount(currentCustomer.getCallCount() + 1);
        
        // 延迟拨打下一个
        handler.postDelayed(dialRunnable, dialInterval);
    }
    
    private final Runnable dialRunnable = this::dialNext;
    
    /**
     * 执行拨打电话
     */
    private void makeCall(String phoneNumber) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));
            callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(callIntent);
            
            successCount++;
            
            Log.d(TAG, "Calling: " + phoneNumber);
            
            if (listener != null) {
                listener.onDialComplete(currentCustomer, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to make call: " + e.getMessage());
            failedCount++;
            
            if (listener != null) {
                listener.onError("拨号失败: " + e.getMessage());
                listener.onDialComplete(currentCustomer, false);
            }
        }
    }
    
    /**
     * 完成拨号
     */
    private void completeDialing() {
        isDialing = false;
        isPaused = false;
        currentCustomer = null;
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        updateNotification("拨号完成 - 成功: " + successCount + " 失败: " + failedCount);
        notifyStatusUpdate();
        
        if (listener != null) {
            listener.onQueueComplete();
        }
        
        Log.d(TAG, String.format("Dialing completed. Total: %d, Success: %d, Failed: %d, Time: %dms",
                totalDialed, successCount, failedCount, elapsedTime));
    }
    
    /**
     * 根据策略排序队列
     */
    private void sortQueue() {
        switch (dialStrategy) {
            case "priority":
                // 按优先级排序（A级优先）
                dialQueue.sort((c1, c2) -> Integer.compare(c1.getPriority(), c2.getPriority()));
                break;
                
            case "time":
                // 按最后联系时间排序（很久没联系的优先）
                dialQueue.sort((c1, c2) -> Long.compare(c1.getLastCallTime(), c2.getLastCallTime()));
                break;
                
            case "random":
                // 随机排序
                java.util.Collections.shuffle(dialQueue);
                break;
                
            default:
                Log.w(TAG, "Unknown dial strategy: " + dialStrategy);
        }
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "自动拨号服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于显示自动拨号状态");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 创建通知
     */
    private Notification createNotification() {
        Intent intent = new Intent(this, AutoDialerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("智能电销王")
                .setContentText("自动拨号服务运行中...")
                .setSmallIcon(R.drawable.ic_phone)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
    
    /**
     * 更新通知
     */
    private void updateNotification(String text) {
        Intent intent = new Intent(this, AutoDialerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        String progress = String.format("进度: %d/%d | 成功: %d | 失败: %d",
                currentIndex, dialQueue.size(), successCount, failedCount);
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("智能电销王 - " + text)
                .setContentText(progress)
                .setSmallIcon(R.drawable.ic_phone)
                .setContentIntent(pendingIntent)
                .setOngoing(isDialing)
                .build();
        
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }
    
    /**
     * 通知状态更新
     */
    private void notifyStatusUpdate() {
        if (listener != null) {
            long elapsed = isDialing ? System.currentTimeMillis() - startTime : 0;
            DialerStatus status = new DialerStatus(
                    dialQueue.size(),
                    currentIndex,
                    successCount,
                    failedCount,
                    isPaused,
                    isDialing,
                    elapsed
            );
            listener.onStatusUpdate(status);
        }
    }
    
    // Getters and Setters
    public void setListener(DialerListener listener) {
        this.listener = listener;
    }
    
    public boolean isDialing() {
        return isDialing;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public Customer getCurrentCustomer() {
        return currentCustomer;
    }
    
    public DialerStatus getStatus() {
        long elapsed = isDialing ? System.currentTimeMillis() - startTime : 0;
        return new DialerStatus(
                dialQueue.size(),
                currentIndex,
                successCount,
                failedCount,
                isPaused,
                isDialing,
                elapsed
        );
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(dialRunnable);
        stopForeground(true);
        Log.d(TAG, "Service destroyed");
    }
}
