package com.wellbeing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private DashboardPanel dashboardPanel;
    private FocusModePanel focusModePanel;
    private GoalsPanel goalsPanel;
    private BreakRemindersPanel breakRemindersPanel;
    private ReportsPanel reportsPanel;
    private JTextField searchField;
    private JButton searchButton;

    public MainFrame() {
        // Setup the frame
        setTitle("Digital Wellbeing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 500));

        initComponents();

        // Create system tray icon to keep app running in background
        setupSystemTray();

        // Add window listener to handle minimize instead of close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    private void initComponents() {
        // Create tab panels
        dashboardPanel = new DashboardPanel();
        focusModePanel = new FocusModePanel();
        goalsPanel = new GoalsPanel();
        breakRemindersPanel = new BreakRemindersPanel();
        reportsPanel = new ReportsPanel();

        // Create tabbed pane and add panels
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Dashboard", new ImageIcon(), dashboardPanel, "View your usage statistics");
        tabbedPane.addTab("Focus Mode", new ImageIcon(), focusModePanel, "Block distracting applications");
        tabbedPane.addTab("Goals & Alerts", new ImageIcon(), goalsPanel, "Set screen time limits");
        tabbedPane.addTab("Break Reminders", new ImageIcon(), breakRemindersPanel, "Configure break reminders");
        tabbedPane.addTab("Reports", new ImageIcon(), reportsPanel, "View usage reports");

        // Create search panel at the top
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel searchLabel = new JLabel("Search: ");
        searchField = new JTextField();
        searchButton = new JButton("Search");

        // Set preferred width for search components
        searchField.setPreferredSize(new Dimension(250, 25));
        searchButton.setPreferredSize(new Dimension(80, 25));

        // Add action to search button
        searchButton.addActionListener(e -> performSearch());

        // Add action to search field (press Enter to search)
        searchField.addActionListener(e -> performSearch());

        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Add components to the frame
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        getContentPane().add(mainPanel);
    }

    private void performSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a search term",
                    "Search",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Get activity data from tracker
        ActivityTracker tracker = ActivityTracker.getInstance();
        Map<String, Long> appUsage = tracker.getAppUsageTimes();

        if (appUsage.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No application usage data available yet.\nUse the app for a while to track application usage.",
                    "No Data Available",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Find matches
        List<String> matchingApps = new ArrayList<>();
        for (String app : appUsage.keySet()) {
            if (app.toLowerCase().contains(searchTerm)) {
                matchingApps.add(app);
            }
        }

        // Display results
        if (matchingApps.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No applications matching '" + searchTerm + "' found",
                    "Search Results",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            StringBuilder results = new StringBuilder();
            results.append("Found ").append(matchingApps.size()).append(" applications matching '")
                    .append(searchTerm).append("':\n\n");

            for (String app : matchingApps) {
                long millis = appUsage.get(app);
                long hours = millis / (1000 * 60 * 60);
                long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
                long seconds = (millis % (1000 * 60)) / 1000;

                String timeUsed = (hours > 0 ? hours + "h " : "") +
                        (minutes > 0 ? minutes + "m " : "") +
                        seconds + "s";

                results.append(app).append(": ").append(timeUsed).append("\n");
            }

            JTextArea textArea = new JTextArea(results.toString());
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            JOptionPane.showMessageDialog(this,
                    scrollPane,
                    "Search Results",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Create a simple tray icon
            Image image = createTrayIconImage();

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            });
            popup.add(openItem);

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                ActivityTracker.getInstance().stopTracking();
                System.exit(0);
            });
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Digital Wellbeing", popup);
            trayIcon.setImageAutoSize(true);

            trayIcon.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            });

            tray.add(trayIcon);

        } catch (Exception e) {
            System.out.println("TrayIcon could not be added: " + e.getMessage());
        }
    }

    private Image createTrayIconImage() {
        // Create a simple 16x16 icon
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // Draw a simple blue circle as the icon
        g2d.setColor(new Color(0, 120, 212));
        g2d.fillOval(0, 0, 16, 16);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(0, 0, 15, 15);

        g2d.dispose();
        return image;
    }
}