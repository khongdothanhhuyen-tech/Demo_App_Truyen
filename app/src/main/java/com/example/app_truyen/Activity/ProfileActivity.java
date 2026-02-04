package com.example.app_truyen.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.API.CloudinaryResponse;
import com.example.app_truyen.API.CloudinaryService;
import com.example.app_truyen.API.RetrofitClient;
import com.example.app_truyen.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvEmail;
    private ImageView imgProfile;
    private BottomNavigationView bottomNavigationView;
    private MaterialCardView cardAvatar;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CloudinaryService cloudinaryService;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imgUri = result.getData().getData();
                    uploadAvatarToCloudinary(imgUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        cloudinaryService = RetrofitClient.getClient().create(CloudinaryService.class);

        tvEmail = findViewById(R.id.tvEmail);
        imgProfile = findViewById(R.id.imgProfile);
        Button btnLogOut = findViewById(R.id.btnLogOut);
        cardAvatar = findViewById(R.id.cardAvatar);
        bottomNavigationView = findViewById(R.id.nav);
        progressBar = findViewById(R.id.progressBar);

        btnLogOut.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginAltActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        cardAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });
        loadUserProfile();
        setupNavigation();
    }

    // Hàm quản lý trạng thái tải
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            cardAvatar.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            cardAvatar.setEnabled(true);
        }
    }

    // Hàm tải ảnh đại diện và email
    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());
            db.collection("TaiKhoan").document(user.getUid())
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String avatarUrl = documentSnapshot.getString("avatarUrl");
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(this).load(avatarUrl).into(imgProfile);
                            }
                        }
                    });
        }
    }

    // Tải ảnh lên Cloudinary
    private void uploadAvatarToCloudinary(Uri imageUri) {
        setLoadingState(true);
        try {
            String tempFileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getCacheDir(), tempFileName);

            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while (true) {
                assert inputStream != null;
                if (!((len = inputStream.read(buffer)) > 0)) break;
                outputStream.write(buffer, 0, len);
            }
            outputStream.close();
            inputStream.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
            String UPLOAD_PRESET = "upload-story";
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), UPLOAD_PRESET);

            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String avtUrl = response.body().getSecure_url();
                        updateAvatarInFirestore(avtUrl);
                    } else {
                        setLoadingState(false);
                        Toast.makeText(ProfileActivity.this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<CloudinaryResponse> call, @NonNull Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(ProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            setLoadingState(false);
            e.printStackTrace();
            Toast.makeText(this, "Không đọc được file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm cập nhật Link ảnh vào Firestore
    private void updateAvatarInFirestore(String url) {
        String uid = auth.getCurrentUser().getUid();
        db.collection("TaiKhoan").document(uid)
                .update("avatarUrl", url)
                .addOnSuccessListener(aVoid -> {
                    setLoadingState(false);
                    Toast.makeText(ProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    Glide.with(this).load(url).into(imgProfile);
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(ProfileActivity.this, "Lỗi lưu vào Database", Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm xử lý thanh điều hướng
    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intentHome = new Intent(ProfileActivity.this, HomeActivity.class);
                startActivity(intentHome);
                finish();
                return true;
            }
            else if (id == R.id.nav_comic) {
                Intent intentComic = new Intent(ProfileActivity.this, ComicActivity.class);
                startActivity(intentComic);
                finish();
                return true;
            }
            else if (id == R.id.nav_library) {
                Intent intentComic = new Intent(ProfileActivity.this, LibraryActivity.class);
                startActivity(intentComic);
                finish();
                return true;
            }
            else if (id == R.id.nav_profile) {
                NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
                if (scrollView != null) {
                    scrollView.smoothScrollTo(0, 0);
                }
                return true;
            }
            return false;
        });
    }
}
