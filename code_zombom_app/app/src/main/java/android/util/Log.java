package android.util;

/**
 * Test stub for android.util.Log so local unit tests
 * can call Log.* without Robolectric / Android runtime.
 * This lives ONLY in src/test/java.
 */
public class Log {

    public static int e(String tag, String msg) {
        System.out.println("LOG_E: " + tag + " - " + msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        System.out.println("LOG_E: " + tag + " - " + msg + " - " + tr);
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("LOG_W: " + tag + " - " + msg);
        return 0;
    }

    public static int w(String tag, String msg, Throwable tr) {
        System.out.println("LOG_W: " + tag + " - " + msg + " - " + tr);
        return 0;
    }

    public static int d(String tag, String msg) {
        System.out.println("LOG_D: " + tag + " - " + msg);
        return 0;
    }

    public static int d(String tag, String msg, Throwable tr) {
        System.out.println("LOG_D: " + tag + " - " + msg + " - " + tr);
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("LOG_I: " + tag + " - " + msg);
        return 0;
    }

    public static int i(String tag, String msg, Throwable tr) {
        System.out.println("LOG_I: " + tag + " - " + msg + " - " + tr);
        return 0;
    }
}
