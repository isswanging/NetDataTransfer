package net.ui;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    LinearLayout sendFile;

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
        Logger.info(this.toString(), "create::" + app.chatId);

        // 设置actionBar
        initActionBar();

        // 设置聊天列表
        chatEditText = (EditText) findViewById(R.id.editText);
        mListView = (ListView) findViewById(R.id.charContentList);
        mAdapter = new ChatMsgAdapter(this, mDataArrays);
        mListView.setAdapter(mAdapter);
        TextView send = (TextView) findViewById(R.id.send);
        send.setOnClickListener(clickListener);
        ImageView more = (ImageView) findViewById(R.id.sendMore);
        more.setOnClickListener(clickListener);
        sendFile = (LinearLayout) findViewById(R.id.sendFile);

        TextView sendImg = (TextView) findViewById(R.id.sendImg);
        TextView sendVideo = (TextView) findViewById(R.id.sendVideo);
        TextView sendAudio = (TextView) findViewById(R.id.sendAudio);
        sendImg.setOnClickListener(clickListener);
        sendVideo.setOnClickListener(clickListener);
        sendAudio.setOnClickListener(clickListener);

        // 注册广播接收者
        chatReceiver = new ChatReceiver();
        filter = new IntentFilter();
        filter.addAction("net.ui.chatFrom");

        mListView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Logger.info(this.toString(), "touch .................");
                sendFile.setVisibility(View.GONE);
                return false;
            }
        });

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

                    final DataPacket dp = new DataPacket(app.hostIP, hostName,
                            chatText, app.text);

                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                app.sendUdpData(new DatagramSocket(),
                                        JSON.toJSONString(dp), targetIp,
                                        app.textPort);
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                break;

            case R.id.sendMore:
                sendFile.setVisibility(View.VISIBLE);
                break;

            case R.id.sendImg:
                sendFile.setVisibility(View.GONE);
                showFileChooser("image/*", image);
                break;

            case R.id.sendVideo:
                sendFile.setVisibility(View.GONE);
                showFileChooser("vedio/*", vedio);
                break;

            case R.id.sendAudio:
                sendFile.setVisibility(View.GONE);
                showFileChooser("audio/*", audio);
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
        unregisterReceiver(chatReceiver);
        Logger.info(this.toString(), "onPause::" + app.chatId);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Logger.info(this.toString(), "chat stop");
        super.onStop();
    }

    @Override
    protected void onResume() {
        app.chatId = targetIp;
        Logger.info(this.toString(), "resume::" + app.chatId);
        registerReceiver(chatReceiver, filter);

        // 如果在后台有新消息来
        if (app.chatTempMap.containsKey(targetIp)) {
            Logger.info(this.toString(), "get new massage");
            app.nManager.cancelAll();
            mDataArrays.addAll(app.chatTempMap.get(targetIp));
            app.chatTempMap.remove(targetIp);
            mAdapter.notifyDataSetChanged();
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Logger.info(this.toString(), "chat:: destory");
        super.onDestroy();
    }

    private void showFileChooser(String type, int code) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);
        // intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "请选择一个要发送的文件"),
                    code);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String filePath = null;

        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();

            switch (requestCode) {
            case image:
            case vedio:
                Logger.info(this.toString(), "image uri::::" + uri.toString());
                filePath = uri2filePath(uri, images, imageID, imageUri, this);
                Logger.info(this.toString(), "path::::" + filePath);
                sendnotifyMsg(filePath);
                break;

            case audio:
                Logger.info(this.toString(), "audio uri::::" + uri.toString());
                filePath = uri2filePath(uri, audios, audioID, audioUri, this);
                Logger.info(this.toString(), "path::::" + filePath);
                sendnotifyMsg(filePath);
                break;

            default:
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void sendnotifyMsg(String filePath) {
        final DataPacket dp = new DataPacket(app.hostIP,
                android.os.Build.MODEL, filePath, app.filePre);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    app.sendUdpData(new DatagramSocket(),
                            JSON.toJSONString(dp), targetIp, app.textPort);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        ChatMsgEntity entity = new ChatMsgEntity(android.os.Build.MODEL,
                app.getDate(), "向对方发送了一个文件", false);
        mDataArrays.add(entity);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(mListView.getCount() - 1);
    }

    // 根据uri获取文件路径
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public String uri2filePath(Uri uri, String type, String tId, Uri tUri,
            Activity activity) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // android4.4以上适用
        if (isKitKat) {
            String path = "";
            if (DocumentsContract.isDocumentUri(activity, uri)) {
                String wholeID = DocumentsContract.getDocumentId(uri);
                String id = wholeID.split(":")[1];
                String[] column = { type };
                String sel = tId + "=?";
                Cursor cursor = activity.getContentResolver().query(tUri,
                        column, sel, new String[] { id }, null);
                int columnIndex = cursor.getColumnIndex(column[0]);
                if (cursor.moveToFirst()) {
                    path = cursor.getString(columnIndex);
                }
                cursor.close();
            } else {
                String[] projection = { type };
                Cursor cursor = activity.getContentResolver().query(uri,
                        projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(type);
                cursor.moveToFirst();
                path = cursor.getString(column_index);

            }

            return path;
        }
        // android 4.4以下适用
        else {
            String[] proj = { type };
            // 好像是android多媒体数据库的封装接口，具体的看Android文档
            Cursor cursor = managedQuery(uri, proj, null, null, null);
            // 按我个人理解 这个是获得用户选择的图片的索引值
            int column_index = cursor.getColumnIndexOrThrow(type);
            // 将光标移至开头 ，这个很重要，不小心很容易引起越界
            cursor.moveToFirst();
            // 最后根据索引值获取图片路径
            String path = cursor.getString(column_index);
            return path;
        }

    }

    private final int image = 997;
    private final int vedio = 998;
    private final int audio = 999;
    private final String images = MediaStore.Images.Media.DATA;
    private final String audios = MediaStore.Audio.Media.DATA;
    private final String imageID = MediaStore.Images.Media._ID;
    private final String audioID = MediaStore.Audio.Media._ID;
    private final Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    private final Uri audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

}
