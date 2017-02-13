package net.app;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodManager;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import net.app.netdatatransfer.R;
import net.log.Logger;
import net.vo.Host;
import net.vo.Progress;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;

public class NetConfApplication extends Application {
    private final String TAG = "NetConfApplication";
    public int wifi = 1;
    public String chatId = "gone";
    public SoundPool soundPool;
    public NotificationManager nManager;
    public int soundID;
    public boolean isLand = false;
    public boolean forceClose = false;
    public String topFragment = "users";

    // 发送普通信息端口
    public final static int textPort = 2324;

    // 发送文件端口
    public final static int filePort = 2324;

    // 广播端口
    public final static int broadcastPort = 2325;

    // 系统信息标识
    public final String SUCCESS = "success";

    public final String ERROR = "error";

    public final String FAIL = "IOException";

    // 判断界面是否加载完成
    public static boolean isUIReady = false;

    // 广播IP
    public static final String broadcastIP = "255.255.255.255";

    // 信号
    public static final int text = 0;
    public static final int filePre = 1;
    public static final int fileConf = 2;
    public static final int refuse = 3;
    // PC上使用的两个标志位
    // public final static int folderPre = 4;
    // public final static int folderConf = 5;
    public static final int end = 6;

    public int getText() {
        return text;
    }

    // 数组buffer
    public final int buffer = 10000;

    // 在线主机列表
    public Vector<Host> hostList = new Vector<Host>();

    public static String hostIP = "";
    public static String hostName = "Android";
    public static String saveFilePath = "";

    // 文件传送的任务id
    public static int taskId = 0;
    // 传输文件任务列表
    public static SparseArray<Progress> sendTaskList = new SparseArray<>();
    public static SparseArray<Progress> getTaskList = new SparseArray<>();

    // 文件格式
    public final static String[] imageSupport = {"BMP", "JPG", "JPEG", "PNG",
            "GIF"};
    public final static String[] videoSupport = {"rmvb", "AVI", "mp4", "3gp",
            "mpg"};
    public final static String[] audioSupport = {"mp3", "wma", "wav", "amr"};

    public final static int add = 0;
    public final static int remove = 1;

    public final static int upMoveCache = 150;
    public final static int downMoveCache = 50;

    // 检查端口
    public String check(boolean isClose) {
        // 获取wifi服务
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // 获取连接状态
        ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetInfo = connectMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiManager.isWifiEnabled() && wifiNetInfo.isConnected()) {
            wifi = 1;
            if (isClose) {
                try {
                    new DatagramSocket(textPort).close();
                    new ServerSocket(filePort).close();
                    Logger.info(TAG, "check result : " + SUCCESS);
                    return SUCCESS;
                } catch (SocketException e) {
                    return ERROR;
                } catch (IOException e) {
                    return FAIL;
                }
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
                    && (h.getState() == host.getState())
                    && (h.getUserName().equals(host.getUserName()))) {
                return true;
            }
        }
        return false;
    }

    // 发送UDP消息
    public void sendUdpData(DatagramSocket broadSocket, String obj,
                            String targetIp, int port) {
        DatagramPacket broadPacket;
        try {
            byte[] info = obj.getBytes();
            Logger.info(TAG, "send udp json byte is---" + NetConfApplication.printByte(info));
            broadPacket = new DatagramPacket(info, info.length,
                    InetAddress.getByName(targetIp), port);
            broadSocket.send(broadPacket);
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
        return intToIp(ipAddress);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        hostIP = getHostIp(this);
        refWatcher = LeakCanary.install(this);
        nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
        return year + "-" + month + "-" + day + " " + hour + ":" + mins;
    }

    // 播放消息提示音
    public void loadVoice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool.Builder().setMaxStreams(10).build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
        }
        soundID = soundPool.load(this, R.raw.notificationsound, 1);
    }

    public void playVoice() {
        // 播放消息提示音乐
        soundPool.play(soundID, 1f, 1f, 0, 0, 1f);
    }

    // 获取sd卡路径
    public String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        return sdDir != null ? sdDir.toString() : null;
    }

    public ArrayList<WifiListener> listeners = new ArrayList<>();

    public interface WifiListener {
        void notifyWifiInfo();
    }

    // 收起输入法键盘
    public void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    //在自己的Application中添加如下代码
    public static RefWatcher getRefWatcher(Context context) {
        NetConfApplication application = (NetConfApplication) context
                .getApplicationContext();
        return application.refWatcher;
    }

    //在自己的Application中添加如下代码
    private RefWatcher refWatcher;

    // 调试用
    public static String printByte(byte[] b) {
        String byteStr = "";
        for (byte aB : b) {
            String hex = Integer.toHexString(aB & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            byteStr += hex.toUpperCase() + " ";
        }
        return byteStr;
    }
}
