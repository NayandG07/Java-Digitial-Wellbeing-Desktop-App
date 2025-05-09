package com.wellbeing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {

    private DashboardPanel dashboardPanel;
    private FocusModePanel focusModePanel;
    private GoalsPanel goalsPanel;
    private BreakRemindersPanel breakRemindersPanel;
    private ReportsPanel reportsPanel;

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

        // Add tabbed pane to the frame
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resources/tray_icon.png"));

            if (image == null) {
                // Fallback if image resource not found
                image = Toolkit.getDefaultToolkit().createImage(new byte[0]);
                System.out.println("Warning: Tray icon image not found");
            }

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
}