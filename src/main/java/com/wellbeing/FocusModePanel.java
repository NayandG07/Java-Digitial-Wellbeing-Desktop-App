package com.wellbeing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FocusModePanel extends JPanel implements ActivityTracker.AppUsageListener {

    private JList<String> appList;
    private DefaultListModel<String> appListModel;
    private JToggleButton focusModeButton;
    private JLabel statusLabel;
    private JTextField searchField;
    private JSpinner durationSpinner;
    private Set<String> blockedApps;
    private Timer notificationTimer;
    private boolean focusModeActive = false;

    public FocusModePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        blockedApps = new HashSet<>();

        initComponents();

        // Register as listener for activity tracking
        ActivityTracker.getInstance().addListener(this);

        // Populate apps list
        updateAppsList();
    }

    private void initComponents() {
        // Top section with search field
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        searchField = new JTextField();
        searchField.setToolTipText("Search applications");
        searchField.addActionListener(e -> filterAppList());

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> filterAppList());

        topPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchButton, BorderLayout.EAST);

        // Center section with apps list
        appListModel = new DefaultListModel<>();
        appList = new JList<>(appListModel);
        appList.setCellRenderer(new AppListCellRenderer());
        appList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane scrollPane = new JScrollPane(appList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Select Apps to Block"));

        // Bottom section with controls
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Duration spinner
        JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        durationPanel.add(new JLabel("Focus Duration (minutes): "));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(25, 5, 120, 5);
        durationSpinner = new JSpinner(spinnerModel);
        durationPanel.add(durationSpinner);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton blockButton = new JButton("Block Selected");
        blockButton.addActionListener(this::blockSelectedApps);

        JButton unblockButton = new JButton("Unblock Selected");
        unblockButton.addActionListener(this::unblockSelectedApps);

        focusModeButton = new JToggleButton("Start Focus Mode");
        focusModeButton.addActionListener(this::toggleFocusMode);

        buttonPanel.add(blockButton);
        buttonPanel.add(unblockButton);
        buttonPanel.add(focusModeButton);

        // Status label
        statusLabel = new JLabel("Focus Mode: Inactive");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        // Assemble bottom panel
        bottomPanel.add(durationPanel, BorderLayout.WEST);
        bottomPanel.add(statusLabel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        // Add components to main panel
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateAppsList() {
        appListModel.clear();
        Map<String, Long> appUsageTimes = ActivityTracker.getInstance().getAppUsageTimes();

        // Add apps to the list sorted by usage time
        appUsageTimes.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> appListModel.addElement(entry.getKey()));
    }

    private void filterAppList() {
        String searchTerm = searchField.getText().toLowerCase().trim();

        if (searchTerm.isEmpty()) {
            updateAppsList();
            return;
        }

        // Filter apps by search term
        appListModel.clear();
        Map<String, Long> appUsageTimes = ActivityTracker.getInstance().getAppUsageTimes();

        appUsageTimes.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().contains(searchTerm))
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> appListModel.addElement(entry.getKey()));
    }

    private void blockSelectedApps(ActionEvent e) {
        for (String app : appList.getSelectedValuesList()) {
            blockedApps.add(app);
        }
        appList.repaint();
    }

    private void unblockSelectedApps(ActionEvent e) {
        for (String app : appList.getSelectedValuesList()) {
            blockedApps.remove(app);
        }
        appList.repaint();
    }

    private void toggleFocusMode(ActionEvent e) {
        focusModeActive = focusModeButton.isSelected();

        if (focusModeActive) {
            // Start focus mode
            focusModeButton.setText("Stop Focus Mode");
            statusLabel.setText("Focus Mode: Active");

            // Start notification timer
            int minutes = (Integer) durationSpinner.getValue();
            if (notificationTimer != null && notificationTimer.isRunning()) {
                notificationTimer.stop();
            }

            // Timer to check for blocked apps
            notificationTimer = new Timer(1000, evt -> checkForBlockedApps());
            notificationTimer.start();

            // Timer to end focus mode
            Timer endTimer = new Timer(minutes * 60 * 1000, evt -> {
                focusModeButton.setSelected(false);
                toggleFocusMode(null);
                JOptionPane.showMessageDialog(
                        this,
                        "Focus session completed! Good job staying focused for " + minutes + " minutes.",
                        "Focus Session Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            });
            endTimer.setRepeats(false);
            endTimer.start();
        } else {
            // End focus mode
            focusModeButton.setText("Start Focus Mode");
            statusLabel.setText("Focus Mode: Inactive");

            if (notificationTimer != null && notificationTimer.isRunning()) {
                notificationTimer.stop();
            }
        }
    }

    private void checkForBlockedApps() {
        if (!focusModeActive || blockedApps.isEmpty()) {
            return;
        }

        String currentApp = ActivityTracker.getInstance().getCurrentApp();
        if (currentApp != null && blockedApps.contains(currentApp)) {
            showBlockNotification(currentApp);
        }
    }

    private void showBlockNotification(String appName) {
        // Show a non-blocking dialog
        JDialog dialog = new JDialog();
        dialog.setTitle("Focus Mode Alert");
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel messageLabel = new JLabel("<html><b>Attention!</b><br><br>" +
                "You're trying to use <b>" + appName + "</b>, which is on your block list.<br><br>" +
                "Remember your focus goals!</html>");

        JButton dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(e -> dialog.dispose());

        panel.add(messageLabel, BorderLayout.CENTER);
        panel.add(dismissButton, BorderLayout.SOUTH);

        dialog.add(panel);
        dialog.setVisible(true);

        // Auto-close after 5 seconds
        Timer autoCloseTimer = new Timer(5000, e -> dialog.dispose());
        autoCloseTimer.setRepeats(false);
        autoCloseTimer.start();
    }

    // CustomListCellRenderer to show blocked apps differently
    private class AppListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            String app = (String) value;

            if (blockedApps.contains(app)) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));

                if (!isSelected) {
                    label.setForeground(new Color(255, 0, 0));
                }

                label.setText(app + " (Blocked)");
            }

            return label;
        }
    }

    // ActivityTracker.AppUsageListener implementation
    @Override
    public void onAppChanged(String appName) {
        if (focusModeActive && blockedApps.contains(appName)) {
            showBlockNotification(appName);
        }
    }

    @Override
    public void onAppTimeUpdated(String appName, long timeInMillis) {
        // Periodically update the apps list (e.g., every 5 minutes)
        if (timeInMillis % (5 * 60 * 1000) < 1000) {
            updateAppsList();
        }
    }

    public void searchApplications(String searchTerm) {
        searchField.setText(searchTerm);
        filterAppList();
    }
}