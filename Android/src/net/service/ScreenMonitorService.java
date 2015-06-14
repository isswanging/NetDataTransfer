package net.service;

import java.net.DatagramSocket;
import java.net.SocketException;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.Host;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.alibaba.fastjson.JSON;

public class ScreenMonitorService extends Service {
    Host host;
    NetConfApplication app;

    boolean isClosed;
    Thread thread;
    SendHostInfo sendHostInfo;

    private static final int SCREEN_Off = 0;
    private static final int SCREEN_ON = 1;
    private WakeLock wakeLock;

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
        registerReceiver(new ScreenListener(), filter);

        isClosed = false;
        sendHostInfo = new SendHostInfo();
        thread = new Thread(sendHostInfo);
        thread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        isClosed = false;
        thread.interrupt();
        Logger.info(this.toString(), "service stop");
        super.onDestroy();
    }

    class ScreenListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Logger.info(this.toString(), "receive a screen broadcast");
            switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_OFF:
                Logger.info(this.toString(), "screen is off");
                sendHostInfo.getHandler().sendEmptyMessage(SCREEN_Off);
                break;

            case Intent.ACTION_SCREEN_ON:
                Logger.info(this.toString(), "screen is on");
                sendHostInfo.getHandler().sendEmptyMessage(SCREEN_ON);
                break;

            default:
                break;
            }
        }
    }

    private class SendHostInfo implements Runnable {

        Handler updateHandler;

        Runnable sendSelfInfo = new Runnable() {

            @Override
            public void run() {
                Logger.info(this.toString(), "send broadcast");
                try {
                    app.sendUdpData(new DatagramSocket(),
                            JSON.toJSONString(host),
                            NetConfApplication.broadcastIP,
                            NetConfApplication.broadcastPort);
                    if (isClosed) {
                        updateHandler.postDelayed(this, 1000);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        };

        @Override
        public void run() {
            Logger.info(this.toString(), "in SendHostInfo");
            Looper.prepare();
            updateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {

                    switch (msg.what) {
                    case SCREEN_Off:
                        isClosed = true;
                        this.postDelayed(sendSelfInfo, 1000);
                        break;
                    case SCREEN_ON:
                        isClosed = false;
                        this.removeCallbacks(sendHostInfo);
                        break;
                    default:
                        break;
                    }
                }
            };
            Looper.loop();

        }

        public Handler getHandler() {
            return updateHandler;
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
