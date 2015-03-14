package net.vo;

public class GetTask {
    int taskId;
    DataPacket dp;

    public GetTask(int id, DataPacket d) {
        taskId = id;
        dp = d;
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
