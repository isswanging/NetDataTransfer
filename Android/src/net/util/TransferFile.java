package net.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.DataPacket;
import net.vo.GetTask;
import net.vo.Progress;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.netdatatransfer.R;

public class TransferFile extends AsyncTask<GetTask, Void, Void> {
    Context context;
    String fileName;
    NetConfApplication app;
    boolean error = false;

    public TransferFile(Service c) {
        context = c;
        app = (NetConfApplication) c.getApplication();
    }

    @Override
    protected Void doInBackground(GetTask... params) {

        Logger.info(this.toString(), "begin accept file");
        DataPacket dp = params[0].getDp();
        int taskId = params[0].getTaskId();
        fileName = params[0].getFileName();

        Socket socket = null;
        DataInputStream in = null;
        DataOutputStream toServer = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            socket = new Socket(dp.getIp(), NetConfApplication.filePort);
            String hostName = NetConfApplication.hostName;// 获取主机名
            String ip = NetConfApplication.hostIP;// 获取ip地址

            // 发送TCP消息建立文件传输连接
            toServer = new DataOutputStream(socket.getOutputStream());
            toServer.writeUTF(JSON.toJSONString(new DataPacket(ip, hostName, dp
                    .getContent(), NetConfApplication.fileConf)));

            // 设置文件大小
            in = new DataInputStream(socket.getInputStream());
            long total = in.readLong();
            Logger.info(this.toString(), "size" + total);
            String path = getFileSavePath(fileName);

            bis = new BufferedInputStream(socket.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(new File(path)));

            // 设置进度条和读文件
            int len;
            long byteRead = 0;

            byte[] bytes = new byte[1024];
            while ((len = bis.read(bytes)) != -1) {
                // Logger.info(this.toString(), "receiveing");
                bos.write(bytes, 0, len);
                bos.flush();
                byteRead += len;
                NetConfApplication.getTaskList.put(taskId, new Progress(
                        fileName, (int) (byteRead * 100 / total)));
            }

            Logger.info(this.toString(), "end file");

            // save in content
            saveInContent(path);

        } catch (IOException e) {
            error = true;
            Logger.error(this.toString(), e.toString());
        } finally {
            NetConfApplication.getTaskList.remove(taskId);
            try {
                in.close();
                toServer.close();
                bos.close();
                bis.close();
                socket.close();
            } catch (IOException e) {
                Logger.info(this.toString(), e.toString());
            }

        }
        return null;
    }

    private void saveInContent(String path) {
        Logger.info(this.toString(), fileName);
        String[] sn = fileName.split("\\.");
        String buffix = sn[sn.length - 1];

        ContentResolver resolver = context.getContentResolver();

        for (String s : NetConfApplication.imageSupport) {
            if (s.equalsIgnoreCase(buffix)) {
                // try {
                // MediaStore.Images.Media.insertImage(
                // context.getContentResolver(), path, fileName, null);
                // } catch (FileNotFoundException e) {
                // e.printStackTrace();
                // }
                ContentValues newValues = new ContentValues(6);
                newValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                newValues.put(MediaStore.Images.Media.DATA, path);
                newValues.put(MediaStore.Images.Media.DATE_MODIFIED,
                        System.currentTimeMillis() / 1000);
                newValues.put(MediaStore.Images.Media.MIME_TYPE, "video/*");
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        newValues);

                context.sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                                .parse("file://" + path)));
            }
        }

        for (String s : NetConfApplication.audioSupport) {
            if (s.equalsIgnoreCase(buffix)) {
                ContentValues newValues = new ContentValues(6);
                newValues.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName);
                newValues.put(MediaStore.Audio.Media.DATA, path);
                newValues.put(MediaStore.Audio.Media.DATE_MODIFIED,
                        System.currentTimeMillis() / 1000);
                newValues.put(MediaStore.Audio.Media.MIME_TYPE, "audio/*");
                resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        newValues);

                context.sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                                .parse("file://" + path)));
            }
        }

        for (String s : NetConfApplication.videoSupport) {
            if (s.equalsIgnoreCase(buffix)) {
                ContentValues newValues = new ContentValues(6);
                newValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
                newValues.put(MediaStore.Video.Media.DATA, path);
                newValues.put(MediaStore.Video.Media.DATE_MODIFIED,
                        System.currentTimeMillis() / 1000);
                newValues.put(MediaStore.Video.Media.MIME_TYPE, "video/*");
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        newValues);

                context.sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri
                                .parse("file://" + path)));
            }
        }

    }

    @Override
    protected void onPostExecute(Void result) {
        if (!error) {
            Logger.info(this.toString(), "toast notify");
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.custom_toast, null);
            TextView title = (TextView) layout.findViewById(R.id.toastInfo);
            title.setText("文件 " + fileName + " 接收完毕");
            Toast toast = new Toast(context);
            toast.setDuration(Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER | Gravity.BOTTOM, 0, 100);
            toast.setView(layout);
            toast.show();
        }
    }

    public String getFileSavePath(String name) {
        // 文件分隔符
        String fs = System.getProperties().getProperty("file.separator");
        // 保存文件路径
        return NetConfApplication.saveFilePath + fs + name;

    }

}
