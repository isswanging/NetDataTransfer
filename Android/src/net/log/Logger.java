package net.log;

import android.util.Log;

// 日志统一打印管理的工具类
public class Logger {
    // log开关
    static boolean tag = true;

    public static void info(String className, String info) {
        if (tag) {
            Log.i(className, info);
        }
    }

    public static void error(String className, String info) {
        if (tag) {
            Log.e(className, info);
        }
    }
}
