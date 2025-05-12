package com.wellbeing;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DataManager {
    private static final String DATA_DIR = "data";
    private static final String USAGE_DIR = DATA_DIR + "/usage";
    private static final String REPORTS_DIR = DATA_DIR + "/reports";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DataManager() {
        // Create necessary directories
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.createDirectories(Paths.get(USAGE_DIR));
            Files.createDirectories(Paths.get(REPORTS_DIR));
        } catch (IOException e) {
            System.err.println("Error creating data directories: " + e.getMessage());
        }
    }

    public void saveDailyUsage(LocalDate date, Map<String, Long> usageData) {
        String fileName = USAGE_DIR + "/" + date.format(DATE_FORMAT) + ".dat";

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(fileName))) {
            oos.writeObject(usageData);
        } catch (IOException e) {
            System.err.println("Error saving usage data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Long> loadDailyUsage(LocalDate date) {
        String fileName = USAGE_DIR + "/" + date.format(DATE_FORMAT) + ".dat";
        File file = new File(fileName);

        if (!file.exists()) {
            return new HashMap<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(fileName))) {
            return (Map<String, Long>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading usage data: " + e.getMessage());
            return new HashMap<>();
        }
    }

    public void generateDailyReport(LocalDate date) {
        Map<String, Long> usageData = loadDailyUsage(date);

        // No data available, inform the user but don't generate sample data
        if (usageData.isEmpty()) {
            System.out.println("No usage data available for " + date.format(DATE_FORMAT));
            // Create an empty report file indicating no data
            try {
                File reportFile = new File(REPORTS_DIR + "/" + date.format(DATE_FORMAT) + "_report.txt");
                if (!reportFile.exists()) {
                    try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
                        writer.println("=== Digital Wellbeing Report for " + date.format(DATE_FORMAT) + " ===");
                        writer.println();
                        writer.println("No usage data recorded for this date.");
                        writer.println("Please use the application actively to track real usage data.");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error creating empty report: " + e.getMessage());
            }
            return;
        }

        String reportFileName = REPORTS_DIR + "/" + date.format(DATE_FORMAT) + "_report.txt";
        String csvFileName = REPORTS_DIR + "/" + date.format(DATE_FORMAT) + "_report.csv";

        // Generate text report
        generateTextReport(date, usageData, reportFileName);

        // Generate CSV report
        generateCSVReport(date, usageData, csvFileName);
    }

    private void generateTextReport(LocalDate date, Map<String, Long> usageData, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("=== Digital Wellbeing Report for " + date.format(DATE_FORMAT) + " ===");
            writer.println();

            // Calculate total screen time
            long totalMillis = usageData.values().stream().mapToLong(Long::longValue).sum();
            long hours = totalMillis / (1000 * 60 * 60);
            long minutes = (totalMillis % (1000 * 60 * 60)) / (1000 * 60);

            writer.println("Total screen time: " + hours + "h " + minutes + "m");
            writer.println();
            writer.println("Application breakdown:");
            writer.println("------------------------");

            // Sort by usage time (descending)
            List<Map.Entry<String, Long>> sortedEntries = usageData.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toList());

            for (Map.Entry<String, Long> entry : sortedEntries) {
                long appMillis = entry.getValue();
                long appHours = appMillis / (1000 * 60 * 60);
                long appMinutes = (appMillis % (1000 * 60 * 60)) / (1000 * 60);
                long appSeconds = (appMillis % (1000 * 60)) / 1000;

                writer.println(entry.getKey() + ": " +
                        (appHours > 0 ? appHours + "h " : "") +
                        (appMinutes > 0 ? appMinutes + "m " : "") +
                        appSeconds + "s");
            }

        } catch (IOException e) {
            System.err.println("Error generating text report: " + e.getMessage());
        }
    }

    private void generateCSVReport(LocalDate date, Map<String, Long> usageData, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("Application,Time (ms),Time (human readable)");

            for (Map.Entry<String, Long> entry : usageData.entrySet()) {
                long appMillis = entry.getValue();
                long appHours = appMillis / (1000 * 60 * 60);
                long appMinutes = (appMillis % (1000 * 60 * 60)) / (1000 * 60);
                long appSeconds = (appMillis % (1000 * 60)) / 1000;

                String humanReadable = (appHours > 0 ? appHours + "h " : "") +
                        (appMinutes > 0 ? appMinutes + "m " : "") +
                        appSeconds + "s";

                // Make sure to handle commas in app names for CSV format
                String appName = entry.getKey().contains(",")
                        ? "\"" + entry.getKey() + "\""
                        : entry.getKey();

                writer.println(appName + "," + appMillis + "," + humanReadable);
            }

        } catch (IOException e) {
            System.err.println("Error generating CSV report: " + e.getMessage());
        }
    }

    public List<LocalDate> getAvailableDates() {
        List<LocalDate> dates = new ArrayList<>();
        File usageDir = new File(USAGE_DIR);

        if (usageDir.exists() && usageDir.isDirectory()) {
            File[] files = usageDir.listFiles((dir, name) -> name.endsWith(".dat"));

            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String dateStr = fileName.substring(0, fileName.lastIndexOf("."));

                    try {
                        LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
                        dates.add(date);
                    } catch (Exception e) {
                        // Skip files with invalid date format
                    }
                }
            }
        }

        // Sort dates from newest to oldest
        dates.sort(Comparator.reverseOrder());
        return dates;
    }
}