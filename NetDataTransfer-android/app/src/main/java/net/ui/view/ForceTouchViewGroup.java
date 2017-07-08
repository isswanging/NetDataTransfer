package net.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;
import net.ui.activity.MainActivity;
import net.util.ScreenHelpUtils;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * 模仿iphone的3D touch效果
 */
public class ForceTouchViewGroup extends RelativeLayout {
    private final String TAG = "ForceTouchViewGroup";

    public TextView title;
    public ViewStub preview;
    public RelativeLayout.LayoutParams previewParams;
    public RelativeLayout.LayoutParams answerParams;
    public ListView answerList;
    public SimpleAdapter answerAdapter;
    public NetConfApplication app;
    public boolean isFirst = false;//判断是否第一次显示
    public boolean isShow = false;// 判断菜单是否显示
    public boolean isLock = false;// 屏蔽activity的事件分发
    public boolean running = false;// 动画是否运行
    public boolean isMove = true;
    public float yDown;
    public float yMove;
    public float yTemp;
    public ActionListener actionListener;
    public int topMargin;
    public int moveTopMargin;
    public int answerHeight;
    public int needMove = 0;// 菜单显示需要上拉的距离
    public int answerListTop;
    public int screenheight;
    public View previewContent;
    public Context mContext;
    public boolean canMove;

    private final int add = 0;
    private final int remove = 1;

    private long time = 0;
    private final int stop = 200;

    public ForceTouchViewGroup(Context context) {
        super(context);
        // 初始化
        app = (NetConfApplication) context.getApplicationContext();
        LayoutInflater.from(app).inflate(R.layout.force_touch_view, this);

        title = (TextView) findViewById(R.id.preview_username);
        preview = (ViewStub) findViewById(R.id.preview);
        answerList = (ListView) findViewById(R.id.answer);
        mContext = context.getApplicationContext();
        canMove = true;
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

        // 填充view和list数据
        if (previewContent == null) {
            preview.setLayoutResource(builder.previewLayout);
            preview.inflate();
            previewContent = findViewById(R.id.cust_preview);
        }
        previewParams = (RelativeLayout.LayoutParams) previewContent.getLayoutParams();
        answerParams = (RelativeLayout.LayoutParams) answerList.getLayoutParams();
        initPostion(mContext);

        answerAdapter = new SimpleAdapter(app, builder.answerListData,
                R.layout.answer_item, new String[]{"text"}, new int[]{R.id.answer_text});
        answerList.setAdapter(answerAdapter);
        actionListener.updateUI(add);
        // 计算list高度和滑动的距离
        if (!isFirst) {
            answerList.post(new Runnable() {
                @Override
                public void run() {
                    int between = getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin);
                    answerHeight = answerList.getMeasuredHeight();
                    needMove = answerHeight - topMargin + between * 2;
                    answerListTop = screenheight - between - answerHeight;
                    Logger.info(TAG, "answerlist height is ==== " + answerHeight);
                    answerParams.topMargin = screenheight;
                    answerList.setLayoutParams(answerParams);
                }
            });
            isFirst = true;
        }

