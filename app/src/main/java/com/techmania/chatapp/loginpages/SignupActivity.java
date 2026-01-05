package com.techmania.chatapp.loginpages;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;
import com.techmania.chatapp.databinding.ActivitySignupBinding;
import com.techmania.chatapp.models.User;
import com.techmania.chatapp.views.MainActivity;
import com.yalantis.ucrop.UCrop;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignupActivity extends AppCompatActivity {

    ActivitySignupBinding signupBinding;
    ActivityResultLauncher<String[]> permissionsResultLauncher;
    ActivityResultLauncher<Intent> photoPickerResultLauncher;
    ActivityResultLauncher<Intent> cropPhotoResultLauncher;
    Uri croppedImageUri;
    ArrayList<String> permissionsList = new ArrayList<>();
    int deniedPermissionCount = 0;

    FirebaseAuth auth = FirebaseAuth.getInstance();
    boolean imageControl = false;
    String userName, userEmail, userPassword, userUniqueId, profileImageUrl;
    FirebaseDatabase firebaseDatabase=FirebaseDatabase.getInstance ();
    DatabaseReference databaseReference=firebaseDatabase.getReference ().child ( "Users" );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        signupBinding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(signupBinding.getRoot());

        if (Build.VERSION.SDK_INT >= 33) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionsList.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        } else if (Build.VERSION.SDK_INT>32) {
            permissionsList.add(Manifest.permission.READ_MEDIA_IMAGES);
            
        } else {
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        registerActivityForMultiplePermission();
        registerActivityForPhotoPicker();
        registerActivityForPhotoCrop();

        signupBinding.imageViewProfileSignup.setOnClickListener(view -> {
            if (hasPermission()){
                openPhotoPicker();
            }
            else{
                shouldShowPermissionRationaleIfNeeded();
            }
        });

        signupBinding.buttonSignup.setOnClickListener(v -> createNewUser());
    }

    public void createNewUser() {
        userName = signupBinding.editTextUserNameSignup.getText().toString().trim();
        userEmail = signupBinding.editTextEmailSignup.getText().toString().trim();
        userPassword = signupBinding.editTextPasswordSignup.getText().toString().trim();

        if (userName.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(this, "Username, email, and password cannot be empty!", Toast.LENGTH_SHORT).show();
        } else {
            signupBinding.buttonSignup.setEnabled(false);
            signupBinding.progressBarSignup.setVisibility(View.VISIBLE);

            auth.createUserWithEmailAndPassword(userEmail, userPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    userUniqueId = auth.getCurrentUser().getUid();
                    if (imageControl && croppedImageUri != null) {
                        File imageFile = new File(croppedImageUri.getPath());
                        uploadPhotoToCloudinary(imageFile);
                    } else {
                        profileImageUrl = "null";
                        saveUserInfoToDatabase();
                    }
                } else {
                    Toast.makeText(this, task.getException().getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    signupBinding.buttonSignup.setEnabled(true);
                    signupBinding.progressBarSignup.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    public void uploadPhotoToCloudinary(File file) {
        String cloudName = "dktt70ypi";
        String uploadPreset = "chatapp_unsigned";

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, MediaType.parse("image/*")))
                .addFormDataPart("upload_preset", uploadPreset)
                .build();

        Request request = new Request.Builder()
                .url("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseBody);
                        profileImageUrl = jsonObject.getString("secure_url");

                        runOnUiThread(() -> {
                            Toast.makeText(SignupActivity.this, "Uploaded!", Toast.LENGTH_SHORT).show();
                            saveUserInfoToDatabase();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Server error", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(SignupActivity.this, "Parsing error", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    public void saveUserInfoToDatabase() {
        User user = new User(userUniqueId, userName, userEmail, profileImageUrl, "online");

        databaseReference.child(userUniqueId).setValue ( user ).addOnCompleteListener (  task->{
            if(task.isSuccessful ()){
                Toast.makeText ( this, "Your account has been successfully created.", Toast.LENGTH_SHORT ).show ( );
                Intent intent=new Intent ( SignupActivity.this, MainActivity.class );
                startActivity ( intent );
                signupBinding.buttonSignup.setEnabled ( true );
                signupBinding.progressBarSignup.setVisibility ( View.INVISIBLE );
                finish ();
            }else{
                Toast.makeText ( getApplicationContext (),task.getException ().getLocalizedMessage (),Toast.LENGTH_LONG ).show ();
            }

        } );
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
                    Picasso.get().load(croppedImageUri).into(signupBinding.imageViewProfileSignup);
                    imageControl = true;
                }
            } else if (cropResult.getResultCode() == UCrop.RESULT_ERROR && cropResult.getData() != null) {
                Toast.makeText(this, UCrop.getError(cropResult.getData()).getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void cropSelectedImage(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped" + System.currentTimeMillis()));
        Intent croppedIntent = UCrop.of(sourceUri, destinationUri).withAspectRatio(1, 1).getIntent(SignupActivity.this);
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
            signupBinding.mainSignup.post(() -> {
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