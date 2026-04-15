package com.mycompany.app;

public class TimerHelper {
    private static final MeasurementBuffer measurementBuffer = new MeasurementBuffer();
    
    static {
        System.out.println("TimerHelper class loaded by: " + TimerHelper.class.getClassLoader());
    }
    
    public static void init() {
        measurementBuffer.init();
    }
    
    /**
     * Starts a timer and returns the start time in nanoseconds
     */
    public static long startTimer() {
        return System.nanoTime();
    }
    
    /**
     * Ends the timer and records the measurement
     * @param startTimeNanos The start time returned by startTimer()
     * @param className The class name in slash format (e.g., "com/sample/app/App")
     * @param methodName The method name
     * @param descriptor The method descriptor (e.g., "(Ljava/lang/String;)V")
     */
    public static void endTimerAndRecord(long startTimeNanos, String className, String methodName, String descriptor) {
        long endTimeNanos = System.nanoTime();
        long executionTimeNanos = endTimeNanos - startTimeNanos;

        // Convert slash format to dot format for class name
        String classNameDots = className.replace('/', '.');

        measurementBuffer.addMeasurement(classNameDots, methodName, descriptor, executionTimeNanos);
    }
    
    /**
     * Shuts down the measurement buffer and flushes remaining measurements
     */
    public static void shutdown() {
        measurementBuffer.shutdown();
    }
}