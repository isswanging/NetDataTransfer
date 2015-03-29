package net.vo;

import java.net.Socket;

public class SendTask {
    int taskId;
    Socket socket;
    String fileName;
    DataPacket dataPacket;

    public SendTask(int id, Socket s, String name,DataPacket dp) {
        taskId = id;
        socket = s;
        fileName = name;
        dataPacket = dp;
    }

    public DataPacket getDataPacket() {
        return dataPacket;
    }

    public void setDataPacket(DataPacket dataPacket) {
        this.dataPacket = dataPacket;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

}
