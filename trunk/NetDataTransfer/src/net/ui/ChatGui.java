package net.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import net.conf.SystemConf;
import net.util.NetDomain;
import net.util.OSUtil;
import net.vo.DataPacket;

public class ChatGui {
	DataPacket dp = null;
	DatagramSocket udpSocket = null;
	JFrame fr;

	public ChatGui(DataPacket dp1, DatagramSocket udp) {
		this.dp = dp1;
		this.udpSocket = udp;

		// 建立界面
		fr = new JFrame("消息");
		JLabel jl = new JLabel("主机" + dp.getSenderName() + " , " + dp.getIp()
				+ "发来消息");
		fr.setSize(350, 300);
		int wide = fr.getWidth();
		int high = fr.getHeight();
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int screenWidth = screenSize.width;
		int screenHeight = screenSize.height;
		fr.setLocation(screenWidth / 2 - wide / 2, screenHeight / 2 - high / 2);
		fr.setLayout(new BorderLayout());

		JTextArea input = new JTextArea(5, 2);
		input.setText((String) dp.getContent());
		final JTextArea output = new JTextArea(5, 2);

		JScrollPane js1 = new JScrollPane(input,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JScrollPane js2 = new JScrollPane(output,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		input.setEditable(false);
		JPanel jp = new JPanel();
		JPanel jp1 = new JPanel();
		jp.setLayout(new BorderLayout());
		JButton close = new JButton("关闭");
		JButton answer = new JButton("回复");
		jp1.add(answer);
		jp1.add(close);
		jp.add(jp1, BorderLayout.SOUTH);
		jp.add(js2, BorderLayout.CENTER);
		fr.add(jl, BorderLayout.NORTH);
		fr.add(js1, BorderLayout.CENTER);
		fr.add(jp, BorderLayout.SOUTH);
		fr.setResizable(false);
		fr.setVisible(true);
		fr.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				fr.dispose();
			}
		});

		answer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (output.getText().equals("")) {
					NoticeGui.warnNotice(fr, "请输入消息");
				} else {
					try {
						InetAddress addr = InetAddress.getLocalHost();
						String hostName = addr.getHostName();// 获取主机名
						String ip = OSUtil.getLocalIP();// 获取ip地址
						String message = output.getText();
						NetDomain.sendUdpData(udpSocket, new DataPacket(ip,
								hostName, message, SystemConf.text),
								dp.getIp(), SystemConf.textPort);
						fr.dispose();
					} catch (UnknownHostException e1) {
						e1.printStackTrace();
					}
				}

			}
		});
	}

}
