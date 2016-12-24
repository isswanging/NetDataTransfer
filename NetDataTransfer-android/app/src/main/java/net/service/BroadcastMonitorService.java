package net.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.alibaba.fastjson.JSON;

import net.app.NetConfApplication;
import net.log.Logger;
import net.service.base.BaseService;
import net.vo.Host;

import java.net.DatagramSocket;
import java.net.SocketException;

public class BroadcastMonitorService extends BaseService {
    private final String TAG = "BroadcastMonitorService";
    Host host;
    NetConfApplication app;
    Handler updateHandler;
    HandlerThread screenHandlerThread;
    Runnable sendSelfInfo = new Runnable() {

        @Override
        public void run() {
            Logger.info(TAG, "send broadcast");
            try {
                app.sendUdpData(new DatagramSocket(),
                        JSON.toJSONString(host),
                        NetConfApplication.broadcastIP,
                        NetConfApplication.broadcastPort);
                if (tag) {
                    updateHandler.postDelayed(this, 1000);
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    };
    private long recTime = 0;

    private static final int SCREEN_Off = 0;
    private static final int SCREEN_ON = 1;
    private WakeLock wakeLock;
    private ScreenAndWifiListener screenAndWifiListener;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        app = (NetConfApplication) getApplication();
        String userName = android.os.Build.MODEL;// 获取用户名
        String userDomain = "Android";// 获取计算机域
        host = new Host(userName, userDomain, NetConfApplication.hostIP,
                NetConfApplication.hostName, 1, 1);

        acquireWakeLock();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        screenAndWifiListener = new ScreenAndWifiListener();
        registerReceiver(screenAndWifiListener, filter);

        tag = false;
        screenHandlerThread = new HandlerThread("screen");
        screenHandlerThread.start();
        updateHandler = new Handler(screenHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {

                switch (msg.what) {
                    case SCREEN_Off:
                        tag = true;
                        this.postDelayed(sendSelfInfo, 1000);
                        break;
                    case SCREEN_ON:
                        tag = false;
                        this.removeCallbacks(sendSelfInfo);
                        break;
                    default:
                        break;
                }
            }
        };
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        unregisterReceiver(screenAndWifiListener);
        Logger.info(TAG, "service stop");
        super.onDestroy();
    }

    // 监听屏幕明暗和wifi连接状态的广播
    class ScreenAndWifiListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Logger.info(TAG, "receive a screen broadcast");
            switch (intent.getAction()) {
                // 屏幕明暗的广播
                case Intent.ACTION_SCREEN_OFF:
                    Logger.info(TAG, "screen is off");
                    updateHandler.sendEmptyMessage(SCREEN_Off);
                    break;
                case Intent.ACTION_SCREEN_ON:
                    Logger.info(TAG, "screen is on");
                    updateHandler.sendEmptyMessage(SCREEN_ON);
                    break;
                // 监听wifi连接状态的广播
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    Logger.info(TAG, "wifi----CONNECTIVITY_ACTION");
                    if ((System.currentTimeMillis() - recTime) < 2000) {
                        Logger.info(TAG, "CONNECTIVITY_ACTION too much,ignore it");
                        recTime = System.currentTimeMillis();
                        break;
                    }
                    recTime = System.currentTimeMillis();
                    if (NetConfApplication.isUIReady) {
                        // 获取wifi服务
                        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        // 获取连接状态
                        ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo wifiNetInfo = connectMgr
                                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                        if (wifiManager.isWifiEnabled() && wifiNetInfo.isConnected()) {
                            app.wifi = 1;
                        } else {
                            app.wifi = 0;
                        }
                        Logger.info(TAG, "wifi listener num ====" + app.listeners.size());
                        for (NetConfApplication.WifiListener listener : app.listeners) {
                            Logger.info(TAG, "wifi is disconnect");
                            listener.notifyWifiInfo();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this
                    .getClass().getCanonicalName());
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
