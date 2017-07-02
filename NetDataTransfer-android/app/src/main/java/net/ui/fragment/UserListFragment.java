package net.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.app.NetConfApplication;
import net.app.netdatatransfer.BuildConfig;
import net.app.netdatatransfer.R;
import net.app.netdatatransfer.databinding.UserListBinding;
import net.base.BaseFragment;
import net.log.Logger;
import net.ui.activity.CaptureActivity;
import net.ui.activity.FileListActivity;
import net.ui.activity.ProgressBarListActivity;
import net.ui.view.DragListView;
import net.ui.view.PullRefreshListView;
import net.util.Commend;
import net.util.CreateQRImage;
import net.vo.DeviceInfo;
import net.vo.EventInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class UserListFragment extends BaseFragment {
    private final String TAG = "UserListFragment";
    // 屏幕长宽
    int screenWidth;
    int screenHeight;
    int statusBarHeight;
    // 菜单是否显示
    boolean isMenuOpen = false;
    int menuWidth;
    LinearLayout menu;
    View.OnClickListener onMenuClickListener;

    int send = 0;
    int get = 1;
    int xoff;
    int yoff = 15;

    private DragListView.DragAdapter userInfoAdapter;
    private PullRefreshListView pullRefreshListView;
    private DrawerLayout drawerLayout;
    private FrameLayout root;
    private LinearLayout listContent;
    private ImageView moreMenu;
    private PopupWindow mPopupWindow;
    private Bundle who = new Bundle();

    String targetIp;
    String targetName;
    long[] pattern = {100, 200};
    Vibrator vibrator;
    boolean isQRReady = false;

    UserListBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) app.getSystemService(Context.VIBRATOR_SERVICE);
        Logger.info(TAG, "UserListFragment onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (isRotate) {
            return viewGroup;
        } else {
            binding = DataBindingUtil.inflate(inflater, R.layout.user_list, container, false);
            viewGroup = binding.getRoot();
            // viewGroup = inflater.inflate(R.layout.user_list, container, false);
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
        if (app.isLand) {
            xoff = -220;
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } else {
            xoff = 10;
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        Logger.info(TAG, "========= onresume =========");
        if (userInfoAdapter != null) {
            Logger.info(TAG, "========= getUserData =========");
            userInfoAdapter.initData();
            userInfoAdapter.notifyDataSetChanged();
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        root.removeView(menu);
        if (pullRefreshListView != null)
            pullRefreshListView.setCanRefresh(true);
        isMenuOpen = false;
        mPopupWindow.dismiss();
        super.onSaveInstanceState(outState);
    }

    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getCommend(EventInfo msg) {
        Logger.info(TAG, "get msg-----" + msg.toString());
        if (msg.getDirection() == EventInfo.tofrg) {
            switch (msg.getCommend()) {
                case login:
                    loadUserListUI();
                    break;
                case retry:
                    loadUserListOrWarn();
                    break;
                case refresh:
                    pullRefreshListView.finishRefreshing();
                    userInfoAdapter.initData();
                    userInfoAdapter.notifyDataSetChanged();
                    break;
                case redraw:
                    if (userInfoAdapter != null) {
                        userInfoAdapter.initData();
                        userInfoAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
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

        DeviceInfo deviceInfo = new DeviceInfo("：" + manufacturerName, "：Android " + systemVersion,
                "：" + deviceName, "版本号：" + appVersion, "：" + tm.getNetworkOperator(),
                "：" + tm.getNetworkOperatorName()
        );
        binding.setDevice(deviceInfo);

        ImageView QRImg = getView(drawerLayout, R.id.QRCode);
        new CreateQRImage(android.os.Build.MODEL + "!!!!" + NetConfApplication.hostIP, QRImg, app);
        isQRReady = true;
    }

    private void initUI() {
        measureSrceen();
        if (!isRotate || !NetConfApplication.isUIReady) {
            if (!isQRReady)
                getDeviceInfo();
            registerForEvent();
            loadUserListOrWarn();
        } else {
            if (getView(root, R.id.wait) != null) {
                Logger.info(TAG, "load ui");
                loadUserListUI();
            }
            if (pullRefreshListView != null &&
                    pullRefreshListView.currentState == PullRefreshListView.Tag.Refreshing) {
                pullRefreshListView.finishRefreshing();
            }
        }
    }

    private void initMenu() {
        LayoutInflater layoutInflater = LayoutInflater.from(app);
        menu = (LinearLayout) layoutInflater.inflate(R.layout.more_menu, null);
        getView(menu, R.id.getProgress).setOnClickListener(onMenuClickListener);
        getView(menu, R.id.sendProgress).setOnClickListener(onMenuClickListener);
        getView(menu, R.id.exit).setOnClickListener(onMenuClickListener);
        getView(menu, R.id.scan).setOnClickListener(onMenuClickListener);
        getView(menu, R.id.openFolder).setOnClickListener(onMenuClickListener);
        mPopupWindow = new PopupWindow(menu, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));
        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                hideMenu();
            }
        });
    }

    public void hideMenu() {
        isMenuOpen = false;
        if (pullRefreshListView != null)
            pullRefreshListView.setCanRefresh(true);

        WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
        params.alpha = 1f;
        getActivity().getWindow().setAttributes(params);
    }

    private void registerForEvent() {
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
                        EventBus.getDefault().post(new EventInfo(Commend.exit, EventInfo.toAct, null));
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
                mPopupWindow.dismiss();
            }
        };
        moreMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetConfApplication.isUIReady) {
                    Toast.makeText(getActivity(), "界面加载中", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isMenuOpen) {
                    if (mPopupWindow == null) {
                        Logger.info(TAG, "creat a new menu");
                        initMenu();
                    }
                    mPopupWindow.showAsDropDown(moreMenu, xoff, yoff);
                    WindowManager.LayoutParams params = getActivity().getWindow().getAttributes();
                    params.alpha = 0.5f;
                    getActivity().getWindow().setAttributes(params);

                    isMenuOpen = true;
                    if (pullRefreshListView != null)
                        pullRefreshListView.setCanRefresh(false);
                } else {
                    mPopupWindow.dismiss();
                }
            }
        });
    }

    // 通过activity调用更新UI的操作设置了一定的延迟，程序第一次启动时调用
    private void loadUserListOrWarn() {
        if (app.wifi == 1) {
            EventBus.getDefault().post(new EventInfo(Commend.login, EventInfo.toAct, null));
        } else {
            EventBus.getDefault().post(new EventInfo(Commend.login, EventInfo.toAct, "net_error"));
        }
    }

    // 更新UI的操作
    private void loadUserListUI() {
        userInfoAdapter = new DragListView.DragAdapter(getActivity());
        Logger.info(TAG, String.valueOf(app.hostList.size()));

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
                EventBus.getDefault().post(new EventInfo(Commend.refresh, EventInfo.toAct, null));
            }
        });
        pullRefreshListView.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (pullRefreshListView.currentState == PullRefreshListView.Tag.Normal) {
                    TextView name = getView(view, R.id.userName);
                    TextView ip = getView(view, R.id.userIP);

                    if (ip.getText().equals(NetConfApplication.hostIP)) {
                        if (!app.isLand)
                            drawerLayout.openDrawer(Gravity.LEFT);
                    } else {
                        Logger.info(TAG, "send notify to show chat fragment");
                        who.putString("name", name.getText().toString());
                        who.putString("ip", ip.getText().toString());
                        EventBus.getDefault().post(new EventInfo(Commend.startChat, EventInfo.toAct, who));
                    }
                }
            }
        });
        pullRefreshListView.getListView().setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                        if (pullRefreshListView.currentState == PullRefreshListView.Tag.Normal &&
                                !app.isLand && android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN
                                && (BuildConfig.DEBUG_LOG || position != 0)) {
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
                            EventBus.getDefault().post(new EventInfo(Commend.pressure, EventInfo.toAct, bundle));

                            return true;
                        } else {
                            return false;
                        }
                    }
                }
        );
        NetConfApplication.isUIReady = true;
    }
}
