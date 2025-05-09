package com.wellbeing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportsPanel extends JPanel {

    private JComboBox<String> dateComboBox;
    private JTextArea reportTextArea;
    private JTable usageTable;
    private DefaultTableModel tableModel;
    private JButton exportButton;
    private JButton generateButton;
    private JLabel totalTimeLabel;

    private DataManager dataManager;
    private LocalDate selectedDate;

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        dataManager = new DataManager();
        selectedDate = LocalDate.now();

        initComponents();

        // Initial update
        populateDatesComboBox();
        updateReportData();
    }

    private void initComponents() {
        // Top panel with controls
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));

        // Date selection
        JPanel datePanel = new JPanel(new BorderLayout(5, 0));
        datePanel.add(new JLabel("Select Date: "), BorderLayout.WEST);

        dateComboBox = new JComboBox<>();
        dateComboBox.addActionListener(e -> {
            if (dateComboBox.getSelectedItem() != null) {
                String selectedItem = (String) dateComboBox.getSelectedItem();
                selectedDate = LocalDate.parse(selectedItem, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                updateReportData();
            }
        });
        datePanel.add(dateComboBox, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        generateButton = new JButton("Generate Report");
        generateButton.addActionListener(this::generateReport);

        exportButton = new JButton("Export to CSV");
        exportButton.addActionListener(this::exportReport);

        buttonPanel.add(generateButton);
        buttonPanel.add(exportButton);

        topPanel.add(datePanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        // Center panel with report content
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // Left panel with usage table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new TitledBorder("Application Usage"));

        String[] columnNames = { "Application", "Time Used", "Percentage" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        usageTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(usageTable);
        tableScrollPane.setPreferredSize(new Dimension(400, 300));

        totalTimeLabel = new JLabel("Total screen time: 0h 0m");
        totalTimeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        totalTimeLabel.setBorder(new EmptyBorder(5, 5, 5, 5));

        tablePanel.add(totalTimeLabel, BorderLayout.NORTH);
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);

        // Right panel with text report
        JPanel reportPanel = new JPanel(new BorderLayout());
        reportPanel.setBorder(new TitledBorder("Daily Report"));

        reportTextArea = new JTextArea();
        reportTextArea.setEditable(false);
        reportTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane reportScrollPane = new JScrollPane(reportTextArea);

        reportPanel.add(reportScrollPane, BorderLayout.CENTER);

        // Split pane for table and report
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, reportPanel);
        splitPane.setResizeWeight(0.5);

        centerPanel.add(splitPane, BorderLayout.CENTER);

        // Add components to main panel
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void populateDatesComboBox() {
        dateComboBox.removeAllItems();

        // Get available dates from data manager
        List<LocalDate> dates = dataManager.getAvailableDates();

        if (dates.isEmpty()) {
            // Add today if no dates available
            dateComboBox.addItem(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            // Add all available dates
            for (LocalDate date : dates) {
                dateComboBox.addItem(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }
        }
    }

    private void updateReportData() {
        // Clear table
        tableModel.setRowCount(0);

        // Load usage data for selected date
        Map<String, Long> usageData = dataManager.loadDailyUsage(selectedDate);

        if (usageData.isEmpty()) {
            reportTextArea
                    .setText("No data available for " + selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            totalTimeLabel.setText("Total screen time: 0h 0m");
            return;
        }

        // Calculate total time
        long totalMillis = usageData.values().stream().mapToLong(Long::longValue).sum();
        long hours = totalMillis / (1000 * 60 * 60);
        long minutes = (totalMillis % (1000 * 60 * 60)) / (1000 * 60);

        totalTimeLabel.setText("Total screen time: " + hours + "h " + minutes + "m");

        // Add data to table
        usageData.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    String appName = entry.getKey();
                    long millis = entry.getValue();

                    // Format time
                    long appHours = millis / (1000 * 60 * 60);
                    long appMinutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
                    long appSeconds = (millis % (1000 * 60)) / 1000;

                    String timeFormatted = (appHours > 0 ? appHours + "h " : "") +
                            (appMinutes > 0 ? appMinutes + "m " : "") +
                            appSeconds + "s";

                    // Calculate percentage
                    double percentage = totalMillis > 0 ? (millis * 100.0) / totalMillis : 0;
                    String percentFormatted = String.format("%.1f%%", percentage);

                    // Add row to table
                    Object[] rowData = { appName, timeFormatted, percentFormatted };
                    tableModel.addRow(rowData);
                });

        // Load report text if exists
        String reportFileName = "data/reports/" + selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + "_report.txt";
        File reportFile = new File(reportFileName);

        if (reportFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line).append("\n");
                }
                reportTextArea.setText(text.toString());
                reportTextArea.setCaretPosition(0);
            } catch (IOException e) {
                reportTextArea.setText("Error loading report: " + e.getMessage());
            }
        } else {
            reportTextArea.setText("No report generated for this date yet.\nClick 'Generate Report' to create one.");
        }
    }

    private void generateReport(ActionEvent e) {
        // Generate a report for the selected date
        dataManager.generateDailyReport(selectedDate);

        // Update the display
        updateReportData();

        JOptionPane.showMessageDialog(
                this,
                "Report generated for " + selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "Report Generated",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportReport(ActionEvent e) {
        // Choose file location
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save CSV Report");
        fileChooser.setSelectedFile(new File("digital_wellbeing_" +
                selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // If file doesn't have .csv extension, add it
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            // Source file
            String sourceFileName = "data/reports/" + selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    + "_report.csv";
            File sourceFile = new File(sourceFileName);

            if (!sourceFile.exists()) {
                // Generate report if it doesn't exist
                dataManager.generateDailyReport(selectedDate);
            }

            // Copy file
            try {
                Files.copy(Paths.get(sourceFileName), Paths.get(fileToSave.getAbsolutePath()));

                JOptionPane.showMessageDialog(
                        this,
                        "Report exported to " + fileToSave.getAbsolutePath(),
                        "Export Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error exporting report: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}