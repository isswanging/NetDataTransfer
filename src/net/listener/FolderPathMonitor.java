package net.listener;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.conf.SystemConf;
import net.util.BuildFolder;

public class FolderPathMonitor implements Runnable {
    ServerSocket server = null;
    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void run() {
        try {
            server = new ServerSocket(SystemConf.pathPort);

            while (true) {
                Socket socket = server.accept();
                new Thread(new GetFilesPath(socket)).start();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class GetFilesPath implements Runnable {
        Socket socket = null;

        public GetFilesPath(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataOutputStream toClient = new DataOutputStream(
                        socket.getOutputStream());
                BufferedInputStream fromeClient = new BufferedInputStream(
                        socket.getInputStream());
                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

                logger.info("即将接收文件路径");
                byte[] bytes = new byte[1024];
                int len;
                while ((len = fromeClient.read(bytes)) != -1) {
                    byteOut.write(bytes, 0, len);
                    byteOut.flush();

                    if (len < 1024)
                        break;

                }
                logger.info("接收完成，路径信息：" + byteOut.toString());

                // 这里的格式是格式是：
                // id！总大小！路径，因此用！切分
                String[] msg = byteOut.toString().split("!");

                String timeId = msg[0];

                // 建立本地存放的目录
                BuildFolder bf = new BuildFolder(
                        SystemConf.savePathList.get(timeId), msg[2]);

                // 存放任务
                SystemConf.taskList.put(timeId, bf.getFiles());
                SystemConf.progress.put(timeId, Long.valueOf(msg[1]));

                // 告诉要发送哪些文件
                logger.info("告诉需要发送的文件夹id");
                toClient.writeUTF(msg[0]);

            } catch (IOException e) {
                logger.error(e.toString());

            }
        }
    }
}
