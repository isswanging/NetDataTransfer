package net.ui;

import java.io.File;

import net.app.NetConfApplication;
import net.log.Logger;
import net.service.BroadcastMonitorService;
import net.service.FileMonitorService;
import net.service.UdpDataMonitorService;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import com.example.netdatatransfer.R;

public class WelcomeActivity extends Activity {
    private NetConfApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        app = (NetConfApplication) getApplication();
        app.nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ImageView welcomeAnim = (ImageView) findViewById(R.id.welcome_img);

        welcomeAnim.setBackgroundResource(R.anim.welcome_anim);
        final AnimationDrawable anim = (AnimationDrawable) welcomeAnim
                .getBackground();
        anim.start();

        // 检查端口
        preCheck();
        if (app.wifi == 1) {
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

    private void listen() {
        this.startService(new Intent(this, UdpDataMonitorService.class));
        this.startService(new Intent(this, BroadcastMonitorService.class));
        this.startService(new Intent(this, FileMonitorService.class));
    }

    private void preCheck() {
        if (app.check(this).endsWith(app.SUCCESS)) {
            NetConfApplication.hostIP = app.getHostIp(this);// 获取ip地址
        }
    }
}
