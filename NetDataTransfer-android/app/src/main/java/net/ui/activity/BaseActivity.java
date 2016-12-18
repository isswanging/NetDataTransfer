package net.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import net.app.NetConfApplication;
import net.service.LoginMonitorService;
import net.service.FileMonitorService;
import net.service.BroadcastMonitorService;
import net.service.UdpDataMonitorService;

import java.lang.reflect.Field;

public class BaseActivity extends Activity implements NetConfApplication.WifiListener {
    NetConfApplication app;

    /**
     * 简化findViewById操作
     */
    public <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (NetConfApplication) getApplication();
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.listeners.add(this);
        if (app.wifi != 1 && NetConfApplication.isUIReady) {
            warnWifiState();
        }
    }

    @Override
    protected void onPause() {
        app.listeners.remove(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        fixInputMethodManagerLeak(this);
        super.onDestroy();
    }

    @Override
    public void notifyWifiInfo() {
        warnWifiState();
    }

    public void warnWifiState() {
        if (app.wifi == 1) {
            Toast.makeText(this, "网络已连接，请继续使用", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this).setTitle("错误")
                    .setMessage("wifi已断开")
                    .setPositiveButton("退出",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setResult(RESULT_OK);// 确定按钮事件
                                    System.exit(0);
                                }
                            })
                    .setNegativeButton("重试",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    handler.sendEmptyMessageDelayed(app.wifi, 1000);
                                }
                            }).setCancelable(false).show();
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            app.check(false);
            warnWifiState();
        }
    };

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

    protected void listen() {
        this.startService(new Intent(this, LoginMonitorService.class));
        this.startService(new Intent(this, UdpDataMonitorService.class));
        this.startService(new Intent(this, FileMonitorService.class));
        this.startService(new Intent(this, BroadcastMonitorService.class));
    }
}
