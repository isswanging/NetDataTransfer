package net.ui;

import java.awt.BorderLayout;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.conf.SystemConf;

public class ProgressBar {
	JProgressBar bar = new JProgressBar(JProgressBar.CENTER);
	JFrame frame = new JFrame("发送进度");
	String taskId;
	Long total;
	public ScheduledThreadPoolExecutor se = new ScheduledThreadPoolExecutor(1);
	ScheduledFuture<?> sf;

	public ProgressBar(String timeId) {
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
		total = SystemConf.progress.get(taskId);
		// 定时任务，绘制进度条
		sf = se.scheduleAtFixedRate(new FixedSchedule(), 0, 10,
				TimeUnit.MILLISECONDS);
	}

	class FixedSchedule implements Runnable {
		public void run() {
			Long byteRead = SystemConf.progress.get(taskId);
			bar.setValue((int) (100 - byteRead * 100 / total));

			if (byteRead == 0) {
				sf.cancel(true);
			}
		}
	}
}
