package com.gmail.efarrisdevelopment.usa.sleeptimer;

public class Alarm {
    private final long endTime;
    private final int id;

    public Alarm(Alarm a) {
        endTime = a.getTime();
        id = a.getId();
    }
    public Alarm(long t, int i) {
        endTime = t;
        id = i;
    }

    public int getId() {return id;}
    public long getTime() {return endTime;}
}
