package net.ui.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.app.NetConfApplication;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import net.app.netdatatransfer.R;

public class FileListActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.file_list);
        initActionBar();
        TextView t = (TextView) findViewById(R.id.filePath);
        t.setText("所在目录：" + NetConfApplication.saveFilePath);

        ListView fileList = (ListView) findViewById(R.id.fileList);
        SimpleAdapter adapter = new SimpleAdapter(this, getData(),
                R.layout.file_item, new String[] { "img", "info" }, new int[] {
                        R.id.fileImg, R.id.fileInfo });
        fileList.setAdapter(adapter);
        super.onCreate(savedInstanceState);
    }

    private List<? extends Map<String, ?>> getData() {
        List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();

        File folder = new File(NetConfApplication.saveFilePath);
        String[] paths = folder.list();
        for (String path : paths) {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("img", R.drawable.file);
            m.put("info", path);
            fileList.add(m);
        }

        return fileList;
    }

    @SuppressLint("InflateParams")
    private void initActionBar() {
        ActionBar title = getActionBar();
        title.setDisplayShowHomeEnabled(false);
        title.setDisplayShowTitleEnabled(false);

        View actionbarLayout = LayoutInflater.from(this).inflate(
                R.layout.common_title, null);
        title.setDisplayShowCustomEnabled(true);
        title.setCustomView(actionbarLayout);
        ((TextView) findViewById(R.id.titleName)).setText("文件列表");

        ImageButton back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}
