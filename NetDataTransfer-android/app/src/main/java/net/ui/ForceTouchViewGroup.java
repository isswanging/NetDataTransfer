package net.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;
import net.util.ScreenUtils;

import java.util.List;
import java.util.Map;

/**
 * 模仿iphone的3D touch效果
 */
public class ForceTouchViewGroup extends LinearLayout {
    TextView title;
    LinearLayout preview;
    RelativeLayout.LayoutParams previewParams;
    ListView answerList;
    SimpleAdapter answerAdapter;
    NetConfApplication app;
    boolean isShow = false;// 判断菜单是否显示
    boolean isLock = false;// 屏蔽activity的事件分发
    boolean running = false;// 动画是否运行
    boolean isMove = true;
    float yDown;
    float yMove;
    float yTemp;
    ViewGroup root;
    int topMargin;
    int moveTopMargin;
    int answerHeight;
    int needMove = 0;// 菜单显示需要上拉的距离
    int answerListTop;
    int answerListBottom;
    int screenheight;

    // 动画
    Animation showAnswerList;
    Animation hideAnswerList;

    public ForceTouchViewGroup(Context context) {
        super(context);
        // 初始化
        addView(LayoutInflater.from(context).inflate(R.layout.force_touch_view, null));
        app = (NetConfApplication) context.getApplicationContext();
        title = (TextView) findViewById(R.id.preview_username);
        preview = (LinearLayout) findViewById(R.id.preview);
        answerList = (ListView) findViewById(R.id.answer);
        previewParams = (RelativeLayout.LayoutParams) preview.getLayoutParams();
        yTemp = 0;

        // 居中
        int height = dp2px(500);
        int stateHeight = ScreenUtils.getStatusHeight(context);
        screenheight = ScreenUtils.getScreenHeight(context);
        topMargin = (screenheight - stateHeight - height) / 2;
        previewParams.topMargin = topMargin;
        preview.setLayoutParams(previewParams);
    }

    public int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public int getNeedMove() {
        return needMove;
    }

    public boolean isShow() {
        return isShow;
    }

    public void setIsLock(boolean isLock) {
        this.isLock = isLock;
    }

    public boolean isLock() {
        return isLock;
    }

