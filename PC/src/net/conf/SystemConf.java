package net.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.vo.Host;

public class SystemConf {
    // 发送普通信息端口
    public final static int textPort = 2324;

    // 发送文件端口
    public final static int filePort = 2324;

    // 发送文件夹端口
    public final static int folderPort = 2325;

    // 发送文件路径端口
    public final static int pathPort = 2326;

    // 系统信息标识
    public final static String SUCCESS = "success";

    public final static String ERROR = "error";

    public final static String FAIL = "IOException";

    // 广播IP
    public final static String broadcastIP = "255.255.255.255";

    // 广播端口
    public final static int broadcastPort = 2325;

    // 信号
    public final static int text = 0;
    public final static int filePre = 1;
    public final static int fileConf = 2;
    public final static int refuse = 3;
    public final static int folderPre = 4;
    public final static int folderConf = 5;
    public final static int end = 6;

    // 数组buffer
    public final static int buffer = 10000;

    // 在线主机列表
    public static Vector<Host> hostList = new Vector<Host>();

    // 本机ip
    public static String hostIP = "";

    // 传输文件夹任务列表
    public static HashMap<String, ArrayList<String>> taskList = new HashMap<String, ArrayList<String>>();

    // 传输文件夹时记录的文件路径
    public static HashMap<String, String> sendPathList = new HashMap<String, String>();

    // 传输文件夹时记录的文件路径
    public static HashMap<String, String> savePathList = new HashMap<String, String>();

    // 记录进度
    public static ConcurrentHashMap<String, Long> progress = new ConcurrentHashMap<String, Long>();

}
