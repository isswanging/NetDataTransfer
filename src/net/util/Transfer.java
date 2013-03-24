package net.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JPanel;

import net.conf.SystemConf;
import net.ui.NoticeGui;
import net.vo.DataPacket;

public class Transfer implements Runnable {
	Socket socket = null;
	DataPacket dp = null;
	String savePath = "";
	String fileName = "";

	public Transfer(String path, String name, DataPacket data) {
		this.savePath = path;
		this.dp = data;
		this.fileName = name;
	}

	@Override
	public void run() {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			String hostName = addr.getHostName();// 获取主机名
			String ip = OSUtil.getLocalIP();// 获取ip地址

			// 发送TCP消息建立文件传输连接
			socket = new Socket(dp.getIp(), SystemConf.filePort);
			ObjectOutputStream toServer = new ObjectOutputStream(
					socket.getOutputStream());
			toServer.writeObject(new DataPacket(ip, hostName, dp.getContent(),
					SystemConf.fileConf));

			// 接收文件
			BufferedOutputStream bos = new BufferedOutputStream(
					new FileOutputStream(new File(savePath)));
			BufferedInputStream bis = new BufferedInputStream(
					socket.getInputStream());

			int len = 0;
			byte[] bytes = new byte[1024];
			System.out.println("Server begin to reaceive!");
			while ((len = bis.read(bytes)) != -1) {
				bos.write(bytes, 0, len);
				// System.out.println("Server writing!");
			}

			System.out.println("get whole file");
			NoticeGui.messageNotice(new JPanel(), "文件：" + fileName
					+ "\n       接收完毕");

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
