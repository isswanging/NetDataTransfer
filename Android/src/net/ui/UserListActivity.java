package net.ui;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.conf.SystemConf;
import net.service.BroadcastMonitorService;
import net.util.NetDomain;
import net.vo.Host;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.example.netdatatransfer.R;

public class UserListActivity extends Activity {
	String hostName;
	String userName;
	String userDomain;
	ImageView waitGif;
	List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
	AnimationDrawable anim;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		stopService(new Intent(this, BroadcastMonitorService.class));
		super.onDestroy();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		waitGif.setBackgroundResource(R.anim.frame);
		anim = (AnimationDrawable) waitGif.getBackground();
		anim.start();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// 检查端口
		preCheck();
		// 建立监听
		listen();
		// 主机登录
		login();
		// 建立界面
		initUI();

	}

	private void initUI() {
		// 隐藏标题栏
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.user_list);
		waitGif = (ImageView) findViewById(R.id.wait);

		// 延迟一点加载列表
		final Handler handler = new ListHandler();
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Message msg = handler.obtainMessage();
				msg.what = 0;
				msg.sendToTarget();
			}
		}).start();

	}

	private void login() {
		userName = android.os.Build.MODEL;// 获取用户名
		hostName = "Android";// 获取主机名
		userDomain = "Android";// 获取计算机域

		// 加入在线列表
		final Host host = new Host(userName, userDomain, SystemConf.hostIP,
				hostName, 1, 0);
		NetDomain.addHost(host);

		// 广播登录信息
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					NetDomain.sendUdpData(new DatagramSocket(), host,
							SystemConf.broadcastIP, SystemConf.broadcastPort);
				} catch (SocketException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	private void listen() {
		this.startService(new Intent(this, BroadcastMonitorService.class));
	}

	private void preCheck() {
		if (NetDomain.check().endsWith(SystemConf.SUCCESS)) {
			SystemConf.hostIP = NetDomain.getHostIp(this);// 获取ip地址
		} else {
			// 弹出警告框并退出
			new AlertDialog.Builder(this)
					.setTitle("错误")
					.setMessage("端口异常，启动失败")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									setResult(RESULT_OK);// 确定按钮事件
									finish();
									System.exit(0);
								}
							}).show();
		}
	}

	private List<Map<String, Object>> getData() {
		for (Host host : SystemConf.hostList) {
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("name", host.getUserName());
			item.put("ip", host.getIp());
			item.put("img", R.drawable.head);
			userList.add(item);
		}

		return userList;
	}

	class ListHandler extends Handler {
		@SuppressLint("InflateParams")
		@Override
		public void handleMessage(Message msg) {

			super.handleMessage(msg);
			if (msg.what == 0) {
				// 更新UI
				if (anim.isRunning())
					anim.stop();
				LinearLayout listConent = (LinearLayout) findViewById(R.id.listContent);
				LayoutInflater layoutInflater = getLayoutInflater();
				listConent.removeView(findViewById(R.id.wait));
				ListView userList = (ListView) layoutInflater.inflate(
						R.layout.list_user, null);
				listConent.addView(userList);
				SimpleAdapter adapter = new SimpleAdapter(
						UserListActivity.this, getData(),
						R.layout.user_list_item, new String[] { "name", "ip",
								"img" }, new int[] { R.id.userName,
								R.id.userIP, R.id.head });
				userList.setAdapter(adapter);
			}
		}
	}
}
