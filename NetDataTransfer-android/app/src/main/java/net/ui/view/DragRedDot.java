package net.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;

import net.db.DBManager;
import net.log.Logger;
import net.util.ScreenHelpUtils;

public class DragRedDot extends View {
    private final String TAG = "DragRedDot";

    /**
     * 拖拽圆的圆心
     */
    PointF mDragCanterPoint = new PointF(-1, -1);
    /**
     * 固定圆的圆心
     */
    PointF mFixCanterPoint = new PointF(-1, -1);
    /**
     * 控制点
     */
    PointF mCanterPoint = new PointF();

    /**
     * 固定圆的切点
     */
    PointF[] mFixTangentPointes = new PointF[2];
    /**
     * 拖拽圆的切点
     */
    PointF[] mDragTangentPoint = new PointF[2];
    /**
     * 拖拽圆半径
     */
    float mDragRadius = 20;
    /**
     * 固定圆半径
     */
    float mFixRadius;
    /**
     * 记录初始值
     */
    float mInitRadius;
    /**
     * 滑动的最大距离
     */
    float maxDistance = 250;
    /**
     * 记录两点之间的距离
     */
    float distance;
    /**
     * 判断放开手指时是否在范围外
     */
    boolean isOut = false;
    /**
     * 判断是否曾经移出过范围
     */
    boolean onceOut = false;

    private RedDotHelper helper;
    private Paint mPaint;
    private Path mPath;
    private int statebarHeight;
    private WindowManager wm;
    private WindowManager.LayoutParams mParams;
    private String IP;
    private DBManager dbm;

    public DragRedDot(Context context, AttributeSet attrs, int[] location, int radius) {
        super(context, attrs);

        mInitRadius = radius;
        mFixRadius = radius;
        statebarHeight = ScreenHelpUtils.getStatusHeight(context);
        wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (location.length == 2) {
            mFixCanterPoint.set(location[0] + radius, location[1] + radius - statebarHeight);
        }
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
        mPath = new Path();
        dbm = new DBManager(context);
    }

    public void addWindow() {
        mParams = new WindowManager.LayoutParams();
        mParams.format = PixelFormat.TRANSLUCENT;
        mParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        wm.addView(this, mParams);
    }

    public void setHelper(RedDotHelper helper) {
        this.helper = helper;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Logger.info(TAG, "drag red dot...." + event.getAction());

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                mDragCanterPoint.set(event.getRawX(), event.getRawY() - statebarHeight);
                distance = countDistance(mDragCanterPoint, mFixCanterPoint);
                updateView();
                break;

            case MotionEvent.ACTION_UP:
                // 在范围外抬手
                if (isOut) {
                    helper.setImageDrawable(null);
                    helper.setVisibility(VISIBLE);
                    wm.removeView(this);
                    dbm.deleteMsg(IP);
                }
                // 没有移动就直接抬手
                else if (mDragCanterPoint.x == -1) {
                    helper.setVisibility(VISIBLE);
                    wm.removeView(this);
                } else {
                    //划出范围后再滑进来抬手
                    if (onceOut) {
                        helper.setVisibility(VISIBLE);
                        wm.removeView(this);
                    }
                    // 在范围内抬手
                    else {
                        showAnimation();
                    }
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    /**
     * 松手后回弹的动画
     */
    private void showAnimation() {
        final PointF start = new PointF(mDragCanterPoint.x, mDragCanterPoint.y);
        ValueAnimator anim = ValueAnimator.ofFloat(1f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float f = animation.getAnimatedFraction();
                mDragCanterPoint.set(getPointByPercent(start.x, mFixCanterPoint.x, f),
                        getPointByPercent(start.y, mFixCanterPoint.y, f));
                distance = countDistance(mDragCanterPoint, mFixCanterPoint);
                updateView();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                helper.setVisibility(VISIBLE);
                wm.removeView(DragRedDot.this);
            }
        });
        anim.setInterpolator(new OvershootInterpolator(2f));
        anim.setDuration(500);
        anim.start();
    }

