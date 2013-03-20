package net.listen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import javax.swing.JFrame;
import javax.swing.JLabel;

import net.conf.SystemConf;
import net.vo.DataPacket;

public class TextMonitor implements Runnable {
	DatagramSocket UdpSocket = null;
	DatagramPacket UdpPacket = null;
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
				DataPacket dp = (DataPacket) objectStream.readObject();

				JFrame fr = new JFrame("消息");
				JLabel jl = new JLabel("get" + dp.getSenderName() + "~~"
						+ dp.getIp() + "~~" + dp.getContent());
				fr.setSize(300, 200);
				fr.add(jl);
				fr.setVisible(true);
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
