package com.techmania.chatapp.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.techmania.chatapp.R;
import com.techmania.chatapp.adapters.UsersAdapter;
import com.techmania.chatapp.databinding.ActivityMainBinding;
import com.techmania.chatapp.loginpages.LoginActivity;
import com.techmania.chatapp.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding activityMainBinding;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    private final DatabaseReference databaseReference = firebaseDatabase.getReference().child("Users");

    private ArrayList<User> usersList = new ArrayList<>();
    private UsersAdapter usersAdapter;
    private ValueEventListener usersValueEventListener;

    private String currentUserId;
    private String currentUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        // Toolbar Menu
        activityMainBinding.toolbarMain.setOverflowIcon(
                AppCompatResources.getDrawable(this, R.drawable.more));
        activityMainBinding.toolbarMain.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.editProfileItem) {
                startActivity(new Intent(MainActivity.this, UpdateProfileActivity.class));
                return true;
            } else if (item.getItemId() == R.id.signOutItem) {
                if (usersValueEventListener != null) {
                    databaseReference.removeEventListener(usersValueEventListener);
                }
                updateUserStatus("offline");
                auth.signOut();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return true;
            } else if (item.getItemId () == R.id.menu_nearby_chat) {
                // ✅ Nearby Chat Activity
                startActivity ( new Intent ( MainActivity.this, NearbyChatActivity.class ) );
                return true;
            }
            else if (item.getItemId () == R.id.offlineGamesItem) {
                startActivity(new Intent(MainActivity.this, OfflineGamesActivity.class));
                return true;
            }
            return false;
        });

        // RecyclerView Setup
        activityMainBinding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersAdapter = new UsersAdapter(usersList, this::openMessagingActivityOnUserClicked);
        activityMainBinding.usersRecyclerView.setAdapter(usersAdapter);

        retrieveUsersFromDatabase();
        sendFcmTokenToFirebase();
    }

    private void openMessagingActivityOnUserClicked(User user) {
        Intent intent = new Intent(MainActivity.this, MessagingActivity.class);
        intent.putExtra("targetUserId", user.getUserId());
        intent.putExtra("targetUserName", user.getUserName());
        intent.putExtra("targetUserImageUrl", user.getImageUrl());
        intent.putExtra("currentUserName", currentUserName);
        intent.putExtra("currentUserId", currentUserId);
        startActivity(intent);
    }

    private void retrieveUsersFromDatabase() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        currentUserId = currentUser.getUid();

        usersValueEventListener = databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                for (DataSnapshot each : snapshot.getChildren()) {
                    User user = each.getValue(User.class);
                    if (user != null && user.getUserId() != null) {
                        if (!user.getUserId().equals(currentUserId)) {
                            usersList.add(user);
                        } else {
                            currentUserName = user.getUserName();
                        }
                    }
                }

                // Sort users by latest message timestamp
                Collections.sort(usersList, (u1, u2) ->
                        Long.compare(u2.getLastMessageTimestamp(), u1.getLastMessageTimestamp()));

                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendFcmTokenToFirebase() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                if (currentUserId != null) {
                    FirebaseDatabase.getInstance().getReference("Users")
                            .child(currentUserId)
                            .child("fcmToken")
                            .setValue(token);
                }
            }
        });
    }

    private void updateUserStatus(String status) {
        if (currentUserId != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);

            if (status.equals("offline")) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String lastSeen = "Last seen at " + sdf.format(new Date());
                userRef.child("status").setValue(lastSeen);
                userRef.child("typingTo").setValue("");
            } else {
                userRef.child("status").setValue("online");
            }
        }
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
}
