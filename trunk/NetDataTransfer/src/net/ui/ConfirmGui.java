package net.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.conf.SystemConf;
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
					String[] s =  dp.getContent().split("\\\\");
					// 保存文件路径
					SystemConf.savePath = path + "\\" + s[s.length-1];
					System.out.println(SystemConf.savePath);

					InetAddress addr;
					try {
						addr = InetAddress.getLocalHost();
						String hostName = addr.getHostName();// 获取主机名
						String ip = addr.getHostAddress();// 获取ip地址

						// 发送TCP消息建立文件传输连接
						Socket socket = new Socket(dp.getIp(),
								SystemConf.filePort);
						ObjectOutputStream toServer = new ObjectOutputStream(
								socket.getOutputStream());
						toServer.writeObject(new DataPacket(ip, hostName, dp
								.getContent(), SystemConf.fileConf));

						// 接收文件
						BufferedOutputStream bos = new BufferedOutputStream(
								new FileOutputStream(new File(
										SystemConf.savePath)));
						BufferedInputStream bis = new BufferedInputStream(
								socket.getInputStream());

						int len = 0;
						byte[] bytes = new byte[1024];
						System.out.println("Server begin to reaceive!");
						while ((len = bis.read(bytes)) != -1) {
							bos.write(bytes, 0, len);
							System.out.println("Server writing!");
						}
						bos.close();
						bis.close();
						socket.close();

					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
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
