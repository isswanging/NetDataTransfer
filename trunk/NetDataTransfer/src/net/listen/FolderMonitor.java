package net.listen;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import net.conf.SystemConf;
import net.vo.DataPacket;

public class FolderMonitor implements Runnable {
	ServerSocket server = null;

	@Override
	public void run() {
		try {
			server = new ServerSocket(SystemConf.folderPort);

			while (true) {
				Socket socket = server.accept();
				new Thread(new SendFiles(socket)).start();

			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public class SendFiles implements Runnable {
		Socket socket = null;
		ObjectInputStream geter = null;

		public SendFiles(Socket socket) {
			this.socket = socket;

		}

		@Override
		public void run() {
			try {
				geter = new ObjectInputStream(socket.getInputStream());
				DataPacket dp = (DataPacket) geter.readObject();

				if (dp.getTag() == SystemConf.folderConf) {
					// 计算总大小
					FileInputStream fis = null;
					String[] filesPath = dp.getContent().split("\\*");
					long total = 0;

					for (int i = 0; i < filesPath.length; i++) {
						fis = new FileInputStream(filesPath[i]);
						total += fis.available();
					}
					System.out.println(total);
					DataOutputStream out = new DataOutputStream(
							socket.getOutputStream());
					DataInputStream in = new DataInputStream(
							socket.getInputStream());
					out.writeLong(total);
					out.flush();

					// 开始一个个的发送文件
					BufferedOutputStream bos = new BufferedOutputStream(
							socket.getOutputStream());
					BufferedInputStream bis = null;
					byte[] bytes = new byte[1024];
					int i = 0;
					// int tag = 0;

					// 发送
					while (true) {
						// while (true) {
						// i = in.readInt();
						// if (i == tag || tag == filesPath.length) {
						// break;
						// }
						// }
						i = in.readInt();
						if (i == filesPath.length) {
							break;
						} else {
							// 传数据
							bis = new BufferedInputStream(new FileInputStream(
									filesPath[i]));
							int len = 0;
							while ((len = bis.read(bytes)) != -1) {
								bos.write(bytes, 0, len);
								bos.flush();
							}

							// tag++;
							System.out.println("next************");

						}
					}

					bis.close();
					bos.close();

				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
}
