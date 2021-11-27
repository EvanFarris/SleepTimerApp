package com.gmail.efarrisdevelopment.usa.sleeptimer;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gmail.efarrisdevelopment.usa.sleeptimer.R;

public class ActiveTimerViewHolder extends RecyclerView.ViewHolder {
    public TextView timeRemaining,clockTime;

    public ActiveTimerViewHolder(View v) {
        super(v);
        timeRemaining = v.findViewById(R.id.timeRemainingView);
        clockTime = v.findViewById(R.id.clockRemaining);
    }


}
