package net.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;
import net.service.BroadcastMonitorService;
import net.service.FileMonitorService;
import net.service.ScreenMonitorService;
import net.service.UdpDataMonitorService;
import net.ui.fragment.BaseFragment;
import net.ui.fragment.ChatFragment;
import net.ui.fragment.UserListFragment;
import net.vo.Host;

import java.lang.ref.WeakReference;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MainActivity extends Activity implements BaseFragment.Notification {
    private String user_TAG = "usersFragment";
    private String chat_TAG = "chatFragment";
    private Handler handler = new ListHandler(this);
    private UserListFragment users;
    private ChatFragment chat;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;
    private NetConfApplication app;

    // 按两次退出的计时
    private long exitTime = 0;

    // 更新消息提示的广播
    NewMsgReceiver msgReceiver;
    IntentFilter filter;

    private final int login = 0;
    private final int refresh = 1;
    private final int retry = 2;
    private final int answer = 3;
    private final int startChat = 4;
    private final int incomingMsg = 5;
    private final int redraw = 6;
    private final int close = 7;
    private final int overlay = 8;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.info(this.toString(), "activity onCreat()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        app = (NetConfApplication) getApplication();
        app.forceClose = false;

        fragmentManager = getFragmentManager();
        Logger.info(this.toString(), "begin fragments users and chat");
        users = (UserListFragment) fragmentManager.findFragmentByTag(user_TAG);
        chat = (ChatFragment) fragmentManager.findFragmentByTag(chat_TAG);
        if (users == null) {
            users = new UserListFragment();
            fragmentManager.beginTransaction().add(R.id.users, users, user_TAG).commit();
        }
        if (chat == null) {
            chat = new ChatFragment();
            fragmentManager.beginTransaction().add(R.id.chat, chat, chat_TAG).commit();
        }
        fragmentAction();
        Logger.info(this.toString(), "end fragments users and chat");

        msgReceiver = new NewMsgReceiver();
        filter = new IntentFilter();
        filter.addAction("net.ui.newMsg");
        filter.addAction("net.ui.chatFrom");
    }

    @Override
    protected void onResume() {
        registerReceiver(msgReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(msgReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (app.forceClose) {
            stopService(new Intent(this, BroadcastMonitorService.class));
            stopService(new Intent(this, UdpDataMonitorService.class));
            stopService(new Intent(this, FileMonitorService.class));
            stopService(new Intent(this, ScreenMonitorService.class));
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
                return false;
            } else {
                app.forceClose = true;
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void notifyInfo(int commend, Object obj) {
        Logger.info(this.toString(), "get commend from fragment==" + commend);
        switch (commend) {
            case login:
            case refresh:
                login(commend);
                handler.sendEmptyMessageDelayed(commend, 2000);
                break;
            case retry:
                handler.sendEmptyMessageDelayed(commend, 2000);
                break;
            case startChat:
                Message msg = handler.obtainMessage();
                msg.what = commend;
                msg.obj = obj;
                msg.sendToTarget();
                break;
            case close:
                fragmentManager.beginTransaction().hide(chat).commit();
                break;
            case redraw:
                handler.sendEmptyMessage(commend);
                break;
            default:
                Logger.error(this.toString(), "====get error commend====" + commend);
        }

    }

    class ListHandler extends Handler {
        WeakReference<MainActivity> refActvity;

        ListHandler(MainActivity activity) {
            refActvity = new WeakReference(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity act = refActvity.get();

            if (act != null) {
                switch (msg.what) {
                    case login:
                    case refresh:
                    case retry:
                    case redraw:
                        users.getCommend(msg);
                        break;
                    case startChat:
                        chat.getCommend(msg);
                        // 显示chatFragment
                        fragmentManager.beginTransaction().show(chat).commit();
                        break;
                }
            }
        }
    }

    public void fragmentAction() {
        Logger.info(this.toString(), "fragmentAction method called");
        if (app.topFragment.equals("users")) {
            fragmentManager.beginTransaction().hide(chat).commit();
        }
        //判断屏幕方向
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            app.isLand = true;
        } else {
            // 竖屏
            app.isLand = false;
        }
    }

    private void login(int commend) {
        final Host host;
        if (commend == login) {
            NetConfApplication.hostIP = app.getHostIp(this);// 获取ip地址
            host = app.hostList.get(0);
            host.setState(0);
            host.setIp(NetConfApplication.hostIP);
        } else {
            app.hostList.clear();
            String userName = android.os.Build.MODEL;// 获取用户名
            String hostName = "Android";// 获取主机名
            String userDomain = "Android";// 获取计算机域

            // 加入在线列表
            host = new Host(userName, userDomain,
                    NetConfApplication.hostIP, hostName, 1, 0);
            app.addHost(host);
        }

        // 广播登录信息
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    // 加入在线列表
                    app.sendUdpData(new DatagramSocket(), JSON.toJSONString(host),
                            NetConfApplication.broadcastIP, NetConfApplication.broadcastPort);

                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    class NewMsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Message msg = Message.obtain();
            // 如果聊天id相同,就更新当前窗口的消息
            if (intent.getAction().equals("net.ui.chatFrom")) {
                msg.what = incomingMsg;
                msg.obj = intent.getExtras();
                chat.getCommend(msg);
            } else {
                // 在界面上显示未读消息
                msg.what = redraw;
                users.getCommend(msg);
            }
        }
    }
}