package net.ui.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import net.app.NetConfApplication;
import net.log.Logger;

/**
 *  实现类似QQ的拖拽小红点功能
 */
public class RedDotHelper extends android.support.v7.widget.AppCompatImageView {
    private static final String TAG = "RedDotHelper";

    private int[] location = new int[2];
    private Context context;
    private AttributeSet attrs;
    private DragRedDot redDot;
    private String IP;

    public RedDotHelper(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
    }

    public DragRedDot getRedDot() {
        return redDot;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() != null) {
            Logger.info(TAG, "has unread msg------" + event.getAction());

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    NetConfApplication.drag = true;
                    getLocationOnScreen(location);
                    Logger.info(TAG, "get red dot x = " + location[0] + "---- y = " + location[1]);
                    redDot = new DragRedDot(context, attrs, location, getWidth() / 2);
                    redDot.setHelper(RedDotHelper.this);
                    redDot.setIP(IP);
                    // 显示拖拽的红点
                    redDot.addWindow();
                    setVisibility(INVISIBLE);
                    break;
                case MotionEvent.ACTION_UP:
                    NetConfApplication.drag = false;
                    break;
            }

            return true;
        } else {
            Logger.info(TAG, "normal process touch event");
            return super.onTouchEvent(event);
        }
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

}
