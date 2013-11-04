package net.util;

import net.conf.SystemConf;
import net.vo.DataPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SendFolder {
	private final Log logger = LogFactory.getLog(this.getClass());

	public SendFolder(DataPacket dp) {
		ExecutorService executorService = Executors.newCachedThreadPool();
		String[] filesPath = dp.getContent().split("\\*");

		for (int i = 0; i < filesPath.length; i++) {
			executorService.execute(sendFile(dp.getIp(), filesPath[i], i,
					dp.getSenderName()));
		}
	}

	private Runnable sendFile(final String Ip, final String filePath,
			final int i, final String taskId) {
		return new Runnable() {
			private Socket socket = null;
			private String ip = Ip;
			private int port = SystemConf.folderPort;

			@Override
			public void run() {
				try {
					// 建立TCP连接
					if (connect()) {
						BufferedInputStream bis = new BufferedInputStream(
								new FileInputStream(new File(filePath)));
						DataOutputStream dos = new DataOutputStream(
								socket.getOutputStream());

						// 发送任务id和文件路径
						dos.writeUTF(taskId);
						dos.flush();
						dos.writeInt(i);
						dos.flush();

						// 发文件
						int len;
						byte[] bytes = new byte[1024];
						while ((len = bis.read(bytes)) != -1) {
							dos.write(bytes, 0, len);
							dos.flush();
						}
						dos.flush();
						bis.close();
						dos.close();
						socket.close();
						logger.info(filePath + " 发送完成");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			protected boolean connect() {
				try {
					socket = new Socket(ip, port);
					logger.info("连接服务器成功！");
					return true;
				} catch (Exception e) {
					logger.error("连接服务器失败！");
					return false;
				}
			}

		};

	}

}
