package com.techmania.chatapp.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.techmania.chatapp.R;
import com.techmania.chatapp.databinding.ActivityUpdateProfileBinding;
import com.techmania.chatapp.models.User;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UpdateProfileActivity extends AppCompatActivity {

    ActivityUpdateProfileBinding updateProfileBinding;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser;
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference databaseReference = firebaseDatabase.getReference().child("Users");

    String userName, userEmail, imageUrl, userId;
    String updatedProfileImageUrl;
    ValueEventListener updateValueEventListener;
    ActivityResultLauncher<String[]> permissionsResultLauncher;
    ActivityResultLauncher<Intent> photoPickerResultLauncher;
    ActivityResultLauncher<Intent> cropPhotoResultLauncher;
    Uri croppedImageUri;
    ArrayList<String> permissionsList = new ArrayList<>();
    int deniedPermissionCount = 0;
    boolean imageControl = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updateProfileBinding = ActivityUpdateProfileBinding.inflate(getLayoutInflater());
        setContentView(updateProfileBinding.getRoot());
        HashMap<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dktt70ypi");
        config.put("api_key", "486644521562446");
        config.put("api_secret", "3FW4NTSMCbiITHZTJ3zCrBbWAaA");

        try {
            MediaManager.init(this, config);
        } catch (IllegalStateException e) {
            // MediaManager already initialized, ignore
        }

        if (Build.VERSION.SDK_INT >= 33) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsList.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        } else if (Build.VERSION.SDK_INT > 32) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        registerActivityForMultiplePermission();
        registerActivityForPhotoPicker();
        registerActivityForPhotoCrop();

        updateProfileBinding.toolbarUpdateProfile.setNavigationOnClickListener(v -> finish());

        currentUser = auth.getCurrentUser();
        getAndShowUserInfo();

        updateProfileBinding.imageViewProfileUpdateProfile.setOnClickListener(v -> {
            if (hasPermission()) openPhotoPicker();
            else shouldShowPermissionRationaleIfNeeded();
        });

        updateProfileBinding.buttonUpdateProfile.setOnClickListener(v -> updatePhoto());
    }

    public void updatePhoto() {
        updateProfileBinding.buttonUpdateProfile.setEnabled(false);
        updateProfileBinding.progressBarUpdateProfile.setVisibility(View.VISIBLE);

        if (imageControl && croppedImageUri != null) {
            MediaManager.get().upload(croppedImageUri)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            updatedProfileImageUrl = resultData.get("secure_url").toString();
                            updateUserData();
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Toast.makeText(UpdateProfileActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                            updateProfileBinding.buttonUpdateProfile.setEnabled(true);
                            updateProfileBinding.progressBarUpdateProfile.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();
        } else {
            updatedProfileImageUrl = imageUrl;
            updateUserData();
        }
    }

    public void updateUserData() {
        String updatedUserName = updateProfileBinding.editTextUserNameUpdateProfile.getText().toString().trim();
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userName", updatedUserName);
        userMap.put("imageUrl", updatedProfileImageUrl);

        databaseReference.child(currentUser.getUid()).updateChildren(userMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Data Updated", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
            updateProfileBinding.buttonUpdateProfile.setEnabled(true);
            updateProfileBinding.progressBarUpdateProfile.setVisibility(View.INVISIBLE);
        });
    }

    public void getAndShowUserInfo() {
        if (currentUser != null) {
            updateValueEventListener = databaseReference.child(currentUser.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        userName = user.getUserName();
                        userEmail = user.getUserEmail();
                        userId = user.getUserId();
                        imageUrl = user.getImageUrl();

                        updateProfileBinding.editTextUserNameUpdateProfile.setText(userName);
                        if ("null".equals(imageUrl)) {
                            updateProfileBinding.imageViewProfileUpdateProfile.setImageResource(R.drawable.default_profile_photo);
                        } else {
                            Picasso.get().load(imageUrl).into(updateProfileBinding.imageViewProfileUpdateProfile);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(UpdateProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (updateValueEventListener != null) {
            databaseReference.child(currentUser.getUid()).removeEventListener(updateValueEventListener);
        }
    }

    public void registerActivityForMultiplePermission() {
        permissionsResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = true;
            for (Boolean isAllowed : result.values()) {
                if (!isAllowed) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) openPhotoPicker();
            else {
                deniedPermissionCount++;
                if (deniedPermissionCount < 2) shouldShowPermissionRationaleIfNeeded();
                else {
                    new AlertDialog.Builder(this)
                            .setTitle("Chat App")
                            .setMessage("Grant photo permissions in settings.")
                            .setPositiveButton("Go to Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.parse("package:" + getPackageName());
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                            .create().show();
                }
            }
        });
    }

    public void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerResultLauncher.launch(intent);
    }

    public void registerActivityForPhotoPicker() {
        photoPickerResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uncroppedImageUri = result.getData().getData();
                cropSelectedImage(uncroppedImageUri);
            }
        });
    }

    public void registerActivityForPhotoCrop() {
        cropPhotoResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), cropResult -> {
            if (cropResult.getResultCode() == RESULT_OK && cropResult.getData() != null) {
                croppedImageUri = UCrop.getOutput(cropResult.getData());
                if (croppedImageUri != null) {
                    Picasso.get().load(croppedImageUri).into(updateProfileBinding.imageViewProfileUpdateProfile);
                    imageControl = true;
                }
            } else if (cropResult.getResultCode() == UCrop.RESULT_ERROR && cropResult.getData() != null) {
                Toast.makeText(this, UCrop.getError(cropResult.getData()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void cropSelectedImage(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped" + System.currentTimeMillis() + ".jpg"));
        Intent croppedIntent = UCrop.of(sourceUri, destinationUri).withAspectRatio(1, 1).getIntent(UpdateProfileActivity.this);
        cropPhotoResultLauncher.launch(croppedIntent);
    }

    public void shouldShowPermissionRationaleIfNeeded() {
        ArrayList<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissionsList) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                deniedPermissions.add(permission);
            }
        }
        if (!deniedPermissions.isEmpty()) {
            updateProfileBinding.mainUpdateProfile.post(() -> {
                new AlertDialog.Builder(this)
                        .setMessage("Please grant permissions to add a profile photo")
                        .setPositiveButton("OK", (dialog, which) ->
                                permissionsResultLauncher.launch(deniedPermissions.toArray(new String[0])))
                        .show();
            });
        } else {
            permissionsResultLauncher.launch(permissionsList.toArray(new String[0]));
        }
    }

    public boolean hasPermission() {
        for (String permission : permissionsList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
