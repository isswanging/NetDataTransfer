package net.ui;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.app.NetConfApplication;
import net.log.Logger;
import net.service.BroadcastMonitorService;
import net.service.FileMonitorService;
import net.service.UdpDataMonitorService;
import net.ui.PullRefreshListView.PullToRefreshListener;
import net.vo.Host;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.netdatatransfer.R;

public class UserListActivity extends Activity {
    private List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();

    private final int login = 0;
    private final int refresh = 1;

    private SimpleAdapter adapter;
    private PullRefreshListView pullRefreshListView;
    private DrawerLayout drawerLayout;

    private Handler handler = new ListHandler(this);
    private NetConfApplication app;

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        setOverflowIconVisible(featureId, menu);
        return super.onMenuOpened(featureId, menu);
    }

    // 按两次退出的计时
    private long exitTime = 0;

    // 是否界面加载完成
    private boolean isReady = false;

    // 更新消息提示的广播
    newMsgReceiver msgReceiver;
    IntentFilter filter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        if (isReady) {
            registerReceiver(msgReceiver, filter);
            getData();
            adapter.notifyDataSetChanged();
        }

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        app = (NetConfApplication) getApplication();
        getActionBar().setTitle(getResources().getString(R.string.titleName));
        super.onCreate(savedInstanceState);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        int send = 0;
        int get = 1;

        switch (item.getItemId()) {
        case R.id.exit:
            finish();
            break;
        case R.id.openFolder:
            startActivity(new Intent(this, FileListActivity.class));
            break;

        case R.id.sendProgress:
            Intent intentSend = new Intent(this, ProgressBarListActivity.class);
            intentSend.setFlags(send);
            startActivity(intentSend);
            break;

        case R.id.getProgress:
            Intent intentGet = new Intent(this, ProgressBarListActivity.class);
            intentGet.setFlags(get);
            startActivity(intentGet);
            break;
        default:
            break;
        }
        return false;
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
        super.onDestroy();
    }

    // 在物理菜单键存在时仍然显示溢出菜单
    private void forceShowOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class
                    .getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 利用反射让隐藏在Overflow中的MenuItem显示Icon图标
    private void setOverflowIconVisible(int featureId, Menu menu) {
        if (featureId == Window.FEATURE_ACTION_BAR && menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                }
            }
        }
    }

    private void login() {

        // 广播登录信息
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String userName = android.os.Build.MODEL;// 获取用户名
                    String userDomain = "Android";// 获取计算机域

                    // 加入在线列表
                    Host host = new Host(userName, userDomain,
                            NetConfApplication.hostIP,
                            NetConfApplication.hostName, 1, 0);
                    app.addHost(host);

                    app.sendUdpData(new DatagramSocket(),
                            JSON.toJSONString(host),
                            NetConfApplication.broadcastIP,
                            NetConfApplication.broadcastPort);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initUI() {
        // 主机登录
        login();

        // 显示menu
        forceShowOverflowMenu();
        setContentView(R.layout.user_list);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

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

        // 延迟一点加载列表
        if (app.wifi == 1) {
            // 注册广播
            msgReceiver = new newMsgReceiver();
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
        } else {
            // 弹出警告框并退出
            new AlertDialog.Builder(this)
                    .setTitle("错误")
                    .setMessage("wifi未连接或端口异常，启动失败")
                    .setPositiveButton("确定",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    setResult(RESULT_OK);// 确定按钮事件
                                    finish();
                                }
                            }).setCancelable(false).show();
        }
    }

    private void getDeviceInfo() {
        String manufacturerName = android.os.Build.MANUFACTURER;
        String systemVersion = android.os.Build.VERSION.RELEASE;
        String deviceName = android.os.Build.HARDWARE;

        TelephonyManager tm = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);

        TextView t1 = (TextView) findViewById(R.id.device_name);
        TextView t2 = (TextView) findViewById(R.id.system_version);
        TextView t3 = (TextView) findViewById(R.id.operate_name);
        TextView t4 = (TextView) findViewById(R.id.mcc_mnc);
        TextView t5 = (TextView) findViewById(R.id.manufacturer_name);
        t1.setText("：" + deviceName);
        t2.setText("：Android " + systemVersion);
        t3.setText("：" + tm.getNetworkOperatorName());
        t4.setText("：" + tm.getNetworkOperator());
        t5.setText("：" + manufacturerName);

    }

    private List<Map<String, Object>> getData() {
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

    class newMsgReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            getData();
            adapter.notifyDataSetChanged();
        }
    }

    static class ListHandler extends Handler {
        WeakReference<UserListActivity> refActvity;

        ListHandler(UserListActivity activity) {
            refActvity = new WeakReference<UserListActivity>(activity);
        }

        @SuppressLint("InflateParams")
        @Override
        public void handleMessage(Message msg) {
            final UserListActivity act = refActvity.get();
            if (act != null) {
                if (msg.what == act.login) {
                    act.adapter = new SimpleAdapter(act, act.getData(),
                            R.layout.user_item, new String[] { "name", "ip",
                                    "img" }, new int[] { R.id.userName,
                                    R.id.userIP, R.id.unread });
                    Logger.info(this.toString(),
                            String.valueOf(act.app.hostList.size()));
                    act.isReady = true;

                    // 更新UI
                    LinearLayout listConent = (LinearLayout) act
                            .findViewById(R.id.listContent);
                    LayoutInflater layoutInflater = act.getLayoutInflater();
                    act.pullRefreshListView = (PullRefreshListView) layoutInflater
                            .inflate(R.layout.users, null);

                    listConent.removeView(act.findViewById(R.id.wait));
                    listConent.addView(act.pullRefreshListView);

                    act.pullRefreshListView.getListView().setAdapter(
                            act.adapter);
                    act.pullRefreshListView.getListView()
                            .setOnItemClickListener(new OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent,
                                        View view, int position, long id) {
                                    TextView name = (TextView) view
                                            .findViewById(R.id.userName);
                                    TextView ip = (TextView) view
                                            .findViewById(R.id.userIP);

                                    if (ip.getText().equals(
                                            NetConfApplication.hostIP)) {
                                        act.drawerLayout
                                                .openDrawer(Gravity.LEFT);
                                    } else {
                                        Bundle bundle = new Bundle();
                                        bundle.putString("name", name.getText()
                                                .toString());
                                        bundle.putString("ip", ip.getText()
                                                .toString());
                                        Intent intent = new Intent(act,
                                                ChatActivity.class);
                                        intent.putExtras(bundle);
                                        act.startActivity(intent);
                                    }
                                }
                            });

                    act.pullRefreshListView
                            .setPullListener(new PullToRefreshListener() {

                                @Override
                                public void onRefresh() {
                                    act.app.hostList.clear();
                                    String userName = android.os.Build.MODEL;// 获取用户名
                                    String hostName = "Android";// 获取主机名
                                    String userDomain = "Android";// 获取计算机域

                                    // 加入在线列表
                                    Host host = new Host(userName, userDomain,
                                            NetConfApplication.hostIP,
                                            hostName, 1, 0);
                                    act.app.addHost(host);
                                    try {
                                        act.app.sendUdpData(
                                                new DatagramSocket(),
                                                JSON.toJSONString(host),
                                                NetConfApplication.broadcastIP,
                                                NetConfApplication.broadcastPort);
                                    } catch (SocketException e) {
                                        e.printStackTrace();
                                    }
                                    Message msg = act.handler.obtainMessage();
                                    msg.what = act.refresh;
                                    act.handler.sendMessageDelayed(msg, 2000);
                                }
                            });
                }

                if (msg.what == act.refresh) {
                    act.pullRefreshListView.finishRefreshing();
                    act.getData();
                    act.adapter.notifyDataSetChanged();
                }
            }
        }
    }
}
