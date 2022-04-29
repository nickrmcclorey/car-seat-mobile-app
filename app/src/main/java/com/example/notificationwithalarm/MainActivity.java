package com.example.notificationwithalarm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private Thread monitor;
    Boolean childInCar = false;
    Boolean parentInCar = false;
    Boolean alertSent = false;
    String error = "original";
    NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        monitor = new Thread(new Runnable() {
            @Override
            public void run() {

                while(true) {
                    try {
                        getCarseatStatus();
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        error = e.toString();
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        error = e.toString();
                        break;
                    }
                }

            }
        });
        monitor.start();

//        Button button = findViewById(R.id.button);
//        button.setOnClickListener(v -> {
//            Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
//            childInCar = true;
//
//            setAlert();
//
//            Intent intent = new Intent(MainActivity.this, ReminderBroadcast.class);
//            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
//
//            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//
//            long timeAtButtonClick = System.currentTimeMillis();
//            long twoSecondsInMillis = 10 * 1000;
//
//            alarmManager.set(AlarmManager.RTC_WAKEUP,
//                    timeAtButtonClick+twoSecondsInMillis,
//                    pendingIntent);
//        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        monitor.interrupt();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "LemubitReminderChannel";
            String description = "Channel for Lemubit Reminder";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("notifyLemubit", name, importance);
            channel.setDescription(description);


            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void getCarseatStatus() {
        try {
            URL url = new URL("http://192.168.4.1:5000");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder result = new StringBuilder();
            while((inputLine = br.readLine()) != null) {
                result.append(inputLine);
            }
            br.close();
            String str = result.toString();
            childInCar = str.toLowerCase().startsWith("true");
            parentInCar = true;
            alertSent = false;
            error = str;

        } catch (ConnectException e) {
            parentInCar = false;
        } catch (Exception e) {
            error = e.toString();
        }

        if (!parentInCar && childInCar && !alertSent) {
            setAlert();
            alertSent = true;
        }
        updateUI();
    }

    private void updateUI() {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView text = (TextView) findViewById(R.id.childStatus);
                String status = childInCar ? "In Carseat" : "Outside";
                text.setText("Child Status: " + status);

                text = (TextView) findViewById(R.id.parentStatus);
                status = parentInCar ? "Inside Car" : "Left Car";
                text.setText("Parent Status: " + status);
            }
        });
    }

    private void setAlert() {
        Intent intent = new Intent(this, ReminderBroadcast.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        long timeAtButtonClick = System.currentTimeMillis();
        alarmManager.set(AlarmManager.RTC_WAKEUP, timeAtButtonClick+2000, pendingIntent);
    }
}