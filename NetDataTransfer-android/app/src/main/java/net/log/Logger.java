package net.log;

import android.util.Log;

import net.app.netdatatransfer.BuildConfig;

// 日志统一打印管理的工具类
public class Logger {
    // log开关
    public static boolean DEBUG = true;

    public static void info(String className, String info) {
        if (BuildConfig.DEBUG_LOG) {
            Log.i(className, "xyz:::" + info);
        }
    }

    public static void error(String className, String info) {
        if (DEBUG) {
            Log.e(className, "xyz:::" + info);
        }
    }
}
