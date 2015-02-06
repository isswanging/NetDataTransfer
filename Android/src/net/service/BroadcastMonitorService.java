package net.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.alibaba.fastjson.JSON;

import net.app.NetConfApplication;
import net.vo.Host;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BroadcastMonitorService extends Service {
	DatagramSocket broadSocket = null;
	ByteArrayInputStream byteArrayStram = null;
	ObjectInputStream objectStream = null;
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

		Log.i(this.toString(), "BroadcastMonitor started");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DatagramPacket broadPacket = new DatagramPacket(
							new byte[512], 512);
					broadSocket = new DatagramSocket(app.broadcastPort);
					while (true) {
						// 收到广播
						broadSocket.receive(broadPacket);
						// 整理信息
						String info = new String(broadPacket.getData(), 0,
								broadPacket.getLength());
						Host host = JSON.parseObject(info, Host.class);
						host.setState(0);

						if (!host.getIp().equals(app.hostIP)) {
							if (!app.containHost(host)) {
								host.setState(1);
								app.addHost(host);

								// 回应广播, 发送本机信息去目标地址
								if (host.getTag() == 0) {
									String userName = android.os.Build.MODEL;// 获取主机名
									String hostName = "Android";// 获取用户名
									String userDomain = "Android";// 获取计算机域

									// 广播主机信息
									Host res = new Host(userName, userDomain,
											app.hostIP, hostName, 1, 1);
									String hostInfo = JSON.toJSONString(res);
									app.sendUdpData(broadSocket, hostInfo,
											host.getIp(), app.broadcastPort);

								}
							}
						}
					}
				} catch (IOException e) {
				}
			}
		}).start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

}
