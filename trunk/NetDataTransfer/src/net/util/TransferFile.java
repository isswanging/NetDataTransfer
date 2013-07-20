package net.util;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.conf.SystemConf;
import net.ui.NoticeGui;
import net.vo.DataPacket;

public class TransferFile implements Runnable {
	JProgressBar bar = new JProgressBar(JProgressBar.CENTER);
	JFrame frame = new JFrame("发送进度");

	Socket socket = null;
	DataPacket dp = null;
	String savePath = "";
	String fileName = "";

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
					e.printStackTrace();
				}
			}

		});
	}

	@Override
	public void run() {
		bar.setStringPainted(true);// 设置在进度条中绘制完成百分比

		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String hostName = addr.getHostName();// 获取主机名
			String ip = SystemConf.hostIP;// 获取ip地址

			// 发送TCP消息建立文件传输连接
			socket = new Socket(dp.getIp(), SystemConf.filePort);
			ObjectOutputStream toServer = new ObjectOutputStream(
					socket.getOutputStream());
			toServer.writeObject(new DataPacket(ip, hostName, dp.getContent(),
					SystemConf.fileConf));

			// 设置文件大小
			DataInputStream in = new DataInputStream(socket.getInputStream());
			long total = in.readLong();

			// 接收文件
			BufferedInputStream bis = new BufferedInputStream(
					socket.getInputStream());
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(new File(savePath)));

			// 设置进度条和读文件
			bar.setMinimum(0);
			bar.setMaximum(100);
			int len = 0;
			long byteRead = 0;

			byte[] bytes = new byte[1024];
			System.out.println("Server begin to reaceive!");
			while ((len = bis.read(bytes)) != -1) {
				bos.write(bytes, 0, len);
				bos.flush();
				// System.out.println("Server writing!");
				byteRead += len;
				bar.setValue((int) (byteRead * 100 / total));
			}

			System.out.println("get whole file");
			frame.dispose();
			NoticeGui.messageNotice(new JPanel(), "文件：" + fileName
					+ "\n       接收完毕");

			in.close();
			toServer.close();
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
