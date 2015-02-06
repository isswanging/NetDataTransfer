package net.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import net.app.NetConfApplication;
import net.vo.DataPacket;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.example.netdatatransfer.R;

public class UdpDataMonitorService extends Service {
	DatagramSocket UdpSocket = null;
	DatagramPacket UdpPacket = null;
	DataPacket dp = null;

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

		Log.i(this.toString(), "UDPdataMonitor started");
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					UdpPacket = new DatagramPacket(new byte[1024], 1024);
					UdpSocket = new DatagramSocket(app.textPort);
					while (true) {
						// 收到消息
						UdpSocket.receive(UdpPacket);

						// 解析处理并显示
						String info = new String(UdpPacket.getData(), 0,
								UdpPacket.getLength());
						dp = JSON.parseObject(info, DataPacket.class);

						// 传文字
						if (dp.getTag() == app.text) {

							// 播放消息提示音乐
							MediaPlayer mp = new MediaPlayer();
							try {
								mp.setDataSource(
										UdpDataMonitorService.this,
										RingtoneManager
												.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
								mp.prepare();
								mp.start();
							} catch (Exception e) {
								e.printStackTrace();
							}

							if (app.chatId.equals(dp.getIp())) {
								// 发广播在交给聊天窗口处理
								Intent intent = new Intent("net.ui.chatFrom");
								Bundle bundle = new Bundle();
								bundle.putString("content", info);
								intent.putExtras(bundle);

								// 发送广播
								sendBroadcast(intent);

							} else {
								// 发送通知
								Intent notifyIntent = new Intent(
										"net.ui.chatting");
								notifyIntent
										.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								Bundle bundle = new Bundle();
								bundle.putString("name", dp.getSenderName());
								bundle.putString("ip", dp.getIp());
								notifyIntent.putExtras(bundle);
								PendingIntent contentIntent = PendingIntent
										.getActivity(
												UdpDataMonitorService.this,
												R.string.app_name,
												notifyIntent,
												PendingIntent.FLAG_UPDATE_CURRENT);

								// 显示
								NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
								Notification notification = new NotificationCompat.Builder(
										UdpDataMonitorService.this)
										.setSmallIcon(R.drawable.notify)
										.setTicker("新消息")
										.setContentTitle("点击查看")
										.setContentText(
												dp.getSenderName() + "发来一条新消息")
										.setContentIntent(contentIntent)
										.build();
								notification.flags = Notification.FLAG_AUTO_CANCEL;
								nManager.notify(R.id.chatName, notification);
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
