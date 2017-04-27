package net.service;

import android.app.Notification;
import android.app.PendingIntent;
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

import net.app.NetConfApplication;
import net.app.netdatatransfer.R;
import net.base.BaseService;
import net.db.DBManager;
import net.log.Logger;
import net.util.BadgeUtil;
import net.util.Commend;
import net.util.TransferFile;
import net.vo.ChatMsgEntity;
import net.vo.DataPacket;
import net.vo.EventInfo;
import net.vo.Progress;
import net.vo.SendTask;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpDataMonitorService extends BaseService {
    private final String TAG = "UdpDataMonitorService";
    DatagramSocket UdpSocket = null;
    DatagramPacket UdpPacket = null;
    DataPacket dp = null;
    NetConfApplication app;
    Vibrator vibrator;
    long[] pattern = {100, 200};

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        app = (NetConfApplication) getApplication();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Logger.info(TAG, "UDPdataMonitor started");
        tag = true;
        cachedThreadPool.execute(new ReceiveInfo());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Logger.info(TAG, "service stop");
        super.onDestroy();
    }

    private void dispatchMessage(String info) {
        if (app.chatId.equals(dp.getIp())) {
            // 发广播在交给聊天窗口处理
            Logger.info(TAG, "mHandler by chat");
            EventBus.getDefault().post(new EventInfo(Commend.incomingMsg, EventInfo.tofrg, info));
        } else {
            ChatMsgEntity entity = new ChatMsgEntity(dp.getSenderName(),
                    app.getDate(), JSON.parseObject(info, DataPacket.class)
                    .getContent(), true);
            new DBManager(this).addMsg(entity, dp.getIp());

            // 发送通知
            Logger.info(TAG, "mHandler by Notification");
            Intent notifyIntent = new Intent("net.ui.main");
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            Bundle bundle = new Bundle();
            bundle.putString("name", dp.getSenderName());
            bundle.putString("ip", dp.getIp());
            notifyIntent.putExtras(bundle);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    UdpDataMonitorService.this, R.string.app_name,
                    notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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
            BadgeUtil.setBadgeCount(getApplicationContext(), new DBManager(this).getUnreadMsgNum());

            // 让界面显示未读消息的红点
            Logger.info(TAG, "mHandler by userList");
            EventBus.getDefault().post(new EventInfo(Commend.redraw, EventInfo.tofrg, null));
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
                    Logger.info(TAG, "get udp json byte is-----" +
                            NetConfApplication.printByte(UdpPacket.getData()));
                    // 解析处理并显示
                    String info = new String(UdpPacket.getData(), 0,
                            UdpPacket.getLength());
                    Logger.info(TAG, "get json data is----" + info);
                    info = info.trim();
                    Logger.info(TAG, "trim udp data----" + info);
                    if (info.length() > 0) {
                        dp = JSON.parseObject(info, DataPacket.class);

                        switch (dp.getTag()) {

                            case NetConfApplication.text:
                                app.playVoice();
                                Logger.info(TAG, "service::" + app.chatId);
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
                                Logger.info(TAG, "file send request");
                                // 振动提示
                                vibrator.vibrate(pattern, -1);

                                String[] s = dp.getContent().replaceAll("\\\\", "/")
                                        .split("/");
                                String fileName = s[s.length - 1];
                                int id = NetConfApplication.taskId++;
                                NetConfApplication.getTaskList.put(id, new Progress(fileName, 0));

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
                }

            } catch (IOException e) {
                Logger.error(TAG, e.toString());
            }
        }
    }
}
