package net.ui;

import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.Progress;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.netdatatransfer.R;

public class ProgressBarListActivity extends Activity {
    int send = 0;
    int get = 1;
    int tag;
    int refresh = 3;

    private ArrayList<Progress> data = new ArrayList<Progress>();
    ProgressAdapter adapter;

    // 定时刷新进度条
    private ScheduledThreadPoolExecutor se = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> sf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tag = getIntent().getFlags();
        setContentView(R.layout.progress);
        initActionBar();
        initData();

        ListView listView = (ListView) findViewById(R.id.progressList);
        adapter = new ProgressAdapter(this);
        listView.setAdapter(adapter);

        // 启动刷新任务
        refresh();

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        sf.cancel(true);
        super.onDestroy();
    }

    private void refresh() {
        sf = se.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                initData();
                try {
                    Message msg = updateHandler.obtainMessage();
                    msg.what = refresh;
                    msg.sendToTarget();
                    Logger.info(this.toString(), "refreshig......");
                } catch (Exception e) {
                    Logger.error(this.toString(), "refreshig error");
                }
            }
        }, 100, 500, TimeUnit.MILLISECONDS);
    }

    private void initData() {
        data.clear();
        if (tag == send) {
            for (Progress p : NetConfApplication.sendTaskList.values()) {
                data.add(p);
            }
        } else if (tag == get) {
            for (Progress p : NetConfApplication.sendTaskList.values()) {
                data.add(p);
            }
        }
    }

    private void initActionBar() {
        ActionBar title = getActionBar();
        title.setDisplayShowHomeEnabled(false);
        title.setDisplayShowTitleEnabled(false);

        View actionbarLayout = LayoutInflater.from(this).inflate(
                R.layout.common_title, null);
        title.setDisplayShowCustomEnabled(true);
        title.setCustomView(actionbarLayout);
        ((TextView) findViewById(R.id.titleName)).setText("进度列表");

        ImageButton back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sf.cancel(true);
                finish();
            }
        });
    }

    class ViewHolder {
        public TextView fileName;
        public ProgressBar progerss;
    }

    class ProgressAdapter extends BaseAdapter {
        private LayoutInflater mInflater = null;

        private ProgressAdapter(Context context) {
            // 根据context上下文加载布局
            this.mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder v = null;
            Progress p = data.get(position);

            if (convertView == null) {
                v = new ViewHolder();
                convertView = mInflater.inflate(R.layout.progress_item, null);
                v.fileName = (TextView) convertView
                        .findViewById(R.id.traFileName);
                v.progerss = (ProgressBar) convertView
                        .findViewById(R.id.progressBar);
                convertView.setTag(v);
                v.progerss.setMax(100);
            } else {
                v = (ViewHolder) convertView.getTag();
            }

            Logger.info(this.toString(), "bbbbbbbbbbbbbbb");
            // 显示进度
            v.fileName.setText(p.getName());
            v.progerss.setProgress(p.getNum());
            return convertView;
        }
    }

    Handler updateHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (msg.what == refresh) {
                adapter.notifyDataSetChanged();
            }

            super.handleMessage(msg);
        }

    };

}
