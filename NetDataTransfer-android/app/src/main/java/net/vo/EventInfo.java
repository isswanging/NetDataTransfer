package net.vo;

import net.util.Commend;

public class EventInfo {
    private Commend commend;
    private Object obj;
    private int direction;

    public static final int toAct = 0;
    public static final int tofrg = 1;

    public EventInfo(Commend commend, int direction, Object obj) {
        this.commend = commend;
        this.direction = direction;
        this.obj = obj;
    }

    public Object getObj() {
        return obj;
    }

    public Commend getCommend() {
        return commend;
    }

    public int getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return "EventInfo{" +
                "commend=" + commend +
                ", obj=" + obj +
                ", direction=" + direction +
                '}';
    }
}
