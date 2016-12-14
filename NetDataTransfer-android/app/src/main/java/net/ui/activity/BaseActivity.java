package net.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import net.service.BroadcastMonitorService;
import net.service.FileMonitorService;
import net.service.ScreenMonitorService;
import net.service.UdpDataMonitorService;

import java.lang.reflect.Field;

public class BaseActivity extends Activity {
    /**
     * 简化findViewById操作
     */
    public <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }

    @Override
    protected void onDestroy() {
        fixInputMethodManagerLeak(this);
        super.onDestroy();
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

    protected void listen() {
        this.startService(new Intent(this, BroadcastMonitorService.class));
        this.startService(new Intent(this, UdpDataMonitorService.class));
        this.startService(new Intent(this, FileMonitorService.class));
        this.startService(new Intent(this, ScreenMonitorService.class));
    }
}
