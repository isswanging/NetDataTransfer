package net.ui.activity;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import net.ui.view.ForceTouchViewGroup;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import net.vo.Host;

import java.lang.ref.WeakReference;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements BaseFragment.Notification {
    private final String TAG = "MainActivity";
    private final String user_TAG = "usersFragment";
    private final String chat_TAG = "chatFragment";
    private Handler handler = new ListHandler(this);
    private UserListFragment users;
    private ChatFragment chat;
    private FragmentManager fragmentManager;
    private NetConfApplication app;

    // 按两次退出的计时
    private long exitTime = 0;

    // 更新消息提示的广播
    NewMsgReceiver msgReceiver;
    IntentFilter filter;

    // 3D touch效果
    ListView previewContent;
    List<Map<String, Object>> answerListData = new ArrayList<Map<String, Object>>();
    List<ChatMsgEntity> mDataArrays = new ArrayList<ChatMsgEntity>();
    RelativeLayout.LayoutParams previewParams;
    LinearLayout preview;
    LinearLayout custPreview;
    FrameLayout root;
    String[] answerData = new String[]{"好", "谢谢", "晚点再说", "自定义"};
    ChatMsgAdapter chatAdapter;
    ForceTouchViewGroup touchView;
    int moveTopMargin;
    int topMargin;
    AlphaAnimation hidePreviewAnim;
    AlphaAnimation showPreviewAnim;
    float yDown;
    float yMove;
    float yTemp;
    String targetIp;
    String targetName;
    Bundle bundle;

    private final int login = 0;
    private final int refresh = 1;
    private final int retry = 2;
    private final int answer = 3;
    private final int startChat = 4;
    private final int incomingMsg = 5;
    private final int redraw = 6;
    private final int close = 7;
    private final int pressure = 8;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.info(TAG, "activity onCreat()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        app = (NetConfApplication) getApplication();
        app.forceClose = false;

        fragmentManager = getFragmentManager();
        Logger.info(TAG, "begin fragments users and chat");
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
        Logger.info(TAG, "end fragments users and chat");

        msgReceiver = new NewMsgReceiver();
        filter = new IntentFilter();
        filter.addAction("net.ui.newMsg");
        filter.addAction("net.ui.chatFrom");

        for (int i = 0; i < answerData.length; i++) {
            Map item = new HashMap();
            item.put("text", answerData[i]);
            answerListData.add(item);
        }
        showPreviewAnim = new AlphaAnimation(0f, 1f);
        hidePreviewAnim = new AlphaAnimation(1f, 0f);
        showPreviewAnim.setDuration(300);
        hidePreviewAnim.setDuration(200);
        bundle = new Bundle();
    }

    @Override
    protected void onResume() {
        registerReceiver(msgReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(msgReceiver);
        app.hideKeyboard(this);
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
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (touchView != null && !touchView.isLock()) {
            Logger.info(TAG, "activity touch event");
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Logger.info(TAG, "in activity touch DOWN");
                    break;
                case MotionEvent.ACTION_UP:
                    Logger.info(TAG, "in activity touch UP");
                    if (!touchView.isShow()) {
                        touchView.clearView();
                        root.removeView(touchView);
                        touchView = null;
                    } else {
                        previewParams.topMargin = topMargin - touchView.getNeedMove();
                        preview.setLayoutParams(previewParams);
                        touchView.setIsLock(true);
                    }
                    yTemp = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    // needMove可能还没准备好
                    if (touchView.getNeedMove() != 0) {
                        yMove = event.getRawY();
                        if (yTemp == 0) {
                            yTemp = yMove;
                        }
                        float gap = yMove - yTemp;

                        // view位于超过显示部分的位置
                        if ((moveTopMargin + gap) <= (topMargin - touchView.getNeedMove())) {
                            // 向上滑减速
                            if (gap < 0) {
                                moveTopMargin = (int) (moveTopMargin + gap * 0.3);
                            }
                            // 向下滑速度正常
                            else {
                                moveTopMargin = (int) (moveTopMargin + gap);
                            }
                            if (!touchView.isShow() && !touchView.running) {
                                touchView.showAnswerList();
                            }
                        }
                        // view处于下压并且继续下拉的状态
                        else if ((moveTopMargin + gap) >= topMargin && yMove > yTemp) {
                            moveTopMargin = (int) (moveTopMargin + gap * 0.1);
                        } else {
                            moveTopMargin = (int) (moveTopMargin + gap);
                            if (touchView.isShow() && !touchView.running) {
                                touchView.hideAnswerList();
                            }
                        }
                        yTemp = yMove;
                        previewParams.topMargin = moveTopMargin;
                        preview.setLayoutParams(previewParams);
                    }
            }
            return true;
        } else {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                yDown = event.getRawY();
            }
            return super.dispatchTouchEvent(event);
        }
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
        Logger.info(TAG, "get commend from fragment==" + commend);
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
            case pressure:
                show3dTouchView((Bundle) obj);
                break;
            default:
                Logger.error(TAG, "====get error commend====" + commend);
        }

    }

    private void show3dTouchView(Bundle bundle) {
        targetIp = bundle.getString("ip");
        targetName = bundle.getString("name");
        // 组装需要显示的界面
        if (custPreview == null)
            custPreview = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.touch_content, null);
        previewContent = (ListView) custPreview.getChildAt(1);
        ((TextView) ((LinearLayout) custPreview.getChildAt(0)).getChildAt(0)).setText(targetName);
        chatAdapter = new ChatMsgAdapter(this, mDataArrays);
        previewContent.setAdapter(chatAdapter);
        mDataArrays.clear();

        // 填充数据
        if (app.chatTempMap.containsKey(targetIp)) {
            Logger.info(TAG, "get new massage");
            app.nManager.cancelAll();
            mDataArrays.addAll(app.chatTempMap.get(targetIp));
            chatAdapter.notifyDataSetChanged();
            previewContent.setSelection(chatAdapter.getCount() - 1);
        }

        // 显示3D touch菜单
        touchView = new ForceTouchViewGroup.Builder(this).
                setBackground(root).
                setIP(bundle.getString("ip")).
                setView(custPreview).
                setData(answerListData).
                setHandler(handler, answer).
                setRoot(root).create();
        touchView.startAnimation(showPreviewAnim);
        preview = (LinearLayout) findViewById(R.id.preview);
        previewParams = (RelativeLayout.LayoutParams) preview.getLayoutParams();
        moveTopMargin = previewParams.topMargin;
        topMargin = previewParams.topMargin;
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
                    case answer:
                        answer(msg);
                        break;
                }
            }
        }
    }

    public void fragmentAction() {
        Logger.info(TAG, "fragmentAction method called");
        if (app.topFragment.equals("users")) {
            fragmentManager.beginTransaction().hide(chat).commit();
        }
        //判断屏幕方向
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            app.isLand = true;
            root = null;
        } else {
            // 竖屏
            app.isLand = false;
            root = (FrameLayout) findViewById(R.id.mainUI);
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

    public void answer(Message msg) {
        // 获取数组的索引
        int i = msg.arg1;
        if ((i + 1) != answerData.length) {
            // 直接回复
            final DataPacket dp = new DataPacket(
                    NetConfApplication.hostIP, android.os.Build.MODEL, answerData[i],
                    NetConfApplication.text);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        app.sendUdpData(new DatagramSocket(),
                                JSON.toJSONString(dp), targetIp,
                                NetConfApplication.textPort);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // 清理操作
            app.chatTempMap.remove(targetIp);
            msg.what = redraw;
            users.getCommend(msg);
        } else {
            // 显示
            bundle.putString("ip", targetIp);
            bundle.putString("name", targetName);
            msg.what = startChat;
            msg.obj = bundle;
            chat.getCommend(msg);
            fragmentManager.beginTransaction().show(chat).commit();
        }
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