package net.ui;

import java.awt.BorderLayout;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.conf.SystemConf;
import net.util.NetDomain;
import net.vo.DataPacket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProgressBar {
    JProgressBar bar = new JProgressBar(JProgressBar.CENTER);
    JFrame frame = new JFrame("发送进度");
    String taskId;
    String targetIp;
    Long total;
    Long byteRead;
    int size;
    private final Log logger = LogFactory.getLog(this.getClass());

    public ScheduledThreadPoolExecutor se = new ScheduledThreadPoolExecutor(1);
    ScheduledFuture<?> sf;

    public ProgressBar(String timeId, long total, String ip) {
        // 设置界面
        JPanel p = new JPanel();
        p.add(bar);
        frame.setLayout(new BorderLayout());
        frame.add(p, BorderLayout.CENTER);
        frame.setLocation(200, 100);
        frame.setSize(180, 70);
        frame.setResizable(false);
        frame.setVisible(true);
        bar.setStringPainted(true);
        bar.setMinimum(0);
        bar.setMaximum(100);
        taskId = timeId;
        this.total = total;
        this.targetIp = ip;
        // 定时任务，绘制进度条
        sf = se.scheduleAtFixedRate(new FixedSchedule(), 0, 10,
                TimeUnit.MILLISECONDS);
    }

    class FixedSchedule implements Runnable {
        public void run() {
            byteRead = SystemConf.progress.get(taskId);
            logger.info("record progress::" + byteRead + " all::" + total);
            int num = (int) (100 - byteRead * 100 / total);
            bar.setValue(num);

            if (num == 100) {
                logger.info("get all...");
                sf.cancel(true);

                // 清理工作
                SystemConf.savePathList.remove(taskId);
                SystemConf.taskList.remove(taskId);
                try {
                    NetDomain.sendUdpData(new DatagramSocket(), new DataPacket("",
                            "", taskId, SystemConf.end), targetIp,
                            SystemConf.textPort);
                } catch (SocketException e) {
                    logger.error("exception: " + e);
                }
                frame.dispose();
                NoticeGui.messageNotice(new JPanel(), "文件夹接收完毕");
            }
        }
    }
}