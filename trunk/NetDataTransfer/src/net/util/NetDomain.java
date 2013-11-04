package net.util;

import net.conf.SystemConf;
import net.vo.Host;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

public class NetDomain {

    // 检查端口
    public static String check() {
        try {
            new DatagramSocket(SystemConf.textPort).close();
            new ServerSocket(SystemConf.filePort).close();
            new ServerSocket(SystemConf.folderPort).close();

            return SystemConf.SUCCESS;
        } catch (SocketException e) {
            return SystemConf.ERROR;
        } catch (IOException e) {
            return SystemConf.FAIL;
        }

    }

    // 添加主机
    public static synchronized boolean addHost(Host host) {
        for (int i = 0; i < SystemConf.hostList.size(); i++) {
            if (SystemConf.hostList.get(i).getIp().equals(host.getIp())) {
                SystemConf.hostList.set(i, host);
                return false;
            }
        }
        SystemConf.hostList.add(host);
        return true;
    }

    // 检查主机是否重复
    public static synchronized boolean containHost(Host host) {
        for (int i = 0; i < SystemConf.hostList.size(); i++) {
            Host h = SystemConf.hostList.get(i);
            if ((h.getIp().equals(host.getIp()))
                    && (h.getState() == host.getState())) {
                return true;
            }
        }
        return false;
    }

    // 发送UDP消息
    public static void sendUdpData(DatagramSocket broadSocket, Object obj,
                                   String targetIp, int port) {
        ByteArrayOutputStream byteArrayStream = null;
        ObjectOutputStream objectStream = null;
        try {
            byteArrayStream = new ByteArrayOutputStream();
            objectStream = new ObjectOutputStream(byteArrayStream);
            objectStream.writeObject(obj);
            byte[] info = byteArrayStream.toByteArray();

            DatagramPacket broadPacket = new DatagramPacket(info, info.length,
                    InetAddress.getByName(targetIp), port);

            broadSocket.send(broadPacket);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (objectStream != null)
                    objectStream.close();
                if (byteArrayStream != null)
                    byteArrayStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

}
