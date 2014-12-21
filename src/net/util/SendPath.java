package net.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.conf.SystemConf;
import net.vo.DataPacket;

public class SendPath {
    Socket socket = null;
    private final Log logger = LogFactory.getLog(this.getClass());

    public SendPath(DataPacket dp) {
        sendpath(dp);
    }

    private void sendpath(DataPacket dp) {
        try {
            socket = new Socket(dp.getIp(), SystemConf.pathPort);
            String path = SystemConf.sendPathList.get(dp.getContent());
            String content = dp.getContent() + "!" + path;
            logger.info("路径即将发送：" + content);

            DataInputStream fromeServer = new DataInputStream(
                    socket.getInputStream());
            BufferedOutputStream toServer = new BufferedOutputStream(
                    socket.getOutputStream());
            ByteArrayInputStream byteIn = new ByteArrayInputStream(
                    content.getBytes());

            byte[] bytes = new byte[1024];
            int len;
            while ((len = byteIn.read(bytes)) != -1) {
                toServer.write(bytes, 0, len);
                toServer.flush();
            }
            toServer.flush();

            logger.info("路径发送完成");
            String id = fromeServer.readUTF();
            fromeServer.close();
            logger.info("文件夹即将发送，任务id：" + id);

            toServer.close();
            fromeServer.close();
            byteIn.close();
            socket.close();

            new SendFolder(id, dp.getIp());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
