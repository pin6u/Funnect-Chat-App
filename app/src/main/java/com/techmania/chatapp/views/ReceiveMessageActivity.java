package com.techmania.chatapp.views;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.techmania.chatapp.R;
import com.techmania.chatapp.models.LocalDatabaseHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveMessageActivity extends AppCompatActivity {

    private TextView statusTextView;
    private boolean isRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_message); // create XML with one TextView

        statusTextView = findViewById(R.id.statusTextView);

        // Start receiving thread
        new Thread(() -> receiveMessages()).start();
    }

    private void receiveMessages() {
        try {
            ServerSocket serverSocket = new ServerSocket(9999); // Same port as sender

            runOnUiThread(() -> statusTextView.setText("Listening for messages..."));

            while (isRunning) {
                Socket socket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                String message = reader.readLine();

                // Save to local DB
                LocalDatabaseHelper db = new LocalDatabaseHelper(getApplicationContext());
                db.saveMessage(message, false); // false = received

                runOnUiThread(() -> {
                    Toast.makeText(this, "Received: " + message, Toast.LENGTH_SHORT).show();
                    statusTextView.setText("Last Message: " + message);
                });

                socket.close();
            }

            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }
}
