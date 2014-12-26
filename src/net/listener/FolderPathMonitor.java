package net.listener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import net.conf.SystemConf;
import net.util.BuildFolder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
                DataInputStream fromeClient = new DataInputStream(
                        socket.getInputStream());
                StringBuilder path = new StringBuilder();

                int loop = fromeClient.readInt();
                for (int i = 1; i <= loop; i++) {
                    path.append(fromeClient.readUTF());
                }

                logger.info("接收完成，路径信息：" + path.toString() + " 长度:"
                        + path.toString().length());

                // 这里的格式是格式是：
                // id>总大小>路径，因此用>切分
                String[] msg = path.toString().split(">");
                String timeId = msg[0];

                // 建立本地存放的目录
                logger.info("path length:::" + msg[2].length());
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
