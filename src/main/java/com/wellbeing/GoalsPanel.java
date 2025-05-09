package com.wellbeing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class GoalsPanel extends JPanel implements ActivityTracker.AppUsageListener {

    private JSpinner dailyLimitSpinner;
    private JToggleButton alertsEnabledButton;
    private JTable appLimitsTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> appComboBox;
    private JSpinner appLimitSpinner;

    private boolean alertsEnabled = false;
    private int dailyLimitMinutes = 240; // Default 4 hours
    private List<AppLimit> appLimits;
    private Timer checkTimer;
    private boolean isDailyLimitWarningNotified = false;
    private boolean isDailyLimitAlertNotified = false;

    private static final int COLUMN_APP = 0;
    private static final int COLUMN_LIMIT = 1;
    private static final int COLUMN_USED = 2;
    private static final int COLUMN_REMAINING = 3;
    private static final int COLUMN_ACTIONS = 4;

    public GoalsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        appLimits = new CopyOnWriteArrayList<>();

        initComponents();

        // Register with activity tracker
        ActivityTracker.getInstance().addListener(this);

        // Start checking for limits
        startLimitChecking();
    }

    private void initComponents() {
        // Daily limit section
        JPanel dailyLimitPanel = new JPanel(new BorderLayout(10, 0));
        dailyLimitPanel.setBorder(BorderFactory.createTitledBorder("Daily Screen Time Limit"));

        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        spinnerPanel.add(new JLabel("Set daily limit (minutes): "));

        SpinnerNumberModel dailyModel = new SpinnerNumberModel(dailyLimitMinutes, 10, 1440, 5);
        dailyLimitSpinner = new JSpinner(dailyModel);
        dailyLimitSpinner.addChangeListener(e -> dailyLimitMinutes = (int) dailyLimitSpinner.getValue());
        spinnerPanel.add(dailyLimitSpinner);

        alertsEnabledButton = new JToggleButton("Enable Alerts");
        alertsEnabledButton.addActionListener(this::toggleAlerts);

        dailyLimitPanel.add(spinnerPanel, BorderLayout.CENTER);
        dailyLimitPanel.add(alertsEnabledButton, BorderLayout.EAST);

        // App limits section
        JPanel appLimitsPanel = new JPanel(new BorderLayout(10, 10));
        appLimitsPanel.setBorder(BorderFactory.createTitledBorder("Application Limits"));

        // Table for app limits
        String[] columnNames = { "Application", "Limit (min)", "Used (min)", "Remaining (min)", "Actions" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COLUMN_ACTIONS; // Only allow editing the actions column
            }
        };

        appLimitsTable = new JTable(tableModel);
        appLimitsTable.getColumnModel().getColumn(COLUMN_ACTIONS).setCellRenderer(new ButtonRenderer());
        appLimitsTable.getColumnModel().getColumn(COLUMN_ACTIONS).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane tableScrollPane = new JScrollPane(appLimitsTable);
        appLimitsPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Add app limit controls
        JPanel addLimitPanel = new JPanel(new BorderLayout(5, 0));
        addLimitPanel.setBorder(BorderFactory.createTitledBorder("Add New Limit"));

        JPanel addControlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        addControlsPanel.add(new JLabel("Application: "));
        appComboBox = new JComboBox<>();
        updateAppComboBox();
        appComboBox.setPreferredSize(new Dimension(300, 25));
        addControlsPanel.add(appComboBox);

        addControlsPanel.add(new JLabel("Limit (minutes): "));
        SpinnerNumberModel appLimitModel = new SpinnerNumberModel(60, 5, 1440, 5);
        appLimitSpinner = new JSpinner(appLimitModel);
        addControlsPanel.add(appLimitSpinner);

        JButton addButton = new JButton("Add Limit");
        addButton.addActionListener(this::addAppLimit);

        addLimitPanel.add(addControlsPanel, BorderLayout.CENTER);
        addLimitPanel.add(addButton, BorderLayout.EAST);

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(dailyLimitPanel, BorderLayout.NORTH);
        mainPanel.add(appLimitsPanel, BorderLayout.CENTER);
        mainPanel.add(addLimitPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void updateAppComboBox() {
        appComboBox.removeAllItems();

        // Get top apps from tracker
        ActivityTracker tracker = ActivityTracker.getInstance();
        List<Map.Entry<String, Long>> topApps = tracker.getTopApps(20);

        for (Map.Entry<String, Long> app : topApps) {
            // Skip apps that already have limits
            boolean hasLimit = false;
            for (AppLimit limit : appLimits) {
                if (limit.appName.equals(app.getKey())) {
                    hasLimit = true;
                    break;
                }
            }

            if (!hasLimit) {
                appComboBox.addItem(app.getKey());
            }
        }
    }

    private void addAppLimit(ActionEvent e) {
        String selectedApp = (String) appComboBox.getSelectedItem();
        int limitMinutes = (int) appLimitSpinner.getValue();

        if (selectedApp != null && !selectedApp.isEmpty()) {
            // Create a new app limit
            AppLimit limit = new AppLimit(selectedApp, limitMinutes);
            appLimits.add(limit);

            // Update table
            updateLimitsTable();

            // Update app combo box
            updateAppComboBox();
        }
    }

    private void removeAppLimit(String appName) {
        // Remove from the limits list
        appLimits.removeIf(limit -> limit.appName.equals(appName));

        // Update the table
        updateLimitsTable();

        // Update app combo box
        updateAppComboBox();
    }

    private void updateLimitsTable() {
        // Clear table
        tableModel.setRowCount(0);

        // Get current app usage
        Map<String, Long> appUsageTimes = ActivityTracker.getInstance().getAppUsageTimes();

        for (AppLimit limit : appLimits) {
            // Calculate used time
            long usedMillis = appUsageTimes.getOrDefault(limit.appName, 0L);
            int usedMinutes = (int) (usedMillis / (1000 * 60));

            // Calculate remaining time
            int remainingMinutes = limit.limitMinutes - usedMinutes;
            if (remainingMinutes < 0)
                remainingMinutes = 0;

            // Add row to table
            Object[] rowData = {
                    limit.appName,
                    limit.limitMinutes,
                    usedMinutes,
                    remainingMinutes,
                    "Remove"
            };

            tableModel.addRow(rowData);
        }
    }

    private void toggleAlerts(ActionEvent e) {
        alertsEnabled = alertsEnabledButton.isSelected();

        if (alertsEnabled) {
            alertsEnabledButton.setText("Disable Alerts");
        } else {
            alertsEnabledButton.setText("Enable Alerts");
        }
    }

    private void startLimitChecking() {
        if (checkTimer != null && checkTimer.isRunning()) {
            checkTimer.stop();
        }

        // Check limits every 30 seconds
        checkTimer = new Timer(30000, e -> checkLimits());
        checkTimer.start();
    }

    private void checkLimits() {
        if (!alertsEnabled) {
            return;
        }

        // Check daily limit
        ActivityTracker tracker = ActivityTracker.getInstance();
        long totalMillis = tracker.getTotalScreenTime();
        int totalMinutes = (int) (totalMillis / (1000 * 60));

        // Check if approaching or exceeding daily limit
        if (totalMinutes >= dailyLimitMinutes) {
            showDailyLimitAlert(totalMinutes, dailyLimitMinutes);
        } else if (totalMinutes >= dailyLimitMinutes * 0.9) { // 90% of limit
            showDailyLimitWarning(totalMinutes, dailyLimitMinutes);
        }

        // Check app limits
        Map<String, Long> appUsageTimes = tracker.getAppUsageTimes();

        for (AppLimit limit : appLimits) {
            long usedMillis = appUsageTimes.getOrDefault(limit.appName, 0L);
            int usedMinutes = (int) (usedMillis / (1000 * 60));

            // Check if exceeding app limit
            if (usedMinutes >= limit.limitMinutes) {
                showAppLimitAlert(limit.appName, usedMinutes, limit.limitMinutes);
            } else if (usedMinutes >= limit.limitMinutes * 0.9) { // 90% of limit
                showAppLimitWarning(limit.appName, usedMinutes, limit.limitMinutes);
            }
        }

        // Update table data
        updateLimitsTable();
    }

    private void showDailyLimitWarning(int used, int limitMinutes) {
        if (!isDailyLimitWarningNotified) {
            JOptionPane.showMessageDialog(
                    this,
                    "You're approaching your daily screen time limit.\n" +
                            "Used: " + used + " minutes out of " + limitMinutes + " minutes.",
                    "Daily Limit Warning",
                    JOptionPane.WARNING_MESSAGE);
            isDailyLimitWarningNotified = true;
        }
    }

    private void showDailyLimitAlert(int used, int limitMinutes) {
        if (!isDailyLimitAlertNotified) {
            JOptionPane.showMessageDialog(
                    this,
                    "You've exceeded your daily screen time limit!\n" +
                            "Used: " + used + " minutes out of " + limitMinutes + " minutes.",
                    "Daily Limit Exceeded",
                    JOptionPane.ERROR_MESSAGE);
            isDailyLimitAlertNotified = true;
        }
    }

    private void showAppLimitWarning(String appName, int used, int limit) {
        for (AppLimit appLimit : appLimits) {
            if (appLimit.appName.equals(appName) && !appLimit.isNotifiedWarning) {
                JOptionPane.showMessageDialog(
                        this,
                        "You're approaching your limit for " + appName + ".\n" +
                                "Used: " + used + " minutes out of " + limit + " minutes.",
                        "App Limit Warning",
                        JOptionPane.WARNING_MESSAGE);
                appLimit.isNotifiedWarning = true;
                break;
            }
        }
    }

    private void showAppLimitAlert(String appName, int used, int limit) {
        for (AppLimit appLimit : appLimits) {
            if (appLimit.appName.equals(appName) && !appLimit.isNotifiedAlert) {
                JOptionPane.showMessageDialog(
                        this,
                        "You've exceeded your limit for " + appName + "!\n" +
                                "Used: " + used + " minutes out of " + limit + " minutes.",
                        "App Limit Exceeded",
                        JOptionPane.ERROR_MESSAGE);
                appLimit.isNotifiedAlert = true;
                break;
            }
        }
    }

    // ActivityTracker.AppUsageListener implementation
    @Override
    public void onAppChanged(String appName) {
        // Not needed for this panel
    }

    @Override
    public void onAppTimeUpdated(String appName, long timeInMillis) {
        // Update the limits table periodically
        if (timeInMillis % 60000 < 1000) { // Every minute
            updateLimitsTable();
        }
    }

    // Inner class for app limits
    private class AppLimit {
        String appName;
        int limitMinutes;
        boolean isNotifiedWarning = false;
        boolean isNotifiedAlert = false;

        AppLimit(String appName, int limitMinutes) {
            this.appName = appName;
            this.limitMinutes = limitMinutes;
        }
    }

    // Button renderer for the "Remove" button in the table
    private class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value.toString());
            return this;
        }
    }

    // Button editor for the "Remove" button in the table
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = value.toString();
            button.setText(label);
            currentRow = row;
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            isPushed = false;
            if ("Remove".equals(label)) {
                // Get the app name from the current row
                String appName = (String) tableModel.getValueAt(currentRow, COLUMN_APP);
                SwingUtilities.invokeLater(() -> removeAppLimit(appName));
            }
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}