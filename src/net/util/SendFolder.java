package net.util;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.conf.SystemConf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SendFolder {
    private final Log logger = LogFactory.getLog(this.getClass());

    public SendFolder(String id, String ip) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        String[] path = SystemConf.sendPathList.get(id).split("\\|");
        String[] filesPath = path[path.length - 1].split("\\*");

        logger.info("需要发送的文件总数为：" + filesPath.length);

        for (int i = 0; i < filesPath.length; i++) {
            executorService.execute(sendFile(ip, filesPath[i], i, id));
        }
    }

    private Runnable sendFile(final String Ip, final String filePath,
            final int i, final String taskId) {
        return new Runnable() {
            private Socket socket = null;
            private String ip = Ip;
            private int port = SystemConf.folderPort;

            @Override
            public void run() {
                logger.info(filePath);
                try {
                    // 建立TCP连接
                    if (connect()) {
                        BufferedInputStream bis = new BufferedInputStream(
                                new FileInputStream(new File(filePath)));
                        DataOutputStream dos = new DataOutputStream(
                                socket.getOutputStream());

                        logger.info("任务id：" + taskId + "文件编号：" + i + "路径："
                                + filePath);

                        // 发送任务id和文件路径
                        dos.writeUTF(taskId);
                        dos.flush();
                        dos.writeInt(i);
                        dos.flush();

                        // 发文件
                        int len;
                        byte[] bytes = new byte[1024];
                        while ((len = bis.read(bytes)) != -1) {
                            dos.write(bytes, 0, len);
                            dos.flush();
                        }
                        dos.flush();
                        bis.close();
                        dos.close();
                        socket.close();
                        logger.info(filePath + " 发送完成");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            protected boolean connect() {
                try {
                    socket = new Socket(ip, port);
                    logger.info("TCP连接成功！");
                    return true;
                } catch (Exception e) {
                    logger.error("TCP连接失败！");
                    return false;
                }
            }
        };
    }

}
