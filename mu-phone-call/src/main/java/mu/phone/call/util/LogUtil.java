package mu.phone.call.util;

import android.util.Log;

public class LogUtil {
    public static final String TAG = "ZZMM";

    public static void d(String tag, String content) {
        Log.d(tag + TAG, content);
    }

    public static void i(String tag, String content) {
        Log.i(tag + TAG, content);
    }

    public static void e(String tag, String content) {
        Log.e(tag + TAG, content);
    }

    public static void w(String tag, String content) {
        Log.w(tag + TAG, content);
    }
}
