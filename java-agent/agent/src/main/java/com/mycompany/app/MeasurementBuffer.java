package com.mycompany.app;

import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MeasurementBuffer {
    private static final int BUFFER_CAPACITY = 10000;
    private static final String CSV_FILE = "measurements.csv";
    private static final long FLUSH_INTERVAL_SECONDS = 5;
    
    private final BlockingQueue<MethodMeasurement> buffer;
    private final ScheduledExecutorService scheduler;
    private boolean initialized = false;
    
    static {
        System.out.println("MeasurementBuffer class loaded by: " + MeasurementBuffer.class.getClassLoader());
    }
    
    public MeasurementBuffer() {
        this.buffer = new LinkedBlockingQueue<>(BUFFER_CAPACITY);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }
    
    public void init() {
        if (initialized) {
            return;
        }
        
        // Initialize CSV file with headers
        initializeCsvFile();
        
        // Start periodic flush
        scheduler.scheduleAtFixedRate(
            this::flushToCsv,
            FLUSH_INTERVAL_SECONDS,
            FLUSH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        initialized = true;
        System.out.println("MeasurementBuffer initialized with " + FLUSH_INTERVAL_SECONDS + "s flush interval");
    }
    
    public void addMeasurement(String className, String methodName, String descriptor, long executionTimeNanos) {
        try {
            MethodMeasurement measurement = new MethodMeasurement(
                className,
                methodName,
                descriptor,
                System.currentTimeMillis(),
                executionTimeNanos
            );
            
            boolean offered = buffer.offer(measurement);
            if (!offered) {
                System.err.println("MeasurementBuffer full - dropping measurement for " + className + "." + methodName);
            }
        } catch (Exception e) {
            System.err.println("Error adding measurement: " + e.getMessage());
        }
    }

    private void initializeCsvFile() {
        try (FileWriter writer = new FileWriter(CSV_FILE, false)) {
            writer.write("className,methodName,descriptor,timestamp,executionTimeNanos,executionTimeMs\n");
            writer.flush();
            System.out.println("Created CSV file: " + CSV_FILE);
        } catch (IOException e) {
            System.err.println("Error initializing CSV file: " + e.getMessage());
        }
    }

    private void flushToCsv() {
        int count = 0;
        try (FileWriter writer = new FileWriter(CSV_FILE, true)) {
            while (!buffer.isEmpty()) {
                MethodMeasurement measurement = buffer.poll();
                if (measurement != null) {
                    writer.write(measurement.toCsvRow());
                    count++;
                }
            }
            writer.flush();
            if (count > 0) {
                System.out.println("Flushed " + count + " measurements to " + CSV_FILE);
            }
        } catch (IOException e) {
            System.err.println("Error flushing to CSV: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        // Flush remaining measurements before shutdown
        flushToCsv();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private static class MethodMeasurement {
        private final String className;
        private final String methodName;
        private final String descriptor;
        private final long timestamp;
        private final long executionTimeNanos;
        
        public MethodMeasurement(String className, String methodName, String descriptor, 
                                long timestamp, long executionTimeNanos) {
            this.className = className;
            this.methodName = methodName;
            this.descriptor = descriptor;
            this.timestamp = timestamp;
            this.executionTimeNanos = executionTimeNanos;
        }
        
        public String toCsvRow() {
            double executionTimeMs = executionTimeNanos / 1_000_000.0;
            return String.format("%s,%s,%s,%d,%d,%.6f\n",
                escapeCsv(className),
                escapeCsv(methodName),
                escapeCsv(descriptor),
                timestamp,
                executionTimeNanos,
                executionTimeMs
            );
        }
        
        private String escapeCsv(String value) {
            if (value == null) {
                return "";
            }
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                return "\"" + value.replace("\"", "\"\"") + "\"";
            }
            return value;
        }
    }
}