    /**
     * 计算两点之间的距离
     */
    private float countDistance(PointF p1, PointF p2) {
        return (float) Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }

    /**
     * Get point between p1 and p2 by percent.
     * 根据百分比获取两点之间的某个点坐标
     */
    public float getPointByPercent(float p1, float p2, float percent) {
        return evaluateValue(percent, p1, p2);
    }

    /**
     * 根据分度值，计算从start到end中，fraction位置的值。fraction范围为0 -> 1
     */
    public float evaluateValue(float fraction, Number start, Number end) {
        return start.floatValue() + (end.floatValue() - start.floatValue()) * fraction;
    }

    private void updateView() {
        if (distance > maxDistance) {
            isOut = true;
            if (!onceOut) {
                onceOut = true;
            }
        } else {
            mFixRadius = mInitRadius * (1 - distance / maxDistance * 0.5f);
            isOut = false;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isOut && !onceOut) {
            Logger.info(TAG,"draw a red dot");
            canvas.drawCircle(mFixCanterPoint.x, mFixCanterPoint.y, mFixRadius, mPaint);

            if (mDragCanterPoint.x != -1) {
                float dy = mDragCanterPoint.y - mFixCanterPoint.y;
                float dx = mDragCanterPoint.x - mFixCanterPoint.x;

                mCanterPoint.set((mDragCanterPoint.x + mFixCanterPoint.x) / 2,
                        (mDragCanterPoint.y + mFixCanterPoint.y) / 2);

                if (dx != 0) {
                    float k1 = dy / dx;
                    float k2 = -1 / k1;
                    mDragTangentPoint = getIntersectionPoints(
                            mDragCanterPoint, mDragRadius, (double) k2);
                    mFixTangentPointes = getIntersectionPoints(
                            mFixCanterPoint, mFixRadius, (double) k2);
                } else {
                    mDragTangentPoint = getIntersectionPoints(
                            mDragCanterPoint, mDragRadius, (double) 0);
                    mFixTangentPointes = getIntersectionPoints(
                            mFixCanterPoint, mFixRadius, (double) 0);
                }

                mPath.reset();
                mPath.moveTo(mFixTangentPointes[0].x, mFixTangentPointes[0].y);
                mPath.quadTo(mCanterPoint.x, mCanterPoint.y,
                        mDragTangentPoint[0].x, mDragTangentPoint[0].y);
                mPath.lineTo(mDragTangentPoint[1].x, mDragTangentPoint[1].y);
                mPath.quadTo(mCanterPoint.x, mCanterPoint.y,
                        mFixTangentPointes[1].x, mFixTangentPointes[1].y);
                mPath.close();
                canvas.drawPath(mPath, mPaint);
            }
        }

        if (mDragCanterPoint.x != -1) {
            canvas.drawCircle(mDragCanterPoint.x, mDragCanterPoint.y,
                    mDragRadius, mPaint);
        }
    }

    /**
     * Get the point of intersection between circle and line.
     * 获取 通过指定圆心，斜率为lineK的直线与圆的交点。
     *
     * @param pMiddle The circle center point.
     * @param radius  The circle radius.
     * @param lineK   The slope of line which cross the pMiddle.
     * @return
     */
    public static PointF[] getIntersectionPoints(PointF pMiddle, float radius, Double lineK) {
        PointF[] points = new PointF[2];

        float radian, xOffset = 0, yOffset = 0;
        if (lineK != null) {

            radian = (float) Math.atan(lineK);
            xOffset = (float) (Math.cos(radian) * radius);
            yOffset = (float) (Math.sin(radian) * radius);
        } else {
            xOffset = radius;
            yOffset = 0;
        }
        points[0] = new PointF(pMiddle.x + xOffset, pMiddle.y + yOffset);
        points[1] = new PointF(pMiddle.x - xOffset, pMiddle.y - yOffset);

        return points;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }
}
