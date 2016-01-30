package net.ui.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.nineoldandroids.view.ViewHelper;

import net.log.Logger;
import net.util.HelpUtils;

public class SideslipMenuView extends HorizontalScrollView {
    private final String TAG = "SideslipMenuView";

    /**
     * 屏幕宽度
     */
    private int mScreenWidth;
    /**
     * dp
     */
    private int mMenuRightPadding;
    /**
     * 菜单的宽度
     */
    private int mMenuWidth;
    private int mHalfMenuWidth;

    private boolean isOpen;

    private boolean once;

    private ViewGroup mMenu;
    private ViewGroup mContent;

    private Context context;
    private EditText eText;
    private ImageView backImg;
    InputMethodManager imm;

    public SideslipMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public SideslipMenuView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mMenuRightPadding = dip2px(context, 150);
        this.context = context;
        imm = (InputMethodManager) this.context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    public SideslipMenuView(Context context) {
        this(context, null, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /**
         * 显示的设置一个宽度
         */
        if (!once) {
            Logger.info(TAG,"in onMeasure method");
            LinearLayout wrapper = (LinearLayout) getChildAt(0);
            mMenu = (ViewGroup) wrapper.getChildAt(0);
            mContent = (ViewGroup) wrapper.getChildAt(1);
            eText = (EditText) ((ViewGroup) ((ViewGroup) ((ViewGroup) mContent
                    .getChildAt(0)).getChildAt(0)).getChildAt(0)).getChildAt(0);
            backImg = (ImageView) ((ViewGroup) ((ViewGroup) ((ViewGroup) mContent
                    .getChildAt(0)).getChildAt(0)).getChildAt(1)).getChildAt(0);
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            mScreenWidth = HelpUtils.getScreenWidth(context);
        } else {
            mScreenWidth = HelpUtils.getScreenWidth(context) * 4 / 7;
        }
        mMenuWidth = mScreenWidth - mMenuRightPadding;
        mHalfMenuWidth = mMenuWidth / 2;
        mMenu.getLayoutParams().width = mMenuWidth;
        mContent.getLayoutParams().width = mScreenWidth;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            backImg.setVisibility(VISIBLE);
        } else {
            backImg.setVisibility(GONE);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            // 将菜单隐藏
            this.scrollTo(mMenuWidth, 0);
            once = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Logger.info(TAG, "port can side");
            int action = ev.getAction();
            switch (action) {
                // Up时，进行判断，如果显示区域大于菜单宽度一半则完全显示，否则隐藏
                case MotionEvent.ACTION_UP:
                    int scrollX = getScrollX();
                    if (scrollX > mHalfMenuWidth) {
                        isOpen = false;
                        closeSoftkeyboard();
                        this.smoothScrollTo(mMenuWidth, 0);

                    } else {
                        isOpen = true;
                        closeSoftkeyboard();
                        this.smoothScrollTo(0, 0);

                    }
                    return true;
            }

            return super.onTouchEvent(ev);
        } else {
            return false;
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        closeSoftkeyboard();
        super.onScrollChanged(l, t, oldl, oldt);
        float scale = l * 1.0f / mMenuWidth;
        float leftScale = 1 - 0.3f * scale;
        float rightScale = 0.8f + scale * 0.2f;

        ViewHelper.setScaleX(mMenu, leftScale);
        ViewHelper.setScaleY(mMenu, leftScale);
        ViewHelper.setAlpha(mMenu, 0.6f + 0.4f * (1 - scale));
        ViewHelper.setTranslationX(mMenu, mMenuWidth * scale * 0.7f);

        ViewHelper.setPivotX(mContent, 0);
        ViewHelper.setPivotY(mContent, mContent.getHeight() / 2);
        ViewHelper.setScaleX(mContent, rightScale);
        ViewHelper.setScaleY(mContent, rightScale);
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void closeSoftkeyboard() {
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0); // 强制隐藏键盘

        if (isOpen) {
            eText.setEnabled(false);
            backImg.setVisibility(GONE);
        } else {
            eText.setEnabled(true);
            backImg.setVisibility(VISIBLE);
        }
    }
}
