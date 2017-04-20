package net.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;


public class DragListView extends ListView {
    private static final String TAG = "DragListView";

    private int postion = INVALID_POSITION;// 记录点击的位置
    private RedDotHelper redDotHelper;

    public DragListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Logger.info(TAG, "dispatchTouchEvent ACTION_DOWN.......");
                postion = pointToPosition((int) event.getX(), (int) event.getY());
                if (postion != INVALID_POSITION) {
                    redDotHelper = (RedDotHelper) getChildAt(postion - getFirstVisiblePosition()).findViewById(R.id.unread);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (NetConfApplication.drag) {
                    Logger.info(TAG, "dispatchTouchEvent ACTION_MOVE");
                    redDotHelper.getRedDot().onTouchEvent(event);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (NetConfApplication.drag) {
                    Logger.info(TAG, "dispatchTouchEvent ACTION_UP");
                    NetConfApplication.drag = false;
                }

                break;

        }


        return super.dispatchTouchEvent(event);
    }
}
