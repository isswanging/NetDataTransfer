package net.ui;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.conf.SystemConf;
import net.service.BroadcastMonitorService;
import net.ui.PullRefreshListView.PullToRefreshListener;
import net.util.NetDomain;
import net.vo.Host;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;

import com.example.netdatatransfer.R;

public class UserListActivity extends Activity {
	String hostName;
	String userName;
	String userDomain;
	ImageView waitGif;
	List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
	AnimationDrawable anim;

	public final int login = 0;
	public final int refresh = 1;

	SimpleAdapter adapter;
	PullRefreshListView pullRefreshListView;

	Handler handler = new ListHandler(this);

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

	private void forceShowOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initUI() {
		forceShowOverflowMenu();
		setContentView(R.layout.user_list);
		waitGif = (ImageView) findViewById(R.id.wait);

		// 延迟一点加载列表
		if (SystemConf.wifi == 1) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Message msg = handler.obtainMessage();
					msg.what = login;
					handler.sendMessageDelayed(msg, 1000);
				}
			}).start();
		}
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
		if (NetDomain.check(this).endsWith(SystemConf.SUCCESS)) {
			SystemConf.hostIP = NetDomain.getHostIp(this);// 获取ip地址
		} else {
			// 弹出警告框并退出
			new AlertDialog.Builder(this)
					.setTitle("错误")
					.setMessage("wifi未连接或端口异常，启动失败")
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
		userList.clear();
		for (Host host : SystemConf.hostList) {
			Map<String, Object> item = new HashMap<String, Object>();
			item.put("name", host.getUserName());
			item.put("ip", host.getIp());
			item.put("img", R.drawable.head);
			userList.add(item);
		}

		return userList;
	}

	static class ListHandler extends Handler {
		WeakReference<UserListActivity> refActvity;

		ListHandler(UserListActivity activity) {
			refActvity = new WeakReference<UserListActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			final UserListActivity act = refActvity.get();
			if (act != null) {
				if (msg.what == act.login) {
					act.adapter = new SimpleAdapter(act, act.getData(),
							R.layout.user_list_item, new String[] { "name",
									"ip", "img" }, new int[] { R.id.userName,
									R.id.userIP, R.id.head });
					Log.i(this.toString(),
							String.valueOf(SystemConf.hostList.size()));

					// 更新UI
					if (act.anim != null && act.anim.isRunning())
						act.anim.stop();
					LinearLayout listConent = (LinearLayout) act
							.findViewById(R.id.listContent);
					LayoutInflater layoutInflater = act.getLayoutInflater();
					act.pullRefreshListView = (PullRefreshListView) layoutInflater
							.inflate(R.layout.users, null);

					listConent.removeView(act.findViewById(R.id.wait));
					listConent.addView(act.pullRefreshListView);

					act.pullRefreshListView.getListView().setAdapter(
							act.adapter);
					act.pullRefreshListView
							.setPullListener(new PullToRefreshListener() {

								@Override
								public void onRefresh() {
									SystemConf.hostList.clear();
									String userName = android.os.Build.MODEL;// 获取用户名
									String hostName = "Android";// 获取主机名
									String userDomain = "Android";// 获取计算机域

									// 加入在线列表
									Host host = new Host(userName, userDomain,
											SystemConf.hostIP, hostName, 1, 0);
									NetDomain.addHost(host);
									try {
										NetDomain.sendUdpData(
												new DatagramSocket(), host,
												SystemConf.broadcastIP,
												SystemConf.broadcastPort);
									} catch (SocketException e) {
										e.printStackTrace();
									}
									Message msg = act.handler.obtainMessage();
									msg.what = act.refresh;
									act.handler.sendMessageDelayed(msg, 2000);
								}
							});
				}

				if (msg.what == act.refresh) {
					act.pullRefreshListView.finishRefreshing();
					act.getData();
					act.adapter.notifyDataSetChanged();
				}
			}
		}
	}
}
