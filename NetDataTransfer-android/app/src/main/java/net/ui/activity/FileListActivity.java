package net.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.base.BaseActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.file_list);

        initToolbar();
        title.setText("文件列表");
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        TextView t = getView(R.id.filePath);
        t.setText("所在目录：" + NetConfApplication.saveFilePath);

        ListView fileList = getView(R.id.fileList);
        SimpleAdapter adapter = new SimpleAdapter(this, getData(),
                R.layout.file_item, new String[]{"img", "info"}, new int[]{
                R.id.fileImg, R.id.fileInfo});
        fileList.setAdapter(adapter);
        super.onCreate(savedInstanceState);
    }

    private List<? extends Map<String, ?>> getData() {
        List<Map<String, Object>> fileList = new ArrayList<Map<String, Object>>();

        File folder = new File(NetConfApplication.saveFilePath);
        String[] paths = folder.list();
        if (paths != null) {
            for (String path : paths) {
                HashMap<String, Object> m = new HashMap<String, Object>();
                m.put("img", R.drawable.file);
                m.put("info", path);
                fileList.add(m);
            }
        }
        return fileList;
    }
}
