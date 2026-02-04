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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
    private Button btnChonAnh;
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
        Button btnSave = findViewById(R.id.btnSaveChapter);
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

    // Hàm đệ quy upload từng ảnh
    private void uploadImg(int index) {
        if (index >= dsUriAnh.size()) {
            saveToFirestore();
            return;
        }

        try {
            Uri imageUri = dsUriAnh.get(index);
            String tempFileName = "chapter_img_" + System.currentTimeMillis() + ".jpg";
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

            // Gọi API Upload
            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String url = response.body().getSecure_url();
                        dsLinkAnhCloudinary.add(url);
                        uploadImg(index + 1);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        findViewById(R.id.btnSaveChapter).setEnabled(true);
                        findViewById(R.id.btnChonAnhChuong).setEnabled(true);
                        Toast.makeText(AddChapterActivity.this, "Lỗi Cloudinary: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CloudinaryResponse> call, @NonNull Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnSaveChapter).setEnabled(true);
                    findViewById(R.id.btnChonAnhChuong).setEnabled(true);
                    Toast.makeText(AddChapterActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            findViewById(R.id.btnSaveChapter).setEnabled(true);
            findViewById(R.id.btnChonAnhChuong).setEnabled(true);
            e.printStackTrace();
            Toast.makeText(this, "Không đọc được file ảnh số " + (index + 1), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToFirestore() {
        String maChuong = edtMaChuong.getText().toString().trim();
        String tenChuong = edtTenChuong.getText().toString().trim();
        if (maChuong.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            findViewById(R.id.btnSaveChapter).setEnabled(true);
            findViewById(R.id.btnChonAnhChuong).setEnabled(true);
            Toast.makeText(this, "Mã chương trống!", Toast.LENGTH_SHORT).show();
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
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.btnSaveChapter).setEnabled(true);
                    findViewById(R.id.btnChonAnhChuong).setEnabled(true);
                    Toast.makeText(AddChapterActivity.this, "Lỗi Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
