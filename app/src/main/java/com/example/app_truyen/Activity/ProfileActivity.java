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
import androidx.annotation.Nullable;
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

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
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
    private ImageView imgPet;
    private TextView tvStreak, tvExp;
    private ProgressBar pbExp;
    private Button btnFeedPet;
    private int currentExp = 0;
    private int currentStreak = 0;
    private int petLevel = 1;
    private String lastCheckIn = "";
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
        initPetUI();
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
            // 1. Tạo RequestBody
            RequestBody requestBody = new RequestBody() {
                @Nullable
                @Override
                public MediaType contentType() {
                    return MediaType.parse("image/*");
                }

                @Override
                public long contentLength() {
                    try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                        return inputStream != null ? inputStream.available() : -1;
                    } catch (IOException e) {
                        return -1;
                    }
                }

                @Override
                public void writeTo(@NonNull BufferedSink sink) throws IOException {
                    try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                        if (inputStream != null) {
                            sink.writeAll(Okio.source(inputStream));
                        }
                    }
                }
            };

            // 2. Tạo MultipartBody
            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestBody);

            String UPLOAD_PRESET = "upload-story";
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), UPLOAD_PRESET);

            // 3. Gọi API
            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String avtUrl = response.body().getSecure_url();
                        updateAvatarInFirestore(avtUrl);
                    } else {
                        setLoadingState(false);
                        Toast.makeText(ProfileActivity.this, "Lỗi upload ảnh: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<CloudinaryResponse> call, @NonNull Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(ProfileActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            setLoadingState(false);
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm cập nhật Link ảnh vào Firestore
    private void updateAvatarInFirestore(String url) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
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
    private void initPetUI() {
        imgPet = findViewById(R.id.imgPet);
        tvStreak = findViewById(R.id.tvStreak);
        tvExp = findViewById(R.id.tvExp);
        pbExp = findViewById(R.id.pbExp);
        btnFeedPet = findViewById(R.id.btnFeedPet);
        btnFeedPet.setOnClickListener(v -> feedPet());
        loadPetData();
    }

    private void updatePetUI() {
        // Tính toán cấp độ linh thú
        if (currentExp >= 1000) {
            petLevel = 3;
            pbExp.setMax(5000);
            imgPet.setImageResource(R.drawable.dragon); // Rồng lớn
            tvExp.setText("Cấp 3: Thần Thú (" + currentExp + "/5000 EXP)");
        } else if (currentExp >= 300) {
            petLevel = 2;
            pbExp.setMax(1000);
            imgPet.setImageResource(R.drawable.baby_dragon); // Rồng con
            tvExp.setText("Cấp 2: Linh Thú Nhỏ (" + currentExp + "/1000 EXP)");
        } else {
            petLevel = 1;
            pbExp.setMax(300);
            imgPet.setImageResource(R.drawable.egg); // Quả trứng
            tvExp.setText("Cấp 1: Đang ấp trứng (" + currentExp + "/300 EXP)");
        }

        pbExp.setProgress(currentExp);
        tvStreak.setText("🔥 Đang có chuỗi " + currentStreak + " ngày!");

        String today = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        if (today.equals(lastCheckIn)) {
            btnFeedPet.setEnabled(false);
            btnFeedPet.setText("Đã cho ăn hôm nay");
            btnFeedPet.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray_text)));
        } else {
            btnFeedPet.setEnabled(true);
            btnFeedPet.setText("Cho ăn (+10 EXP)");
            btnFeedPet.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.orange)));
        }
    }


    // 1. Hàm tính khoảng cách ngày
    private long getDaysDifference(String lastDateStr, String todayStr) {
        if (lastDateStr == null || lastDateStr.isEmpty()) return -1;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        try {
            java.util.Date lastDate = sdf.parse(lastDateStr);
            java.util.Date todayDate = sdf.parse(todayStr);
            long diffInMillis = todayDate.getTime() - lastDate.getTime();
            return diffInMillis / (1000 * 60 * 60 * 24);
        } catch (Exception e) {
            return -1;
        }
    }

    // 2. Hàm tải dữ liệu
    private void loadPetData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("TaiKhoan").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentExp = doc.getLong("exp") != null ? doc.getLong("exp").intValue() : 0;
                        currentStreak = doc.getLong("streak") != null ? doc.getLong("streak").intValue() : 0;
                        lastCheckIn = doc.getString("lastCheckIn") != null ? doc.getString("lastCheckIn") : "";

                        String today = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                        long daysDiff = getDaysDifference(lastCheckIn, today);

                        if (daysDiff > 1) {
                            currentStreak = 0;
                            currentExp = 0;

                            db.collection("TaiKhoan").document(user.getUid())
                                    .update("exp", 0, "streak", 0);
                        }

                        updatePetUI();
                    }
                });
    }

    // 3. Hàm Cho ăn (Điểm danh)
    private void feedPet() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String today = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());

        if (!today.equals(lastCheckIn)) {
            long daysDiff = getDaysDifference(lastCheckIn, today);

            if (daysDiff == 1) {
                currentStreak++;
            } else {
                currentStreak = 1;
                currentExp = 0;
            }
            currentExp += 10;
            lastCheckIn = today;

            db.collection("TaiKhoan").document(user.getUid())
                    .update("exp", currentExp, "streak", currentStreak, "lastCheckIn", today)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cho ăn! +10 EXP", Toast.LENGTH_SHORT).show();
                        updatePetUI();
                    });
        }
    }
}