package net.listener;

import net.conf.SystemConf;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class FolderMonitor implements Runnable {
    private final Log logger = LogFactory.getLog(this.getClass());
    ServerSocket server = null;

    @Override
    public void run() {
        try {
            server = new ServerSocket(SystemConf.folderPort);

            while (true) {
                Socket socket = server.accept();
                new Thread(new SendFiles(socket)).start();

            }
        } catch (SocketException e) {
            logger.error("exception: " + e);
        } catch (IOException e) {
            logger.error("exception: " + e);
        }
    }

    public class SendFiles implements Runnable {
        Socket socket = null;

        public SendFiles(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            DataInputStream dis = null;
            DataOutputStream dos = null;

            try {
                dis = new DataInputStream(new BufferedInputStream(
                        socket.getInputStream()));
                String taskId = dis.readUTF();
                int i = dis.readInt();
                String filePath = SystemConf.taskList.get(taskId).get(i);
                dos = new DataOutputStream(new BufferedOutputStream(
                        new FileOutputStream(filePath)));

                int len;
                byte[] bytes = new byte[1024];
                while ((len = dis.read(bytes)) != -1) {
                    dos.write(bytes, 0, len);
                    // Long temp = SystemConf.progress.get(taskId);
                    // temp = temp - len;
                    SystemConf.progress.put(taskId, SystemConf.progress.get(taskId)-len);
                    dos.flush();
                }
                logger.info(filePath + " 接收完成");
            } catch (IOException e) {
                logger.error("exception: " + e);
            } finally {
                try {
                    if (dos != null) {
                        dos.close();
                    }
                    if (dis != null) {
                        dis.close();
                    }
                } catch (IOException e) {
                    logger.error("exception: " + e);
                }
            }
        }
    }

}
