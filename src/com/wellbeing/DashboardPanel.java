package com.wellbeing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel implements ActivityTracker.AppUsageListener {

    private JLabel totalTimeLabel;
    private JLabel currentAppLabel;
    private JPanel topAppsPanel;
    private ChartPanel pieChartPanel;

    public DashboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents();

        // Register as listener for app usage updates
        ActivityTracker.getInstance().addListener(this);

        // Initial update
        updateDashboard();
    }

    private void initComponents() {
        // Top panel with total time and current app
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // Total screen time section
        JPanel totalTimePanel = new JPanel(new BorderLayout());
        totalTimePanel.setBorder(BorderFactory.createTitledBorder("Total Screen Time Today"));
        totalTimeLabel = new JLabel("0h 0m", JLabel.CENTER);
        totalTimeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        totalTimePanel.add(totalTimeLabel, BorderLayout.CENTER);

        // Current application section
        JPanel currentAppPanel = new JPanel(new BorderLayout());
        currentAppPanel.setBorder(BorderFactory.createTitledBorder("Current Application"));
        currentAppLabel = new JLabel("", JLabel.CENTER);
        currentAppLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        currentAppPanel.add(currentAppLabel, BorderLayout.CENTER);

        // Add to top panel
        topPanel.add(totalTimePanel, BorderLayout.CENTER);
        topPanel.add(currentAppPanel, BorderLayout.EAST);

        // Top apps panel
        topAppsPanel = new JPanel();
        topAppsPanel.setLayout(new BoxLayout(topAppsPanel, BoxLayout.Y_AXIS));
        topAppsPanel.setBorder(BorderFactory.createTitledBorder("Top Applications"));

        // Chart panel
        pieChartPanel = new ChartPanel();
        pieChartPanel.setBorder(BorderFactory.createTitledBorder("Usage Distribution"));

        // Add components to main panel
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        centerPanel.add(topAppsPanel);
        centerPanel.add(pieChartPanel);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void updateDashboard() {
        ActivityTracker tracker = ActivityTracker.getInstance();

        // Update total time
        long totalMillis = tracker.getTotalScreenTime();
        long hours = totalMillis / (1000 * 60 * 60);
        long minutes = (totalMillis % (1000 * 60 * 60)) / (1000 * 60);
        totalTimeLabel.setText(hours + "h " + minutes + "m");

        // Update top apps
        topAppsPanel.removeAll();
        List<Map.Entry<String, Long>> topApps = tracker.getTopApps(5);

        for (Map.Entry<String, Long> app : topApps) {
            JPanel appPanel = createAppPanel(app.getKey(), app.getValue());
            topAppsPanel.add(appPanel);
            topAppsPanel.add(Box.createVerticalStrut(5));
        }

        // Update chart
        pieChartPanel.updateChart(tracker.getAppUsageTimes());

        // Refresh UI
        revalidate();
        repaint();
    }

    private JPanel createAppPanel(String appName, long timeInMillis) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Format time
        long hours = timeInMillis / (1000 * 60 * 60);
        long minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (timeInMillis % (1000 * 60)) / 1000;

        String timeText = (hours > 0 ? hours + "h " : "") +
                (minutes > 0 ? minutes + "m " : "") +
                seconds + "s";

        // App name (truncate if too long)
        String displayName = appName.length() > 50 ? appName.substring(0, 47) + "..." : appName;
        JLabel nameLabel = new JLabel(displayName);

        // Time label
        JLabel timeLabel = new JLabel(timeText);
        timeLabel.setHorizontalAlignment(JLabel.RIGHT);

        // Progress bar showing percentage of total time
        long totalTime = ActivityTracker.getInstance().getTotalScreenTime();
        int percentage = totalTime > 0 ? (int) ((timeInMillis * 100) / totalTime) : 0;

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(percentage);
        progressBar.setStringPainted(true);
        progressBar.setString(percentage + "%");

        // Assemble panel
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.add(nameLabel, BorderLayout.CENTER);
        topPanel.add(timeLabel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);

        return panel;
    }

    // ActivityTracker.AppUsageListener implementation
    @Override
    public void onAppChanged(String appName) {
        currentAppLabel.setText(appName);
    }

    @Override
    public void onAppTimeUpdated(String appName, long timeInMillis) {
        // Periodically update the dashboard (not on every update to avoid UI flicker)
        if (timeInMillis % 5000 < 1000) {
            updateDashboard();
        }
    }

    // Simple chart panel for displaying usage distribution
    private class ChartPanel extends JPanel {
        private Map<String, Long> data;
        private final Color[] COLORS = {
                new Color(66, 133, 244), // Google Blue
                new Color(219, 68, 55), // Google Red
                new Color(244, 180, 0), // Google Yellow
                new Color(15, 157, 88), // Google Green
                new Color(171, 71, 188), // Purple
                new Color(0, 172, 193), // Teal
                new Color(255, 87, 34), // Deep Orange
                Color.LIGHT_GRAY // Other
        };

        public ChartPanel() {
            setPreferredSize(new Dimension(300, 300));
        }

        public void updateChart(Map<String, Long> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (data == null || data.isEmpty()) {
                g.setColor(Color.LIGHT_GRAY);
                g.drawString("No data available", getWidth() / 2 - 50, getHeight() / 2);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate total time
            long total = data.values().stream().mapToLong(Long::longValue).sum();
            if (total <= 0)
                return;

            // Get top N apps for the pie chart
            List<Map.Entry<String, Long>> topApps = ActivityTracker.getInstance().getTopApps(7);

            // Find the center and radius
            int width = getWidth();
            int height = getHeight();
            int x = width / 2;
            int y = height / 2;
            int radius = Math.min(width, height) / 2 - 30;

            // Draw pie chart
            int startAngle = 0;
            int colorIndex = 0;
            DecimalFormat df = new DecimalFormat("0.#");

            for (Map.Entry<String, Long> entry : topApps) {
                if (colorIndex >= COLORS.length - 1)
                    break;

                long value = entry.getValue();
                int arcAngle = (int) (360.0 * value / total);

                if (arcAngle > 0) {
                    g2d.setColor(COLORS[colorIndex]);
                    g2d.fillArc(x - radius, y - radius, radius * 2, radius * 2, startAngle, arcAngle);

                    // Draw label
                    double radians = Math.toRadians(startAngle + arcAngle / 2);
                    int labelX = x + (int) ((radius + 20) * Math.cos(radians));
                    int labelY = y + (int) ((radius + 20) * Math.sin(radians));

                    String percentage = df.format(100.0 * value / total) + "%";
                    g2d.drawString(percentage, labelX - 15, labelY);

                    startAngle += arcAngle;
                    colorIndex++;
                }
            }

            // Draw legend
            int legendX = 10;
            int legendY = height - 10;

            for (int i = topApps.size() - 1; i >= 0; i--) {
                if (i >= COLORS.length - 1)
                    continue;

                Map.Entry<String, Long> entry = topApps.get(i);
                String appName = entry.getKey();

                // Truncate app name if too long
                if (appName.length() > 20) {
                    appName = appName.substring(0, 17) + "...";
                }

                g2d.setColor(COLORS[i]);
                g2d.fillRect(legendX, legendY - 10, 10, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawString(appName, legendX + 15, legendY);

                legendY -= 20;
                if (legendY < 20)
                    break; // Prevent drawing outside the panel
            }
        }
    }
}