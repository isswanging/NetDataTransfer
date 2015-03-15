package net.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import net.app.NetConfApplication;
import net.log.Logger;
import net.vo.DataPacket;
import net.vo.GetTask;
import net.vo.Progress;
import android.app.Service;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

public class TransferFile extends AsyncTask<GetTask, Void, Void> {
    Context context;
    String fileName;
    NetConfApplication app;

    public TransferFile(Service c) {
        context = c;
        app = (NetConfApplication) c.getApplication();
    }

    @Override
    protected Void doInBackground(GetTask... params) {

        Logger.info(this.toString(), "begin accept file");
        DataPacket dp = params[0].getDp();
        int taskId = params[0].getTaskId();
        try {
            Socket socket = new Socket(dp.getIp(), NetConfApplication.filePort);

            String hostName = NetConfApplication.hostName;// 获取主机名
            String ip = NetConfApplication.hostIP;// 获取ip地址

            // 发送TCP消息建立文件传输连接
            DataOutputStream toServer = new DataOutputStream(
                    socket.getOutputStream());
            toServer.writeUTF(JSON.toJSONString(new DataPacket(ip, hostName, dp
                    .getContent(), NetConfApplication.fileConf)));

            // 设置文件大小
            DataInputStream in = new DataInputStream(socket.getInputStream());
            long total = in.readLong();
            Logger.info(this.toString(), "size" + total);

            BufferedInputStream bis = new BufferedInputStream(
                    socket.getInputStream());
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(new File(
                            getFileSavePath(dp.getContent()))));

            // 设置进度条和读文件
            int len;
            long byteRead = 0;

            byte[] bytes = new byte[1024];
            while ((len = bis.read(bytes)) != -1) {
                // Logger.info(this.toString(), "receiveing");
                bos.write(bytes, 0, len);
                bos.flush();
                byteRead += len;
                if (app.sendTaskList.containsKey(taskId)) {
                    app.sendTaskList.get(taskId).setNum(
                            (int) (byteRead * 100 / total));
                } else {
                    app.sendTaskList.put(taskId, new Progress(fileName,
                            (int) (byteRead * 100 / total)));
                }
            }

            in.close();
            toServer.close();
            bos.close();
            bis.close();
            socket.close();
            Logger.info(this.toString(), "end file");
            app.sendTaskList.remove(taskId);

        } catch (IOException e) {
            app.sendTaskList.remove(taskId);
            Logger.error(this.toString(), e.toString());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        Toast.makeText(context, "文件 " + fileName + " 接收完毕", Toast.LENGTH_LONG)
                .show();
    }

    public String getFileSavePath(String path) {
        // 获取文件名
        String[] s = path.replaceAll("\\\\", "/").split("/");
        fileName = s[s.length - 1];
        // 文件分隔符
        String fs = System.getProperties().getProperty("file.separator");
        // 保存文件路径
        return NetConfApplication.saveFilePath + fs + s[s.length - 1];

    }

}
