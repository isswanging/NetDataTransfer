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
import net.vo.DataPacket;
import android.os.AsyncTask;
import android.util.Log;

import com.alibaba.fastjson.JSON;

public class TransferFile extends AsyncTask<DataPacket, Void, Void> {

    @Override
    protected Void doInBackground(DataPacket... params) {
        try {
            Log.i(this.toString(), "begin accept file");
            DataPacket dp = params[0];
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
            Log.i(this.toString(), "size" + total);

            // 接收文件
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
                Log.i(this.toString(), "receiveing");
                bos.write(bytes, 0, len);
                bos.flush();
                byteRead += len;
            }

            in.close();
            toServer.close();
            bos.close();
            bis.close();
            socket.close();
            Log.i(this.toString(), "end file");

        } catch (IOException e) {
            Log.i(this.toString(), e.toString());
        }
        return null;
    }

    public String getFileSavePath(String path) {

        // 获取文件名
        String[] s = path.replaceAll("\\\\", "/").split("/");
        // 文件分隔符
        String fs = System.getProperties().getProperty("file.separator");
        // 保存文件路径
        return NetConfApplication.saveFilePath + fs + s[s.length - 1];

    }

}
