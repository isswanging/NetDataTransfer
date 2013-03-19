package net.listen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

import net.conf.SystemConf;
import net.util.NetDomain;
import net.vo.Host;

public class GetBroadcastPacket implements Runnable {

	@SuppressWarnings("resource")
	@Override
	public void run() {

		try {
			DatagramPacket broadPacket = new DatagramPacket(new byte[512], 512);
			DatagramSocket broadSocket = new DatagramSocket(
					SystemConf.broadcastPort);
			while (true) {
				// 收到广播
				broadSocket.receive(broadPacket);
				// 整理信息
				byte[] buf = new byte[broadPacket.getLength()];
				System.arraycopy(broadPacket.getData(), 0, buf, 0, buf.length);
				ByteArrayInputStream byteArrayStram = new ByteArrayInputStream(
						buf);
				ObjectInputStream objectStream = new ObjectInputStream(
						byteArrayStram);
				Host host = (Host) objectStream.readObject();
				host.setState(0);
				
				objectStream.close();
				byteArrayStram.close();

				if (!host.getIp().equals(
						InetAddress.getLocalHost().getHostAddress())) {
					if (!NetDomain.containHost(host)) {
						host.setState(1);
						NetDomain.addHost(host);
						System.out.println(SystemConf.hostList.size());

						// 回应广播, 发送本机信息去目标地址
						if (host.getTag() == 0) {
							InetAddress addr = InetAddress.getLocalHost();
							String hostName = addr.getHostName();// 获取主机名
							String ip = addr.getHostAddress();// 获取ip地址

							Map<String, String> map = System.getenv();
							String userName = map.get("USERNAME");// 获取用户名
							String userDomain = map.get("USERDOMAIN");// 获取计算机域

							// 广播主机信息
							Host res = new Host(userName, userDomain, ip,
									hostName, 1, 1);

							NetDomain.broadcast(res, host.getIp());
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
