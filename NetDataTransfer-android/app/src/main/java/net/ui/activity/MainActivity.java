package net.ui.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
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
import net.base.BaseActivity;
import net.db.DBManager;
import net.log.Logger;
import net.service.BroadcastMonitorService;
import net.service.FileMonitorService;
import net.service.LoginMonitorService;
import net.service.UdpDataMonitorService;
import net.base.BaseFragment;
import net.ui.fragment.ChatFragment;
import net.ui.fragment.CustAlertDialog;
import net.ui.fragment.UserListFragment;
import net.ui.view.ForceTouchViewGroup;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import net.vo.Host;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class MainActivity extends BaseActivity implements BaseFragment.Notification {
    private static final String TAG = "MainActivity";
    private final String user_TAG = "usersFragment";
    private final String chat_TAG = "chatFragment";
    private Handler handler = new ListHandler(this);
    private ActionListener listener = new ActionListener(this);
    private UserListFragment users;
    private ChatFragment chat;
    private FragmentManager fragmentManager;
    String permission = "com.android.permission.RECV_NDT_NOTIFY";

    // 按两次退出的计时
    private long exitTime = 0;
    // 防止转屏退出程序
    private boolean isExit;

    // 更新消息提示的广播
    NewMsgReceiver msgReceiver;
    IntentFilter filter;

    // 3D touch效果
    ListView previewContent;
    List<Map<String, Object>> answerListData = new ArrayList<>();
    List<ChatMsgEntity> mDataArrays = new ArrayList<>();
    RelativeLayout.LayoutParams previewParams;
    RelativeLayout.LayoutParams answerParams;
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
    Host host;

    private static final int login = 0;
    private static final int refresh = 1;
    private static final int retry = 2;
    private static final int answer = 3;
    private static final int startChat = 4;
    private static final int incomingMsg = 5;
    private static final int redraw = 6;
    private static final int close = 7;
    private static final int pressure = 8;
    private static final int exit = 9;

    private final int SHOW = 1;
    private final int HIDE = 0;

    private static final int add = 0;
    private static final int remove = 1;
    ForceTouchViewGroup.Builder builder;
    DialogInterface.OnClickListener loadingListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            notifyInfo(retry, null);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.info(TAG, "activity onCreat()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        app.forceClose = false;
        isExit = true;

        if (savedInstanceState != null) {
            // 防止转屏时导致列表混乱
            app.hostList.clear();
            String s = savedInstanceState.getString("users");
            Vector<Host> list = new Vector<>(JSON.parseArray(s, Host.class));
            Logger.info(TAG, "save host num is " + list.size());
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
        login(refresh);
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
        isExit = false;
    }

    @Override
    protected void onDestroy() {
        if (app.forceClose) {
            stopService(new Intent(this, LoginMonitorService.class));
            stopService(new Intent(this, UdpDataMonitorService.class));
            stopService(new Intent(this, FileMonitorService.class));
            stopService(new Intent(this, BroadcastMonitorService.class));
        }
        handler.removeCallbacksAndMessages(null);
        app.hostList.clear();
        Logger.info(TAG, "host list length: " + app.hostList.size());
        new DBManager(this).closeDB();
        try {
            new DatagramSocket(app.textPort).close();
            new ServerSocket(app.filePort).close();
        } catch (SocketException e) {
            Logger.error(TAG, "socket close error" + e.toString());
        } catch (IOException e) {
            Logger.error(TAG, "socket close error" + e.toString());
        }
        super.onDestroy();
        if (isExit) {
            Logger.info(TAG, "exit apk");
            System.exit(0);
        }
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
                    touchView.setCanMove(true);
                    if (!touchView.isShow()) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        root.removeView(touchView);
                        touchView = null;
                    } else {
                        previewParams.topMargin = topMargin - touchView.getNeedMove();
                        custPreview.setLayoutParams(previewParams);
                        answerParams.topMargin = topMargin - touchView.getNeedMove() +
                                getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin) +
                                custPreview.getMeasuredHeight();
                        touchView.answerList.setLayoutParams(answerParams);
                        touchView.setIsLock(true);
                    }
                    yTemp = 0;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    Logger.info(TAG, "in activity touch ACTION_POINTER_DOWN");
                    touchView.setCanMove(false);
                    Logger.info(TAG, "stop move");
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    Logger.info(TAG, "in activity touch ACTION_POINTER_UP");
                    touchView.setCanMove(true);
                    if (!touchView.isShow()) {
                        root.removeView(touchView);
                        touchView = null;
                        yTemp = 0;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    // needMove可能还没准备好
                    if (touchView.getNeedMove() != 0 && touchView.isCanMove()) {
                        // Logger.info(TAG, "in activity touch MOVE");
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
                        }
                        // view处于下压并且继续下拉的状态
                        else if ((moveTopMargin + gap) >= topMargin && yMove > yTemp) {
                            moveTopMargin = (int) (moveTopMargin + gap * 0.15);
                        } else {
                            moveTopMargin = (int) (moveTopMargin + gap);
                        }
                        yTemp = yMove;
                        previewParams.topMargin = moveTopMargin;
                        custPreview.setLayoutParams(previewParams);

                        if (!touchView.running && moveTopMargin >= (topMargin - touchView.getNeedMove()) &&
                                moveTopMargin <= (topMargin - touchView.getNeedMove() + NetConfApplication.upMoveCache)) {
                            if (!touchView.isShow && gap < 0) {
                                Logger.info(TAG, "start running animator");
                                touchView.showAnswerList(moveTopMargin + custPreview.getHeight() +
                                        getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin));
                            } else if (touchView.isShow) {
                                answerParams.topMargin = moveTopMargin + custPreview.getHeight() +
                                        getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin);
                                touchView.answerList.setLayoutParams(answerParams);
                            }
                        }

                        if (!touchView.running && moveTopMargin < (topMargin - touchView.getNeedMove())) {
                            answerParams.topMargin = topMargin - touchView.getNeedMove() +
                                    getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin) +
                                    custPreview.getMeasuredHeight();
                            touchView.answerList.setLayoutParams(answerParams);
                        }

                        if (moveTopMargin > (topMargin - touchView.getNeedMove() + NetConfApplication.downMoveCache) &&
                                !touchView.running && touchView.isShow && gap > 0) {
                            touchView.hideAnswerList(moveTopMargin + custPreview.getHeight() +
                                    getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin));
                        }
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
                Fragment dialog = getFragmentManager().findFragmentByTag("setup_warn");
                if (dialog != null) {
                    getFragmentManager().beginTransaction().remove(dialog).commit();
                }
                if (obj != null) {
                    CustAlertDialog cd = new CustAlertDialog();
                    cd.setTitle("错误");
                    cd.setAlertText("网络未连接或端口占用，加载失败");
                    cd.setListener(loadingListener);
                    cd.setCancelable(false);
                    cd.show(getFragmentManager(), "setup_warn");
                    break;
                }
            case refresh:
                login(commend);
                if (obj == null) {
                    handler.sendEmptyMessageDelayed(commend, 2000);
                } else {
                    handler.sendEmptyMessage(commend);
                }
                break;
            case retry:
                app.check(true);
                listen();
                handler.sendEmptyMessageDelayed(commend, 1000);
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

    @Override
    public void notifyWifiInfo() {
        super.notifyWifiInfo();
        if (app.wifi == 1) {
            login(refresh);
            handler.sendEmptyMessage(refresh);
        }
    }

    private void cleanAndExit() {
        app.forceClose = true;
        app.hostList.clear();
        // new DBManager(this).deleteMsg(null);
        app.nManager.cancel(R.id.chatName);
        if (touchView != null) {
            touchView.answerList.clearAnimation();
            touchView.setActionListener(null);
            touchView.answerList = null;
            touchView.previewContent = null;
        }
        finish();
    }

    private void show3dTouchView(Bundle bundle) {
        targetIp = bundle.getString("ip");
        targetName = bundle.getString("name");

        // 显示3D touch菜单
        if (builder == null)
            builder = new ForceTouchViewGroup.Builder(this);
        touchView = builder.setBackground(root).setIP(targetIp).setView(R.layout.touch_content)
                .setData(answerListData).setHandler(handler, answer).create();
        touchView.setActionListener(listener);
        touchView.show(builder);
        touchView.startAnimation(showPreviewAnim);

        // 组装需要显示的界面
        if (custPreview == null)
            custPreview = (LinearLayout) touchView.findViewById(R.id.cust_preview);
        previewContent = (ListView) custPreview.getChildAt(1);
        ((TextView) ((LinearLayout) custPreview.getChildAt(0)).getChildAt(0)).setText(targetName);
        chatAdapter = new ChatMsgAdapter(new WeakReference<>(this), mDataArrays);
        previewContent.setAdapter(chatAdapter);
        mDataArrays.clear();

        // 填充数据
        ArrayList<ChatMsgEntity> list = new DBManager(this).queryMsg(targetIp);
        if (list != null && list.size() > 0) {
            Logger.info(TAG, "get new massage");
            app.nManager.cancelAll();
            mDataArrays.addAll(list);
            chatAdapter.notifyDataSetChanged();
            previewContent.setSelection(chatAdapter.getCount() - 1);
        }

        previewParams = (RelativeLayout.LayoutParams) custPreview.getLayoutParams();
        answerParams = (RelativeLayout.LayoutParams) touchView.answerList.getLayoutParams();
        moveTopMargin = previewParams.topMargin;
        topMargin = previewParams.topMargin;
    }

    static class ActionListener implements ForceTouchViewGroup.ActionListener {
        WeakReference<MainActivity> refActvity;

        ActionListener(MainActivity activity) {
            refActvity = new WeakReference<>(activity);
        }

        @Override
        public void updateUI(int cmd) {
            final MainActivity act = refActvity.get();
            switch (cmd) {
                case add:
                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    act.root.addView(act.touchView);
                    Logger.info(TAG, "answerlist height is = " + act.touchView.answerList.getHeight());
                    break;
                case remove:
                    act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    act.root.removeView(act.touchView);
                    break;
            }
        }
    }

    static class ListHandler extends Handler {
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
                        act.getView(R.id.chat).setVisibility(View.VISIBLE);
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
        if (!app.topFragment.equals("users")) {
            Logger.info(TAG, "show chat fragment");
            animFragmentEffect(SHOW, chat);
        } else {
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
            root = getView(R.id.mainUI);
        }
    }

    private void login(int commend) {
        NetConfApplication.hostIP = app.getHostIp(this);// 获取ip地址
        if (commend == refresh) {
            if (app.hostList.isEmpty()) {
                String userName = Build.MODEL;// 获取用户名
                String hostName = "Android";// 获取主机名
                String userDomain = "Android";// 获取计算机域
                app.hostList.add(new Host(userName, userDomain,
                        NetConfApplication.hostIP, hostName, 1, 0));
            }
            host = app.hostList.get(0);
            host.setState(0);
            host.setIp(NetConfApplication.hostIP);
        } else if (commend == login) {
            String userName = Build.MODEL;// 获取用户名
            String hostName = "Android";// 获取主机名
            String userDomain = "Android";// 获取计算机域

            // 加入在线列表
            host = new Host(userName, userDomain,
                    NetConfApplication.hostIP, hostName, 1, 0);
        }
        app.hostList.clear();
        app.addHost(host);

        // 广播登录信息
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    // 加入在线列表
                    String hostInfo = JSON.toJSONString(host);
                    Logger.info(TAG, "send json data is-----" + hostInfo);
                    app.sendUdpData(new DatagramSocket(), hostInfo, NetConfApplication.broadcastIP,
                            NetConfApplication.broadcastPort);

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
                        String dpInf = JSON.toJSONString(dp);
                        Logger.info(TAG, "send json data is-----" + dpInf);
                        app.sendUdpData(new DatagramSocket(), dpInf, targetIp,
                                NetConfApplication.textPort);
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // 清理操作
            new DBManager(this).deleteMsg(targetIp);
            msg.what = redraw;
            users.getCommend(msg);
        } else {
            // 显示
            bundle.putString("ip", targetIp);
            bundle.putString("name", targetName);
            msg.what = startChat;
            msg.obj = bundle;
            getView(R.id.chat).setVisibility(View.VISIBLE);
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
            getView(R.id.chat).setVisibility(View.VISIBLE);
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

}