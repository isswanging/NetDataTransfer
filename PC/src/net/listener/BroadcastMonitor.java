package net.listener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alibaba.fastjson.JSON;

import net.conf.SystemConf;
import net.util.NetDomain;
import net.vo.Host;

public class BroadcastMonitor implements Runnable {
    DatagramSocket broadSocket = null;
    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void run() {

        try {
            DatagramPacket broadPacket = new DatagramPacket(new byte[512], 512);
            broadSocket = new DatagramSocket(SystemConf.broadcastPort);
            while (true) {
                // 收到广播
                broadSocket.receive(broadPacket);

                // 整理信息
                String info = new String(broadPacket.getData(), 0,
                        broadPacket.getLength());
                Host host = JSON.parseObject(info, Host.class);
                host.setState(0);

                if (!NetDomain.containHost(host)) {
                    host.setState(1);
                    NetDomain.addHost(host);
                    logger.info("hostList size: " + SystemConf.hostList.size());

                    // 回应广播, 发送本机信息去目标地址
                    if (host.getTag() == 0) {
                        InetAddress addr = InetAddress.getLocalHost();
                        String hostName = addr.getHostName();// 获取主机名
                        String ip = SystemConf.hostIP;// 获取ip地址

                        Map<String, String> map = System.getenv();
                        String userName = map.get("USERNAME");// 获取用户名
                        String userDomain = map.get("USERDOMAIN");// 获取计算机域

                        // 广播主机信息
                        Host res = new Host(userName, userDomain, ip, hostName,
                                1, 1);
                        NetDomain.sendUdpData(broadSocket,
                                JSON.toJSONString(res), host.getIp(),
                                SystemConf.broadcastPort);
                        logger.info("send a broadcast: ");
                    }
                }
            }
        } catch (IOException e) {

        }
    }
}
