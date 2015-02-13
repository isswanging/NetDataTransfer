package net.app;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.vo.ChatMsgEntity;
import net.vo.Host;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetConfApplication extends Application {
	public int wifi = 0;
	public String chatId = "none";

	public NotificationManager nManager;
	
	// 发送普通信息端口
	public final int textPort = 2324;

	// 发送文件端口
	public final int filePort = 2324;

	// 发送文件夹端口
	public final int folderPort = 2325;

	// 发送文件路径端口
	public final int pathPort = 2326;

	// 系统信息标识
	public final String SUCCESS = "success";

	public final String ERROR = "error";

	public final String FAIL = "IOException";

	// 广播IP
	public final String broadcastIP = "224.0.0.1";

	// 广播端口
	public final int broadcastPort = 2325;

	// 信号
	public final int text = 0;
	public final int filePre = 1;
	public final int fileConf = 2;
	public final int refuse = 3;
	public final int folderPre = 4;
	public final int folderConf = 5;
	public final int end = 6;

	public int getText() {
		return text;
	}

	// 数组buffer
	public final int buffer = 10000;

	// 在线主机列表
	public Vector<Host> hostList = new Vector<Host>();

	// 本机ip
	public String hostIP = "";

	// 传输文件夹任务列表
	public HashMap<String, ArrayList<String>> taskList = new HashMap<String, ArrayList<String>>();

	// 传输文件夹时记录的文件路径
	public HashMap<String, String> sendPathList = new HashMap<String, String>();

	// 传输文件夹时记录的文件路径
	public HashMap<String, String> savePathList = new HashMap<String, String>();

	// 记录进度
	public ConcurrentHashMap<String, Long> progress = new ConcurrentHashMap<String, Long>();

	// 记录聊天内容
	public HashMap<String, ArrayList<ChatMsgEntity>> chatTempMap = new HashMap<String, ArrayList<ChatMsgEntity>>();

	// 检查端口
	public String check(Context userListActivity) {
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) userListActivity
				.getSystemService(Context.WIFI_SERVICE);
		// 获取连接状态
		ConnectivityManager connectMgr = (ConnectivityManager) userListActivity
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiNetInfo = connectMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiManager.isWifiEnabled() && wifiNetInfo.isConnected()) {
			wifi = 1;
			try {
				new DatagramSocket(textPort).close();
				new ServerSocket(filePort).close();
				new ServerSocket(folderPort).close();
				new ServerSocket(pathPort).close();

				return SUCCESS;
			} catch (SocketException e) {
				return ERROR;
			} catch (IOException e) {
				return FAIL;
			}
		} else
			wifi = 0;
		return ERROR;

	}

	// 添加主机
	public synchronized boolean addHost(Host host) {
		for (int i = 0; i < hostList.size(); i++) {
			if (hostList.get(i).getIp().equals(host.getIp())) {
				hostList.set(i, host);
				return false;
			}
		}
		hostList.add(host);
		return true;
	}

	// 检查主机是否重复
	public synchronized boolean containHost(Host host) {
		for (int i = 0; i < hostList.size(); i++) {
			Host h = hostList.get(i);
			if ((h.getIp().equals(host.getIp()))
					&& (h.getState() == host.getState())) {
				return true;
			}
		}
		return false;
	}

	// 发送UDP消息
	public void sendUdpData(DatagramSocket broadSocket, String obj,
			String targetIp, int port) {
		byte[] info = obj.getBytes();

		DatagramPacket broadPacket;
		try {
			broadPacket = new DatagramPacket(info, info.length,
					InetAddress.getByName(targetIp), port);
			broadSocket.send(broadPacket);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// 获取本机IP
	public String getHostIp(Context context) {
		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String ip = intToIp(ipAddress);
		return ip;
	}

	@Override
	public void onCreate() {
		hostIP = getHostIp(this);
	}

	private String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}

	// 获取日期
	public String getDate() {
		Calendar c = Calendar.getInstance();
		String year = String.valueOf(c.get(Calendar.YEAR));
		String month = String.valueOf(c.get(Calendar.MONTH));
		String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH) + 1);
		String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
		String mins = String.valueOf(c.get(Calendar.MINUTE));
		StringBuffer sbBuffer = new StringBuffer();
		sbBuffer.append(year + "-" + month + "-" + day + " " + hour + ":"
				+ mins);
		return sbBuffer.toString();
	}
}
