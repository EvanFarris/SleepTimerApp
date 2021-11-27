package com.gmail.efarrisdevelopment.usa.sleeptimer;

public class Alarm {
    private long time;
    private int id;

    public Alarm(Alarm a) {
        time = a.getTime();
        id = a.getId();
    }
    public Alarm(long t, int i) {
        time = t;
        id = i;
    }

    public int getId() {return id;}
    public long getTime() {return time;}
}
