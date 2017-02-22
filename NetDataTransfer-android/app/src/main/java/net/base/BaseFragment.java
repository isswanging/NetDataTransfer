package net.base;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Message;
import android.view.View;

import net.app.NetConfApplication;

public abstract class BaseFragment extends Fragment {
    public Notification notification;
    public NetConfApplication app;

    public final int login = 0;
    public final int refresh = 1;
    public final int retry = 2;
    public final int answer = 3;
    public final int startChat = 4;
    public final int incomingMsg = 5;
    public final int redraw = 6;
    public final int close = 7;
    public final int pressure = 8;
    public final int exit = 9;

    public boolean isRotate = false;
    public View viewGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 翻转时fragment不销毁
        setRetainInstance(true);
        app = (NetConfApplication) getActivity().getApplication();
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

    // 简化findViewById操作
    public <T extends View> T getView(View v, int id) {
        return (T) v.findViewById(id);
    }
}