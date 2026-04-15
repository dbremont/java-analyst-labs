package com.sample.app;

import java.math.BigInteger;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws InterruptedException{
        System.out.println("Starting timing test...");

        // Test various methods
        fastMethod();
        slowMethod();
        verySlowMethod();
        calculateFactorial(10);

        // Let the buffer flush
        Thread.sleep(6000);
        
        System.out.println("Test complete. Check measurements.csv for results.");
    }
    
    private static void fastMethod() {
        int sum = 0;
        for (int i = 0; i < 100; i++) {
            sum += i;
        }
    }
    
    private static void slowMethod() throws InterruptedException {
        Thread.sleep(100);
        int result = 0;
        for (int i = 0; i < 1000; i++) {
            result += i * i;
        }
    }

    private static void verySlowMethod() throws InterruptedException {
        Thread.sleep(500);
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= 20; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
    }

    private static BigInteger calculateFactorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }
}