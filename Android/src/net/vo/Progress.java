package net.vo;

public class Progress {
    String name;
    int num;

    public Progress(String n0, int n1) {
        name = n0;
        num = n1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

}
