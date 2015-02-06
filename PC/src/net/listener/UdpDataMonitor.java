package net.listener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.swing.JPanel;

import net.conf.SystemConf;
import net.ui.ChatGui;
import net.ui.ConfirmGui;
import net.ui.NoticeGui;
import net.util.SendPath;
import net.vo.DataPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;

public class UdpDataMonitor implements Runnable {
    DatagramSocket UdpSocket = null;
    DatagramPacket UdpPacket = null;
    DataPacket dp = null;

    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void run() {
        try {

            UdpPacket = new DatagramPacket(new byte[1024], 1024);
            UdpSocket = new DatagramSocket(SystemConf.textPort);
            while (true) {
                // 收到消息
                UdpSocket.receive(UdpPacket);

                // 解析处理并显示
                String info = new String(UdpPacket.getData(), 0,
                        UdpPacket.getLength());
                dp = JSON.parseObject(info, DataPacket.class);

                switch (dp.getTag()) {
                case SystemConf.text:
                    new ChatGui(dp, UdpSocket);
                    break;

                case SystemConf.folderPre:
                case SystemConf.filePre:
                    new ConfirmGui(dp);
                    break;

                case SystemConf.refuse:
                    NoticeGui.messageNotice(new JPanel(), "传输被" + dp.getIp()
                            + "拒绝");
                    break;

                case SystemConf.folderConf:
                    new Thread(new SendPath(dp)).start();
                    break;

                case SystemConf.end:
                    SystemConf.sendPathList.remove(dp.getContent());
                    break;

                default:
                    break;
                }

            }
        } catch (IOException e) {
            logger.error("exception: " + e);
        }
    }

}
