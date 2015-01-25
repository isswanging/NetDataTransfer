package net.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import com.example.netdatatransfer.R;

public class ChatActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 隐藏标题栏
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chat);

		TextView titleName = (TextView) findViewById(R.id.titleName);
		Bundle bundle = getIntent().getExtras();
		titleName.setText(bundle.getString("name"));
	}
}
