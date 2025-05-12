package com.wellbeing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class MainFrame extends JFrame {

    private DashboardPanel dashboardPanel;
    private FocusModePanel focusModePanel;
    private GoalsPanel goalsPanel;
    private BreakRemindersPanel breakRemindersPanel;
    private ReportsPanel reportsPanel;
    private JTextField searchField;
    private JTabbedPane tabbedPane;

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
        // Create main panels
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Create search panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");

        searchPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Add search functionality
        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());

        // Create tab panels
        dashboardPanel = new DashboardPanel();
        focusModePanel = new FocusModePanel();
        goalsPanel = new GoalsPanel();
        breakRemindersPanel = new BreakRemindersPanel();
        reportsPanel = new ReportsPanel();

        // Create tabbed pane and add panels
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Dashboard", new ImageIcon(), dashboardPanel, "View your usage statistics");
        tabbedPane.addTab("Focus Mode", new ImageIcon(), focusModePanel, "Block distracting applications");
        tabbedPane.addTab("Goals & Alerts", new ImageIcon(), goalsPanel, "Set screen time limits");
        tabbedPane.addTab("Break Reminders", new ImageIcon(), breakRemindersPanel, "Configure break reminders");
        tabbedPane.addTab("Reports", new ImageIcon(), reportsPanel, "View usage reports");

        // Add components to main panel
        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Add main panel to the frame
        getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    private void performSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            return;
        }

        // Search logic depends on selected tab
        int currentTab = tabbedPane.getSelectedIndex();
        switch (currentTab) {
            case 0: // Dashboard
                searchInDashboard(searchTerm);
                break;
            case 1: // Focus Mode
                searchInFocusMode(searchTerm);
                break;
            case 2: // Goals & Alerts
                searchInGoals(searchTerm);
                break;
            case 3: // Break Reminders
                searchInBreakReminders(searchTerm);
                break;
            case 4: // Reports
                searchInReports(searchTerm);
                break;
        }
    }

    private void searchInDashboard(String searchTerm) {
        // Search in dashboard - typically focuses on app names in usage statistics
        JOptionPane.showMessageDialog(this,
                "Searching for '" + searchTerm + "' in Dashboard",
                "Search Results",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void searchInFocusMode(String searchTerm) {
        // Use the existing search function in focus mode
        focusModePanel.searchApplications(searchTerm);
    }

    private void searchInGoals(String searchTerm) {
        // Search in goals panel
        JOptionPane.showMessageDialog(this,
                "Searching for '" + searchTerm + "' in Goals & Alerts",
                "Search Results",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void searchInBreakReminders(String searchTerm) {
        // Search in break reminders panel
        JOptionPane.showMessageDialog(this,
                "Searching for '" + searchTerm + "' in Break Reminders",
                "Search Results",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void searchInReports(String searchTerm) {
        // Search in reports panel
        JOptionPane.showMessageDialog(this,
                "Searching for '" + searchTerm + "' in Reports",
                "Search Results",
                JOptionPane.INFORMATION_MESSAGE);
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