package net.conf;

import java.util.Vector;

import net.vo.Host;

public class SystemConf {
	// 发送普通信息端口
	public final static int textPort = 80;

	// 发送文件端口
	public final static int filePort = 80;

	// 发送文件夹端口
	public final static int folerPort = 800;

	// 系统信息标识
	public final static String SUCCESS = "success";

	public final static String ERROR = "error";

	public final static String FAIL = "IOException";

	// 广播IP
	public final static String broadcastIP = "255.255.255.255";

	// 广播端口
	public final static int broadcastPort = 232;

	// 信号
	public static int text = 0;
	public static int filePre = 1;
	public static int fileConf = 2;
	public static int fileTran = 3;

	// 在线主机列表
	public static Vector<Host> hostList = new Vector<Host>();

	// 保存文件路径
	public static String savePath = "";
}
