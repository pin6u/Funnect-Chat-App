package com.techmania.chatapp.views;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.techmania.chatapp.R;
import com.techmania.chatapp.models.LocalDatabaseHelper;

import java.io.PrintWriter;
import java.net.Socket;

public class SendMessageOfflineActivity extends AppCompatActivity {

    EditText editText;
    Button sendButton;
    String SERVER_IP = "192.168.43.1"; // 📡 Use Hotspot IP of receiver
    int SERVER_PORT = 9999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message_offline);

        editText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> {
            String msg = editText.getText().toString().trim();
            if (!msg.isEmpty()) {
                new Thread(() -> sendMessage(msg)).start();
                editText.setText("");

                LocalDatabaseHelper db = new LocalDatabaseHelper(this);
                db.saveMessage(msg, true); // true = sent
            }
        });
    }

    private void sendMessage(String message) {
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            writer.println(message);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
