package net.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

import net.app.NetConfApplication;
import net.log.Logger;
import net.util.TransferFile;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.netdatatransfer.R;

public class UdpDataMonitorService extends Service {
    DatagramSocket UdpSocket = null;
    DatagramPacket UdpPacket = null;
    DataPacket dp = null;
    Thread thread;
    boolean tag;
    NetConfApplication app;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        app = (NetConfApplication) getApplication();

        Logger.info(this.toString(), "UDPdataMonitor started");
        tag = true;
        thread = new Thread(new ReceiveInfo());
        thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        tag = false;
        thread.interrupt();
        Logger.info(this.toString(), "service stop");
        super.onDestroy();
    }

    private class ReceiveInfo implements Runnable {
        @Override
        public void run() {
            try {

                UdpPacket = new DatagramPacket(new byte[1024], 1024);
                UdpSocket = new DatagramSocket(app.textPort);
                while (tag) {
                    // 收到消息
                    UdpSocket.receive(UdpPacket);

                    // 解析处理并显示
                    String info = new String(UdpPacket.getData(), 0,
                            UdpPacket.getLength());
                    dp = JSON.parseObject(info, DataPacket.class);

                    switch (dp.getTag()) {

                    case NetConfApplication.text:

                        app.playVoice();

                        Logger.info(this.toString(), "service::" + app.chatId);
                        if (app.chatId.equals(dp.getIp())) {
                            // 发广播在交给聊天窗口处理
                            Intent intent = new Intent("net.ui.chatFrom");
                            Bundle bundle = new Bundle();
                            bundle.putString("content", info);
                            intent.putExtras(bundle);

                            // 发送广播
                            sendBroadcast(intent);

                        } else {
                            // 发送通知
                            Intent notifyIntent = new Intent("net.ui.chatting");
                            notifyIntent
                                    .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            Bundle bundle = new Bundle();
                            bundle.putString("name", dp.getSenderName());
                            bundle.putString("ip", dp.getIp());
                            notifyIntent.putExtras(bundle);
                            PendingIntent contentIntent = PendingIntent
                                    .getActivity(UdpDataMonitorService.this,
                                            R.string.app_name, notifyIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT);

                            ChatMsgEntity entity = new ChatMsgEntity(
                                    dp.getSenderName(), app.getDate(),
                                    dp.getContent(), true);
                            if (app.chatTempMap.containsKey(dp.getIp())) {
                                app.chatTempMap.get(dp.getIp()).add(entity);
                            } else {
                                ArrayList<ChatMsgEntity> list = new ArrayList<ChatMsgEntity>();
                                list.add(entity);
                                app.chatTempMap.put(dp.getIp(), list);
                            }

                            // 显示
                            Notification notification = new NotificationCompat.Builder(
                                    UdpDataMonitorService.this)
                                    .setSmallIcon(R.drawable.notify)
                                    .setTicker("新消息")
                                    .setContentTitle("点击查看")
                                    .setContentText(
                                            dp.getSenderName() + "发来一条新消息")
                                    .setContentIntent(contentIntent).build();
                            notification.flags = Notification.FLAG_AUTO_CANCEL;
                            app.nManager.notify(R.id.chatName, notification);

                            // 让界面显示未读消息的红点
                            Intent unReadIntent = new Intent("net.ui.newMsg");
                            sendBroadcast(unReadIntent);
                        }

                        break;

                    case NetConfApplication.refuse:
                        Looper.prepare();
                        Toast.makeText(UdpDataMonitorService.this,
                                dp.getIp() + "拒绝了文件传输", Toast.LENGTH_SHORT)
                                .show();
                        Looper.loop();
                        break;

                    case NetConfApplication.filePre:
                        Logger.info(this.toString(), "file send request");
                        // accept
                        new TransferFile().execute(dp);
                        break;

                    default:
                        break;
                    }

                }

            } catch (IOException e) {
                Logger.error(this.toString(), e.toString());
            }
        }
    }
}
