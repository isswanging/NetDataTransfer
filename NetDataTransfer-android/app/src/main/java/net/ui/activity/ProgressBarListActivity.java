package net.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.daimajia.numberprogressbar.NumberProgressBar;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.base.BaseActivity;
import net.log.Logger;
import net.vo.Progress;

import java.util.ArrayList;

public class ProgressBarListActivity extends BaseActivity {
    int send = 0;
    int get = 1;
    int tag;

    private ArrayList<Progress> data = new ArrayList<Progress>();
    ProgressAdapter adapter;

    @Override
    public void initToolbarStyle() {
        mToolbar.setNavigationIcon(R.drawable.back);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tag = getIntent().getFlags();
        setContentView(R.layout.progress);

        initToolbar();
        title.setText("进度列表");
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateHandler.removeCallbacks(running);
                finish();
            }
        });

        initData();
        ListView listView = getView(R.id.progressList);
        adapter = new ProgressAdapter(this);
        listView.setAdapter(adapter);

        // 启动刷新任务
        updateHandler.postDelayed(running, 500);
    }

    @Override
    protected void onDestroy() {
        updateHandler.removeCallbacks(running);
        super.onDestroy();
    }

    private void initData() {
        data.clear();
        if (tag == send)
            initData(NetConfApplication.sendTaskList);
        else if (tag == get)
            initData(NetConfApplication.getTaskList);
    }

    private void initData(SparseArray<Progress> sa) {
        int key;
        Logger.info(this.toString(), "SparseArray size: " + sa.size());
        for (int i = 0, nsize = sa.size(); i < nsize; i++) {
            key = sa.keyAt(i);
            Logger.info(this.toString(), "key----" + key);
            if (key >= 0) {
                data.add(sa.get(key));
            }
        }
        Logger.info(this.toString(), "data size" + data.size());
    }

    class ViewHolder {
        public TextView fileName;
        public NumberProgressBar progerss;
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

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder v = null;
            Progress p = data.get(position);

            if (convertView == null) {
                v = new ViewHolder();
                convertView = mInflater.inflate(R.layout.progress_item, null);
                v.fileName = (TextView) convertView
                        .findViewById(R.id.traFileName);
                v.progerss = (NumberProgressBar) convertView
                        .findViewById(R.id.number_progress_bar);
                convertView.setTag(v);
                v.progerss.setMax(100);
            } else {
                v = (ViewHolder) convertView.getTag();
            }

            // 显示进度
            v.fileName.setText(p.getName());
            v.progerss.setProgress(p.getNum());
            return convertView;
        }
    }

    Handler updateHandler = new Handler();

    Runnable running = new Runnable() {

        @Override
        public void run() {
            try {
                initData();
                adapter.notifyDataSetChanged();
                updateHandler.postDelayed(this, 500);
            } catch (Exception e) {
                updateHandler.removeCallbacks(running);
            }
        }
    };

}
