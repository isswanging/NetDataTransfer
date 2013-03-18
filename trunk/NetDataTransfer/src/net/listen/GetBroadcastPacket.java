package net.listen;

import java.io.IOException;
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
				String loginMsg = new String(buf);

				Host host = NetDomain.getHost(loginMsg);

				// 回应广播
				if (!NetDomain.containHost(host)) {
					// 发送本机信息去目标地址
					host.setState(1);
					NetDomain.addHost(host);

					InetAddress addr = InetAddress.getLocalHost();
					String hostName = addr.getHostName();// 获取主机名
					String ip = addr.getHostAddress();// 获取ip地址

					Map<String, String> map = System.getenv();
					String userName = map.get("USERNAME");// 获取用户名
					String userDomain = map.get("USERDOMAIN");// 获取计算机域

					// 广播主机信息
					String message = userName + "@" + userDomain + "@"
							+ hostName + "@" + ip ;
					byte[] info = message.getBytes();

					DatagramSocket respondSocket = new DatagramSocket();
					DatagramPacket respondPacket = new DatagramPacket(info,
							info.length, InetAddress.getByName(host.getIp()),
							SystemConf.broadcastPort);

					respondSocket.send(respondPacket);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
