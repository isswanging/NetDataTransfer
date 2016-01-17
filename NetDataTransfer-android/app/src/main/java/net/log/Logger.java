package net.log;

import android.util.Log;

// 日志统一打印管理的工具类
public class Logger {
    // log开关
    public static boolean DEBUG = true;

    public static void info(String className, String info) {
        if (DEBUG) {
            Log.i(className, "xyz:::" + info);
        }
    }

    public static void error(String className, String info) {
        if (DEBUG) {
            Log.e(className, "xyz:::" + info);
        }
    }
}
