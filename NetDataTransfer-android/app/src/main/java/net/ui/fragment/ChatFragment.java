package net.ui.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.base.BaseFragment;
import net.db.DBManager;
import net.log.Logger;
import net.ui.activity.ChatMsgAdapter;
import net.ui.view.SideslipMenuView;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends BaseFragment {
    private final String TAG = "ChatFragment";
    FrameLayout chatMain;
    SideslipMenuView sideslipMenuView;

    private ChatOnClickListener clickListener = new ChatOnClickListener();
    // 聊天内容的适配器
    private ChatMsgAdapter mAdapter;
    private ListView mListView;
    // 聊天的内容
    private List<ChatMsgEntity> mDataArrays = new ArrayList<>();
    private EditText chatEditText;
    private String targetName;
    private String targetIp;

    LinearLayout sendFile;
    TextView otherName;
    TextView otherIP;

    private final int COPY = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (isRotate) {
            Logger.info(TAG, "not create new ui");
            // 设置聊天列表
            TextView chatCurName = getView(chatMain, R.id.chatCurName);
            chatCurName.setText(targetName);

            mAdapter = new ChatMsgAdapter(app, mDataArrays);
            mListView.setAdapter(mAdapter);
            otherName.setText(targetName);
            otherIP.setText(targetIp);
            return viewGroup;
        } else {
            Logger.info(TAG, "create new ui");
            viewGroup = inflater.inflate(R.layout.chat, container, false);
            sideslipMenuView = getView(viewGroup, R.id.id_menu);
            chatMain = getView(viewGroup, R.id.chatMain);
            chatEditText = getView(viewGroup, R.id.editText);
            mListView = getView(viewGroup, R.id.charContentList);
            otherName = getView(viewGroup, R.id.otherName);
            otherIP = getView(viewGroup, R.id.otherIP);

            TextView send = getView(viewGroup, R.id.send);
            send.setOnClickListener(clickListener);
            ImageView more = getView(viewGroup, R.id.sendMore);
            more.setOnClickListener(clickListener);
            sendFile = getView(viewGroup, R.id.sendFile);

            TextView sendImg = getView(viewGroup, R.id.sendImg);
            TextView sendVideo = getView(viewGroup, R.id.sendVideo);
            TextView sendAudio = getView(viewGroup, R.id.sendAudio);
            sendImg.setOnClickListener(clickListener);
            sendVideo.setOnClickListener(clickListener);
            sendAudio.setOnClickListener(clickListener);
            getView(viewGroup, R.id.closeCurChat).setOnClickListener(clickListener);
            getView(viewGroup, R.id.closeChat).setOnClickListener(clickListener);

            if (app.topFragment.equals("chat")) {
                // 设置聊天列表
                TextView chatCurName = getView(chatMain, R.id.chatCurName);
                chatCurName.setText(targetName);

                mAdapter = new ChatMsgAdapter(app, mDataArrays);
                mListView.setAdapter(mAdapter);
                mListView.setSelection(mListView.getCount() - 1);
                otherName.setText(targetName);
                otherIP.setText(targetIp);
                Logger.info(TAG, "hide overlay");
            }

            mListView.setOnTouchListener(new View.OnTouchListener() {

                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    sendFile.setVisibility(View.GONE);
                    return false;
                }
            });

            registerForContextMenu(mListView);

            return viewGroup;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.info(TAG, "resume::" + app.chatId);
        if (!app.chatId.equals("gone")) {
            checkUnreadMsg();
        }
    }

    @Override
    public void onDestroy() {
        Logger.info(TAG, "chatfragment destroy");
        app.topFragment = "users";
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String filePath;

        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();

            switch (requestCode) {
                case image:
                case vedio:
                    Logger.info(TAG, "image uri::::" + uri.toString());
                    filePath = uri2filePath(uri, images, imageID, imageUri, getActivity());
                    Logger.info(TAG, "path::::" + filePath);
                    sendNotifyMsg(filePath);
                    break;

                case audio:
                    Logger.info(TAG, "audio uri::::" + uri.toString());
                    filePath = uri2filePath(uri, audios, audioID, audioUri, getActivity());
                    Logger.info(TAG, "path::::" + filePath);
                    sendNotifyMsg(filePath);
                    break;

                default:
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        sideslipMenuView.setOpen(false);
        int width = (int) (getResources().getDimension(R.dimen.editText_width));
        Logger.info(TAG, "edit width = " + width);
        chatEditText.getLayoutParams().width = width;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, COPY, 0, R.string.copy_menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case COPY:
                ChatMsgEntity entity = mDataArrays.get(menuInfo.position);
                String text = entity.getText();
                ClipboardManager cbm = (ClipboardManager) app.getSystemService(Context.CLIPBOARD_SERVICE);
                cbm.setPrimaryClip(ClipData.newPlainText(null, text));

                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void getCommend(Message msg) {
        Bundle bundle;
        switch (msg.what) {
            case startChat:
                Logger.info(TAG, "get commend from activity =" + msg.what);
                app.topFragment = "chat";
                bundle = (Bundle) msg.obj;
                showChatContent(bundle);
                break;

            case incomingMsg:
                bundle = (Bundle) msg.obj;
                DataPacket dp = JSON.parseObject(bundle.getString("content"),
                        DataPacket.class);
                ChatMsgEntity entity = new ChatMsgEntity(dp.getSenderName(),
                        app.getDate(), dp.getContent(), true);
                mDataArrays.add(entity);
                mAdapter.notifyDataSetChanged();
                mListView.setSelection(mListView.getCount() - 1);
                break;
        }
    }

    private void checkUnreadMsg() {
        // 如果有未读消息
        DBManager dbm = new DBManager(app);
        if (dbm.contains(targetIp)){
            Logger.info(TAG, "get new massage");
            app.nManager.cancelAll();
            mDataArrays.addAll(dbm.queryMsg(targetIp));
            dbm.deleteMsg(targetIp);
            mAdapter.notifyDataSetChanged();
        }
        mListView.setSelection(mListView.getCount() - 1);
        notification.notifyInfo(redraw, null);
    }

    public void sendNotifyMsg(String filePath) {
        final DataPacket dp = new DataPacket(NetConfApplication.hostIP,
                android.os.Build.MODEL, filePath, NetConfApplication.filePre);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    app.sendUdpData(new DatagramSocket(),
                            JSON.toJSONString(dp), targetIp,
                            NetConfApplication.textPort);
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

    private void showChatContent(Bundle bundle) {
        targetName = bundle.getString("name");
        targetIp = bundle.getString("ip");
        app.chatId = targetIp;
        Logger.info(TAG, "create::" + app.chatId);

        // 设置聊天列表
        TextView chatCurName = getView(chatMain, R.id.chatCurName);
        chatCurName.setText(targetName);

        mAdapter = new ChatMsgAdapter(app, mDataArrays);
        mListView.setAdapter(mAdapter);
        checkUnreadMsg();

        otherName.setText(targetName);
        otherIP.setText(targetIp);

    }

    public class ChatOnClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.closeChat:
                    sideslipMenuView.scrollTo(sideslipMenuView.getmMenuWidth(), 0);
                    sideslipMenuView.setOpen(false);
                case R.id.closeCurChat:
                    app.chatId = "gone";
                    app.topFragment = "users";
                    notification.notifyInfo(close, null);
                    mDataArrays.clear();
                    app.hideKeyboard(getActivity());
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

                        final DataPacket dp = new DataPacket(
                                NetConfApplication.hostIP, hostName, chatText,
                                NetConfApplication.text);

                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    app.sendUdpData(new DatagramSocket(),
                                            JSON.toJSONString(dp), targetIp,
                                            NetConfApplication.textPort);
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
                    showFileChooser("video/*", vedio);
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
            Toast.makeText(app, "请安装文件管理器", Toast.LENGTH_SHORT).show();
        }
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
                String[] column = {type};
                String sel = tId + "=?";
                Cursor cursor = activity.getContentResolver().query(tUri,
                        column, sel, new String[]{id}, null);
                int columnIndex = cursor.getColumnIndex(column[0]);
                if (cursor.moveToFirst()) {
                    path = cursor.getString(columnIndex);
                }
                cursor.close();
            } else {
                String[] projection = {type};
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
            String[] proj = {type};
            // 好像是android多媒体数据库的封装接口，具体的看Android文档
            @SuppressWarnings("deprecation")
            Cursor cursor = getActivity().managedQuery(uri, proj, null, null, null);
            // 按我个人理解 这个是获得用户选择的图片的索引值
            int column_index = cursor.getColumnIndexOrThrow(type);
            // 将光标移至开头 ，这个很重要，不小心很容易引起越界
            cursor.moveToFirst();
            // 最后根据索引值获取图片路径
            return cursor.getString(column_index);
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
