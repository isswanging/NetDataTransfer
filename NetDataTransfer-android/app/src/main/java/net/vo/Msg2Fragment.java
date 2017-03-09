package net.vo;

import net.util.Commend;

public class Msg2Fragment {
    private Commend commend;
    private Object obj;

    public Msg2Fragment(Commend commend, Object obj) {
        this.commend = commend;
        this.obj = obj;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Commend getCommend() {
        return commend;
    }

    public void setCommend(Commend commend) {
        this.commend = commend;
    }

    @Override
    public String toString() {
        return "Msg2Fragment{" +
                "commend=" + commend +
                ", obj=" + obj +
                '}';
    }
}
