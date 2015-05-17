package net.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.Host;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;

import com.alibaba.fastjson.JSON;

public class BroadcastMonitorService extends Service {
    MulticastSocket broadSocket = null;
    DatagramPacket broadPacket = null;
    NetConfApplication app;
    Thread thread;
    boolean tag;
    WifiManager.MulticastLock wifiLock;

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
        WifiManager manager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        wifiLock = manager.createMulticastLock("wifi");
        wifiLock.acquire();

        app = (NetConfApplication) getApplication();
        Logger.info(this.toString(), "BroadcastMonitor started");
        tag = true;
        thread = new Thread(new ReceiveHost());
        thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        wifiLock.release();
        tag = false;
        thread.interrupt();
        Logger.info(this.toString(), "service stop");
        super.onDestroy();
    }

    private class ReceiveHost implements Runnable {

        @Override
        public void run() {
            try {
                Logger.info(this.toString(), "start a service");
                broadPacket = new DatagramPacket(new byte[512], 512);
                broadSocket = new MulticastSocket(
                        NetConfApplication.broadcastPort);
                broadSocket.joinGroup(InetAddress
                        .getByName(NetConfApplication.broadcastIP));
                while (tag) {
                    // 收到广播
                    broadSocket.receive(broadPacket);
                    Logger.info(this.toString(), "receive a broadcast");
                    // 整理信息
                    String info = new String(broadPacket.getData(), 0,
                            broadPacket.getLength());
                    Host host = JSON.parseObject(info, Host.class);
                    host.setState(0);
                    Logger.info(this.toString(), "host: " + host.getHostName());

                    Logger.info(this.toString(), "receive a host");
                    if (!app.containHost(host)) {
                        host.setState(1);
                        app.addHost(host);
                        Logger.info(this.toString(), "add a host");

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
                Logger.error(this.toString(), e.toString());
            }

        }

    }
}
