package com.techmania.chatapp.views;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techmania.chatapp.R;
import com.techmania.chatapp.adapters.MessagesAdapter;
import com.techmania.chatapp.databinding.ActivityMessagingBinding;
import com.techmania.chatapp.models.MessagesModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessagingActivity extends AppCompatActivity implements MessagesAdapter.OnMessageClickListener {

    ActivityMessagingBinding binding;
    String targetUserName, targetUserId, targetUserImageUrl;
    String currentUserName, currentUserId;

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference("Messages");

    ArrayList<MessagesModel> messagesList = new ArrayList<>();
    MessagesAdapter messagesAdapter;
    private static boolean cloudinaryInitialized = false;
    private static final int REQUEST_STORAGE_PERMISSION = 1001;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_STORAGE_PERMISSION);
                        } else {
                            uploadMediaToCloudinary(fileUri);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessagingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!cloudinaryInitialized) {
            HashMap config = new HashMap();
            config.put("cloud_name", "dktt70ypi");
            config.put("api_key", "486644521562446");
            config.put("api_secret", "3FW4NTSMCbiITHZTJ3zCrBbWAaA");
            MediaManager.init(this, config);
            cloudinaryInitialized = true;
        }

        targetUserName = getIntent().getStringExtra("targetUserName");
        targetUserId = getIntent().getStringExtra("targetUserId");
        targetUserImageUrl = getIntent().getStringExtra("targetUserImageUrl");
        currentUserName = getIntent().getStringExtra("currentUserName");
        currentUserId = getIntent().getStringExtra("currentUserId");

        binding.textViewFriendName.setText(targetUserName);
        if (targetUserImageUrl != null && !targetUserImageUrl.equals("null")) {
            Picasso.get().load(targetUserImageUrl).into(binding.imageViewFriendProfile);
        }

        binding.recyclerViewMessage.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessagesAdapter(messagesList, currentUserId, this);
        binding.recyclerViewMessage.setAdapter(messagesAdapter);

        setupTypingIndicator();
        observeTypingFromTargetUser();

        binding.imageSendMessage.setOnClickListener(v -> {
            String message = binding.editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) sendMessage(message, "text", null);
        });

        if (binding.imageAttachFile != null) {
            binding.imageAttachFile.setOnClickListener(v -> openFilePicker());
        }

        binding.imageViewGoMain.setOnClickListener(v -> finish());

        getMessagesFromDatabase();
        updateSeenStatus();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});
        filePickerLauncher.launch(intent);
    }

    private void uploadMediaToCloudinary(Uri fileUri) {
        String fileName = getFileName(fileUri);
        Toast.makeText(this, "Uploading " + fileName, Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(fileUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        String format = (String) resultData.get("format");
                        String type = format.equals("pdf") ? "pdf" : "image";
                        sendMessage(type.equals("pdf") ? "📄 PDF File" : "🖼️ Image", type, url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Toast.makeText(MessagingActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void sendMessage(String content, String type, String mediaUrl) {
        String key = databaseReference.child(currentUserId).child(targetUserId).push().getKey();
        long timestamp = System.currentTimeMillis();

        MessagesModel messageModel = new MessagesModel(currentUserId, targetUserId, key, content, timestamp);
        messageModel.setStatus("sent");
        messageModel.setType(type);
        messageModel.setMediaUrl(mediaUrl);

        databaseReference.child(currentUserId).child(targetUserId).child(key).setValue(messageModel);
        databaseReference.child(targetUserId).child(currentUserId).child(key).setValue(messageModel);

        binding.editTextMessage.setText("");
    }

    private void getMessagesFromDatabase() {
        databaseReference.child(currentUserId).child(targetUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messagesList.clear();
                        for (DataSnapshot each : snapshot.getChildren()) {
                            MessagesModel message = each.getValue(MessagesModel.class);
                            if (message != null) {
                                messagesList.add(message);
                            }
                        }
                        messagesAdapter.notifyDataSetChanged();
                        binding.recyclerViewMessage.scrollToPosition(messagesList.size() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MessagingActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSeenStatus() {
        databaseReference.child(targetUserId).child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot each : snapshot.getChildren()) {
                            MessagesModel msg = each.getValue(MessagesModel.class);
                            if (msg != null && !msg.getSenderId().equals(currentUserId) && !"seen".equals(msg.getStatus())) {
                                each.getRef().child("status").setValue("seen");
                            }
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupTypingIndicator() {
        binding.editTextMessage.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                FirebaseDatabase.getInstance().getReference("Users")
                        .child(currentUserId).child("typingTo")
                        .setValue(s.toString().trim().isEmpty() ? "" : targetUserId);
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeTypingFromTargetUser() {
        FirebaseDatabase.getInstance().getReference("Users")
                .child(targetUserId).child("typingTo")
                .addValueEventListener(new ValueEventListener() {
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String typingTo = snapshot.getValue(String.class);
                        binding.textViewFriendStatus.setText(
                                currentUserId.equals(typingTo) ? "Typing..." : ""
                        );
                    }
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private String getFileName(Uri uri) {
        String result = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    result = cursor.getString(index);
                }
            }
        }
        return result != null ? result : "file";
    }

    @Override
    public void onMessageClicked(MessagesModel messagesModel) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setItems(new String[]{"Delete for Me", "Delete for Everyone"}, (dialog, which) -> {
                    if (which == 0) {
                        databaseReference.child(currentUserId).child(targetUserId)
                                .child(messagesModel.getMessageId()).removeValue();
                    } else if (messagesModel.getSenderId().equals(currentUserId)) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("message", "This message was deleted.");
                        map.put("deleted", true);
                        map.put("type", "text");
                        map.put("mediaUrl", null);
                        databaseReference.child(currentUserId).child(targetUserId)
                                .child(messagesModel.getMessageId()).updateChildren(map);
                        databaseReference.child(targetUserId).child(currentUserId)
                                .child(messagesModel.getMessageId()).updateChildren(map);
                    } else {
                        Toast.makeText(this, "You can only delete your own message for everyone", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateUserStatus("offline");
    }

    private void updateUserStatus(String status) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        if ("offline".equals(status)) {
            String lastSeen = "Last seen at " + new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
            userRef.child("status").setValue(lastSeen);
            userRef.child("typingTo").setValue("");
        } else {
            userRef.child("status").setValue("online");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. You can now upload files.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied. Cannot access storage.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}