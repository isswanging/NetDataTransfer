package net.service;

import android.content.Intent;
import android.os.IBinder;

import com.alibaba.fastjson.JSON;

import net.app.NetConfApplication;
import net.log.Logger;
import net.service.base.BaseService;
import net.vo.Host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class BroadcastMonitorService extends BaseService {
    private final String TAG="BroadcastMonitorService";
    DatagramSocket broadSocket = null;
    DatagramPacket broadPacket = null;
    NetConfApplication app;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        app = (NetConfApplication) getApplication();
        Logger.info(TAG, "BroadcastMonitor started");
        tag = true;
        cachedThreadPool.execute(new ReceiveHost());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Logger.info(TAG, "service stop");
        super.onDestroy();
    }

    private class ReceiveHost implements Runnable {

        @Override
        public void run() {
            try {
                Logger.info(TAG, "start a service");
                broadPacket = new DatagramPacket(new byte[512], 512);
                broadSocket = new DatagramSocket(NetConfApplication.broadcastPort);
                while (tag) {
                    // 收到广播
                    broadSocket.receive(broadPacket);
                    Logger.info(TAG, "receive a broadcast");
                    // 整理信息
                    String info = new String(broadPacket.getData(), 0,
                            broadPacket.getLength());
                    Host host = JSON.parseObject(info, Host.class);
                    host.setState(0);
                    Logger.info(TAG, "from ip: " + host.getIp());

                    if (!app.containHost(host)) {
                        host.setState(1);
                        app.addHost(host);
                        Logger.info(TAG, "add a host");

                        // 回应广播, 发送本机信息去目标地址
                        if (host.getTag() == 0) {
                            String userName = android.os.Build.MODEL;// 获取主机名
                            String hostName = "Android";// 获取用户名
                            String userDomain = "Android";// 获取计算机域

                            // 广播主机信息
                            Host res = new Host(userName, userDomain,
                                    NetConfApplication.hostIP, hostName, 1, 1);
                            String hostInfo = JSON.toJSONString(res);
                            app.sendUdpData(broadSocket, hostInfo,
                                    host.getIp(),
                                    NetConfApplication.broadcastPort);
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error(TAG, e.toString());
            }
        }
    }
}
