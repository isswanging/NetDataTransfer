package net.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.DataPacket;
import net.vo.Progress;
import net.vo.SendTask;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import com.alibaba.fastjson.JSON;

public class FileMonitorService extends Service {
    Thread thread;
    boolean tag;
    NetConfApplication app;
    ServerSocket server = null;
    DataInputStream geter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        app = (NetConfApplication) getApplication();
        Logger.info(this.toString(), "FileMonitorService started");
        tag = true;
        thread = new Thread(new ReceiveInfo());
        thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        tag = false;
        thread.interrupt();
        Logger.info(this.toString(), "service stop");
        super.onDestroy();
    }

    private class ReceiveInfo implements Runnable {

        @Override
        public void run() {
            try {
                server = new ServerSocket(app.filePort);
                while (tag) {
                    Socket socket = server.accept();
                    Logger.info(this.toString(), "send file request");

                    new SendFileTask().executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR, new SendTask(
                                    app.taskId++, socket));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    class SendFileTask extends AsyncTask<SendTask, Integer, Void> {

        @Override
        protected Void doInBackground(SendTask... params) {

            Socket s = params[0].getSocket();
            int taskId = params[0].getTaskId();
            try {
                geter = new DataInputStream(s.getInputStream());
                DataPacket dp = JSON.parseObject(geter.readUTF(),
                        DataPacket.class);

                if (dp.getTag() == app.fileConf) {
                    BufferedInputStream bis = new BufferedInputStream(
                            new FileInputStream(new File(dp.getContent())));

                    String[] sn = dp.getContent().replaceAll("\\\\", "/")
                            .split("/");
                    String fileName = sn[sn.length - 1];

                    // 文件大小
                    long total = bis.available();
                    long byteRead = 0;
                    DataOutputStream o = new DataOutputStream(
                            s.getOutputStream());
                    o.writeLong(total);

                    BufferedOutputStream bos = new BufferedOutputStream(
                            s.getOutputStream());
                    int len;
                    byte[] bytes = new byte[1024];
                    while ((len = bis.read(bytes)) != -1) {
                        bos.write(bytes, 0, len);
                        bos.flush();
                        byteRead += len;

                        app.sendTaskList.put(taskId, new Progress(fileName,
                                (int) (byteRead * 100 / total)));
                    }
                    bis.close();
                    bos.close();
                    app.sendTaskList.remove(taskId);
                }

            } catch (IOException e) {
                app.getTaskList.remove(taskId);
                Logger.info(this.toString(), e.toString());
            }

            return null;
        }

    }
}
