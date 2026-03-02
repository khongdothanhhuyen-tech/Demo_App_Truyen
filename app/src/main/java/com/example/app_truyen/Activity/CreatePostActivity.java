package com.example.app_truyen.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.List;
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
    private ImageView imgUserAvt;
    private TextView tvUserName;
    private ProgressBar progressBar;
    private LinearLayout layoutSelectedImages; // KHUNG CHỨA ẢNH MỚI

    private List<Uri> dsUriAnh = new ArrayList<>(); // Danh sách Uri theo chuẩn của bạn
    private List<String> dsLinkAnhCloudinary = new ArrayList<>(); // Danh sách Link

    private String currentUserId, currentUserName, currentUserAvatarUrl = "";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CloudinaryService cloudinaryService;

    // LAUNCHER CHỌN NHIỀU ẢNH
    private final ActivityResultLauncher<Intent> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    dsUriAnh.clear();
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            dsUriAnh.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        dsUriAnh.add(result.getData().getData());
                    }
                    // HIỂN THỊ ẢNH RA MÀN HÌNH NGAY SAU KHI CHỌN
                    displaySelectedImages();
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
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Cho phép chọn nhiều
            pickImagesLauncher.launch(Intent.createChooser(intent, "Chọn ảnh"));
        });

        findViewById(R.id.btnPost).setOnClickListener(v -> {
            String content = edtContent.getText().toString().trim();
            if (content.isEmpty() && dsUriAnh.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung hoặc chọn ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }

            findViewById(R.id.btnPost).setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);

            if (!dsUriAnh.isEmpty()) {
                dsLinkAnhCloudinary.clear();
                uploadImg(content, 0); // GỌI HÀM CỦA ĐẠO HỮU BẮT ĐẦU TỪ INDEX 0
            } else {
                saveToFirestore(content, new ArrayList<>());
            }
        });
    }

    private void initViews() {
        edtContent = findViewById(R.id.edtContent);
        imgUserAvt = findViewById(R.id.imgUserAvt);
        tvUserName = findViewById(R.id.tvUserName);
        progressBar = findViewById(R.id.progressBar);
        layoutSelectedImages = findViewById(R.id.layoutSelectedImages); // Ánh xạ layout chứa ảnh
    }

    // HÀM VẼ ẢNH RA MÀN HÌNH CHỜ (PREVIEW)
    private void displaySelectedImages() {
        layoutSelectedImages.removeAllViews();
        for (Uri uri : dsUriAnh) {
            ImageView imageView = new ImageView(this);
            // Thiết lập kích thước 110dp cho mỗi ảnh vuông
            int size = (int) (110 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, 0, (int) (10 * getResources().getDisplayMetrics().density), 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(this).load(uri).into(imageView);
            layoutSelectedImages.addView(imageView);
        }
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

    // HÀM UPLOAD ẢNH THEO CHUẨN OKIO CỦA ĐẠO HỮU GỢI Ý
    private void uploadImg(String content, int index) {
        if (index >= dsUriAnh.size()) {
            saveToFirestore(content, dsLinkAnhCloudinary);
            return;
        }
        try {
            Uri imageUri = dsUriAnh.get(index);
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
            String fileName = "post_" + System.currentTimeMillis() + "_" + index + ".jpg";
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestBody);

            String UPLOAD_PRESET = "upload-story";
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), UPLOAD_PRESET);

            // 3. Gọi API Upload
            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String url = response.body().getSecure_url();
                        dsLinkAnhCloudinary.add(url);
                        uploadImg(content, index + 1); // Tiếp tục đệ quy
                    } else {
                        handleUploadError("Lỗi Cloudinary: " + response.message());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CloudinaryResponse> call, @NonNull Throwable t) {
                    handleUploadError("Lỗi mạng: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            handleUploadError("Lỗi xử lý file: " + e.getMessage());
        }
    }

    private void handleUploadError(String msg) {
        progressBar.setVisibility(View.GONE);
        findViewById(R.id.btnPost).setEnabled(true);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void saveToFirestore(String content, List<String> imageUrls) {
        if (currentUserName == null || currentUserName.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            currentUserName = (user != null && user.getEmail() != null) ? user.getEmail().split("@")[0] : "Đạo hữu";
        }

        String postId = UUID.randomUUID().toString();
        Post post = new Post(postId, currentUserId, currentUserName, currentUserAvatarUrl, content, imageUrls, Timestamp.now());

        db.collection("DienDan").document(postId).set(post).addOnSuccessListener(aVoid -> {
            addExpForAction("lastPosted");
            Toast.makeText(this, "Đã đăng bài thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    private void addExpForAction(String actionField) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        String today = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        FirebaseFirestore.getInstance().collection("TaiKhoan").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String lastAction = doc.getString(actionField);
                // Nếu hôm nay chưa làm hành động này thì cộng điểm
                if (lastAction == null || !lastAction.equals(today)) {
                    int exp = doc.getLong("exp") != null ? doc.getLong("exp").intValue() : 0;
                    FirebaseFirestore.getInstance().collection("TaiKhoan").document(uid)
                            .update("exp", exp + 10, actionField, today);
                }
            }
        });
    }
}