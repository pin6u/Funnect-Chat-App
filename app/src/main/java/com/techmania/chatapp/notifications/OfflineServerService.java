package com.techmania.chatapp.notifications;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import com.techmania.chatapp.models.LocalDatabaseHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class OfflineServerService extends Service {

    private static final int PORT = 9999;
    private boolean isRunning = true;

    @Override
    public void onCreate() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg = reader.readLine();

                    LocalDatabaseHelper db = new LocalDatabaseHelper(this);
                    db.saveMessage(msg, false); // false = received

                    runOnUiThread(() -> Toast.makeText(this, "Message Received: " + msg, Toast.LENGTH_SHORT).show());
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runOnUiThread(Runnable r) {
        android.os.Handler mainHandler = new android.os.Handler(getMainLooper());
        mainHandler.post(r);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }
}
