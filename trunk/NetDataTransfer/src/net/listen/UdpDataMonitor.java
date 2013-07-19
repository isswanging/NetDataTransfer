package net.listen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.swing.JPanel;

import net.conf.SystemConf;
import net.ui.ChatGui;
import net.ui.ConfirmGui;
import net.ui.NoticeGui;
import net.vo.DataPacket;

public class UdpDataMonitor implements Runnable {
	DatagramSocket UdpSocket = null;
	DatagramPacket UdpPacket = null;
	DataPacket dp = null;
	ObjectInputStream objectStream = null;
	ByteArrayInputStream byteArrayStram = null;

	@Override
	public void run() {
		try {

			UdpPacket = new DatagramPacket(new byte[1024], 1024);
			UdpSocket = new DatagramSocket(SystemConf.textPort);
			while (true) {
				// 收到消息
				UdpSocket.receive(UdpPacket);

				// 解析处理并显示
				byte[] buf = new byte[UdpPacket.getLength()];
				System.arraycopy(UdpPacket.getData(), 0, buf, 0, buf.length);
				byteArrayStram = new ByteArrayInputStream(buf);
				objectStream = new ObjectInputStream(byteArrayStram);
				dp = (DataPacket) objectStream.readObject();

				if (dp.getTag() == SystemConf.text) {
					new ChatGui(dp, UdpSocket);
				}
				if (dp.getTag() == SystemConf.filePre
						|| dp.getTag() == SystemConf.folderPre) {
					new ConfirmGui(dp, UdpSocket);
				}
				if (dp.getTag() == SystemConf.refuse) {
					NoticeGui.messageNotice(new JPanel(), "传输被" + dp.getIp()
							+ "拒绝");
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				UdpSocket.close();
				objectStream.close();
				byteArrayStram.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
