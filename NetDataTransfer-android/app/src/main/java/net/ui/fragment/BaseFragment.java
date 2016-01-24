package net.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Message;
import android.view.View;

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
    final int pressure = 8;

    boolean isRotate = false;
    View viewGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 翻转时fragment不销毁
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
    public void onSaveInstanceState(Bundle outState) {
        isRotate = true;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        notification = null;
        super.onDetach();
    }

    // 和宿主activity通信的接口
    public interface Notification {
        void notifyInfo(int commend, Object obj);
    }

    // 接受activity更新指令的UI操作
    public abstract void getCommend(Message msg);

}
