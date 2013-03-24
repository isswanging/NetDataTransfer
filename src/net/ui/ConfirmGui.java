package net.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramSocket;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.util.Transfer;
import net.vo.DataPacket;

public class ConfirmGui {
	JFrame fr;
	DatagramSocket udpSocket = null;
	DataPacket dp = null;

	public ConfirmGui(DataPacket dp2, DatagramSocket udp) {
		this.dp = dp2;
		this.udpSocket = udp;

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

		y.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser jFileChooser = new JFileChooser();
				jFileChooser.setMultiSelectionEnabled(true);
				jFileChooser
						.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				if (jFileChooser.showOpenDialog(jFileChooser) == JFileChooser.APPROVE_OPTION) {
					fr.dispose();

					String path = jFileChooser.getSelectedFile().getPath();
					System.out.println(dp.getContent());
					// 获取文件名
					String[] s = dp.getContent().replaceAll("\\\\", "/")
							.split("/");
					// 文件分隔符
					String fs = System.getProperties().getProperty(
							"file.separator");
					// 保存文件路径
					String savePath = path + fs + s[s.length - 1];
					System.out.println(savePath);

					new Thread(new Transfer(savePath, s[s.length - 1], dp))
							.start();

				}
			}
		});

		n.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fr.dispose();
			}
		});
	}
}
