package com.techmania.chatapp.views;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.techmania.chatapp.R;

import java.nio.charset.StandardCharsets;

public class NearbyChatActivity extends AppCompatActivity {

    private static final String SERVICE_ID = "com.example.chatapp.NEARBY";
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    private ConnectionsClient connectionsClient;
    private TextView chatTextView;
    private EditText messageEditText;
    private ScrollView chatScrollView;
    private String endpointIdConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_chat);

        connectionsClient = Nearby.getConnectionsClient(this);

        chatTextView = findViewById( R.id.chatTextView);
        messageEditText = findViewById(R.id.messageEditText);
        Button sendButton = findViewById(R.id.sendButton);
        Button advertiseButton = findViewById(R.id.advertiseButton);
        Button discoverButton = findViewById(R.id.discoverButton);


        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString();
            if (!message.isEmpty() && endpointIdConnected != null) {
                byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                connectionsClient.sendPayload(endpointIdConnected, Payload.fromBytes(payload));
                appendMessage("Me: " + message);
                messageEditText.setText("");
            }
        });

        advertiseButton.setOnClickListener(v -> startAdvertising());
        discoverButton.setOnClickListener(v -> startDiscovery());
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(
                "User-" + System.currentTimeMillis(),
                SERVICE_ID,
                connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        );
    }

    private void startDiscovery() {
        connectionsClient.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        );
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        endpointIdConnected = endpointId;
                        appendMessage("Connected to " + endpointId);
                    } else {
                        appendMessage("Connection failed.");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    appendMessage("Disconnected from " + endpointId);
                }
            };

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    connectionsClient.requestConnection(
                            "User-" + System.currentTimeMillis(),
                            endpointId,
                            connectionLifecycleCallback
                    );
                }

                @Override
                public void onEndpointLost(String endpointId) {}
            };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            String receivedMsg = new String(payload.asBytes(), StandardCharsets.UTF_8);
            appendMessage("Friend: " + receivedMsg);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {}
    };

    private void appendMessage(String msg) {
        chatTextView.append(msg + "\n");
        chatTextView.post(() -> {
            int scrollAmount = chatTextView.getLayout().getLineTop(chatTextView.getLineCount()) - chatTextView.getHeight();
            if (scrollAmount > 0) {
                chatTextView.scrollTo(0, scrollAmount);
            } else {
                chatTextView.scrollTo(0, 0);
            }
        });
    }
}
