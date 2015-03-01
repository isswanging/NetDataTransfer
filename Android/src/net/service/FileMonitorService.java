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

import com.alibaba.fastjson.JSON;

import net.app.NetConfApplication;
import net.vo.DataPacket;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

public class FileMonitorService extends Service {
    Thread thread;
    boolean tag;
    NetConfApplication app;
    ServerSocket server = null;
    SendFileTask sendfile;
    DataInputStream geter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendfile = new SendFileTask();
        app = (NetConfApplication) getApplication();
        Log.i(this.toString(), "FileMonitorService started");
        tag = true;
        thread = new Thread(new ReceiveInfo());
        thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        tag = false;
        thread.interrupt();
        Log.i(this.toString(), "service stop");
        super.onDestroy();
    }

    private class ReceiveInfo implements Runnable {

        @Override
        public void run() {
            try {
                server = new ServerSocket(app.filePort);
                while (tag) {
                    Socket socket = server.accept();
                    Log.i(this.toString(), "send file request");
                    sendfile.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            socket);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    class SendFileTask extends AsyncTask<Socket, Integer, Void> {

        @Override
        protected Void doInBackground(Socket... params) {
            try {
                Socket s = params[0];
                geter = new DataInputStream(s.getInputStream());
                DataPacket dp = JSON.parseObject(geter.readUTF(),
                        DataPacket.class);

                if (dp.getTag() == app.fileConf) {
                    BufferedInputStream bis = new BufferedInputStream(
                            new FileInputStream(new File(dp.getContent())));

                    // 文件大小
                    long total = bis.available();
                    DataOutputStream o = new DataOutputStream(
                            s.getOutputStream());
                    o.writeLong(total);

                    // 发文件
                    BufferedOutputStream bos = new BufferedOutputStream(
                            s.getOutputStream());
                    int len;
                    byte[] bytes = new byte[1024];
                    while ((len = bis.read(bytes)) != -1) {
                        bos.write(bytes, 0, len);
                        bos.flush();
                    }
                    bis.close();
                    bos.close();
                }

            } catch (IOException e) {
                Log.i(this.toString(), "IOException");
            }

            return null;
        }

    }
}
