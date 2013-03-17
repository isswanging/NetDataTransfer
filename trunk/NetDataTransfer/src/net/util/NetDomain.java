package net.util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;

import net.conf.SystemConf;

// 网络管理
public class NetDomain {

	// 查看端口是否被占用
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

}
