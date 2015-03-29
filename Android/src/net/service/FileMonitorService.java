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
    ServerSocket server = null;
    DataInputStream geter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
                server = new ServerSocket(NetConfApplication.filePort);
                while (tag) {
                    Socket socket = server.accept();
                    Logger.info(this.toString(), "send file request");

                    DataPacket dp = JSON.parseObject(
                            new DataInputStream(socket.getInputStream())
                                    .readUTF(), DataPacket.class);

                    String[] sn = dp.getContent().replaceAll("\\\\", "/")
                            .split("/");
                    String fileName = sn[sn.length - 1];
                    int id = NetConfApplication.taskId++;
                    NetConfApplication.sendTaskList.put(id, new Progress(
                            fileName, 0));

                    new SendFileTask().executeOnExecutor(
                            AsyncTask.THREAD_POOL_EXECUTOR, new SendTask(id,
                                    socket, fileName, dp));
                }
            } catch (IOException e) {
                Logger.info(this.toString(), e.toString());
            }
        }
    }

    class SendFileTask extends AsyncTask<SendTask, Integer, Void> {

        @Override
        protected Void doInBackground(SendTask... params) {
            String fileName = params[0].getFileName();
            Socket s = params[0].getSocket();
            int taskId = params[0].getTaskId();
            DataPacket dp = params[0].getDataPacket();

            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            DataOutputStream o = null;

            try {
                geter = new DataInputStream(s.getInputStream());

                if (dp.getTag() == NetConfApplication.fileConf) {
                    bis = new BufferedInputStream(new FileInputStream(new File(
                            dp.getContent())));

                    // 文件大小
                    long total = bis.available();
                    long byteRead = 0;
                    o = new DataOutputStream(s.getOutputStream());
                    o.writeLong(total);
                    Logger.info(this.toString(), "file size:::" + total);

                    bos = new BufferedOutputStream(s.getOutputStream());
                    int len;
                    byte[] bytes = new byte[1024];
                    while ((len = bis.read(bytes)) != -1) {
                        bos.write(bytes, 0, len);
                        bos.flush();
                        byteRead += len;

                        NetConfApplication.sendTaskList.put(taskId,
                                new Progress(fileName,
                                        (int) (byteRead * 100 / total)));
                    }

                }

            } catch (IOException e) {
                Logger.info(this.toString(), e.toString());
            } finally {
                NetConfApplication.getTaskList.remove(taskId);
                try {
                    bis.close();
                    bos.close();
                    o.close();
                    s.close();
                } catch (IOException e) {
                    Logger.info(this.toString(), e.toString());
                }

            }

            return null;
        }

    }
}
