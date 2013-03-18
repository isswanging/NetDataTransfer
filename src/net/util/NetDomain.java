package net.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;

import net.conf.SystemConf;
import net.ui.MainWindow;
import net.vo.Host;

public class NetDomain {

	// 检查端口
	public static String check() {
		try {
			new DatagramSocket(SystemConf.textPort).close();
			new ServerSocket(SystemConf.filePort).close();
			new ServerSocket(SystemConf.folerPort).close();

			return SystemConf.SUCCESS;
		} catch (SocketException e) {
			return SystemConf.ERROR;
		} catch (IOException e) {
			return SystemConf.FAIL;
		}

	}

	// 构造Host对象
	public static Host getHost(String message) {
		Host host = new Host();
		String[] info = message.split("@");
		host.setUserName(info[0]);
		host.setGroupName(info[1]);
		host.setHostName(info[2]);
		host.setIp(info[3]);
		host.setState(0);

		return host;
	}

	// 广播消息并且寻找线上主机交换消息
	public static void broadcast(DatagramSocket broadSocket,
			DatagramPacket broadPacket) {
		try {
			broadSocket.send(broadPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 添加主机
	public static synchronized boolean addHost(Host host) {
		for (int i = 0; i < MainWindow.hostList.size(); i++) {
			if (MainWindow.hostList.get(i).getIp().equals(host.getIp())) {
				MainWindow.hostList.set(i, host);
				return false;
			}
		}
		MainWindow.hostList.add(host);
		return true;
	}

	// 检查主机是否重复
	public static synchronized boolean containHost(Host host) {
		for (int i = 0; i < MainWindow.hostList.size(); i++) {
			Host h = MainWindow.hostList.get(i);
			if ((h.getIp().equals(host.getIp()))
					&& (h.getState() == host.getState())) {
				return true;
			}
		}
		return false;
	}

}
