package net.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
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
import net.ui.PullRefreshListView.PullToRefreshListener;
import net.util.CreateQRImage;
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

public class UserListActivity extends Activity {
    private List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
    private List<Map<String, Object>> answerListData = new ArrayList<Map<String, Object>>();

    private final int login = 0;
    private final int refresh = 1;
    private final int retry = 2;
    private final int answer = 3;

    private SimpleAdapter userInfoAdapter;
    private PullRefreshListView pullRefreshListView;
    private DrawerLayout drawerLayout;

    private Handler handler = new ListHandler(this);
    private NetConfApplication app;

    private FrameLayout root;
    private LinearLayout listConent;

    // 按两次退出的计时
    private long exitTime = 0;

    // 是否界面加载完成
    private boolean isReady = false;

    // 更新消息提示的广播
    NewMsgReceiver msgReceiver;
    IntentFilter filter;

    // 屏幕长宽
    int screenWidth;
    int screenHeight;
    int statusBarHeight;

    // 菜单是否显示
    boolean isMenuOpen = false;
    int menuWidth = 180;//单位dp
    LinearLayout menu;
    View.OnClickListener onMenuClickListener;
    ScaleAnimation hideMenuAnim;
    ScaleAnimation showMenuAnim;
    AlphaAnimation hidePreviewAnim;
    AlphaAnimation showPreviewAnim;

    int send = 0;
    int get = 1;

    ForceTouchViewGroup touchView;
    ChatMsgAdapter chatAdapter;
    List<ChatMsgEntity> mDataArrays = new ArrayList<ChatMsgEntity>();
    ListView previewContent;
    float yDown;
    float yMove;
    float yTemp;
    int topMargin;
    RelativeLayout.LayoutParams previewParams;
    int moveTopMargin;
    LinearLayout preview;
    String[] answerData = new String[]{"好", "谢谢", "晚点再说", "自定义"};
    String targetIp;
    String targetName;
    long[] pattern = {100, 200};
    Vibrator vibrator;

    @Override
    protected void onResume() {
        if (isReady) {
            registerReceiver(msgReceiver, filter);
            getUserData();
            userInfoAdapter.notifyDataSetChanged();
        }

        super.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (NetConfApplication) getApplication();

        // 获取屏幕长宽和状态栏高度
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;// 获取屏幕分辨率宽度
        screenHeight = dm.heightPixels;
        Rect frame = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        statusBarHeight = frame.top;

        showMenuAnim = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF,
                1f, Animation.RELATIVE_TO_SELF, 0f);
        hideMenuAnim = new ScaleAnimation(1f, 0f, 1f, 0f, Animation.RELATIVE_TO_SELF,
                1f, Animation.RELATIVE_TO_SELF, 0f);
        showPreviewAnim = new AlphaAnimation(0f, 1f);
        hidePreviewAnim = new AlphaAnimation(1f, 0f);
        showMenuAnim.setDuration(100);
        hideMenuAnim.setDuration(100);
        showPreviewAnim.setDuration(300);
        hidePreviewAnim.setDuration(200);

        onMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.exit:
                        finish();
                        break;

                    case R.id.openFolder:
                        startActivity(new Intent(UserListActivity.this, FileListActivity.class));
                        break;

                    case R.id.sendProgress:
                        Intent intentSend = new Intent(UserListActivity.this, ProgressBarListActivity.class);
                        intentSend.setFlags(send);
                        startActivity(intentSend);
                        break;

                    case R.id.getProgress:
                        Intent intentGet = new Intent(UserListActivity.this, ProgressBarListActivity.class);
                        intentGet.setFlags(get);
                        startActivity(intentGet);
                        break;

