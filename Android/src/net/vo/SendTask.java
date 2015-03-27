package net.vo;

import java.net.Socket;

public class SendTask {
    int taskId;
    Socket socket;
    String fileName;

    public SendTask(int id, Socket s, String name) {
        taskId = id;
        socket = s;
        fileName = name;
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
