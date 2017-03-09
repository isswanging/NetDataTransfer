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

    public Commend getCommend() {
        return commend;
    }

    @Override
    public String toString() {
        return "Msg2Fragment{" +
                "commend=" + commend +
                ", obj=" + obj +
                '}';
    }
}
