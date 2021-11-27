package com.gmail.efarrisdevelopment.usa.sleeptimer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gmail.efarrisdevelopment.usa.sleeptimer.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class ActiveTimerAdapter extends RecyclerView.Adapter<ActiveTimerViewHolder> {

    private final ArrayList<Alarm> alarmList;
    private int lastSelected;
    private Context context;
    private View lastView;
    public ActiveTimerAdapter(ArrayList<Alarm> aList, Context context)
    {
        alarmList=aList;
        lastView = null;
        lastSelected= -1;
        this.context = context;
    }

    @NonNull
    @Override
    public ActiveTimerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.active_timer,parent,false);
        return new ActiveTimerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveTimerViewHolder holder, int position) {
        int pos = position;
        long timer = alarmList.get(position).getTime();
        Calendar c = Calendar.getInstance();
        long cTime = c.getTimeInMillis();
        long timeRemaining = timer - cTime;
        timeRemaining = (long) Math.ceil((double) (timeRemaining/60000));
        String trStr = "(" + timeRemaining+ " minute";
        if(timeRemaining > 1)
            trStr+="s";
        trStr+=")";
        String dfTime = android.text.format.DateFormat.getTimeFormat(context).format(new Date(timer));
        holder.clockTime.setText(dfTime);
        holder.timeRemaining.setText(trStr);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(lastView!=null)
                {
                    lastView.setBackgroundColor(Color.TRANSPARENT);
                }
                MainActivity m = (MainActivity) view.getContext();
                int maPos = m.getActiveAlarmPosition();
                if(maPos == pos)
                {
                    m.setActiveAlarmPosition(-1,0);
                    view.setBackgroundColor(Color.TRANSPARENT);
                    lastView = null;
                }
                else
                {
                    m.setActiveAlarmPosition(pos,timer);
                    view.setBackgroundColor(Color.GRAY);
                }
                lastView = view;
            }
        });

    }


    @Override
    public int getItemCount() {
        return alarmList.size();
    }
    public void setLastViewBackgroundTransparent()
    {
        lastView.setBackgroundColor(Color.TRANSPARENT);
        lastView = null;
    }

}
