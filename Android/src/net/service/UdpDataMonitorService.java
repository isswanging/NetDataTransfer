package net.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.log.Logger;
import net.util.BadgeUtil;
import net.util.TransferFile;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import net.vo.Progress;
import net.vo.SendTask;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;

public class UdpDataMonitorService extends Service {
    DatagramSocket UdpSocket = null;
    DatagramPacket UdpPacket = null;
    DataPacket dp = null;
    Thread thread;
    boolean tag;
    NetConfApplication app;
    Vibrator vibrator;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        app = (NetConfApplication) getApplication();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

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

    private void dispatchMessage(String info) {
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
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            Bundle bundle = new Bundle();
            bundle.putString("name", dp.getSenderName());
            bundle.putString("ip", dp.getIp());
            notifyIntent.putExtras(bundle);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    UdpDataMonitorService.this, R.string.app_name,
                    notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            ChatMsgEntity entity = new ChatMsgEntity(dp.getSenderName(),
                    app.getDate(), JSON.parseObject(info, DataPacket.class)
                            .getContent(), true);
            if (app.chatTempMap.containsKey(dp.getIp())) {
                app.chatTempMap.get(dp.getIp()).add(entity);
            } else {
                ArrayList<ChatMsgEntity> list = new ArrayList<ChatMsgEntity>();
                list.add(entity);
                app.chatTempMap.put(dp.getIp(), list);
            }

            // 显示
            Notification notification = new NotificationCompat.Builder(
                    UdpDataMonitorService.this).setSmallIcon(R.drawable.notify)
                    .setTicker("新消息").setContentTitle("点击查看")
                    .setContentText(dp.getSenderName() + "发来一条新消息")
                    .setContentIntent(contentIntent).build();
            notification.flags = Notification.FLAG_AUTO_CANCEL
                    | Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = 0x00FF00;
            notification.ledOffMS = 100;
            notification.ledOnMS = 100;
            app.nManager.notify(R.id.chatName, notification);

            // 快捷方式显示数字(部分品牌有效)
            BadgeUtil.setBadgeCount(getApplicationContext(),
                    app.getUnreadMsgNum());

            // 让界面显示未读消息的红点
            Intent unReadIntent = new Intent("net.ui.newMsg");
            sendBroadcast(unReadIntent);
        }
    }

    private class ReceiveInfo implements Runnable {
        @Override
        public void run() {
            try {
                UdpPacket = new DatagramPacket(new byte[1024], 1024);
                UdpSocket = new DatagramSocket(NetConfApplication.textPort);
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
                        dispatchMessage(info);
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
                        // 振动提示
                        vibrator.vibrate(700);

                        String[] s = dp.getContent().replaceAll("\\\\", "/")
                                .split("/");
                        String fileName = s[s.length - 1];
                        int id = NetConfApplication.taskId++;
                        NetConfApplication.getTaskList.put(id, new Progress(
                                fileName, 0));

                        // accept file
                        new TransferFile(UdpDataMonitorService.this)
                                .executeOnExecutor(
                                        AsyncTask.THREAD_POOL_EXECUTOR,
                                        new SendTask(id, null, fileName, dp));
                        DataPacket dpClone = new DataPacket();
                        dpClone.setContent("向你发来了一个文件");
                        dpClone.setIp(dp.getIp());
                        dpClone.setSenderName(dp.getSenderName());
                        dpClone.setTag(dp.getTag());
                        dispatchMessage(JSON.toJSONString(dpClone));
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
