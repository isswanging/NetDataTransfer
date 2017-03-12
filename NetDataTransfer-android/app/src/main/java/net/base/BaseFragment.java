package net.base;

import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

import net.app.NetConfApplication;
import net.vo.EventInfo;

import org.greenrobot.eventbus.EventBus;

public abstract class BaseFragment extends Fragment {
    public NetConfApplication app;

    public boolean isRotate = false;
    public View viewGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 翻转时fragment不销毁
        setRetainInstance(true);
        app = (NetConfApplication) getActivity().getApplication();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        isRotate = true;
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    // 接受activity更新指令的UI操作
    public abstract void getCommend(EventInfo msg);

    // 简化findViewById操作
    public <T extends View> T getView(View v, int id) {
        return (T) v.findViewById(id);
    }
}
