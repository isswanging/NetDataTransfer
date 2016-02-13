package net.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;
import net.ui.activity.CaptureActivity;
import net.ui.activity.FileListActivity;
import net.ui.activity.ProgressBarListActivity;
import net.ui.view.PullRefreshListView;
import net.util.CreateQRImage;
import net.vo.Host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserListFragment extends BaseFragment {
    private final String TAG = "UserListFragment";
    NetConfApplication app;
    // 屏幕长宽
    int screenWidth;
    int screenHeight;
    int statusBarHeight;
    // 菜单是否显示
    boolean isMenuOpen = false;
    int menuWidth;
    LinearLayout menu;
    View.OnClickListener onMenuClickListener;

    AlphaAnimation hideMenuAnim;
    ScaleAnimation showMenuAnim;

    int send = 0;
    int get = 1;

    private SimpleAdapter userInfoAdapter;
    private PullRefreshListView pullRefreshListView;
    private DrawerLayout drawerLayout;
    private FrameLayout root;
    private LinearLayout listContent;
    private ImageView moreMenu;

    private List<Map<String, Object>> userList = new ArrayList<>();
    private Bundle who = new Bundle();

    String targetIp;
    String targetName;
    long[] pattern = {100, 200};
    Vibrator vibrator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (NetConfApplication) getActivity().getApplication();
        // 菜单显示与隐藏的动画
        showMenuAnim = new ScaleAnimation(0f, 1f, 0f, 1f, Animation.RELATIVE_TO_SELF,
                1f, Animation.RELATIVE_TO_SELF, 0f);
        hideMenuAnim = new AlphaAnimation(1f, 0f);
        showMenuAnim.setDuration(100);
        hideMenuAnim.setDuration(200);
        vibrator = (Vibrator) app.getSystemService(app.VIBRATOR_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (isRotate) {
            return viewGroup;
        } else {
            viewGroup = inflater.inflate(R.layout.user_list, container, false);
            root = getView(viewGroup, R.id.mainContent);
            drawerLayout = getView(viewGroup, R.id.drawer_layout);
            listContent = getView(root, R.id.listContent);
            moreMenu = getView(root, R.id.moreMenu);
            return viewGroup;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // 初始化界面
        initUI();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        if (app.isLand)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        else
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        super.onStart();
    }

    @Override
    public void onResume() {
        Logger.info(TAG, "========= onresume =========");
        if (userInfoAdapter != null) {
            Logger.info(TAG, "========= getUserData =========");
            getUserData();
            userInfoAdapter.notifyDataSetChanged();
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        root.removeView(menu);
        isMenuOpen = false;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void getCommend(Message msg) {
        switch (msg.what) {
            case login:
                loadUserListUI();
                break;
            case retry:
                loadUserListOrWarn();
                break;
            case refresh:
                pullRefreshListView.finishRefreshing();
                getUserData();
                userInfoAdapter.notifyDataSetChanged();
                break;
            case redraw:
                if (userInfoAdapter != null) {
                    getUserData();
                    userInfoAdapter.notifyDataSetChanged();
                }
                break;
        }
    }

    private List<Map<String, Object>> getUserData() {
        userList.clear();
        for (Host host : app.hostList) {
            Map<String, Object> item = new HashMap<>();
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

    public void measureSrceen() {
        // 获取屏幕长宽和状态栏高度
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        if (app.isLand)
            screenWidth = dm.widthPixels * 3 / 7;
        else
            screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

        Rect frame = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);
        statusBarHeight = frame.top;
        menuWidth = getResources().getDimensionPixelSize(R.dimen.menu_width);
    }

    private void getDeviceInfo() {
        String manufacturerName = android.os.Build.MANUFACTURER;
        String systemVersion = android.os.Build.VERSION.RELEASE;
        String deviceName = android.os.Build.HARDWARE;
        String appVersion = "";

        TelephonyManager tm = (TelephonyManager) app.getSystemService(Context.TELEPHONY_SERVICE);

        try {
            PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            appVersion = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        TextView t1 = getView(drawerLayout, R.id.device_name);
        TextView t2 = getView(drawerLayout, R.id.system_version);
        TextView t3 = getView(drawerLayout, R.id.operate_name);
        TextView t4 = getView(drawerLayout, R.id.mcc_mnc);
        TextView t5 = getView(drawerLayout, R.id.manufacturer_name);
        TextView t6 = getView(drawerLayout, R.id.appVersion);

        t1.setText("：" + deviceName);
        t2.setText("：Android " + systemVersion);
        t3.setText("：" + tm.getNetworkOperatorName());
        t4.setText("：" + tm.getNetworkOperator());
        t5.setText("：" + manufacturerName);
        t6.setText("版本号：" + appVersion);

        ImageView QRImg = getView(drawerLayout, R.id.QRCode);
        new CreateQRImage(android.os.Build.MODEL + "!!!!" + app.hostIP, QRImg, app);
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

    private void initUI() {
        measureSrceen();
        if (!isRotate) {
            getDeviceInfo();
            registerForEvent();
            loadUserListOrWarn();
        } else {
            if (getView(root, R.id.wait) != null) {
                loadUserListUI();
            }
            if (pullRefreshListView != null &&
                    pullRefreshListView.currentState == PullRefreshListView.Tag.Refreshing) {
                pullRefreshListView.finishRefreshing();
            }
        }

    }

    private void registerForEvent() {
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isMenuOpen) {
                    hideMenu();
                    return true;
                } else return false;
            }
        });

        getView(drawerLayout, R.id.left_drawer).setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // 吞掉点击事件使下拉刷新失效
                        return true;
                    }
                });
        onMenuClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.exit:
                        app.forceClose = true;
                        getActivity().finish();
                        break;
                    case R.id.openFolder:
                        startActivity(new Intent(getActivity(), FileListActivity.class));
                        break;
                    case R.id.sendProgress:
                        Intent intentSend = new Intent(getActivity(), ProgressBarListActivity.class);
                        intentSend.setFlags(send);
                        startActivity(intentSend);
                        break;
                    case R.id.getProgress:
                        Intent intentGet = new Intent(getActivity(), ProgressBarListActivity.class);
                        intentGet.setFlags(get);
                        startActivity(intentGet);
                        break;
                    case R.id.scan:
                        Intent intentScan = new Intent(getActivity(), CaptureActivity.class);
                        intentScan.setFlags(get);
                        startActivity(intentScan);
                        break;
                    default:
                        break;
                }
                hideMenu();
            }
        };
        moreMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMenuOpen) {
                    if (menu == null) {
                        Logger.info(TAG, "creat a new menu");
                        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
                        menu = (LinearLayout) layoutInflater.inflate(R.layout.more_menu, null);
                        getView(menu, R.id.getProgress).setOnClickListener(onMenuClickListener);
                        getView(menu, R.id.sendProgress).setOnClickListener(onMenuClickListener);
                        getView(menu, R.id.exit).setOnClickListener(onMenuClickListener);
                        getView(menu, R.id.scan).setOnClickListener(onMenuClickListener);
                        getView(menu, R.id.openFolder).setOnClickListener(onMenuClickListener);

                    }
                    FrameLayout.LayoutParams params = new FrameLayout.
                            LayoutParams(menuWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.topMargin = getResources().getDimensionPixelSize(R.dimen.title_height
                            + statusBarHeight);
                    params.leftMargin = screenWidth - menuWidth;
                    menu.setLayoutParams(params);

                    root.addView(menu);
                    showMenu();
                    isMenuOpen = true;
                    if (pullRefreshListView != null)
                        pullRefreshListView.setCanRefresh(false);
                } else {
                    hideMenu();
                    isMenuOpen = false;
                }
            }
        });
    }

    // 通过activity调用更新UI的操作设置了一定的延迟，程序第一次启动时调用
    private void loadUserListOrWarn() {
        if (app.wifi == 1) {
            notification.notifyInfo(login, null);
        } else {
            // 弹出警告框并退出
            new AlertDialog.Builder(getActivity()).setTitle("错误")
                    .setMessage("wifi未连接或端口异常，启动失败")
                    .setPositiveButton("退出",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getActivity().setResult(getActivity().RESULT_OK);// 确定按钮事件
                                    getActivity().finish();
                                }
                            })
                    .setNegativeButton("重试",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    app.check(getActivity());
                                    notification.notifyInfo(retry, null);
                                }
                            }).setCancelable(false).show();
        }
    }

    // 更新UI的操作
    private void loadUserListUI() {
        userInfoAdapter = new SimpleAdapter(getActivity(), getUserData(),
                R.layout.user_item, new String[]{"name", "ip",
                "img"}, new int[]{R.id.userName,
                R.id.userIP, R.id.unread});
        Logger.info(TAG,
                String.valueOf(app.hostList.size()));

        // 更新UI
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        pullRefreshListView = (PullRefreshListView) layoutInflater
                .inflate(R.layout.users, null);

        listContent.removeView(getView(root, R.id.wait));
        listContent.addView(pullRefreshListView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        pullRefreshListView.getListView().setAdapter(userInfoAdapter);
        pullRefreshListView.setPullListener(new PullRefreshListView.PullToRefreshListener() {
            @Override
            public void onRefresh() {
                notification.notifyInfo(refresh, null);
            }
        });
        pullRefreshListView.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView name = getView(view, R.id.userName);
                TextView ip = getView(view, R.id.userIP);

                if (ip.getText().equals(NetConfApplication.hostIP)) {
                    if (!app.isLand)
                        drawerLayout.openDrawer(Gravity.LEFT);
                } else {
                    Logger.info(TAG, "send notify to show chat fragment");
                    who.putString("name", name.getText().toString());
                    who.putString("ip", ip.getText().toString());
                    notification.notifyInfo(startChat, who);
                }
            }
        });
        pullRefreshListView.getListView().setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (!app.isLand) {
                            Logger.info(TAG, "in 3d touch effect");
                            TextView name = getView(view, R.id.userName);
                            TextView ip = getView(view, R.id.userIP);

                            // 组装需要显示的界面
                            targetIp = ip.getText().toString();
                            targetName = name.getText().toString();
                            Bundle bundle = new Bundle();
                            bundle.putString("ip", targetIp);
                            bundle.putString("name", targetName);
                            // 振动提示
                            vibrator.vibrate(pattern, -1);
                            notification.notifyInfo(pressure, bundle);

                            return true;
                        } else {
                            return false;
                        }
                    }
                }
        );
    }
}