                    case R.id.scan:
                        Intent intentScan = new Intent(UserListActivity.this, CaptureActivity.class);
                        intentScan.setFlags(get);
                        startActivity(intentScan);
                        break;
                    default:
                        break;
                }
                hideMenu();
            }
        };

        for (int i = 0; i < answerData.length; i++) {
            Map item = new HashMap();
            item.put("text", answerData[i]);
            answerListData.add(item);
        }
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        // 建立界面
        initUI();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
                return false;
            } else {
                finish();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        if (isReady) {
            unregisterReceiver(msgReceiver);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, BroadcastMonitorService.class));
        stopService(new Intent(this, UdpDataMonitorService.class));
        stopService(new Intent(this, FileMonitorService.class));
        stopService(new Intent(this, ScreenMonitorService.class));
        super.onDestroy();
    }

    private void login() {
        NetConfApplication.hostIP = app.getHostIp(this);// 获取ip地址
        Host host = app.hostList.get(0);
        host.setState(0);
        host.setIp(NetConfApplication.hostIP);

        // 广播登录信息
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    // 加入在线列表
                    app.sendUdpData(new DatagramSocket(),
                            JSON.toJSONString(app.hostList.get(0)),
                            NetConfApplication.broadcastIP,
                            NetConfApplication.broadcastPort);

                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initUI() {
        setContentView(R.layout.user_list);
        root = (FrameLayout) findViewById(R.id.mainContent);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        listConent = (LinearLayout) findViewById(R.id.listContent);

        root.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isMenuOpen) {
                    hideMenu();
                    return true;
                } else return false;
            }
        });

        findViewById(R.id.left_drawer).setOnTouchListener(
                new OnTouchListener() {
                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // 吞掉点击事件使下拉刷新失效
                        return true;
                    }
                });

        getDeviceInfo();

        ImageView moreMenu = (ImageView) findViewById(R.id.moreMenu);
        moreMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMenuOpen) {
                    if (menu == null) {
                        Logger.info(this.toString(), "creat a new menu");
                        LayoutInflater layoutInflater = getLayoutInflater();
                        menu = (LinearLayout) layoutInflater.inflate(R.layout.more_menu, null);
                        FrameLayout.LayoutParams params = new FrameLayout.
                                LayoutParams((int) dp2px(menuWidth),
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.topMargin = (int) (dp2px(48) + statusBarHeight + 2);
                        params.leftMargin = (int) (screenWidth - dp2px(menuWidth) - 4);
                        menu.setLayoutParams(params);

                        root.addView(menu);
                        findViewById(R.id.getProgress).setOnClickListener(onMenuClickListener);
                        findViewById(R.id.sendProgress).setOnClickListener(onMenuClickListener);
                        findViewById(R.id.exit).setOnClickListener(onMenuClickListener);
                        findViewById(R.id.scan).setOnClickListener(onMenuClickListener);
                        findViewById(R.id.openFolder).setOnClickListener(onMenuClickListener);
                    } else {
                        root.addView(menu);
                    }

                    showMenu();
                    isMenuOpen = true;
                    if (pullRefreshListView != null)
                        pullRefreshListView.setCanRefresh(false);
                }
            }
        });

        // 延迟一点加载列表
        if (app.wifi == 1) {
            loadUserList();
        } else {
            // 弹出警告框并退出
            new AlertDialog.Builder(this).setTitle("错误")
                    .setMessage("wifi未连接或端口异常，启动失败")
                    .setPositiveButton("退出",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setResult(RESULT_OK);// 确定按钮事件
                                    finish();
                                }
                            })
                    .setNegativeButton("重试",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    app.check(UserListActivity.this);
                                    Message msg = handler.obtainMessage();
                                    msg.what = retry;
                                    handler.sendMessageDelayed(msg, 2000);
                                }
                            }).setCancelable(false).show();
        }
    }

    private void showMenu() {
        menu.startAnimation(showMenuAnim);
    }

    private void hideMenu() {
        menu.startAnimation(hideMenuAnim);
        root.removeView(menu);
        if (pullRefreshListView != null)
            pullRefreshListView.setCanRefresh(true);
        isMenuOpen = false;
    }

    private void loadUserList() {
        // 主机登录
        login();

        // 注册广播
        msgReceiver = new NewMsgReceiver();
        filter = new IntentFilter();
        filter.addAction("net.ui.newMsg");
        registerReceiver(msgReceiver, filter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = handler.obtainMessage();
                msg.what = login;
                handler.sendMessageDelayed(msg, 2000);
            }
        }).start();
    }

    private void getDeviceInfo() {
        String manufacturerName = android.os.Build.MANUFACTURER;
        String systemVersion = android.os.Build.VERSION.RELEASE;
        String deviceName = android.os.Build.HARDWARE;
        String appVersion = "";

        TelephonyManager tm = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);

        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersion = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView t1 = (TextView) findViewById(R.id.device_name);
        TextView t2 = (TextView) findViewById(R.id.system_version);
        TextView t3 = (TextView) findViewById(R.id.operate_name);
        TextView t4 = (TextView) findViewById(R.id.mcc_mnc);
        TextView t5 = (TextView) findViewById(R.id.manufacturer_name);
        TextView t6 = (TextView) findViewById(R.id.appVersion);

        t1.setText("：" + deviceName);
        t2.setText("：Android " + systemVersion);
        t3.setText("：" + tm.getNetworkOperatorName());
        t4.setText("：" + tm.getNetworkOperator());
        t5.setText("：" + manufacturerName);
        t6.setText("版本号：" + appVersion);

        ImageView QRImg = (ImageView) findViewById(R.id.QRCode);
        new CreateQRImage(android.os.Build.MODEL + "!!!!" + app.hostIP, QRImg,
                this);
    }

    private List<Map<String, Object>> getUserData() {
        userList.clear();
        for (Host host : app.hostList) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("name", host.getUserName());
            item.put("ip", host.getIp());
            if (app.chatTempMap.containsKey(host.getIp())) {
                item.put("img", R.drawable.unread);
            } else {
                item.put("img", 0);
            }
            userList.add(item);
        }
        return userList;
    }

    class NewMsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            getUserData();
            userInfoAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (touchView != null && !touchView.isLock()) {
            Logger.info(this.toString(), "activity touch event");
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Logger.info(this.toString(), "in activity touch DOWN");
                    break;
                case MotionEvent.ACTION_UP:
                    Logger.info(this.toString(), "in activity touch UP");
                    if (!touchView.isShow()) {
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
                            Logger.info(this.toString(), "slow up");
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
                            Logger.info(this.toString(), "slow down");
                            moveTopMargin = (int) (moveTopMargin + gap * 0.1);
                        } else {
                            Logger.info(this.toString(), "normal move");
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

    public float dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return dp * scale + 0.5f;
    }

    class ListHandler extends Handler {
        WeakReference<UserListActivity> refActvity;

        ListHandler(UserListActivity activity) {
            refActvity = new WeakReference<UserListActivity>(activity);
        }

        @SuppressLint("InflateParams")
        @Override
        public void handleMessage(Message msg) {
            final UserListActivity act = refActvity.get();

            if (act != null) {
                switch (msg.what) {
                    case login:
                        userInfoAdapter = new SimpleAdapter(act, getUserData(),
                                R.layout.user_item, new String[]{"name", "ip",
                                "img"}, new int[]{R.id.userName,
                                R.id.userIP, R.id.unread});
                        Logger.info(this.toString(),
                                String.valueOf(app.hostList.size()));
                        isReady = true;

                        // 更新UI
                        LayoutInflater layoutInflater = getLayoutInflater();
                        pullRefreshListView = (PullRefreshListView) layoutInflater
                                .inflate(R.layout.users, null);

                        listConent.removeView(findViewById(R.id.wait));
                        listConent.addView(pullRefreshListView, new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                        pullRefreshListView.getListView().setAdapter(userInfoAdapter);
                        pullRefreshListView.getListView().setOnItemClickListener(new OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                TextView name = (TextView) view.findViewById(R.id.userName);
                                TextView ip = (TextView) view.findViewById(R.id.userIP);

                                if (ip.getText().equals(NetConfApplication.hostIP)) {
                                    drawerLayout.openDrawer(Gravity.LEFT);
                                } else {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("name", name.getText().toString());
                                    bundle.putString("ip", ip.getText().toString());
                                    Intent intent = new Intent(act, ChatActivity.class);
                                    intent.putExtras(bundle);
                                    startActivity(intent);
                                }
                            }
                        });
                        // 模仿iphone的3D Touch效果
                        pullRefreshListView.getListView().
                                setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                                    @Override
                                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                                        Logger.info(this.toString(), "in 3d touch effect");
                                        TextView name = (TextView) view.findViewById(R.id.userName);
                                        TextView ip = (TextView) view.findViewById(R.id.userIP);

                                        // 组装需要显示的界面
                                        targetIp = ip.getText().toString();
                                        targetName = name.getText().toString();
                                        LinearLayout custPreview = (LinearLayout) LayoutInflater.from(act).inflate(R.layout.touch_content, null);
                                        previewContent = (ListView) custPreview.getChildAt(1);
                                        ((TextView) ((LinearLayout) custPreview.getChildAt(0)).getChildAt(0)).setText(name.getText());
                                        chatAdapter = new ChatMsgAdapter(act, mDataArrays);
                                        previewContent.setAdapter(chatAdapter);
                                        mDataArrays.clear();
                                        // 填充数据
                                        if (app.chatTempMap.containsKey(targetIp)) {
                                            Logger.info(this.toString(), "get new massage");
                                            app.nManager.cancelAll();
                                            mDataArrays.addAll(app.chatTempMap.get(targetIp));
                                            chatAdapter.notifyDataSetChanged();
                                            previewContent.setSelection(chatAdapter.getCount() - 1);
                                        }

                                        // 振动提示
                                        vibrator.vibrate(pattern, -1);

                                        // 显示3D touch菜单
                                        touchView = new ForceTouchViewGroup.Builder(act).
                                                setBackground(root).
                                                setIP(ip.getText().toString()).
                                                setView(custPreview).
                                                setData(answerListData).
                                                setHandler(handler, answer).
                                                setRoot(root).create();
                                        touchView.startAnimation(showPreviewAnim);
                                        preview = (LinearLayout) findViewById(R.id.preview);
                                        previewParams = (RelativeLayout.LayoutParams) preview.getLayoutParams();
                                        moveTopMargin = previewParams.topMargin;
                                        topMargin = previewParams.topMargin;
                                        return true;
                                    }
                                });

                        pullRefreshListView.setPullListener(new PullToRefreshListener() {

                            @Override
                            public void onRefresh() {
                                app.hostList.clear();
                                String userName = android.os.Build.MODEL;// 获取用户名
                                String hostName = "Android";// 获取主机名
                                String userDomain = "Android";// 获取计算机域

                                // 加入在线列表
                                Host host = new Host(userName, userDomain,
                                        NetConfApplication.hostIP, hostName, 1, 0);
                                app.addHost(host);
                                try {
                                    app.sendUdpData(new DatagramSocket(), JSON.toJSONString(host),
                                            NetConfApplication.broadcastIP, NetConfApplication.broadcastPort);
                                } catch (SocketException e) {
                                    e.printStackTrace();
                                }
                                Message msg = handler.obtainMessage();
                                msg.what = refresh;
                                handler.sendMessageDelayed(msg, 2000);
                            }
                        });
                        break;
                    case refresh:
                        pullRefreshListView.finishRefreshing();
                        getUserData();
                        userInfoAdapter.notifyDataSetChanged();
                        break;
                    case retry:
                        if (app.wifi == 1) {
                            // wifi打开
                            loadUserList();
                        } else {
                            // wifi关闭,给出提示
                            new AlertDialog.Builder(act)
                                    .setTitle("错误")
                                    .setMessage("wifi未连接或端口异常，启动失败")
                                    .setPositiveButton("退出",
                                            new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int which) {
                                                    setResult(RESULT_OK);// 确定按钮事件
                                                    finish();
                                                }
                                            })
                                    .setNegativeButton("重试",
                                            new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int which) {
                                                    app.check(act);
                                                    Message msg = obtainMessage();
                                                    msg.what = retry;
                                                    sendMessageDelayed(msg, 2000);
                                                }
                                            }).setCancelable(false).show();
                        }
                        break;
                    case answer:
                        Logger.info(this.toString(), "get answer from 3d touch");
                        // 获取数组的索引
                        int i = msg.arg1;
                        if ((i + 1) != answerData.length) {
                            // 直接回复
                            final DataPacket dp = new DataPacket(
                                    NetConfApplication.hostIP, android.os.Build.MODEL, answerData[i],
                                    NetConfApplication.text);
                            // 清理操作
                            app.chatTempMap.remove(targetIp);
                            getUserData();
                            userInfoAdapter.notifyDataSetChanged();

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
                        } else {
                            // 启动聊天窗口
                            Bundle bundle = new Bundle();
                            bundle.putString("name", targetName);
                            bundle.putString("ip", targetIp);
                            Intent intent = new Intent(act, ChatActivity.class);
                            intent.putExtras(bundle);
                            startActivity(intent);
                        }
                        break;
                    default:
                        Logger.error(this.toString(), "error======no event match=======");
                        break;
                }
            }
        }
    }

    // TODO: 2015/12/10
    /*
     * 有的机器上，消息的声音不响
     * 沉浸式菜单
     */
}
