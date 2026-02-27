package com.example.app_truyen.Activity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

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

    ///
    private Uri selectedPdfUri = null;
    private Button btnChonPdf;
    ///
    private long selectedPublishTime = 0;
    private Button btnChonNgay;


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

                        selectedPdfUri = null; // THÊM
                        btnChonPdf.setEnabled(false); // THÊM
                    }
                }
            });
    private final ActivityResultLauncher<Intent> pickPdfLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedPdfUri = result.getData().getData();
                            btnChonPdf.setText("Đã chọn PDF");
                            dsUriAnh.clear(); // Nếu chọn PDF thì bỏ ảnh
                            btnChonAnh.setText("Chọn Ảnh Chương");
                            btnChonAnh.setEnabled(false); //
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

        ///
        btnChonPdf = findViewById(R.id.btnChonPdf);
        btnChonPdf.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            pickPdfLauncher.launch(intent);
        });
        ///
        btnChonNgay = findViewById(R.id.btnChonNgay);
        btnChonNgay.setOnClickListener(v -> showDateTimePicker());

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
            btnChonNgay.setEnabled(false);

            if (dsUriAnh.isEmpty() && selectedPdfUri == null) {
                Toast.makeText(this, "Chọn ảnh hoặc PDF!", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnChonAnh.setEnabled(false);
            btnChonPdf.setEnabled(false);

            if (selectedPdfUri != null) {
                uploadPdfToCloudinary(selectedPdfUri);
            } else {
                dsLinkAnhCloudinary.clear();
                uploadImg(0);
            }
        });

        tvBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
    }

    // Hàm đệ quy upload từng ảnh lên Cloudinary
    private void uploadImg(int index) {
        if (index >= dsUriAnh.size()) {
            saveToFirestore(); // giữ nguyên vì nó lưu ảnh();
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
        btnChonPdf.setEnabled(true);
        btnChonNgay.setEnabled(true);
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

        ///
        if (selectedPublishTime == 0) {
            selectedPublishTime = System.currentTimeMillis();
        }
        chapter.setPublishTime(selectedPublishTime);

        db.collection("Truyen").document(maTruyenGoc)
                .collection("chuong").document(maChuong)
                .set(chapter).addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddChapterActivity.this, "Thêm chương thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> handleUploadError("Lỗi Firestore: " + e.getMessage()));
    }

    private void uploadPdfToCloudinary(Uri pdfUri) {

        try {
            String tempFileName = "chapter_pdf_" + System.currentTimeMillis() + ".pdf";
            File file = new File(getCacheDir(), tempFileName);

            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }

            outputStream.close();
            inputStream.close();

            RequestBody requestFile =
                    RequestBody.create(MediaType.parse("application/pdf"), file);

            MultipartBody.Part body =
                    MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            String UPLOAD_PRESET = "upload-story";
            RequestBody uploadPreset =
                    RequestBody.create(MediaType.parse("text/plain"), UPLOAD_PRESET);

            cloudinaryService.uploadPdf(uploadPreset, body)
                    .enqueue(new Callback<CloudinaryResponse>() {

                        @Override
                        public void onResponse(Call<CloudinaryResponse> call,
                                               Response<CloudinaryResponse> response) {

                            if (response.isSuccessful() && response.body() != null) {

                                String pdfUrl = response.body().getSecure_url();
                                savePdfToFirestore(pdfUrl);

                            } else {
                                showError("Upload PDF lỗi");
                            }
                        }

                        @Override
                        public void onFailure(Call<CloudinaryResponse> call, Throwable t) {
                            showError("Lỗi mạng: " + t.getMessage());
                        }
                    });

        } catch (IOException e) {
            showError("Không đọc được file PDF");
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        findViewById(R.id.btnSaveChapter).setEnabled(true);
        findViewById(R.id.btnChonAnhChuong).setEnabled(true);
        btnChonPdf.setEnabled(true);
        btnChonNgay.setEnabled(true);

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void savePdfToFirestore(String pdfUrl) {

        String maChuong = edtMaChuong.getText().toString().trim();
        String tenChuong = edtTenChuong.getText().toString().trim();

        if (maChuong.isEmpty()) {
            showError("Mã chương trống!");
            return;
        }

        Chapter chapter = new Chapter(maChuong, tenChuong, pdfUrl);
        if (selectedPublishTime == 0) {
            selectedPublishTime = System.currentTimeMillis();
        }

        chapter.setPublishTime(selectedPublishTime);

        db.collection("Truyen").document(maTruyenGoc)
                .collection("chuong").document(maChuong)
                .set(chapter)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Thêm chương PDF thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void showDateTimePicker() {

        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {

                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (timeView, hour, minute) -> {

                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);

                                selectedPublishTime = calendar.getTimeInMillis();

                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                                String formattedDate = sdf.format(calendar.getTime());
                                btnChonNgay.setText(formattedDate);

                                Toast.makeText(this, "Đã chọn thời gian đăng", Toast.LENGTH_SHORT).show();

                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true);

                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }
}