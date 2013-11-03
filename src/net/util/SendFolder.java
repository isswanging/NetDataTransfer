package net.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.vo.DataPacket;

public class SendFolder {

	public SendFolder(DataPacket dp) {
		ExecutorService executorService = Executors.newCachedThreadPool();
		String[] filesPath = dp.getContent().split("\\*");

		for (int i = 0; i < filesPath.length; i++) {
			executorService.execute(sendFile(filesPath[i], i,
					dp.getSenderName()));
		}
	}

	private Runnable sendFile(String string, int i, String taskId) {
		return new Runnable() {

			@Override
			public void run() {
				
			}
			
		};
	}

}
