package com.gmail.efarrisdevelopment.usa.sleeptimer;

import java.util.Comparator;

public class AlarmComparator implements Comparator<Alarm> {
    @Override
    public int compare(Alarm alarm, Alarm t1) {
        return Long.compare(alarm.getTime(),t1.getTime());
    }
}
