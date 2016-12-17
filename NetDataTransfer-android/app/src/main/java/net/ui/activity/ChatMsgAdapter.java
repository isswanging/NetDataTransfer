package net.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.app.netdatatransfer.R;
import net.vo.ChatMsgEntity;

import java.lang.ref.WeakReference;
import java.util.List;

@SuppressLint("InflateParams")
public class ChatMsgAdapter extends BaseAdapter {

    private List<ChatMsgEntity> data;
    private LayoutInflater mInflater;

    // ListView视图的内容由MsgViewType决定
    public interface MsgViewType {
        // 对方发来的信息
        int get_msg = 0;
        // 自己发出的信息
        int send_msg = 1;
    }

    public ChatMsgAdapter(WeakReference<MainActivity> refActvity, List<ChatMsgEntity> data) {
        this(refActvity.get(), data);
    }

    public ChatMsgAdapter(Context context, List<ChatMsgEntity> data) {
        this.data = data;
        mInflater = LayoutInflater.from(context);
    }

    // 获取ListView的项个数
    public int getCount() {
        return data.size();
    }

    // 获取项
    public Object getItem(int position) {
        return data.get(position);
    }

    // 获取项的ID
    public long getItemId(int position) {
        return position;
    }

    // 获取项的类型
    public int getItemViewType(int position) {
        ChatMsgEntity entity = data.get(position);

        if (entity.getMsgType())
            return MsgViewType.get_msg;
        else
            return MsgViewType.send_msg;
    }

    // 获取项的类型数
    public int getViewTypeCount() {
        return 2;
    }

    // 获取View
    public View getView(int position, View convertView, ViewGroup parent) {

        ChatMsgEntity entity = data.get(position);
        boolean isComMsg = entity.getMsgType();

        ViewHolder viewHolder = null;
        if (convertView == null) {
            if (isComMsg) {
                // 如果是对方发来的消息，则显示的是左气泡
                convertView = mInflater.inflate(R.layout.chat_msg_left, null);
            } else {
                // 如果是自己发出的消息，则显示的是右气泡
                convertView = mInflater.inflate(R.layout.chat_msg_right, null);
            }

            viewHolder = new ViewHolder();
            viewHolder.sendTime = (TextView) convertView
                    .findViewById(R.id.sendTime);
            viewHolder.content = (TextView) convertView
                    .findViewById(R.id.chatContent);
            viewHolder.isComMsg = isComMsg;

            viewHolder.content.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    v.showContextMenu();
                    return true;
                }
            });

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.sendTime.setText(entity.getDate());
        viewHolder.content.setText(entity.getText());

        return convertView;
    }

    // 通过ViewHolder显示项的内容
    class ViewHolder {
        public TextView sendTime;
        public TextView content;
        public boolean isComMsg;
    }

}
