package net.ui.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.InputMethodManager;
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
import net.util.HelpUtils;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import net.vo.Host;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MainActivity extends Activity implements BaseFragment.Notification {
    private final String TAG = "MainActivity";
    private final String user_TAG = "usersFragment";
    private final String chat_TAG = "chatFragment";
    private Handler handler = new ListHandler(this);
    private UserListFragment users;
    private ChatFragment chat;
    private FragmentManager fragmentManager;
    private NetConfApplication app;
    String permission = "com.android.permission.RECV_NDT_NOTIFY";

    // 按两次退出的计时
    private long exitTime = 0;

    // 更新消息提示的广播
    NewMsgReceiver msgReceiver;
    IntentFilter filter;

    // 3D touch效果
    ListView previewContent;
    List<Map<String, Object>> answerListData = new ArrayList<>();
    List<ChatMsgEntity> mDataArrays = new ArrayList<>();
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
    private final int exit = 9;

    private final int SHOW = 1;
    private final int HIDE = 0;

    private final int add = 0;
    private final int remove = 1;
    ForceTouchViewGroup.Builder builder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.info(TAG, "activity onCreat()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        app = (NetConfApplication) getApplication();
        app.forceClose = false;

        if (savedInstanceState != null) {
            String s = savedInstanceState.getString("users");
            Vector<Host> list = new Vector<>(JSON.parseArray(s, Host.class));
            for (Host h : list) {
                app.hostList.add(h);
            }
            savedInstanceState.clear();
        }

        fragmentManager = getFragmentManager();
        Logger.info(TAG, "begin fragments users and chat");
        users = (UserListFragment) fragmentManager.findFragmentByTag(user_TAG);
        chat = (ChatFragment) fragmentManager.findFragmentByTag(chat_TAG);

        if (chat == null) {
            chat = new ChatFragment();
            fragmentManager.beginTransaction().add(R.id.chat, chat, chat_TAG).commit();
        }

        if (users == null) {
            users = new UserListFragment();
            fragmentManager.beginTransaction().add(R.id.users, users, user_TAG).commit();
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Bundle b = getIntent().getExtras();
        if (b != null) {
            Message msg = handler.obtainMessage();
            msg.obj = b;
            msg.what = startChat;
            msg.sendToTarget();
        }
    }

    @Override
    protected void onResume() {
        registerReceiver(msgReceiver, filter, permission, null);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(msgReceiver);
        app.hideKeyboard(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("users", JSON.toJSONString(app.hostList));
        Logger.info(TAG, "in onSaveInstanceState");
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
        app.hostList.clear();
        Logger.info(TAG, "host list length: " + app.hostList.size());
        fixInputMethodManagerLeak(this);
        System.exit(0);
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (touchView != null && !touchView.isLock()) {
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
                cleanAndExit();
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
                animFragmentEffect(HIDE, chat);
                break;
            case redraw:
                handler.sendEmptyMessage(commend);
                break;
            case pressure:
                show3dTouchView((Bundle) obj);
                break;
            case exit:
                cleanAndExit();
                break;
            default:
                Logger.error(TAG, "====get error commend====" + commend);
        }
    }

    private void cleanAndExit() {
        app.forceClose = true;
        app.hostList.clear();
        app.chatTempMap.clear();
        app.nManager.cancel(R.id.chatName);
        if (touchView != null) {
            touchView.setActionListener(null);
            touchView.preview = null;
            touchView.answerList = null;
            touchView.hideAnswerList = null;
            touchView.showAnswerList = null;
            touchView.previewContent = null;
            ForceTouchViewGroup.setInstance();
            builder.previewContent = null;
        }
        finish();
    }

    private void show3dTouchView(Bundle bundle) {
        targetIp = bundle.getString("ip");
        targetName = bundle.getString("name");
        // 组装需要显示的界面
        if (custPreview == null)
            custPreview = (LinearLayout) LayoutInflater.from(app).inflate(R.layout.touch_content, null);
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
        builder = new ForceTouchViewGroup.Builder(this);
        touchView = builder.setBackground(root).setIP(bundle.getString("ip")).
                setView(custPreview).setData(answerListData).
                setHandler(handler, answer).create();

        touchView.setActionListener(new ForceTouchViewGroup.ActionListener() {
            @Override
            public void updateUI(int cmd) {

                switch (cmd) {
                    case add:
                        root.addView(touchView);
                        break;
                    case remove:
                        root.removeView(touchView);
                        break;
                }
            }
        });
        touchView.show(builder);

        touchView.startAnimation(showPreviewAnim);
        preview = HelpUtils.getView(this, R.id.preview);
        previewParams = (RelativeLayout.LayoutParams) preview.getLayoutParams();
        moveTopMargin = previewParams.topMargin;
        topMargin = previewParams.topMargin;
    }

    static class ListHandler extends Handler {
        private final int login = 0;
        private final int refresh = 1;
        private final int retry = 2;
        private final int answer = 3;
        private final int startChat = 4;
        private final int incomingMsg = 5;
        private final int redraw = 6;
        private final int close = 7;
        private final int pressure = 8;

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
                        act.users.getCommend(msg);
                        break;
                    case startChat:
                        act.findViewById(R.id.chat).setVisibility(View.VISIBLE);
                        act.chat.getCommend(msg);
                        // 显示chatFragment
                        act.animFragmentEffect(act.SHOW, act.chat);
                        break;
                    case answer:
                        act.answer(msg);
                        break;
                }
            }
        }
    }

    public void fragmentAction() {
        Logger.info(TAG, "fragmentAction method called");
        if (app.topFragment.equals("users")) {
            animFragmentEffect(HIDE, chat);
        }
        //判断屏幕方向
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏
            app.isLand = true;
            root = null;
        } else {
            // 竖屏
            app.isLand = false;
            root = HelpUtils.getView(this, R.id.mainUI);
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
            String userName = Build.MODEL;// 获取用户名
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
                    NetConfApplication.hostIP, Build.MODEL, answerData[i],
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
            findViewById(R.id.chat).setVisibility(View.VISIBLE);
            chat.getCommend(msg);
            animFragmentEffect(SHOW, chat);
        }
    }

    /**
     * fragment切换的动画效果
     * commend：0 隐藏，1 显示
     */
    public void animFragmentEffect(int commend, Fragment fragment) {
        if (commend == 0) {
            //hide
            fragmentManager.beginTransaction().
                    setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).
                    hide(fragment).commit();
        } else {
            //show
            fragmentManager.beginTransaction().
                    setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out).
                    show(fragment).commit();
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

    public void fixInputMethodManagerLeak(Context destContext) {
        if (destContext == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) destContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }

        String[] arr = new String[]{"mCurRootView", "mServedView", "mNextServedView"};
        Field f = null;
        Object obj_get = null;
        for (int i = 0; i < arr.length; i++) {
            String param = arr[i];
            try {
                f = imm.getClass().getDeclaredField(param);
                if (f.isAccessible() == false) {
                    f.setAccessible(true);
                } // author: sodino mail:sodino@qq.com
                obj_get = f.get(imm);
                if (obj_get != null && obj_get instanceof View) {
                    View v_get = (View) obj_get;
                    if (v_get.getContext() == destContext) { // 被InputMethodManager持有引用的context是想要目标销毁的
                        f.set(imm, null); // 置空，破坏掉path to gc节点
                    } else {
                        // 不是想要目标销毁的，即为又进了另一层界面了，不要处理，避免影响原逻辑,也就不用继续for循环了
                        break;
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}