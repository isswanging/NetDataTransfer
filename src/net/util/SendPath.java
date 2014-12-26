package net.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import net.conf.SystemConf;
import net.vo.DataPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
            String content = dp.getContent() + ">" + path;
            logger.info("路径即将发送：" + content);

            DataInputStream fromeServer = new DataInputStream(
                    socket.getInputStream());
            DataOutputStream toServer = new DataOutputStream(
                    socket.getOutputStream());

            int len = content.length();
            // 循环次数
            int loop;
            // 最后一次发送的长度
            int last = len % SystemConf.buffer;

            if (last == 0) {
                loop = len / SystemConf.buffer;
            } else {
                loop = len / SystemConf.buffer + 1;
            }
            logger.info("loop:" + loop + "  last:" + last);

            // 循环发送
            toServer.writeInt(loop);
            for (int i = 1; i <= loop; i++) {
                if (i == loop) {
                    toServer.writeUTF(content
                            .substring(len - last, len));
                } else {
                    toServer.writeUTF(content.substring((i - 1)
                            * SystemConf.buffer, i * SystemConf.buffer));
                }
            }

            logger.info("路径发送完成");
            String id = fromeServer.readUTF();
            fromeServer.close();
            logger.info("文件夹即将发送，任务id：" + id);

            toServer.close();
            fromeServer.close();
            socket.close();

            new SendFolder(id, dp.getIp());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
