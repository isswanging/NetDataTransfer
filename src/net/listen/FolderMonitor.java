package net.listen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
					String taskId = in.readUTF();

					// 开始一个个的发送文件
					ExecutorService executorService = Executors
							.newCachedThreadPool();
					for (int i = 0; i < filesPath.length; i++) {
						executorService.execute(sendFile(taskId,filesPath, i,socket));
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public Runnable sendFile(String taskId, String[] filesPath, int i,
			Socket socket) {
		return new Runnable(){

			@Override
			public void run() {
				
			}
			
		};
	}

}