        // 注册点击事件
        answerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Logger.info(TAG, "in item click event");
                Message msg = ((Handler) builder.pHandler.get()).obtainMessage();
                msg.what = builder.pWhat;
                msg.arg1 = position;
                msg.sendToTarget();
                actionListener.updateUI(remove);
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                yDown = event.getRawY();
                moveTopMargin = previewParams.topMargin;
                Logger.info(TAG, "yDown====" + yDown + "===answerListTop====" + answerListTop);
                isMove = !(yDown > answerListTop && isShow);
                break;
            case MotionEvent.ACTION_UP:
                Logger.info(TAG, "in forceTouchView touch UP");
                if (!isShow) {
                    actionListener.updateUI(remove);
                } else {
                    previewParams.topMargin = topMargin - needMove;
                    previewContent.setLayoutParams(previewParams);
                    answerParams.topMargin = topMargin - getNeedMove() +
                            getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin) +
                            previewContent.getMeasuredHeight();
                    answerList.setLayoutParams(answerParams);
                    yTemp = 0;
                }
                setCanMove(true);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                Logger.info(TAG, "in activity touch ACTION_POINTER_DOWN");
                setCanMove(false);
                Logger.info(TAG, "stop move");
                break;
            case MotionEvent.ACTION_POINTER_UP:
                Logger.info(TAG, "in activity touch ACTION_POINTER_UP");
                if (!isShow) {
                    actionListener.updateUI(remove);
                }
                setCanMove(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isMove && canMove) {
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
                    }
                    // view处于下压并且继续下拉的状态
                    else if ((moveTopMargin + gap) >= topMargin && yMove > yTemp) {
                        moveTopMargin = (int) (moveTopMargin + gap * 0.15);
                    } else {
                        moveTopMargin = (int) (moveTopMargin + gap);
                    }
                    yTemp = yMove;
                    previewParams.topMargin = moveTopMargin;
                    previewContent.setLayoutParams(previewParams);

                    if (!running && moveTopMargin >= (topMargin - getNeedMove()) &&
                            moveTopMargin <= (topMargin - getNeedMove() + NetConfApplication.upMoveCache)) {
                        if (!isShow && gap < -NetConfApplication.delay_distance) {
                            Logger.info(TAG, "start running animator");
                            showAnswerList(moveTopMargin + previewContent.getHeight() +
                                    getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin));
                        } else if (isShow) {
                            answerParams.topMargin = moveTopMargin + previewContent.getHeight() +
                                    getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin);
                            answerList.setLayoutParams(answerParams);
                        }
                    }

                    if (!running && moveTopMargin < (topMargin - getNeedMove())) {
                        answerParams.topMargin = topMargin - getNeedMove() +
                                getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin) +
                                previewContent.getMeasuredHeight();
                        answerList.setLayoutParams(answerParams);
                    }

                    if (moveTopMargin > (topMargin - getNeedMove() + NetConfApplication.downMoveCache) &&
                            !running && isShow && gap > NetConfApplication.delay_distance) {
                        hideAnswerList(moveTopMargin + previewContent.getHeight() +
                                getResources().getDimensionPixelSize(R.dimen.force_touch_view_margin));
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    public static class Builder {
        WeakReference<MainActivity> refActvity;
        Context context;
        Bitmap blurBg;
        String pTargetIP;
        WeakReference pHandler;
        int pWhat;
        float scale = 0.1f;
        int previewLayout;
        List<Map<String, Object>> answerListData;
        ForceTouchViewGroup forceTouchViewGroup;

        public Builder(MainActivity c) {
            refActvity = new WeakReference<>(c);
            context = refActvity.get();
        }

        public Builder setBackground(View view) {
            if (blurBg == null) {
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
            }
            return this;
        }

        public Builder setIP(String ip) {
            pTargetIP = ip;
            return this;
        }

        // 设置主界面
        public Builder setView(int res) {
            previewLayout = res;
            return this;
        }

        public ForceTouchViewGroup create() {
            if (forceTouchViewGroup == null)
                forceTouchViewGroup = new ForceTouchViewGroup(context.getApplicationContext());
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
        Bitmap blurBitmap(Bitmap bitmap) {
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
            pHandler = new WeakReference(handler);
            pWhat = what;
            return this;
        }
    }

    // 将位置初始化
    public void initPostion(Context context) {
        yTemp = 0;
        answerList.setVisibility(INVISIBLE);

        isShow = false;// 判断菜单是否显示
        isLock = false;// 屏蔽activity的事件分发
        running = false;// 动画是否运行

        // 居中
        int height = getResources().getDimensionPixelSize(R.dimen.preview_content_height);
        //int stateHeight = ScreenHelpUtils.getStatusHeight(context);
        screenheight = ScreenHelpUtils.getScreenHeight(context);
        topMargin = (screenheight - height) / 2;
        previewParams.topMargin = topMargin;
        previewContent.setLayoutParams(previewParams);
    }

    public void showAnswerList(int endPostion) {
        ValueAnimator animator = ValueAnimator.ofInt(screenheight, endPostion);
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                answerParams.topMargin = (int) (Integer) valueAnimator.getAnimatedValue();
                answerList.setLayoutParams(answerParams);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                running = false;
                isShow = true;
                time = System.currentTimeMillis();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                answerList.setVisibility(VISIBLE);
                running = true;
            }
        });
        if ((System.currentTimeMillis() - time) > stop)
            animator.start();
    }

    public void hideAnswerList(int startPostion) {
        ValueAnimator animator = ValueAnimator.ofInt(startPostion, screenheight);
        animator.setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                answerParams.topMargin = (int) (Integer) valueAnimator.getAnimatedValue();
                answerList.setLayoutParams(answerParams);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                answerList.setVisibility(VISIBLE);
                running = false;
                time = System.currentTimeMillis();
            }

            @Override
            public void onAnimationStart(Animator animation) {
                running = true;
                isShow = false;
            }
        });
        if ((System.currentTimeMillis() - time) > stop)
            animator.start();
    }


    public interface ActionListener {
        void updateUI(int cmd);
    }

    public void setActionListener(ActionListener listener) {
        actionListener = listener;
    }

    public boolean isCanMove() {
        return canMove;
    }

    public void setCanMove(boolean move) {
        canMove = move;
    }
}