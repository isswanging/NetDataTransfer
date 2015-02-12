package net.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import net.app.NetConfApplication;
import net.vo.Host;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.alibaba.fastjson.JSON;

public class BroadcastMonitorService extends Service {
	DatagramSocket broadSocket = null;
	DatagramPacket broadPacket = null;
	NetConfApplication app;
	Thread thread;
	boolean tag;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		app = (NetConfApplication) getApplication();
		Log.i(this.toString(), "BroadcastMonitor started");
		tag = true;
		thread = new Thread(new ReceiveHost());
		thread.start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		tag = false;
		thread.interrupt();
		Log.i(this.toString(), "service stop");
		super.onDestroy();
	}

	private class ReceiveHost implements Runnable {

		@Override
		public void run() {
			try {
				Log.i(this.toString(), "start a service");
				broadPacket = new DatagramPacket(new byte[512], 512);
				broadSocket = new DatagramSocket(app.broadcastPort);
				while (tag) {
					// 收到广播
					broadSocket.receive(broadPacket);
					Log.i(this.toString(), "receive a broadcast");
					// 整理信息
					String info = new String(broadPacket.getData(), 0,
							broadPacket.getLength());
					Host host = JSON.parseObject(info, Host.class);
					host.setState(0);
					Log.i(this.toString(), "host: " + host.getHostName());

					Log.i(this.toString(), "receive a host");
					if (!app.containHost(host)) {
						host.setState(1);
						app.addHost(host);
						Log.i(this.toString(), "add a host");

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
			} catch (IOException e) {
			}

		}

	}
}
