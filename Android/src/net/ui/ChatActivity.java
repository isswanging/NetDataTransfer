package net.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.netdatatransfer.R;

public class ChatActivity extends Activity {
	private ChatOnClickListener clickListener = new ChatOnClickListener(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);

		ActionBar title = getActionBar();
		Bundle bundle = getIntent().getExtras();
		title.setDisplayShowHomeEnabled(false);
		title.setDisplayShowTitleEnabled(false);

		View actionbarLayout = LayoutInflater.from(this).inflate(
				R.layout.chat_title, null);
		title.setDisplayShowCustomEnabled(true);
		title.setCustomView(actionbarLayout);
		((TextView) findViewById(R.id.chatUserName)).setText(bundle
				.getString("name"));

		 ImageButton back = (ImageButton) findViewById(R.id.back);
		 back.setOnClickListener(clickListener);
	}


	public class ChatOnClickListener implements OnClickListener {
		Activity context;

		public ChatOnClickListener(Activity chatActivity) {
			context = chatActivity;

		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.back:
				Intent intent = new Intent(context, UserListActivity.class);
				context.startActivity(intent);
				context.finish();
				break;

			default:
				break;
			}

		}

	}
}
