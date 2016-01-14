package net.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Message;

public abstract class BaseFragment extends Fragment {
    Notification notification;

    final int login = 0;
    final int refresh = 1;
    final int retry = 2;
    final int answer = 3;
    final int startChat = 4;
    final int incomingMsg = 5;
    final int redraw = 6;
    final int close = 7;
    final int overlay = 8;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        try {
            notification = (Notification) activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        notification = null;
    }

    // 和宿主activity通信的接口
    public interface Notification {
        void notifyInfo(int commend, Object obj);
    }

    // 接受activity的更新UI操作
    public abstract void getCommend(Message msg);

}
