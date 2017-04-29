package net.ui.view;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.db.DBManager;
import net.log.Logger;
import net.vo.Host;

import java.util.ArrayList;

public class DragListView extends ListView {
    private static final String TAG = "DragListView";

    private int postion = INVALID_POSITION;// 记录点击的位置
    private RedDotHelper redDotHelper;

    public DragListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Logger.info(TAG, "dispatchTouchEvent ACTION_DOWN.......");
                postion = pointToPosition((int) event.getX(), (int) event.getY());
                if (postion != INVALID_POSITION) {
                    int p = postion - getFirstVisiblePosition();
                    redDotHelper = (RedDotHelper) getChildAt(p).findViewById(R.id.unread);
                    String ip = ((TextView) getChildAt(p).findViewById(R.id.userIP)).
                            getText().toString();
                    redDotHelper.setIP(ip);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (NetConfApplication.drag) {
                    Logger.info(TAG, "dispatchTouchEvent ACTION_MOVE");
                    redDotHelper.getRedDot().onTouchEvent(event);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                Logger.info(TAG, "dispatchTouchEvent ACTION_UP");
                if (NetConfApplication.drag) {
                    Logger.info(TAG, "dispatchTouchEvent ACTION_UP to reddot view");
                    if (redDotHelper.getRedDot().isOut) {
                        updateSingleItem(postion, true);
                    }
                    redDotHelper.getRedDot().onTouchEvent(event);
                    redDotHelper = null;
                    NetConfApplication.drag = false;
                    return true;
                }

                break;

        }

        return super.dispatchTouchEvent(event);
    }

    public static class DragAdapter extends BaseAdapter {
        private ArrayList<Item> resIdList;
        private LayoutInflater mInflater;
        private DBManager dbm;
        private NetConfApplication app;

        public DragAdapter(Context context) {
            resIdList = new ArrayList<>();
            dbm = new DBManager(context);
            app = (NetConfApplication) ((Activity) context).getApplication();
            mInflater = LayoutInflater.from(context);
            initData();
        }

        public void initData() {
            resIdList.clear();
            for (Host host : app.hostList) {
                if (dbm.contains(host.getIp())) {
                    resIdList.add(new Item(host.getUserName(), host.getIp(), R.drawable.unread));
                } else {
                    resIdList.add(new Item(host.getUserName(), host.getIp(), 0));
                }
            }
        }

        @Override
        public int getCount() {
            return resIdList.size();
        }

        @Override
        public Object getItem(int position) {
            return resIdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Item item = resIdList.get(position);

            ViewHold viewHold = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.user_item, null);
                viewHold = new ViewHold();
                viewHold.name = (TextView) convertView.findViewById(R.id.userName);
                viewHold.ip = (TextView) convertView.findViewById(R.id.userIP);
                viewHold.unread = (ImageView) convertView.findViewById(R.id.unread);

                convertView.setTag(viewHold);
            } else {
                viewHold = (ViewHold) convertView.getTag();
            }

            viewHold.name.setText(item.name);
            viewHold.ip.setText(item.ip);
            viewHold.unread.setImageResource(item.resId);

            return convertView;
        }

        class Item {
            String name;
            String ip;
            int resId;

            public Item(String name, String ip, int resId) {
                this.name = name;
                this.ip = ip;
                this.resId = resId;
            }

            public void setResId(int resId) {
                this.resId = resId;
            }
        }

        class ViewHold {
            TextView name;
            TextView ip;
            ImageView unread;
        }

    }

    /**
     * 更新单个Item
     */
    public void updateSingleItem(int postion, boolean isRead) {
        DragAdapter adp = (DragAdapter) getAdapter();
        if (isRead) {
            adp.resIdList.get(postion).setResId(0);
        } else {
            adp.resIdList.get(postion).setResId(R.drawable.unread);
        }
    }
}
