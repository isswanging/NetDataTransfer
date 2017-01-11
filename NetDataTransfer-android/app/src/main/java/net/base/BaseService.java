package net.base;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BaseService extends Service {
    public ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    public boolean tag;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        tag = false;
        cachedThreadPool.shutdown();
        super.onDestroy();
    }
}
