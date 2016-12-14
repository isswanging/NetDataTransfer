package net.ui.activity;

import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ImageView;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;
import net.vo.Host;

import java.io.File;
import java.util.Random;

public class WelcomeActivity extends BaseActivity {
    private final String TAG = "WelcomeActivity";
    private NetConfApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        app = (NetConfApplication) getApplication();

        // 创建快捷方式
        createShortCut();
        // 启动动画
        ImageView welcomeAnim = getView(R.id.welcome_img);
        welcomeAnim.setBackgroundResource(getImg());
        // 检查端口
        preCheck();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent("net.ui.main");
                startActivity(intent);
                finish();
            }
        }, 2400);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void createShortCut() {
        // 调试版本不创建快捷方式
        if (!Logger.DEBUG) {
            Intent shortcut = new Intent(
                    "com.android.launcher.action.INSTALL_SHORTCUT");

            // 快捷方式的名称
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                    getString(R.string.app_name));
            // 快捷方式的图标
            ShortcutIconResource iconRes = Intent.ShortcutIconResource
                    .fromContext(this, R.drawable.ic_launcher);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes);
            shortcut.putExtra("duplicate", false); // 不允许重复创建
            Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
            shortcutIntent.setClass(this, WelcomeActivity.class);
            shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            shortcutIntent.setClass(this, WelcomeActivity.class);// 设置第一个页面
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);

            sendBroadcast(shortcut);
        }
    }

    private void preCheck() {
        String userName = android.os.Build.MODEL;// 获取用户名
        String userDomain = "Android";// 获取计算机域
        Host host = new Host(userName, userDomain, "0.0.0.0", NetConfApplication.hostName, 1, 0);
        app.addHost(host);
        // 加载音乐
        app.loadVoice();
        // 创建接收文件的目录
        NetConfApplication.saveFilePath = app.getSDPath()
                + "/NetDataTransfer/recFile";
        Logger.info(TAG, NetConfApplication.saveFilePath);

        if (app.check().endsWith(app.SUCCESS)) {
            // 建立监听
            listen();
            Logger.info(TAG, "prepare something");
            File recFile = new File(NetConfApplication.saveFilePath);
            if (!recFile.exists()) {
                Logger.info(TAG, "create dir");
                recFile.mkdirs();
            }
        }
    }

    public int getImg() {
        switch (new Random().nextInt(12) + 1) {
            case 1:
                return R.drawable.img01;
            case 2:
                return R.drawable.img02;
            case 3:
                return R.drawable.img03;
            case 4:
                return R.drawable.img04;
            case 5:
                return R.drawable.img05;
            case 6:
                return R.drawable.img06;
            case 7:
                return R.drawable.img07;
            case 8:
                return R.drawable.img08;
            case 9:
                return R.drawable.img09;
            case 10:
                return R.drawable.img10;
            case 11:
                return R.drawable.img11;
            default:
                return R.drawable.img12;
        }
    }
}
