package com.wellbeing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BreakRemindersPanel extends JPanel {

    private JToggleButton enabledToggleButton;
    private JSpinner workIntervalSpinner;
    private JSpinner breakDurationSpinner;
    private JCheckBox showNotificationCheckbox;
    private JTextArea messageTextArea;
    private JLabel statusLabel;
    private JLabel nextBreakLabel;
    private JComboBox<String> startTimeComboBox;
    private JComboBox<String> endTimeComboBox;

    private boolean remindersEnabled = false;
    private int workIntervalMinutes = 45;
    private int breakDurationMinutes = 5;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;
    private Timer countdownTimer;
    private long nextBreakTimeMillis;

    public BreakRemindersPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();

        // Initialize scheduler
        scheduler = Executors.newScheduledThreadPool(1);
    }

    private void initComponents() {
        // Main panel with settings
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Settings panel
        JPanel settingsPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        settingsPanel.setBorder(new TitledBorder("Break Reminder Settings"));

        // Work interval
        settingsPanel.add(new JLabel("Work interval (minutes):"));
        workIntervalSpinner = new JSpinner(new SpinnerNumberModel(workIntervalMinutes, 10, 120, 5));
        workIntervalSpinner.addChangeListener(e -> {
            workIntervalMinutes = (Integer) workIntervalSpinner.getValue();
            if (remindersEnabled) {
                restartSchedule();
            }
        });
        settingsPanel.add(workIntervalSpinner);

        // Break duration
        settingsPanel.add(new JLabel("Break duration (minutes):"));
        breakDurationSpinner = new JSpinner(new SpinnerNumberModel(breakDurationMinutes, 1, 30, 1));
        breakDurationSpinner.addChangeListener(e -> breakDurationMinutes = (Integer) breakDurationSpinner.getValue());
        settingsPanel.add(breakDurationSpinner);

        // Active hours
        settingsPanel.add(new JLabel("Active hours:"));
        JPanel hoursPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        // Start time combo
        startTimeComboBox = new JComboBox<>();
        populateTimeComboBox(startTimeComboBox, "08:00");

        // End time combo
        endTimeComboBox = new JComboBox<>();
        populateTimeComboBox(endTimeComboBox, "18:00");

        hoursPanel.add(startTimeComboBox);
        hoursPanel.add(new JLabel(" to "));
        hoursPanel.add(endTimeComboBox);

        settingsPanel.add(hoursPanel);

        // Show notifications option
        settingsPanel.add(new JLabel("Show notifications:"));
        showNotificationCheckbox = new JCheckBox("Show system notifications", true);
        settingsPanel.add(showNotificationCheckbox);

        // Enable/disable toggle
        settingsPanel.add(new JLabel("Break reminders:"));
        enabledToggleButton = new JToggleButton("Enable Break Reminders");
        enabledToggleButton.addActionListener(this::toggleReminders);
        settingsPanel.add(enabledToggleButton);

        // Status section
        JPanel statusPanel = new JPanel(new BorderLayout(10, 10));
        statusPanel.setBorder(new TitledBorder("Status"));

        statusLabel = new JLabel("Break reminders are disabled");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        nextBreakLabel = new JLabel("Next break: Not scheduled");

        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(nextBreakLabel, BorderLayout.CENTER);

        // Break message section
        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messagePanel.setBorder(new TitledBorder("Break Reminder Message"));

        messageTextArea = new JTextArea(
                "Time for a break!\n\n" +
                        "Remember to:\n" +
                        "- Stand up and stretch\n" +
                        "- Look away from the screen\n" +
                        "- Take a few deep breaths\n\n" +
                        "Your eyes and body will thank you!");
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(messageTextArea);
        messageScrollPane.setPreferredSize(new Dimension(400, 150));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton testButton = new JButton("Test Reminder");
        testButton.addActionListener(e -> showBreakReminder());
        buttonPanel.add(testButton);

        messagePanel.add(messageScrollPane, BorderLayout.CENTER);
        messagePanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add all sections to main panel
        mainPanel.add(settingsPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(statusPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(messagePanel);

        // Add main panel to this panel
        add(mainPanel, BorderLayout.CENTER);
    }

    private void populateTimeComboBox(JComboBox<String> comboBox, String defaultTime) {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 30) {
                String time = String.format("%02d:%02d", hour, minute);
                comboBox.addItem(time);

                if (time.equals(defaultTime)) {
                    comboBox.setSelectedItem(time);
                }
            }
        }

        // Add listener to restart schedule if time range changes
        comboBox.addActionListener(e -> {
            if (remindersEnabled) {
                restartSchedule();
            }
        });
    }

    private void toggleReminders(ActionEvent e) {
        remindersEnabled = enabledToggleButton.isSelected();

        if (remindersEnabled) {
            enabledToggleButton.setText("Disable Break Reminders");
            statusLabel.setText("Break reminders are enabled");

            // Start scheduling breaks
            startSchedulingBreaks();
        } else {
            enabledToggleButton.setText("Enable Break Reminders");
            statusLabel.setText("Break reminders are disabled");
            nextBreakLabel.setText("Next break: Not scheduled");

            // Stop scheduling breaks
            stopSchedulingBreaks();
        }
    }

    private void startSchedulingBreaks() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }

        // Calculate initial delay
        long delayMillis = workIntervalMinutes * 60 * 1000;

        // Check if current time is within active hours
        if (isWithinActiveHours()) {
            // Schedule the first break
            nextBreakTimeMillis = System.currentTimeMillis() + delayMillis;
            scheduledTask = scheduler.schedule(this::onBreakTime, delayMillis, TimeUnit.MILLISECONDS);

            // Start countdown timer
            startCountdownTimer();
        } else {
            // Schedule for next active period start
            long nextStartMillis = getNextActiveStartMillis();
            nextBreakTimeMillis = nextStartMillis;

            // Schedule task for next active period
            long initialDelay = nextStartMillis - System.currentTimeMillis();
            scheduledTask = scheduler.schedule(this::onBreakTime, initialDelay, TimeUnit.MILLISECONDS);

            // Start countdown timer
            startCountdownTimer();
        }
    }

    private void stopSchedulingBreaks() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }

        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
    }

    private void restartSchedule() {
        if (remindersEnabled) {
            stopSchedulingBreaks();
            startSchedulingBreaks();
        }
    }

    private void startCountdownTimer() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            countdownTimer.stop();
        }

        countdownTimer = new Timer(1000, e -> updateCountdown());
        countdownTimer.start();
    }

    private void updateCountdown() {
        long remainingMillis = nextBreakTimeMillis - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            nextBreakLabel.setText("Break time now!");
        } else {
            long minutes = remainingMillis / (60 * 1000);
            long seconds = (remainingMillis % (60 * 1000)) / 1000;
            nextBreakLabel.setText(String.format("Next break in: %02d:%02d", minutes, seconds));
        }
    }

    private void onBreakTime() {
        // Show break reminder
        if (isWithinActiveHours()) {
            showBreakReminder();

            // Schedule next break
            long delayMillis = workIntervalMinutes * 60 * 1000;
            nextBreakTimeMillis = System.currentTimeMillis() + delayMillis;
            scheduledTask = scheduler.schedule(this::onBreakTime, delayMillis, TimeUnit.MILLISECONDS);
        } else {
            // Schedule for next active period
            long nextStartMillis = getNextActiveStartMillis();
            nextBreakTimeMillis = nextStartMillis;

            // Schedule task for next active period
            long initialDelay = nextStartMillis - System.currentTimeMillis();
            scheduledTask = scheduler.schedule(this::onBreakTime, initialDelay, TimeUnit.MILLISECONDS);
        }
    }

    private void showBreakReminder() {
        // Create and show break reminder dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Time for a Break", false);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel titleLabel = new JLabel("Break Reminder");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);

        JTextArea messageArea = new JTextArea(messageTextArea.getText());
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        messageArea.setBackground(panel.getBackground());

        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel timerLabel = new JLabel(breakDurationMinutes + ":00");
        timerLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        timerPanel.add(timerLabel);

        JButton dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(e -> dialog.dispose());

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        panel.add(timerPanel, BorderLayout.SOUTH);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(dismissButton, BorderLayout.SOUTH);

        // Set up break countdown timer
        Timer breakTimer = new Timer(1000, e -> {
            String currentText = timerLabel.getText();
            String[] parts = currentText.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);

            seconds--;
            if (seconds < 0) {
                seconds = 59;
                minutes--;
            }

            if (minutes < 0) {
                ((Timer) e.getSource()).stop();
                dialog.dispose();
            } else {
                timerLabel.setText(String.format("%d:%02d", minutes, seconds));
            }
        });

        breakTimer.start();

        // Show the dialog
        dialog.setVisible(true);

        // Also show system notification if enabled
        if (showNotificationCheckbox.isSelected()) {
            if (SystemTray.isSupported()) {
                try {
                    SystemTray tray = SystemTray.getSystemTray();
                    TrayIcon[] icons = tray.getTrayIcons();

                    if (icons.length > 0) {
                        icons[0].displayMessage(
                                "Break Reminder",
                                "Time for a " + breakDurationMinutes + " minute break!",
                                TrayIcon.MessageType.INFO);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isWithinActiveHours() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.parse((String) startTimeComboBox.getSelectedItem(),
                DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime endTime = LocalTime.parse((String) endTimeComboBox.getSelectedItem(),
                DateTimeFormatter.ofPattern("HH:mm"));

        return !now.isBefore(startTime) && now.isBefore(endTime);
    }

    private long getNextActiveStartMillis() {
        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.parse((String) startTimeComboBox.getSelectedItem(),
                DateTimeFormatter.ofPattern("HH:mm"));

        // Calculate milliseconds until next start time
        if (now.isBefore(startTime)) {
            // Start time is later today
            return System.currentTimeMillis() +
                    (startTime.toSecondOfDay() - now.toSecondOfDay()) * 1000;
        } else {
            // Start time is tomorrow
            return System.currentTimeMillis() +
                    ((24 * 60 * 60) - now.toSecondOfDay() + startTime.toSecondOfDay()) * 1000;
        }
    }
}