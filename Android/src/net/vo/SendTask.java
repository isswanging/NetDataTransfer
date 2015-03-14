package net.vo;

import java.net.Socket;

public class SendTask {
    int taskId;
    Socket socket;

    public SendTask(int id, Socket s) {
        taskId = id;
        socket = s;
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
