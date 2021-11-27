package com.gmail.efarrisdevelopment.usa.sleeptimer;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final String spKey = "ef_sleep_timer";
    private final String idKey = "ef_ids";
    private final String alarmKey = "ef_alarms";
    private final String sleepAction = "goToSleep";
    private final int maxTimer = 600;

    private int calledFromSeekBar;
    private int activeAlarmPosition;
    private boolean active;
    private SeekBar timeSeek;
    private TextView timeTextView;
    private DevicePolicyManager dpm;
    private ComponentName cName;
    private SharedPreferences prefs;
    private SharedPreferences.Editor prefs_editor;
    private ActiveTimerAdapter rAdapter;
    private ArrayList<Alarm> activeAlarmList;
    private Button canButton;
    private TextView noActiveTextView;
    private int alarmID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeVariables();
        setListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Set<String> timeList = new HashSet<>();
        Set<String> idList = new HashSet<>();

        for(Alarm a : activeAlarmList) {
            timeList.add(String.valueOf(a.getTime()));
            idList.add(String.valueOf(a.getId()));
        }

        prefs_editor.putStringSet(alarmKey, timeList);
        prefs_editor.putStringSet(idKey, idList);
        prefs_editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ArrayList<String> timeList = new ArrayList<>(prefs.getStringSet(alarmKey,null));
        ArrayList<String> idList = new ArrayList<>(prefs.getStringSet(idKey,null));
        long curTime = Calendar.getInstance().getTimeInMillis();

        activeAlarmList.clear();

        if(timeList.size() > 0 && idList.size() > 0) {
            int aId;
            long eTime;
            for(int i = 0; i < timeList.size(); i++) {
                aId = Integer.parseInt(idList.get(i));
                eTime = Long.parseLong(timeList.get(i));
                if(eTime > curTime) {activeAlarmList.add(new Alarm(eTime, aId));}
                if(aId >= alarmID) {alarmID = aId + 1;}
            }

            if(activeAlarmList.isEmpty()) {disableButton();}
            else {
                activeAlarmList.sort(new AlarmComparator());
                enableButton();
            }

            rAdapter.notifyItemRangeChanged(0,activeAlarmList.size());
        }
        if(activeAlarmList.size() == 0) {alarmID = 0;}

    }

    public void initializeVariables() {
        timeSeek = findViewById(R.id.timeBar);
        timeTextView = findViewById(R.id.minuteTextBox);
        cName = new ComponentName(this, SleepReceiver.class);
        dpm = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        calledFromSeekBar = -1;
        active = dpm.isAdminActive(cName);
        prefs = getSharedPreferences(spKey,Context.MODE_PRIVATE);
        prefs_editor = prefs.edit();
        activeAlarmList = new ArrayList<>();
        activeAlarmPosition = -1;
        canButton = findViewById(R.id.cancelButton);
        noActiveTextView = findViewById(R.id.noAlarmView);
        RecyclerView r;
        r = findViewById(R.id.AlarmRecyclerView);
        rAdapter = new ActiveTimerAdapter(activeAlarmList,this);
        alarmID = -1;

        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(rAdapter);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.SET_ALARM},1);
        }
        if(!active)
            createAdmin();
    }


    public void createAdmin() {
        Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,cName);
        i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"The sleep timer locks your phone once the timer ends.");
        startActivity(i);
    }

    public void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(timeTextView.getWindowToken(),0);
    }

    public void setListeners() {
        timeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(calledFromSeekBar == 1) {
                    timeTextView.setText(String.valueOf(i));
                    closeKeyboard();
                    timeTextView.clearFocus();
                } else {calledFromSeekBar = 1;}
                seekBar.setProgress(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(timeTextView.getText().toString().equals(""))
                {
                    if(seekBar.getProgress()!= 0)
                        seekBar.setProgress(0);
                }
                else if(Integer.parseInt(timeTextView.getText().toString()) != seekBar.getProgress())
                    timeTextView.setText(String.valueOf(seekBar.getProgress()));
            }
        });

        timeTextView.addTextChangedListener(new TextWatcher() {
            int index;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(i2 == 0) {index = i == 0 ? 0 : 1;}
                else {index = i+1;}
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                calledFromSeekBar = 0;
                if(timeTextView.getText().toString().equals("")) {timeSeek.setProgress(0);}
                else {
                    if(timeTextView.getText().toString().charAt(0) == '0') {
                        timeTextView.setText(timeTextView.getText().subSequence(1,timeTextView.getText().length()));
                    } else {
                        int time = Integer.parseInt(timeTextView.getText().toString());
                        if(time > 600) {
                            timeSeek.setProgress(maxTimer);
                            timeTextView.setText(String.valueOf(maxTimer));
                            Selection.setSelection(editable,timeTextView.length());
                        }
                        else {
                            timeSeek.setProgress(time);
                            Selection.setSelection(editable,index);
                        }
                    }
                }
            }
        });
    }

    public void clearFields() {
        timeTextView.setText("");
        timeSeek.setProgress(0);
        closeKeyboard();
    }

    public void clickSleepTimer(View v) {
        active = dpm.isAdminActive(cName);
        if(!active) {
            createAdmin();
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.SET_ALARM) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.SET_ALARM},123);
                Toast.makeText(this, "Permission not found",Toast.LENGTH_LONG).show();
            } else {
                if(!timeTextView.getText().toString().equals("")) {
                    Intent intent  = new Intent(this, SleepReceiver.class);
                    intent.setAction(sleepAction);
                    PendingIntent alarmIntent = PendingIntent.getBroadcast(this,alarmID,intent,PendingIntent.FLAG_IMMUTABLE);
                    long alarmTime = Calendar.getInstance().getTimeInMillis() + Long.parseLong(timeTextView.getText().toString()) * 60000;

                    AlarmManager alarmM = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
                    alarmM.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
                    activeAlarmList.add(new Alarm(alarmTime, alarmID++));
                    activeAlarmList.sort(new AlarmComparator());
                    rAdapter.notifyItemRangeChanged(0,activeAlarmList.size());
                    Toast.makeText(this, "Sleep timer Created",Toast.LENGTH_LONG).show();
                    clearFields();
                    enableButton();
                }
            }
        }
    }

    public void cancelAlarm(View v) {
        if(activeAlarmPosition != -1) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            long timerToCancel = activeAlarmList.get(activeAlarmPosition).getTime();
            String dfString = "Ending time: " + android.text.format.DateFormat.getTimeFormat(this).format(new Date(timerToCancel));
            builder.setTitle(R.string.cancel_confirmation).setMessage(dfString);
            builder.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                long curTime = Calendar.getInstance().getTimeInMillis();
                if(curTime < timerToCancel) {
                    Intent intent = new Intent(this,SleepReceiver.class);
                    intent.setAction(sleepAction);
                    PendingIntent alarmIntent = PendingIntent.getBroadcast(this,activeAlarmList.get(activeAlarmPosition).getId(),intent,PendingIntent.FLAG_IMMUTABLE);
                    AlarmManager alarmM = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

                    alarmM.cancel(alarmIntent);
                    activeAlarmList.remove(activeAlarmPosition);
                    rAdapter.notifyItemRemoved(activeAlarmPosition);
                    rAdapter.notifyItemRangeChanged(activeAlarmPosition,activeAlarmList.size());
                    activeAlarmPosition = -1;
                    rAdapter.setLastViewBackgroundTransparent();
                    clearFields();
                    if(activeAlarmList.isEmpty()){
                        disableButton();
                    }
                }
            }).setNegativeButton(R.string.no, (dialogInterface, i) -> {});
            builder.create().show();
        }
    }

    public void setActiveAlarmPosition(int pos, long timer) {
        if(pos == -1 || activeAlarmList.get(pos).getTime() == timer) {activeAlarmPosition = pos;}
    }

    public int getActiveAlarmPosition() {return activeAlarmPosition;}

    public void disableButton() {
        canButton.setBackgroundColor(Color.GRAY);
        canButton.setClickable(false);
        noActiveTextView.setVisibility(View.VISIBLE);
    }

    public void enableButton() {
        canButton.setBackgroundColor(Color.BLUE);
        canButton.setClickable(true);
        noActiveTextView.setVisibility(View.INVISIBLE);
    }

}