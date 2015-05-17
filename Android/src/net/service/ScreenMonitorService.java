package net.service;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.Host;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.alibaba.fastjson.JSON;

public class ScreenMonitorService extends Service {
    Host host;
    NetConfApplication app;

    public ScheduledThreadPoolExecutor se = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> sf;

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

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(new ScreenListener(), filter);
        return super.onStartCommand(intent, flags, startId);
    }

    class ScreenListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Logger.info(this.toString(), "receive a screen broadcast");
            switch (intent.getAction()) {
            case Intent.ACTION_SCREEN_OFF:
                Logger.info(this.toString(), "screen is off");
                sf = se.scheduleAtFixedRate(sendSelfInfo, 100, 500, TimeUnit.MILLISECONDS);
                break;

            case Intent.ACTION_SCREEN_ON:
                Logger.info(this.toString(), "screen is on");
                sf.cancel(true);
                break;
                
            default:
                break;
            }
        }
    }

    Runnable sendSelfInfo = new Runnable() {

        @Override
        public void run() {
            Logger.info(this.toString(), "send broadcast");
            try {
                app.sendUdpData(new DatagramSocket(), JSON.toJSONString(host),
                        NetConfApplication.broadcastIP,
                        NetConfApplication.broadcastPort);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    };

}
