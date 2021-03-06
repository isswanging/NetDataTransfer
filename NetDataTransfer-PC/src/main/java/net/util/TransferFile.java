package net.util;

import net.conf.SystemConf;
import net.ui.NoticeGui;
import net.vo.DataPacket;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TransferFile implements Runnable {
    JProgressBar bar = new JProgressBar(JProgressBar.CENTER);
    JFrame frame = new JFrame("发送进度");

    Socket socket = null;
    DataPacket dp = null;
    String savePath = "";
    String fileName = "";

    private final Log logger = LogFactory.getLog(this.getClass());

    public TransferFile(String path, String name, DataPacket data) {
        this.savePath = path;
        this.dp = data;
        this.fileName = name;

        // 设置界面
        JPanel p = new JPanel();
        p.add(bar);
        frame.setLayout(new BorderLayout());
        frame.add(p, BorderLayout.CENTER);
        frame.setLocation(200, 100);
        frame.setSize(180, 70);
        frame.setResizable(false);
        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent arg0) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.error("exception: " + e);
                }
            }

        });
    }

    @Override
    public void run() {
        bar.setStringPainted(true);// 设置在进度条中绘制完成百分比
        InetAddress address;

        DataOutputStream toServer = null;
        DataInputStream in = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            address = InetAddress.getLocalHost();
            String hostName = address.getHostName();// 获取主机名
            String ip = SystemConf.hostIP;// 获取ip地址

            // 发送TCP消息建立文件传输连接
            socket = new Socket(dp.getIp(), SystemConf.filePort);
            toServer = new DataOutputStream(socket.getOutputStream());
            toServer.writeUTF(JSON.toJSONString(new DataPacket(ip, hostName, dp
                    .getContent(), SystemConf.fileConf)));

            // 设置文件大小
            in = new DataInputStream(socket.getInputStream());
            long total = in.readLong();

            // 接收文件
            bis = new BufferedInputStream(socket.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(new File(
                    savePath)));

            // 设置进度条和读文件
            bar.setMinimum(0);
            bar.setMaximum(100);
            int len;
            long byteRead = 0;

            byte[] bytes = new byte[1024];
            while ((len = bis.read(bytes)) != -1) {
                bos.write(bytes, 0, len);
                bos.flush();
                byteRead += len;
                bar.setValue((int) (byteRead * 100 / total));
            }

            frame.dispose();
            NoticeGui.messageNotice(new JPanel(), "   文件：" + fileName
                    + "\n       接收完毕");

        } catch (UnknownHostException e) {
            logger.error("exception: " + e);
        } catch (IOException e) {
            logger.error("exception: " + e);
        } finally {
            try {
                in.close();
                toServer.close();
                bos.close();
                bis.close();
                socket.close();
            } catch (IOException e) {
                logger.error("exception: " + e);
            }

        }
    }
}
