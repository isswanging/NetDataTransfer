package net.ui;

import java.io.File;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;
import net.service.BroadcastMonitorService;
import net.service.FileMonitorService;
import net.service.ScreenMonitorService;
import net.service.UdpDataMonitorService;
import net.vo.Host;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

public class WelcomeActivity extends Activity {
    private NetConfApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        app = (NetConfApplication) getApplication();
        app.nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建快捷方式
        createShortCut();
        // 启动动画
        ImageView welcomeAnim = (ImageView) findViewById(R.id.welcome_img);
        welcomeAnim.setBackgroundResource(R.anim.welcome_anim);
        final AnimationDrawable anim = (AnimationDrawable) welcomeAnim
                .getBackground();
        anim.start();
        // 检查端口
        preCheck();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent("net.ui.userList");
                startActivity(intent);
                anim.stop();
                finish();
            }
        }, 2400);
    }

    private void createShortCut() {
        // 调试版本不创建快捷方式
        if (!Logger.tag) {
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

    private void listen() {
        this.startService(new Intent(this, BroadcastMonitorService.class));
        this.startService(new Intent(this, UdpDataMonitorService.class));
        this.startService(new Intent(this, FileMonitorService.class));
        this.startService(new Intent(this, ScreenMonitorService.class));
    }

    private void preCheck() {
        if (app.check(this).endsWith(app.SUCCESS)) {
            String userName = android.os.Build.MODEL;// 获取用户名
            String userDomain = "Android";// 获取计算机域
            Host host = new Host(userName, userDomain, "0.0.0.0",
                    NetConfApplication.hostName, 1, 0);
            app.addHost(host);
            
            // 建立监听
            listen();
            // 加载音乐
            app.loadVoice();
            // 创建接收文件的目录
            NetConfApplication.saveFilePath = app.getSDPath()
                    + "/NetDataTransfer/recFile";
            Logger.info(this.toString(), NetConfApplication.saveFilePath);
            File recFile = new File(NetConfApplication.saveFilePath);
            if (!recFile.exists()) {
                Logger.info(this.toString(), "create dir");
                recFile.mkdirs();
            }
        }
    }
}
