package net.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import net.conf.SystemConf;
import net.vo.DataPacket;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class UdpDataMonitorService extends Service {
	DatagramSocket UdpSocket = null;
	DatagramPacket UdpPacket = null;
	DataPacket dp = null;
	ObjectInputStream objectStream = null;
	ByteArrayInputStream byteArrayStream = null;

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
		Log.i(this.toString(), "UDPdataMonitor started");
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					UdpPacket = new DatagramPacket(new byte[1024], 1024);
					UdpSocket = new DatagramSocket(SystemConf.textPort);
					while (true) {
						// 收到消息
						UdpSocket.receive(UdpPacket);

						// 解析处理并显示
						byteArrayStream = new ByteArrayInputStream(
								UdpPacket.getData());
						objectStream = new ObjectInputStream(byteArrayStream);
						dp = (DataPacket) objectStream.readObject();

						switch (dp.getTag()) {
						case SystemConf.text:
							if (SystemConf.isChating) {
								// 发广播在交给聊天窗口处理
								Intent intent = new Intent("net.ui.chatFrom");
								Bundle bundle = new Bundle();
								bundle.putSerializable("content", dp);
								intent.putExtras(bundle);

								// 发送广播
								sendBroadcast(intent);

							} else {
								// 发送通知

							}
							break;
						}
					}

				} catch (IOException e) {
				} catch (ClassNotFoundException e) {
				} finally {
					try {
						UdpSocket.close();
						objectStream.close();
						byteArrayStream.close();
					} catch (Exception e) {
					}
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
