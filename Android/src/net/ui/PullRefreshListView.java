package net.ui;

import net.log.Logger;
import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.netdatatransfer.R;

public class PullRefreshListView extends LinearLayout implements
        OnTouchListener {

    // 下拉状态
    public enum Tag {
        // 下拉提示可刷新
        Pull_to_Refresh,
        // 释放刷新
        Release_to_Refresh,
        // 刷新中
        Refreshing,
        // 正常
        Normal
    }

    private Tag currentState;
    private Tag lastState;

    private View pullRefreshListView;

    // 下拉头的View
    private View header;

    // header的布局参数
    private MarginLayoutParams headerLayoutParams;

    // header的隐藏高度
    public int hideHeaderHeight;

    // 需要去下拉刷新的ListView
    private ListView listView;

    // 刷新时显示的进度条
    private ProgressBar progressBar;

    // 状态的文字描述
    private TextView description;

    // 是否可下拉刷新
    private boolean pullable;

    // 记录手按下的位置
    private float yDown;

    // 记录滑动中的位置
    private float yMove;

    // 弹性系数
    private double elastic = 0.3;

    // 回滚速度
    private int speed = -15;

    private PullToRefreshListener pullListener;

    // 初始化操作
    public PullRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        pullRefreshListView = LayoutInflater.from(context).inflate(
                R.layout.pull_refresh_listview, this);
        header = pullRefreshListView.findViewById(R.id.refresh_head);
        headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
        listView = (ListView) pullRefreshListView.findViewById(R.id.userList);
        progressBar = (ProgressBar) header.findViewById(R.id.refreshProgress);
        description = (TextView) header.findViewById(R.id.refreshText);
        listView.setOnTouchListener(this);

        hideHeaderHeight = (int) -getResources().getDimension(
                R.dimen.refresh_height);
        currentState = Tag.Normal;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        pullable = isPullable();

        if (pullable) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                yDown = event.getRawY();
                Logger.info(this.toString(), "yDown   " + String.valueOf(yDown));
                return false;
            case MotionEvent.ACTION_MOVE:
                yMove = event.getRawY();
                int distance = (int) (yMove - yDown);
                if (distance <= 0
                        && headerLayoutParams.topMargin <= hideHeaderHeight) {
                    return false;
                }

                if (currentState != Tag.Refreshing) {
                    // 设置偏移
                    headerLayoutParams.topMargin = (int) (distance * elastic + hideHeaderHeight);
                    header.setLayoutParams(headerLayoutParams);

                    if (headerLayoutParams.topMargin - 10 <= 0) {
                        currentState = Tag.Pull_to_Refresh;
                    } else {
                        currentState = Tag.Release_to_Refresh;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentState == Tag.Release_to_Refresh) {
                    // 松手时如果是释放立即刷新状态，就去调用正在刷新
                    new RefreshingTask().execute();
                } else if (currentState == Tag.Pull_to_Refresh) {
                    // 松手时如果是下拉状态，就去调用隐藏下拉头
                    new HideHeaderTask().execute();
                } else if (currentState == Tag.Normal) {
                    return false;
                }

                break;
            }
        }

        // 更新显示信息
        if (currentState == Tag.Pull_to_Refresh
                || currentState == Tag.Release_to_Refresh) {
            updateHeaderView();
            // 当前正处于下拉或释放状态，要让ListView失去焦点，否则被点击的那一项会一直处于选中状态
            listView.setPressed(false);
            listView.setFocusable(false);
            listView.setFocusableInTouchMode(false);
            lastState = currentState;
            // 当前正处于下拉或释放状态，通过返回true屏蔽掉ListView的滚动事件
            return false;
        }
        return false;
    }

    // 判断listView是否可下拉刷新
    public boolean isPullable() {
        // 屏幕第一个可见的item
        View firstVisableItem = listView.getChildAt(0);
        // 第一个可见item在listView中的序号
        int firstVisableIndex = listView.getFirstVisiblePosition();

        if (firstVisableItem == null)
            return true;
        else {
            if (firstVisableIndex == 0 && firstVisableItem.getTop() == 0)
                return true;
            else
                return false;
        }
    }

    // 更新下拉的提示
    private void updateHeaderView() {
        if (lastState != currentState) // 防止重复操作
        {
            switch (currentState) {
            case Pull_to_Refresh:
                description.setText(getResources().getString(
                        R.string.pull_to_refresh));
                description.setVisibility(VISIBLE);
                progressBar.setVisibility(GONE);
                break;

            case Release_to_Refresh:
                description.setText(getResources().getString(
                        R.string.release_to_refresh));
                description.setVisibility(VISIBLE);
                progressBar.setVisibility(GONE);
                break;

            case Refreshing:
                description.setVisibility(GONE);
                progressBar.setVisibility(VISIBLE);
                break;
            case Normal:
                description.setVisibility(INVISIBLE);
                progressBar.setVisibility(GONE);
                break;
            }
        }
    }

    public void finishRefreshing() {
        currentState = Tag.Normal;
        new HideHeaderTask().execute();
    }

    // 刷新状态
    class RefreshingTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (true) {
                topMargin = topMargin + speed;
                if (topMargin <= 0) {
                    topMargin = 0;
                    break;
                }
                publishProgress(topMargin);
            }
            currentState = Tag.Refreshing;
            publishProgress(0);
            if (pullListener != null) {
                pullListener.onRefresh();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... topMargin) {
            updateHeaderView();
            headerLayoutParams.topMargin = topMargin[0];
            header.setLayoutParams(headerLayoutParams);
        }

    }

    // 回到初始状态
    class HideHeaderTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            int topMargin = headerLayoutParams.topMargin;
            while (true) {
                topMargin = topMargin + speed;
                if (topMargin <= hideHeaderHeight) {
                    topMargin = hideHeaderHeight;
                    break;
                }
                publishProgress(topMargin);
            }
            return topMargin;
        }

        @Override
        protected void onProgressUpdate(Integer... topMargin) {
            headerLayoutParams.topMargin = topMargin[0];
            header.setLayoutParams(headerLayoutParams);
        }

        @Override
        protected void onPostExecute(Integer topMargin) {
            headerLayoutParams.topMargin = topMargin;
            header.setLayoutParams(headerLayoutParams);
            currentState = Tag.Normal;
        }

    }

    public ListView getListView() {
        return listView;
    }

    public void setListView(ListView listView) {
        this.listView = listView;
    }

    // 耗时任务的接口
    public interface PullToRefreshListener {
        void onRefresh();
    }

    public float getyMove() {
        return yMove;
    }

    public void setyMove(float yMove) {
        this.yMove = yMove;
    }

    public PullToRefreshListener getPullListener() {
        return pullListener;
    }

    public void setPullListener(PullToRefreshListener pullListener) {
        this.pullListener = pullListener;
    }

}
