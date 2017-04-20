package net.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import net.app.netdatatransfer.R;
import net.log.Logger;
import net.util.ScreenHelpUtils;

public class DragRedDot extends View {
    private final String TAG = "DragRedDot";

    private int x;
    private int y;
    private int circleRadius;
    private int statebarHeight;
    private WindowManager wm;
    private WindowManager.LayoutParams mParams;

    public DragRedDot(Context context, AttributeSet attrs, int[] location, int radius) {
        super(context, attrs);
        if (location.length == 2) {
            x = location[0];
            y = location[1];
        }
        circleRadius = radius;
        statebarHeight = ScreenHelpUtils.getStatusHeight(context);
        Logger.info(TAG, "circleRadius is " + circleRadius);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void addWindow() {
        mParams = new WindowManager.LayoutParams();
        mParams.format = PixelFormat.TRANSLUCENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        wm.addView(this, mParams);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Logger.info(TAG, "drag red dot...." + event.getAction());
        updateView();
        return super.onTouchEvent(event);
    }

    private void updateView() {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);

        canvas.drawCircle(x + circleRadius, y - statebarHeight + circleRadius, circleRadius, paint);
    }
}
