package com.wellbeing;

import com.sun.jna.*;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ActivityTracker {
    private static ActivityTracker instance;
    private ScheduledExecutorService scheduler;
    private ConcurrentHashMap<String, Long> appUsageTimes;
    private String currentApp;
    private long currentAppStartTime;
    private DataManager dataManager;
    private List<AppUsageListener> listeners;

    private ActivityTracker() {
        appUsageTimes = new ConcurrentHashMap<>();
        listeners = new ArrayList<>();
        dataManager = new DataManager();

        // Load today's data if exists
        Map<String, Long> todaysData = dataManager.loadDailyUsage(LocalDate.now());
        if (todaysData != null) {
            appUsageTimes.putAll(todaysData);
        }
    }

    public static synchronized ActivityTracker getInstance() {
        if (instance == null) {
            instance = new ActivityTracker();
        }
        return instance;
    }

    public void startTracking() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkActiveWindow, 0, 1, TimeUnit.SECONDS);

        // Schedule daily report generation at midnight
        scheduleDailyReport();
    }

    public void stopTracking() {
        if (scheduler != null && !scheduler.isShutdown()) {
            updateCurrentAppUsage(); // Update one last time
            scheduler.shutdown();

            // Save data
            dataManager.saveDailyUsage(LocalDate.now(), new HashMap<>(appUsageTimes));
        }
    }

    private void checkActiveWindow() {
        try {
            // Get active window
            String activeApp = getActiveWindowTitle();

            if (activeApp == null || activeApp.trim().isEmpty()) {
                activeApp = "Unknown";
            }

            // If app changed, update timings
            if (!activeApp.equals(currentApp)) {
                updateCurrentAppUsage();
                currentApp = activeApp;
                currentAppStartTime = System.currentTimeMillis();
                notifyAppChanged(activeApp);
            }

        } catch (Exception e) {
            System.err.println("Error tracking active window: " + e.getMessage());
        }
    }

    private void updateCurrentAppUsage() {
        if (currentApp != null) {
            long now = System.currentTimeMillis();
            long duration = now - currentAppStartTime;

            appUsageTimes.compute(currentApp, (app, time) -> time == null ? duration : time + duration);

            // Autosave data every minute
            if (now % 60000 < 1000) {
                dataManager.saveDailyUsage(LocalDate.now(), new HashMap<>(appUsageTimes));
            }

            // Notify listeners
            notifyTimeUpdated(currentApp, appUsageTimes.getOrDefault(currentApp, 0L));
        }
    }

    private String getActiveWindowTitle() {
        HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        if (hwnd == null) {
            return null;
        }

        char[] buffer = new char[1024];
        User32.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
        return Native.toString(buffer);
    }

    private void scheduleDailyReport() {
        // Calculate time until midnight
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);

        long initialDelay = tomorrow.getTimeInMillis() - System.currentTimeMillis();

        // Schedule daily report at midnight
        scheduler.scheduleAtFixedRate(() -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            dataManager.generateDailyReport(yesterday);

            // Reset counters for new day
            appUsageTimes.clear();
        }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    // Methods to get usage data
    public Map<String, Long> getAppUsageTimes() {
        updateCurrentAppUsage(); // Make sure current app time is up to date
        return new HashMap<>(appUsageTimes);
    }

    public List<Map.Entry<String, Long>> getTopApps(int limit) {
        Map<String, Long> usageTimes = getAppUsageTimes();
        return usageTimes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public long getTotalScreenTime() {
        return getAppUsageTimes().values().stream().mapToLong(Long::longValue).sum();
    }

    // Get current active application
    public String getCurrentApp() {
        return currentApp;
    }

    // Observer pattern for UI updates
    public interface AppUsageListener {
        void onAppChanged(String appName);

        void onAppTimeUpdated(String appName, long timeInMillis);
    }

    public void addListener(AppUsageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(AppUsageListener listener) {
        listeners.remove(listener);
    }

    private void notifyAppChanged(String appName) {
        for (AppUsageListener listener : listeners) {
            listener.onAppChanged(appName);
        }
    }

    private void notifyTimeUpdated(String appName, long timeInMillis) {
        for (AppUsageListener listener : listeners) {
            listener.onAppTimeUpdated(appName, timeInMillis);
        }
    }
}