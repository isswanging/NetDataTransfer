package net.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import net.conf.SystemConf;
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
	public static Host getHost() {
		try {
			InetAddress addr = InetAddress.getLocalHost();
			Host host = new Host();
			String hostName = addr.getHostName();// 获取主机名
			String ip = addr.getHostAddress();// 获取ip地址ַ

			Map<String, String> map = System.getenv();
			String userName = map.get("USERNAME");// 获取用户名
			String userDomain = map.get("USERDOMAIN");// 获取计算机域

			host.setGroupName(userDomain);
			host.setHostName(hostName);
			host.setIp(ip);
			host.setUserName(userName);

			return host;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}

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

}
