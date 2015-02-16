package net.ui;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import net.app.NetConfApplication;
import net.service.BroadcastMonitorService;
import net.service.UdpDataMonitorService;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.netdatatransfer.R;

public class ChatActivity extends Activity {
    private ChatOnClickListener clickListener = new ChatOnClickListener();
    // 聊天内容的适配器
    private ChatMsgAdapter mAdapter;
    private ListView mListView;
    // 聊天的内容
    private List<ChatMsgEntity> mDataArrays = new ArrayList<ChatMsgEntity>();
    private EditText chatEditText;
    private String targetName;
    private String targetIp;

    ChatReceiver chatReceiver;
    IntentFilter filter;

    NetConfApplication app;

    private void initActionBar() {
        ActionBar title = getActionBar();
        title.setDisplayShowHomeEnabled(false);
        title.setDisplayShowTitleEnabled(false);

        View actionbarLayout = LayoutInflater.from(this).inflate(
                R.layout.chat_title, null);
        title.setDisplayShowCustomEnabled(true);
        title.setCustomView(actionbarLayout);
        ((TextView) findViewById(R.id.chatUserName)).setText(targetName);

        ImageButton back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(clickListener);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        app = (NetConfApplication) getApplication();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
        Bundle bundle = getIntent().getExtras();
        targetName = bundle.getString("name");
        targetIp = bundle.getString("ip");
        app.chatId = targetIp;
        Log.i(this.toString(), "create::" + app.chatId);

        // 设置actionBar
        initActionBar();

        // 设置聊天列表
        chatEditText = (EditText) findViewById(R.id.editText);
        mListView = (ListView) findViewById(R.id.charContentList);
        mAdapter = new ChatMsgAdapter(this, mDataArrays);
        mListView.setAdapter(mAdapter);
        TextView send = (TextView) findViewById(R.id.send);
        send.setOnClickListener(clickListener);
        TextView more = (TextView) findViewById(R.id.sendMore);
        more.setOnClickListener(clickListener);

        // 注册广播接收者
        chatReceiver = new ChatReceiver();
        filter = new IntentFilter();
        filter.addAction("net.ui.chatFrom");

    }

    public class ChatOnClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.back:
                finish();
                break;

            case R.id.send:
                String chatText = chatEditText.getText().toString();
                String hostName = android.os.Build.MODEL;
                if (!chatText.equals("")) {
                    ChatMsgEntity entity = new ChatMsgEntity(hostName,
                            app.getDate(), chatEditText.getText().toString(),
                            false);
                    mDataArrays.add(entity);
                    mAdapter.notifyDataSetChanged();
                    chatEditText.setText("");
                    mListView.setSelection(mListView.getCount() - 1);

                    DataPacket dp = new DataPacket(app.hostIP, hostName,
                            chatText, app.text);
                    final String dpInfo = JSON.toJSONString(dp);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                app.sendUdpData(new DatagramSocket(), dpInfo,
                                        targetIp, app.textPort);
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                break;

            case R.id.sendMore:
                showFileChooser();
                break;

            default:
                break;
            }
        }
    }

    class ChatReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context content, Intent intent) {
            Bundle bundle = intent.getExtras();
            DataPacket dp = JSON.parseObject(bundle.getString("content"),
                    DataPacket.class);

            // 判断是否当前聊天窗口
            ChatMsgEntity entity = new ChatMsgEntity(dp.getSenderName(),
                    app.getDate(), dp.getContent(), true);
            mDataArrays.add(entity);
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(mListView.getCount() - 1);
        }
    }

    @Override
    protected void onPause() {
        app.chatId = "none";
        Log.i(this.toString(), "onPause::" + app.chatId);
        super.onPause();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(chatReceiver);
        Log.i(this.toString(), "chat stop");
        super.onStop();
    }

    @Override
    protected void onResume() {
        app.chatId = targetIp;
        Log.i(this.toString(), "resume::" + app.chatId);
        registerReceiver(chatReceiver, filter);

        // 如果在后台有新消息来
        if (app.chatTempMap.containsKey(targetIp)) {
            Log.i(this.toString(), "get new massage");
            app.nManager.cancelAll();
            mDataArrays.addAll(app.chatTempMap.get(targetIp));
            app.chatTempMap.remove(targetIp);
            mAdapter.notifyDataSetChanged();
        }

        super.onResume();
    }
    
    @Override
    protected void onDestroy() {
        Log.i(this.toString(), "chat:: destory");
        super.onDestroy();
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
       intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "请选择一个要上传的文件"),1024);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        if (resultCode == Activity.RESULT_OK) {
            // Get the Uri of the selected file

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