    public void show(final Builder builder) {
        // 设置模糊背景
        Drawable drawable = new BitmapDrawable(getResources(), builder.blurBg);
        drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        setBackground(drawable);

        root = builder.pRoot;

        // 填充view和list数据
        preview.addView(builder.preview,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        answerAdapter = new SimpleAdapter(app, builder.answerListData,
                R.layout.answer_item, new String[]{"text"}, new int[]{R.id.answer_text});
        answerList.setAdapter(answerAdapter);
        root.addView(this);
        // 计算list高度和滑动的距离
        answerList.post(new Runnable() {
            @Override
            public void run() {
                answerHeight = answerList.getHeight();
                needMove = answerHeight - topMargin + dp2px(20);
                answerListTop = screenheight - dp2px(10) - answerHeight;
                answerListBottom = screenheight - dp2px(10);
                showAnswerList = AnimationUtils.loadAnimation(app, R.anim.show_answerlist_anim);
                hideAnswerList = AnimationUtils.loadAnimation(app, R.anim.hide_answerlist_anim);

                Logger.info(this.toString(), "answerList height====" + answerHeight);
                Logger.info(this.toString(), "answerListTop==" + answerListTop + "===answerListBottom" + answerListBottom);

                showAnswerList.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        running = true;
                        answerList.setVisibility(VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        isShow = true;
                        running = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                hideAnswerList.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        running = true;
                        isShow = false;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        answerList.setVisibility(INVISIBLE);
                        running = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
            }
        });

        // 注册点击事件
        answerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Logger.info(this.toString(), "in item click event");
                Message msg = builder.pHandler.obtainMessage();
                msg.what = builder.pWhat;
                msg.arg1 = position;
                msg.sendToTarget();
                root.removeView(ForceTouchViewGroup.this);
            }
        });

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                yDown = event.getRawY();
                moveTopMargin = previewParams.topMargin;
                Logger.info(this.toString(), "yDown====" + yDown + "===answerListTop====" + answerListTop);
                if (yDown > answerListTop && isShow)
                    isMove = false;
                else
                    isMove = true;
                break;
            case MotionEvent.ACTION_UP:
                Logger.info(this.toString(), "in forceTouchView touch UP");
                if (!isShow) {
                    root.removeView(this);
                } else {
                    previewParams.topMargin = topMargin - needMove;
                    preview.setLayoutParams(previewParams);
                    yTemp = 0;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isMove) {
                    yMove = event.getRawY();
                    if (yTemp == 0) {
                        yTemp = yMove;
                    }
                    float gap = yMove - yTemp;

                    // view位于超过显示部分的位置
                    if ((moveTopMargin + gap) <= (topMargin - needMove)) {
                        // 向上滑减速
                        if (gap < 0) {
                            moveTopMargin = (int) (moveTopMargin + gap * 0.3);
                        }
                        // 向下滑速度正常
                        else {
                            moveTopMargin = (int) (moveTopMargin + gap);
                        }
                        if (!isShow && !running) {
                            showAnswerList();
                        }
                    }
                    // view处于下压并且继续下拉的状态
                    else if ((moveTopMargin + gap) >= topMargin && yMove > yTemp) {
                        moveTopMargin = (int) (moveTopMargin + gap * 0.1);
                    } else {
                        moveTopMargin = (int) (moveTopMargin + gap);
                        if (isShow && !running) {
                            hideAnswerList();
                        }
                    }
                    yTemp = yMove;
                    previewParams.topMargin = moveTopMargin;
                    preview.setLayoutParams(previewParams);
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    static class Builder {
        Context context;
        Bitmap blurBg;
        String pTargetIP;
        ViewGroup pRoot;
        Handler pHandler;
        int pWhat;
        float scale = 0.1f;
        View preview;
        List<Map<String, Object>> answerListData;

        public Builder(Context c) {
            context = c;
        }

        public Builder setBackground(View view) {
            //创建一块画布
            Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            //画出原图
            view.draw(canvas);
            //缩小原图提高运行效率
            Bitmap background = scaleBitmap(bitmap);
            //高斯模糊处理
            blurBg = blurBitmap(bitmap);
            background.recycle();
            bitmap.recycle();

            return this;
        }

        public Builder setIP(String ip) {
            pTargetIP = ip;
            return this;
        }

        // 设置主界面
        public Builder setView(View view) {
            preview = view;
            return this;
        }

        // 设置主界面的父节点
        public Builder setRoot(ViewGroup v) {
            pRoot = v;
            return this;
        }

        public ForceTouchViewGroup create() {
            ForceTouchViewGroup forceTouchViewGroup = new ForceTouchViewGroup(context);
            forceTouchViewGroup.show(this);
            return forceTouchViewGroup;
        }

        // 放缩图片
        private Bitmap scaleBitmap(Bitmap bitmap) {
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale); //长和宽放大缩小的比例
            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        //高斯模糊效果
        public Bitmap blurBitmap(Bitmap bitmap) {
            //Let's create an empty bitmap with the same size of the bitmap we want to blur
            Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            //Instantiate a new Renderscript
            RenderScript rs = RenderScript.create(context);
            //Create an Intrinsic Blur Script using the Renderscript
            ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            //Create the Allocations (in/out) with the Renderscript and the in/out bitmaps
            Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
            Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
            //Set the radius of the blur
            blurScript.setRadius(25);
            //Perform the Renderscript
            blurScript.setInput(allIn);
            blurScript.forEach(allOut);
            //Copy the final bitmap created by the out Allocation to the outBitmap
            allOut.copyTo(outBitmap);
            //recycle the original bitmap
            bitmap.recycle();
            //After finishing everything, we destroy the Renderscript.
            rs.destroy();
            return outBitmap;
        }

        public Builder setData(List<Map<String, Object>> answerListData) {
            this.answerListData = answerListData;
            return this;
        }

        public Builder setHandler(Handler handler, int what) {
            pHandler = handler;
            pWhat = what;
            return this;
        }
    }

    public void showAnswerList() {
        answerList.startAnimation(showAnswerList);
    }

    public void hideAnswerList() {
        answerList.startAnimation(hideAnswerList);
    }

}