package net.util;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.conf.SystemConf;
import net.ui.NoticeGui;
import net.vo.DataPacket;

public class TransferFolder implements Runnable {
	JProgressBar bar = new JProgressBar(JProgressBar.CENTER);
	JFrame frame = new JFrame("发送进度");
	ArrayList<String> files = new ArrayList<String>();
	String name = "";

	Socket socket = null;
	DataPacket dp = null;

	public TransferFolder(String name, ArrayList<String> files, DataPacket dp) {
		this.dp = dp;
		this.files = files;
		this.name = name;

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

			socket = new Socket(dp.getIp(), SystemConf.folderPort);

			// 把要传输的文件的路径发过去
			ObjectOutputStream toServer = new ObjectOutputStream(
					socket.getOutputStream());
			toServer.writeObject(new DataPacket(ip, hostName, dp.getContent(),
					SystemConf.folderConf));

			// 收到文件大小
			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());
			long total = in.readLong();

			// 接收文件
			BufferedInputStream bis = new BufferedInputStream(
					socket.getInputStream());
			BufferedOutputStream bos = null;
			byte[] bytes = new byte[1024];

			// 设置进度条和读文件
			bar.setMinimum(0);
			bar.setMaximum(100);
			int len = 0;
			long byteRead = 0;

			for (int i = 0; i < files.size(); i++) {
				out.writeInt(i);
				out.flush();

				bos = new BufferedOutputStream(new FileOutputStream(new File(
						files.get(i))));
				while ((len = bis.read(bytes)) != -1) {
					bos.write(bytes, 0, len);
					bos.flush();
					byteRead += len;
					bar.setValue((int) (byteRead * 100 / total));
				}
				
				System.out.println("get one file-----------------");
			}

			System.out.println("get whole file");
			frame.dispose();
			NoticeGui.messageNotice(new JPanel(), "文件夹：" + name
					+ "\n       接收完毕");

			in.close();
			toServer.close();
			bos.close();
			bis.close();
			out.close();
			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
