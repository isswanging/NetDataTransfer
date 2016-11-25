package net.ui.activity;

import android.app.Activity;
import android.view.View;

public class BaseActivity extends Activity {
    /**
     * 简化findViewById操作
     */
    public <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }
}
