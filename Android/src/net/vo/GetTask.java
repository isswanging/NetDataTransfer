package net.vo;

public class GetTask {
    int taskId;
    DataPacket dp;
    String fileName;

    public GetTask(int id, DataPacket d, String name) {
        taskId = id;
        dp = d;
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

    public DataPacket getDp() {
        return dp;
    }

    public void setDp(DataPacket dp) {
        this.dp = dp;
    }

}
