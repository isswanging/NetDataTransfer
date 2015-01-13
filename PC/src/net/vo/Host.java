package net.vo;

import java.io.Serializable;

// 主机对象
public class Host implements Serializable {

    private static final long serialVersionUID = -6145729630822090496L;

    private String userName; // 用户名
    private String groupName; // 工作组
    private String ip; // IP地址ַ
    private String hostName; // 主机名
    private int state;// 主机状态，0表示新登录，1表示在线
    private int tag;// 表示是否需要回应,0表示需要回答，1表示不需要回答

    public Host(String userName, String groupName, String ip, String hostName,
                int i, int t) {
        this.userName = userName;
        this.groupName = groupName;
        this.ip = ip;
        this.hostName = hostName;
        this.state = i;
        this.tag = t;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Host() {
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

}
