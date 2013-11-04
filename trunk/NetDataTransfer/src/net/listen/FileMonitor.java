package net.listen;

import net.conf.SystemConf;
import net.vo.DataPacket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class FileMonitor implements Runnable {
    ServerSocket server = null;

    @Override
    public void run() {
        try {
            server = new ServerSocket(SystemConf.filePort);

            while (true) {
                Socket socket = server.accept();
                new Thread(new SendFile(socket)).start();

            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class SendFile implements Runnable {
        Socket socket = null;
        ObjectInputStream geter = null;

        public SendFile(Socket s) {
            socket = s;
        }

        @Override
        public void run() {
            try {
                geter = new ObjectInputStream(socket.getInputStream());
                DataPacket dp = (DataPacket) geter.readObject();

                if (dp.getTag() == SystemConf.fileConf) {
                    BufferedInputStream bis = new BufferedInputStream(
                            new FileInputStream(new File(
                                    dp.getContent())));

                    // 文件大小
                    long total = bis.available();
                    DataOutputStream o = new DataOutputStream(
                            socket.getOutputStream());
                    o.writeLong(total);

                    // 发文件
                    BufferedOutputStream bos = new BufferedOutputStream(
                            socket.getOutputStream());
                    int len;
                    byte[] bytes = new byte[1024];
                    while ((len = bis.read(bytes)) != -1) {
                        bos.write(bytes, 0, len);
                        bos.flush();
                    }
                    bis.close();
                    bos.close();
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
