package com.example.app_truyen.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app_truyen.API.CloudinaryResponse;
import com.example.app_truyen.API.CloudinaryService;
import com.example.app_truyen.API.RetrofitClient;
import com.example.app_truyen.Models.Chapter;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddChapterActivity extends AppCompatActivity {
    private EditText edtMaChuong, edtTenChuong;
    private FirebaseFirestore db;
    private String maTruyenGoc;
    private final ArrayList<Uri> dsUriAnh = new ArrayList<>();
    private final ArrayList<String> dsLinkAnhCloudinary = new ArrayList<>();
    private ProgressBar progressBar;
    private Button btnChonAnh, btnSave;
    private CloudinaryService cloudinaryService;

    private final ActivityResultLauncher<Intent> pickImgLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    dsUriAnh.clear();

                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            dsUriAnh.add(imageUri);
                        }
                    }
                    else if (result.getData().getData() != null) {
                        dsUriAnh.add(result.getData().getData());
                    }
                    if (dsUriAnh.isEmpty()) {
                        btnChonAnh.setText("Chọn Ảnh Chương");
                    } else {
                        String buttonText = "Đã chọn " + dsUriAnh.size() + " ảnh";
                        btnChonAnh.setText(buttonText);
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_item_chapter);

        db = FirebaseFirestore.getInstance();
        edtMaChuong = findViewById(R.id.edtMaChuong);
        edtTenChuong = findViewById(R.id.edtTenChuong);
        btnChonAnh = findViewById(R.id.btnChonAnhChuong);
        btnSave = findViewById(R.id.btnSaveChapter);
        Button btnCancel = findViewById(R.id.btnCancelChapter);
        TextView tvBack = findViewById(R.id.tvBack);
        progressBar = findViewById(R.id.progressBarChapter);

        cloudinaryService = RetrofitClient.getClient().create(CloudinaryService.class);
        maTruyenGoc = getIntent().getStringExtra("MA_TRUYEN");
        if (maTruyenGoc == null || maTruyenGoc.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy mã truyện!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnChonAnh.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImgLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> {
            if (dsUriAnh.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnChonAnh.setEnabled(false);
            dsLinkAnhCloudinary.clear();
            uploadImg(0);
        });

        tvBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
    }

    // Hàm đệ quy upload từng ảnh lên Cloudinary
    private void uploadImg(int index) {
        if (index >= dsUriAnh.size()) {
            saveToFirestore();
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
            String fileName = "chapter_" + System.currentTimeMillis() + "_" + index + ".jpg";
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", fileName, requestBody);

            String UPLOAD_PRESET = "upload-story"; // Đảm bảo đúng tên preset của bạn
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), UPLOAD_PRESET);

            // 3. Gọi API Upload
            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String url = response.body().getSecure_url();
                        dsLinkAnhCloudinary.add(url);
                        uploadImg(index + 1);
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

    // Hàm xử lý lỗi chung
    private void handleUploadError(String message) {
        progressBar.setVisibility(View.GONE);
        btnSave.setEnabled(true);
        btnChonAnh.setEnabled(true);
        Toast.makeText(AddChapterActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void saveToFirestore() {
        String maChuong = edtMaChuong.getText().toString().trim();
        String tenChuong = edtTenChuong.getText().toString().trim();
        if (maChuong.isEmpty()) {
            handleUploadError("Mã chương trống!");
            return;
        }

        Chapter chapter = new Chapter(maChuong, tenChuong, dsLinkAnhCloudinary);
        db.collection("Truyen").document(maTruyenGoc)
                .collection("chuong").document(maChuong)
                .set(chapter).addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddChapterActivity.this, "Thêm chương thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> handleUploadError("Lỗi Firestore: " + e.getMessage()));
    }
}