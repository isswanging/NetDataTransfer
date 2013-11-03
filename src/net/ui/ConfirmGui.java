package net.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.conf.SystemConf;
import net.util.BuildFolder;
import net.util.NetDomain;
import net.util.OSUtil;
import net.util.TransferFile;
import net.vo.DataPacket;

public class ConfirmGui {
	JFrame fr;
	DatagramSocket udpSocket = null;
	DataPacket dp = null;

	public ConfirmGui(DataPacket dp2) {
		this.dp = dp2;

		// 构造用户界面
		fr = new JFrame("消息");
		JButton y = new JButton("接收");
		JButton n = new JButton("拒绝");
		JLabel label = new JLabel(dp.getIp() + "发来文件，是否接受",
				SwingConstants.CENTER);
		JPanel jp = new JPanel();

		jp.add(y);
		jp.add(n);
		fr.setLayout(new BorderLayout());
		fr.add(label, BorderLayout.CENTER);
		fr.add(jp, BorderLayout.SOUTH);
		fr.setSize(300, 180);
		fr.setLocation(400, 300);
		fr.setResizable(false);
		fr.setVisible(true);

		// 接收文件的操作
		if (dp.getTag() == SystemConf.filePre) {
			y.addActionListener(new FileAction());
		}
		// 接收文件夹的操作
		if (dp.getTag() == SystemConf.folderPre) {
			y.addActionListener(new FolderAction());
		}

		n.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					NetDomain.sendUdpData(new DatagramSocket(), new DataPacket(
							SystemConf.hostIP, null, null, SystemConf.refuse),
							dp.getIp(), SystemConf.textPort);
				} catch (SocketException e1) {
					e1.printStackTrace();
				}
				fr.dispose();
			}
		});
	}

	private class FileAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			JFileChooser jFileChooser = new JFileChooser();
			jFileChooser.setMultiSelectionEnabled(true);
			jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (jFileChooser.showOpenDialog(jFileChooser) == JFileChooser.APPROVE_OPTION) {
				fr.dispose();

				String path = jFileChooser.getSelectedFile().getPath();
				// 获取文件名
				String[] s = dp.getContent().replaceAll("\\\\", "/").split("/");
				// 文件分隔符
				String fs = System.getProperties()
						.getProperty("file.separator");
				// 保存文件路径
				String savePath = path + fs + s[s.length - 1];
				System.out.println(savePath);

				new Thread(new TransferFile(savePath, s[s.length - 1], dp))
						.start();
			}
		}
	}

	private class FolderAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser jFileChooser = new JFileChooser();
			jFileChooser.setMultiSelectionEnabled(true);
			jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			if (jFileChooser.showOpenDialog(jFileChooser) == JFileChooser.APPROVE_OPTION) {
				fr.dispose();

				String path = jFileChooser.getSelectedFile().getPath();
				String content = dp.getContent();

				// 获取当前时间作为任务id
				String timeId = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.format(new Date());
				// 建立本地存放的目录
				BuildFolder bf = new BuildFolder(path, content);
				// 存放
				SystemConf.taskList.put(timeId, bf.getFiles());
				SystemConf.progress.put(timeId,
						Long.valueOf(dp.getSenderName()));

				if (bf.getFiles().size() == 0) {
					// 如果是空文件夹，传输就就结束了
					NoticeGui.messageNotice(new JPanel(), "传送完毕");
				} else {
					// 把需要传输的文件的路径发过去即可
					String[] paths = content.split("\\|");
					dp.setContent(paths[paths.length - 1]);
					dp.setTag(SystemConf.folderConf);
					dp.setSenderName(timeId);
					String targetIp = dp.getIp();
					dp.setIp(OSUtil.getLocalIP());
					try {
						NetDomain.sendUdpData(new DatagramSocket(), dp,
								targetIp, SystemConf.textPort);
					} catch (SocketException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}
}
