// app/src/test/java/android/util/Log.java
package android.util;

/**
 * Mock implementation of Android's Log class for unit tests.
 * This avoids errors when Log methods are called during unit tests.
 */
public class Log {
    public static int d(String tag, String msg) {
        System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    public static int e(String tag, String msg) {
        System.err.println("ERROR: " + tag + ": " + msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        System.err.println("ERROR: " + tag + ": " + msg);
        tr.printStackTrace();
        return 0;
    }
}