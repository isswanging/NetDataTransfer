package net.listen;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import net.conf.SystemConf;

public class GetBroadcastPacket implements Runnable {

	@Override
	public void run() {
		try {
			DatagramPacket inPacket;
			DatagramSocket broadSocket = new DatagramSocket(
					SystemConf.broadcastPort);

			while (true) {
				inPacket = new DatagramPacket(new byte[1024], 1024);
				broadSocket.receive(inPacket);
				inPacket.getData();
				System.out.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
