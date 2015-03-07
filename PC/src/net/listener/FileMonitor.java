package net.listener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import net.conf.SystemConf;
import net.vo.DataPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;

public class FileMonitor implements Runnable {
    ServerSocket server = null;
    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void run() {
        try {
            server = new ServerSocket(SystemConf.filePort);

            while (true) {
                Socket socket = server.accept();
                new Thread(new SendFile(socket)).start();

            }
        } catch (SocketException e) {
            logger.error("exception: " + e);
        } catch (IOException e) {
            logger.error("exception: " + e);
        }
    }

    public class SendFile implements Runnable {
        Socket socket = null;

        public SendFile(Socket s) {
            socket = s;
        }

        @Override
        public void run() {
            try {
                DataOutputStream o = new DataOutputStream(
                        socket.getOutputStream());
                DataInputStream geter = new DataInputStream(
                        socket.getInputStream());
                DataPacket dp = JSON.parseObject(geter.readUTF(),
                        DataPacket.class);

                if (dp.getTag() == SystemConf.fileConf) {
                    BufferedInputStream bis = new BufferedInputStream(
                            new FileInputStream(new File(dp.getContent())));

                    // 文件大小
                    long total = bis.available();
                    logger.info("size" + total);
                    o.writeLong(total);

                    // 发文件
                    BufferedOutputStream bos = new BufferedOutputStream(
                            socket.getOutputStream());
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
                logger.error("exception: " + e);
            }
        }
    }
}
