package net.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.Window;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.example.netdatatransfer.R;

public class UserListActivity extends Activity {

	List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 隐藏标题栏
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.user_list);
		ListView userList = (ListView) this.findViewById(R.id.userList);
		SimpleAdapter adapter = new SimpleAdapter(this, getData(),
				R.layout.user_list_item, new String[] { "name", "ip", "img" },
				new int[] { R.id.userName, R.id.userIP, R.id.head });
		userList.setAdapter(adapter);
	}

	private List<Map<String, Object>> getData() {
		Map<String, Object> item = new HashMap<String, Object>();
		item.put("name", android.os.Build.MODEL);
		item.put("ip", getIp());
		item.put("img", R.drawable.head);

		userList.add(item);
		userList.add(item);
		userList.add(item);
		userList.add(item);
		return userList;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	public String getIp() {

		// 获取wifi服务
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		// 判断wifi是否开启
		if (!wifiManager.isWifiEnabled()) {
			wifiManager.setWifiEnabled(true);
		}
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		String ip = intToIp(ipAddress);
		return ip;
	}

	private String intToIp(int i) {

		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + (i >> 24 & 0xFF);
	}

}
