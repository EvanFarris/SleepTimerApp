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
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;


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
    private final int maxTimer = 60 * 24;
    private final String qtKey1 = "ef_qt1";
    private final String qtKey2 = "ef_qt2";
    private final String qtKey3 = "ef_qt3";

    private int activeAlarmPosition;
    private boolean active;
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
    private int[] qtList;
    private Button qtButton1;
    private Button qtButton2;
    private Button qtButton3;
    private Button[] qtButtonArray;

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
        prefs_editor.putInt(qtKey1,qtList[0]);
        prefs_editor.putInt(qtKey2,qtList[1]);
        prefs_editor.putInt(qtKey3,qtList[2]);
        prefs_editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Set<String> timeSet = prefs.getStringSet(alarmKey,null);
        Set<String> idSet = prefs.getStringSet(idKey,null);
        ArrayList<String> timeList;
        ArrayList<String> idList;

        alarmID = 0;
        qtList = new int[]{prefs.getInt(qtKey1,0),prefs.getInt(qtKey2,0),prefs.getInt(qtKey3,0)};
        updateQuickTimeButton(0);
        updateQuickTimeButton(1);
        updateQuickTimeButton(2);

        if(timeSet != null && idSet != null) {
            timeList = new ArrayList<>(timeSet);
            idList = new ArrayList<>(idSet);
        } else {
            timeList = new ArrayList<>();
            idList = new ArrayList<>();
        }

        long curTime = Calendar.getInstance().getTimeInMillis();
        int aListSize = activeAlarmList.size();
        activeAlarmList.clear();
        rAdapter.notifyItemRangeRemoved(0, aListSize);

        if(timeList.size() > 0 && idList.size() > 0) {
            int aId;
            long eTime;

            for(int i = 0; i < timeList.size(); i++) {
                aId = Integer.parseInt(idList.get(i));
                eTime = Long.parseLong(timeList.get(i));
                if(eTime > curTime) {activeAlarmList.add(new Alarm(eTime, aId));}
                if(aId >= alarmID) {alarmID = aId + 1;}
            }

            if(!activeAlarmList.isEmpty()){
                activeAlarmList.sort(new AlarmComparator());
                rAdapter.notifyItemRangeChanged(0,activeAlarmList.size());
                enableButton();
            }
        }

        if(activeAlarmList.size() == 0) {
            alarmID = 0;
            disableButton();
        }
    }

    public void initializeVariables() {
        timeTextView = findViewById(R.id.minuteTextBox);
        cName = new ComponentName(this, SleepReceiver.class);
        dpm = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        active = dpm.isAdminActive(cName);
        prefs = getSharedPreferences(spKey,Context.MODE_PRIVATE);
        prefs_editor = prefs.edit();
        activeAlarmList = new ArrayList<>();
        activeAlarmPosition = -1;
        canButton = findViewById(R.id.cancelButton);
        noActiveTextView = findViewById(R.id.noAlarmView);
        RecyclerView r = findViewById(R.id.AlarmRecyclerView);
        rAdapter = new ActiveTimerAdapter(activeAlarmList,this);
        alarmID = -1;
        qtButton1 = findViewById(R.id.quickTimer1);
        qtButton2 = findViewById(R.id.quickTimer2);
        qtButton3 = findViewById(R.id.quickTimer3);
        qtButtonArray = new Button[3];
        qtButtonArray[0] = qtButton1;
        qtButtonArray[1] = qtButton2;
        qtButtonArray[2] = qtButton3;

        r.setLayoutManager(new LinearLayoutManager(this));
        r.setAdapter(rAdapter);

        String permissionType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionType = Manifest.permission.SCHEDULE_EXACT_ALARM;
        } else {
            permissionType = Manifest.permission.SET_ALARM;
        }

        if(ContextCompat.checkSelfPermission(this, permissionType) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {permissionType},1);
        }

        if(!active) {createAdmin();}

        MobileAds.initialize(this, initializationStatus -> {
        });

        AdView adView = findViewById(R.id.maAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
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
        timeTextView.addTextChangedListener(new TextWatcher() {
            int index;
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(i2 == 0) {index = i == 0 ? 0 : 1;}
                else {index = i + 1;}
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(!timeTextView.getText().toString().equals("")) {
                    if(timeTextView.getText().toString().charAt(0) == '0') {
                        timeTextView.setText(timeTextView.getText().subSequence(1, timeTextView.getText().length()));
                    } else {
                        int time = Integer.parseInt(timeTextView.getText().toString());
                        if(time > maxTimer) {
                            timeTextView.setText(String.valueOf(maxTimer));
                            Selection.setSelection(editable, timeTextView.getText().toString().length());
                        }
                        else {
                            Selection.setSelection(editable, index);
                        }
                    }
                }
            }
        });

        qtButton1.setOnLongClickListener(view -> {
            resetQuickTimer(view);
            return true;
        });
        qtButton2.setOnLongClickListener(view -> {
            resetQuickTimer(view);
            return true;
        });
        qtButton3.setOnLongClickListener(view -> {
            resetQuickTimer(view);
            return true;
        });
    }

    public void clearFields() {
        timeTextView.setText("");
        closeKeyboard();
    }

    public void clickSleepTimer(View v) {
        active = dpm.isAdminActive(cName);
        if(!active) {
            createAdmin();
        } else {
            String perm;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {perm = Manifest.permission.SCHEDULE_EXACT_ALARM;}
            else {perm = Manifest.permission.SET_ALARM;}

            if(ContextCompat.checkSelfPermission(this, perm) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[] {perm},123);
            } else {
                if(!timeTextView.getText().toString().equals("")) {
                    createSleepTimer(Long.parseLong(timeTextView.getText().toString()));
                    clearFields();
                }
            }
        }
    }

    public void createSleepTimer(long minutes) {
        Intent intent  = new Intent(this, SleepReceiver.class);
        intent.setAction(sleepAction);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this,alarmID,intent,PendingIntent.FLAG_IMMUTABLE);
        long alarmTime = Calendar.getInstance().getTimeInMillis() + minutes * 60000;

        AlarmManager alarmM = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmM.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
        activeAlarmList.add(new Alarm(alarmTime, alarmID++));
        activeAlarmList.sort(new AlarmComparator());
        rAdapter.notifyItemRangeChanged(0,activeAlarmList.size());
        Toast.makeText(this, "Sleep timer Created",Toast.LENGTH_LONG).show();
        enableButton();
    }

    public void cancelSleepTimer(View v) {
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

    public void clickQuickTimer(View v) {
        int vId = getButtonNum(v);
        if(qtList[vId] != 0) {
            createSleepTimer(qtList[vId]);}
        else {quickTimerDialog(vId);}
    }

    public void quickTimerDialog(int buttonNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set the duration of the new Quick Timer.");

        final EditText dialogTB= new EditText(this);
        dialogTB.setInputType(InputType.TYPE_CLASS_NUMBER);
        dialogTB.setHint(getString(R.string.timeEditViewHint));
        builder.setView(dialogTB);
        builder.setPositiveButton(R.string.setQT,(dialogInterface, i) -> {
            if(!dialogTB.getText().toString().equals("")) {
                int num = Integer.parseInt(dialogTB.getText().toString());
                if(num > 0 && num <= 1440) {
                    qtList[buttonNum] = num;
                    updateQuickTimeButton(buttonNum);
                } else {
                    Toast.makeText(this, getString(R.string.validTimer),Toast.LENGTH_LONG).show();
                }
            }
        }).setNegativeButton(R.string.cancel, (dialogInterface, i) -> {});

        builder.create().show();
    }

    public void updateQuickTimeButton(int buttonNumber) {
        String temp;

        if(qtList[buttonNumber] == 0) {temp = getString(R.string.notSet);}
        else {temp = qtList[buttonNumber] + " " + getString(R.string.quickTimerEnding);}

        qtButtonArray[buttonNumber].setText(temp);
    }

    public void resetQuickTimer(View v) {
        int buttonNum = getButtonNum(v);

        if(buttonNum != -1 && qtList[buttonNum] != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Do you want to reset this Quick Timer?");

            builder.setPositiveButton(R.string.yes,(dialogInterface, i) -> {
                qtList[buttonNum] = 0;
                updateQuickTimeButton(buttonNum);
            }).setNegativeButton(R.string.no, (dialogInterface, i) -> {});

            builder.create().show();
        }
    }

    public int getButtonNum(View v) {
        int buttonNum = v.getId();

        if(buttonNum == R.id.quickTimer1) {return 0;}
        else if(buttonNum == R.id.quickTimer2) {return 1;}
        else if (buttonNum == R.id.quickTimer3) {return 2;}
        else {return -1;}
    }

}