package net.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;

import net.conf.SystemConf;
import net.vo.Host;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetDomain {

	// 检查端口
	public static String check(Context userListActivity) {
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) userListActivity
				.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			SystemConf.wifi = 1;
			try {
				new DatagramSocket(SystemConf.textPort).close();
				new ServerSocket(SystemConf.filePort).close();
				new ServerSocket(SystemConf.folderPort).close();
				new ServerSocket(SystemConf.pathPort).close();

				return SystemConf.SUCCESS;
			} catch (SocketException e) {
				return SystemConf.ERROR;
			} catch (IOException e) {
				return SystemConf.FAIL;
			}
		} else
			SystemConf.wifi = 0;
			return SystemConf.ERROR;

	}

	// 添加主机
	public static synchronized boolean addHost(Host host) {
		for (int i = 0; i < SystemConf.hostList.size(); i++) {
			if (SystemConf.hostList.get(i).getIp().equals(host.getIp())) {
				SystemConf.hostList.set(i, host);
				return false;
			}
		}
		SystemConf.hostList.add(host);
		return true;
	}

	// 检查主机是否重复
	public static synchronized boolean containHost(Host host) {
		for (int i = 0; i < SystemConf.hostList.size(); i++) {
			Host h = SystemConf.hostList.get(i);
			if ((h.getIp().equals(host.getIp()))
					&& (h.getState() == host.getState())) {
				return true;
			}
		}
		return false;
	}

	// 发送UDP消息
	public static void sendUdpData(DatagramSocket broadSocket, Object obj,
			String targetIp, int port) {
		ByteArrayOutputStream byteArrayStream = null;
		ObjectOutputStream objectStream = null;
		try {
			byteArrayStream = new ByteArrayOutputStream();
			objectStream = new ObjectOutputStream(byteArrayStream);
			objectStream.writeObject(obj);
			byte[] info = byteArrayStream.toByteArray();

			DatagramPacket broadPacket = new DatagramPacket(info, info.length,
					InetAddress.getByName(targetIp), port);

			broadSocket.send(broadPacket);

		} catch (IOException e) {
		} finally {
			try {
				if (objectStream != null)
					objectStream.close();
				if (byteArrayStream != null)
					byteArrayStream.close();
			} catch (IOException e) {
			}

		}
	}

	// 获取本机IP
	public static String getHostIp(Context context) {
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		// 判断wifi是否开启
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String ip = intToIp(ipAddress);
		return ip;
	}

	private static String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}
}