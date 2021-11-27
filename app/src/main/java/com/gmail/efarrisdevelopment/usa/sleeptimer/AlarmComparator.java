package com.gmail.efarrisdevelopment.usa.sleeptimer;

import java.util.Comparator;

public class AlarmComparator implements Comparator<Alarm> {
    @Override
    public int compare(Alarm alarm, Alarm t1) {
        if(alarm.getTime() > t1.getTime())
            return (1);
        else if (alarm.getTime() == t1.getTime())
            return 0;
        else
            return (-1);
    }
}
