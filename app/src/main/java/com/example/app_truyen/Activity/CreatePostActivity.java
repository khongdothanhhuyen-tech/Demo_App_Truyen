package com.example.app_truyen.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.app_truyen.API.CloudinaryResponse;
import com.example.app_truyen.API.CloudinaryService;
import com.example.app_truyen.API.RetrofitClient;
import com.example.app_truyen.Models.Post;
import com.example.app_truyen.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {
    private EditText edtContent;
    private ImageView imgPreview, imgUserAvt;
    private TextView tvUserName;
    private ProgressBar progressBar;
    private Uri selectedImageUri;
    private String currentUserId, currentUserName, currentUserAvatarUrl = "";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CloudinaryService cloudinaryService;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imgPreview.setVisibility(View.VISIBLE);
                    imgPreview.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        cloudinaryService = RetrofitClient.getClient().create(CloudinaryService.class);
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews();
        fetchUserInfo();

        findViewById(R.id.imgBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPickImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        findViewById(R.id.btnPost).setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (content.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show();
                return;
            }

            findViewById(R.id.btnPost).setEnabled(false);

            if (selectedImageUri != null) {
                uploadToCloudinary(content);
            } else {
                savePostToFirestore(content, "");
            }
        });
    }

    private void initViews() {
        edtContent = findViewById(R.id.edtContent);
        imgPreview = findViewById(R.id.imgPreview);
        imgUserAvt = findViewById(R.id.imgUserAvt);
        tvUserName = findViewById(R.id.tvUserName);
        progressBar = findViewById(R.id.progressBar);
    }

    private void fetchUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            currentUserName = user.getEmail().split("@")[0];
            tvUserName.setText(currentUserName);
        } else {
            currentUserName = "Đạo hữu";
            tvUserName.setText(currentUserName);
        }

        db.collection("TaiKhoan").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentUserAvatarUrl = doc.getString("avatarUrl");
                if (currentUserAvatarUrl != null && !currentUserAvatarUrl.isEmpty()) {
                    Glide.with(this).load(currentUserAvatarUrl).circleCrop().into(imgUserAvt);
                }
            }
        });
    }

    //Hàm upload ảnh lên Cloudinary
    private void uploadToCloudinary(String content) {
        try {
            RequestBody requestBody = new RequestBody() {
                @Nullable
                @Override
                public MediaType contentType() {
                    return MediaType.parse("image/*");
                }

                @Override
                public long contentLength() {
                    try (InputStream inputStream = getContentResolver().openInputStream(selectedImageUri)) {
                        return inputStream != null ? inputStream.available() : -1;
                    } catch (IOException e) {
                        return -1;
                    }
                }

                @Override
                public void writeTo(@NonNull BufferedSink sink) throws IOException {
                    try (InputStream inputStream = getContentResolver().openInputStream(selectedImageUri)) {
                        if (inputStream != null) {
                            sink.writeAll(Okio.source(inputStream));
                        }
                    }
                }
            };

            String fileName = "post_" + System.currentTimeMillis() + ".jpg";
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestBody);
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), "upload-story");

            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        savePostToFirestore(content, response.body().getSecure_url());
                    } else {
                        handleError("Lỗi Cloudinary từ chối ảnh!");
                    }
                }
                @Override
                public void onFailure(@NonNull Call<CloudinaryResponse> call, @NonNull Throwable t) {
                    handleError("Lỗi kết nối mạng: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            handleError("Lỗi chuẩn bị file ảnh: " + e.getMessage());
        }
    }

    private void handleError(String msg) {
        progressBar.setVisibility(View.GONE);
        findViewById(R.id.btnPost).setEnabled(true);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void savePostToFirestore(String content, String imageUrl) {
        if (currentUserName == null || currentUserName.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            currentUserName = (user != null && user.getEmail() != null) ? user.getEmail().split("@")[0] : "Đạo hữu";
        }

        String postId = UUID.randomUUID().toString();
        Post post = new Post(postId, currentUserId, currentUserName, currentUserAvatarUrl, content, imageUrl, Timestamp.now());
        db.collection("DienDan").document(postId).set(post).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Đã đăng bài!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}