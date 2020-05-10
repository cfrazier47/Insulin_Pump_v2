package com.example.insulinpump;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

// Note: This is an activity and is included in the AndroidManifest.xml as:
// <activity android:name=".DisplayLog"></activity>
// Also requires this in the <application .../> :
// android:theme="@style/Theme.AppCompat"

public class DisplayLog extends AppCompatActivity {
    private static final String TAG = "DisplayLog.java";

    database_configuration db;

    DecimalFormat noZero = new DecimalFormat("#"); // deletes decimal point

    private ListView lstLog;
    private TextView lblBatteryPercent, lblInsulinPercentLog;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            lblBatteryPercent.setText(String.valueOf(level)+"%");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_display);
        lstLog = (ListView) findViewById(R.id.lstLog);
        db = new database_configuration(this);

        generateListView();

        // Updates insulin units
        lblInsulinPercentLog = findViewById(R.id.lblInsulinPercent);
        lblInsulinPercentLog.setText(noZero.format(Math.floor(MainActivity.insulinReservoir)) + "u");

        // Loads top bar
        Thread myThread = null;
        Runnable runnable = new DisplayLog.CountDownRunner();
        myThread = new Thread(runnable);
        myThread.start();
        lblBatteryPercent = (TextView) findViewById(R.id.lblBatteryLog);
        this.registerReceiver(this.broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    }

    public void doWork() {
        runOnUiThread(new Runnable() {
            public void run() {
                try{
                    TextView txtCurrentTime= (TextView)findViewById(R.id.lblTimeLog);
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                    String dateTime = simpleDateFormat.format(calendar.getTime());

                    txtCurrentTime.setText(dateTime);
                } catch (Exception e) {}
            }
        });
    }

    class CountDownRunner implements Runnable{
        // @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try {
                    doWork();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }catch(Exception e){
                }
            }
        }
    }

    private void generateListView() {
        Log.d(TAG, "generateListView(): Adding data to (ListView) lstLog");
        int useLayout = R.layout.log_row_format;

        Cursor data = db.getData(); // receives contents of the table
        ArrayList<String> listData = new ArrayList<>();

        while(data.moveToNext()) {
            // Lists data: 99:99AM 9999 99 BASAL *IF FAIL
            String date, time, glucose, insulin, isBasal, description; // in order of appearance
            date = data.getString(6);
            time = data.getString(7);
            glucose = String.format("%1$4s", data.getString(1));
            insulin = String.format("%1$2s",data.getString(2));
            isBasal = data.getString(3);
            description = data.getString(5);
            if (isBasal.equals("1")) {
                isBasal = "BASAL";
            } else {
                isBasal = "BOLUS";
            }
            listData.add(date + " " + time + " " +
                    glucose + " " + insulin + " " +
                    isBasal + " " + description);
        }

        // Creating a list adapter to view data
        ListAdapter adapter = new ArrayAdapter<>(this, useLayout, listData);
        lstLog.setAdapter(adapter);
    }

    private void toastMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // Returns to MainActivity.java
    public void goHome(android.view.View View) {
        Intent intent = new Intent(DisplayLog.this, MainActivity.class);
        startActivity(intent);
    }

}
