package net.ui;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.app.NetConfApplication;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

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
		app.isChating = true;
		Bundle bundle = getIntent().getExtras();
		targetName = bundle.getString("name");
		targetIp = bundle.getString("ip");

		// 设置actionBar
		initActionBar();

		// 设置聊天列表
		chatEditText = (EditText) findViewById(R.id.editText);
		mListView = (ListView) findViewById(R.id.charContentList);
		mAdapter = new ChatMsgAdapter(this, mDataArrays);
		mListView.setAdapter(mAdapter);
		TextView send = (TextView) findViewById(R.id.send);
		send.setOnClickListener(clickListener);

		// 注册广播接收者
		chatReceiver = new ChatReceiver();
		filter = new IntentFilter();
		filter.addAction("net.ui.chatFrom");

		// 如果是从提示消息进入
		if (null != bundle.getSerializable("content")) {

		}

	}

	// 获取日期
	private String getDate() {
		Calendar c = Calendar.getInstance();
		String year = String.valueOf(c.get(Calendar.YEAR));
		String month = String.valueOf(c.get(Calendar.MONTH));
		String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH) + 1);
		String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
		String mins = String.valueOf(c.get(Calendar.MINUTE));
		StringBuffer sbBuffer = new StringBuffer();
		sbBuffer.append(year + "-" + month + "-" + day + " " + hour + ":"
				+ mins);
		return sbBuffer.toString();
	}

	public class ChatOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back:
				app.isChating = false;
				finish();
				break;

			case R.id.send:
				String chatText = chatEditText.getText().toString();
				String hostName = android.os.Build.MODEL;
				if (!chatText.equals("")) {
					ChatMsgEntity entity = new ChatMsgEntity(hostName,
							getDate(), chatEditText.getText().toString(), false);
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
								app.sendUdpData(new DatagramSocket(), dp,
										targetIp, app.textPort);
							} catch (SocketException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}

			default:
				break;
			}
		}
	}

	class ChatReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context content, Intent intent) {
			Bundle bundle = intent.getExtras();
			DataPacket dp = (DataPacket) bundle.getSerializable("content");

			// 判断是否当前聊天窗口
			if (dp.getIp().equals(targetIp)) {
				// 播放消息提示音乐
				MediaPlayer mp = new MediaPlayer();
				try {
					mp.setDataSource(ChatActivity.this, RingtoneManager
							.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
					mp.prepare();
					mp.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				ChatMsgEntity entity = new ChatMsgEntity(dp.getSenderName(),
						getDate(), dp.getContent(), true);
				mDataArrays.add(entity);
				mAdapter.notifyDataSetChanged();
				mListView.setSelection(mListView.getCount() - 1);
			}
			// 如果不是当前窗口就发送notify通知
			else {
				// 点击启动activity
				Intent notifyIntent = new Intent("net.ui.chatting");
				notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				bundle.putString("name", dp.getSenderName());
				bundle.putString("ip", dp.getIp());
				notifyIntent.putExtras(bundle);
				PendingIntent contentIntent = PendingIntent.getActivity(
						ChatActivity.this, R.string.app_name, notifyIntent,
						PendingIntent.FLAG_UPDATE_CURRENT);

				// 显示
				NotificationManager nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				Notification notification = new NotificationCompat.Builder(
						ChatActivity.this).setSmallIcon(R.drawable.notify)
						.setTicker("新消息").setContentTitle("点击查看")
						.setContentText(dp.getSenderName() + "发来一条新消息")
						.setContentIntent(contentIntent).build();
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notification.defaults |= Notification.DEFAULT_SOUND;
				nManager.notify(R.id.chatName, notification);

			}
		}
	}

	@Override
	protected void onStop() {
		unregisterReceiver(chatReceiver);
		super.onStop();
	}

	@Override
	protected void onResume() {
		registerReceiver(chatReceiver, filter);
		super.onResume();
	}
}
