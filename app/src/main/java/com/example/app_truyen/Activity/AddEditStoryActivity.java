package com.example.app_truyen.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/// /
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class AddEditStoryActivity extends AppCompatActivity {

    private final String[] listTheLoai = {"Hành động", "Tình cảm", "Học đường", "Phiêu lưu", "Sát thủ", "Kinh dị", "Hài hước", "Viễn tưởng", "Khoa học viễn tưởng", "Siêu anh hùng"};
    private boolean[] checkedTheLoai;
    private final ArrayList<String> userSelectedTheLoai = new ArrayList<>();
    TextView tvBack, tvHistory;
    EditText edtMaTruyen, edtTenTruyen, edtTheLoai, edtMoTa, edtTacGia;
    Button btnChonAnh, btnSave, btnCancel;
    FirebaseFirestore db;
    CloudinaryService cloudinaryService;
    private ProgressBar progressBar;
    private Story truyenCu;
    private boolean isEditMode = false;
    private Uri selectedImageUri;
    CheckBox cbAllowComment;

    private boolean oldAllowComment = true;


    // Launcher chọn ảnh
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Toast.makeText(this, "Đã chọn ảnh!", Toast.LENGTH_SHORT).show();
                    btnChonAnh.setText("Đã Chọn 1 Ảnh");
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.option_edit);

        tvBack = findViewById(R.id.tvBack);
        edtMaTruyen = findViewById(R.id.edtMaTruyen);
        edtTenTruyen = findViewById(R.id.edtTenTruyen);
        edtTheLoai = findViewById(R.id.edtTheLoai);
        edtMoTa = findViewById(R.id.edtMoTa);
        edtTacGia = findViewById(R.id.edtTacGia);
        btnChonAnh = findViewById(R.id.btnChonAnh);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
        db = FirebaseFirestore.getInstance();
        cloudinaryService = RetrofitClient.getClient().create(CloudinaryService.class);
        checkedTheLoai = new boolean[listTheLoai.length];

        ///
        cbAllowComment = findViewById(R.id.cbAllowComment);
        ///
        tvHistory = findViewById(R.id.tvHistory);
        tvHistory.setOnClickListener(v -> {

            if (isEditMode && truyenCu != null) {

                Intent intent = new Intent(this, StoryHistoryActivity.class);
                intent.putExtra("MA_TRUYEN", truyenCu.getMaTruyen());
                startActivity(intent);

            } else {
                Toast.makeText(this,
                        "Phải lưu truyện trước khi xem lịch sử",
                        Toast.LENGTH_SHORT).show();
            }

        });

        edtTheLoai.setFocusable(false);
        edtTheLoai.setClickable(true);

        edtTheLoai.setOnClickListener(v -> showGenreDialog());

        tvBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnChonAnh.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> {
            setLoadingState(true);
            save();
        });
        checkMode();
        setupUI();
    }

    // Hàm bật/tắt trạng thái tải
    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnChonAnh.setEnabled(false);
            btnCancel.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnChonAnh.setEnabled(true);
            btnCancel.setEnabled(true);
        }
    }

    // Hàm kiểm tra trạng thái Sửa hay Thêm
    private void checkMode() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("TRUYEN_DATA")) {
            isEditMode = true;
            truyenCu = (Story) intent.getSerializableExtra("TRUYEN_DATA");
        } else {
            isEditMode = false;
            truyenCu = null;
        }
    }
    // Hàm hiển thị Sửa hay Thêm
    private void setupUI() {
        if (isEditMode) {
            btnSave.setText("Cập Nhật Truyện");
            edtMaTruyen.setText(truyenCu.getMaTruyen());
            edtMaTruyen.setEnabled(false);

            /// /
            FirebaseFirestore.getInstance()
                    .collection("StorySettings")
                    .document(truyenCu.getMaTruyen())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean allow = documentSnapshot.getBoolean("allowComment");
                            if (allow != null) {
                                oldAllowComment = allow;
                                cbAllowComment.setChecked(allow);
                            }
                        } else {
                            cbAllowComment.setChecked(true); // mặc định bật comment
                        }
                    });

            edtTenTruyen.setText(truyenCu.getTenTruyen());

            if (truyenCu.getTheLoai() != null) {
                userSelectedTheLoai.clear();
                userSelectedTheLoai.addAll(truyenCu.getTheLoai());

                for (int i = 0; i < listTheLoai.length; i++) {
                    checkedTheLoai[i] = userSelectedTheLoai.contains(listTheLoai[i]);
                }
                edtTheLoai.setText(String.join(", ", userSelectedTheLoai));
            }

            edtMoTa.setText(truyenCu.getMoTa());
            edtTacGia.setText(truyenCu.getTacGia());
            btnChonAnh.setText("Chọn Ảnh Mới ");
        } else {
            tvHistory.setVisibility(View.GONE);
            btnSave.setText("Lưu Truyện Mới");
            edtMaTruyen.setEnabled(true);
        }
    }
    // Hàm hiển thị dialog chọn thể loại
    private void showGenreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn thể loại");

        builder.setMultiChoiceItems(listTheLoai, checkedTheLoai, (dialog, position, isChecked) -> {
            if (isChecked) {
                if (!userSelectedTheLoai.contains(listTheLoai[position])) {
                    userSelectedTheLoai.add(listTheLoai[position]);
                }
            } else {
                userSelectedTheLoai.remove(listTheLoai[position]);
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> edtTheLoai.setText(String.join(", ", userSelectedTheLoai)));
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    // Hàm lưu
    private void save() {
        if (selectedImageUri != null) {
            uploadImageToCloudinary(selectedImageUri);
        } else {
            String imageUrl = (isEditMode && truyenCu != null) ? truyenCu.getAnhBiaUrl() : "";
            saveDataToFirestore(imageUrl);
        }
    }
    // Hàm upload ảnh lên Cloudinary
    private void uploadImageToCloudinary(Uri imageUri) {
        setLoadingState(true);
        try {
            RequestBody requestBody = new RequestBody() {
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
                public void writeTo(@NonNull okio.BufferedSink sink) throws IOException {
                    try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                        if (inputStream != null) {
                            sink.writeAll(okio.Okio.source(inputStream));
                        }
                    }
                }
            };

            // 2. Tạo MultipartBody
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "upload_" + System.currentTimeMillis() + ".jpg", requestBody);

            String UPLOAD_PRESET = "upload-story";
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), UPLOAD_PRESET);

            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String imageUrl = response.body().getSecure_url();
                        saveDataToFirestore(imageUrl);
                    } else {
                        setLoadingState(false);
                        Toast.makeText(AddEditStoryActivity.this, "Lỗi Cloudinary: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CloudinaryResponse> call, @NonNull Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(AddEditStoryActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            setLoadingState(false);
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo file upload!", Toast.LENGTH_SHORT).show();
        }
    }
    // Hàm lưu dữ liệu vào Firestore
    private void saveDataToFirestore(String imgUrl) {
        String maTruyen = edtMaTruyen.getText().toString().trim();
        String tenTruyen = edtTenTruyen.getText().toString().trim();
        String moTa = edtMoTa.getText().toString().trim();
        String tacGia = edtTacGia.getText().toString().trim();

        if (maTruyen.isEmpty()) {
            setLoadingState(false);
            Toast.makeText(this, "Mã truyện không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userSelectedTheLoai.isEmpty()) {
            setLoadingState(false);
            Toast.makeText(this, "Bạn phải chọn ít nhất 1 thể loại!", Toast.LENGTH_SHORT).show();
            return;
        }

        Story truyenMoi = new Story(maTruyen, tenTruyen, userSelectedTheLoai, tacGia, moTa, imgUrl);
        ///
        saveEditHistory(maTruyen, truyenMoi);
        db.collection("Truyen").document(maTruyen)
                .set(truyenMoi).addOnSuccessListener(aVoid -> {

                    // TẠI ĐÂY
                    FirebaseFirestore.getInstance()
                            .collection("StorySettings")
                            .document(maTruyen)
                            .set(java.util.Collections.singletonMap(
                                    "allowComment",
                                    cbAllowComment.isChecked()
                            ));

                    setLoadingState(false);
                    Toast.makeText(AddEditStoryActivity.this, "Lưu thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false); // Tắt trạng thái tải
                    Toast.makeText(AddEditStoryActivity.this, "Lỗi khi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveEditHistory(String maTruyen, Story newStory) {

        if (!isEditMode || truyenCu == null) return;

        Map<String, Object> changes = new HashMap<>();

        // So sánh ảnh bìa
        if (!truyenCu.getAnhBiaUrl().equals(newStory.getAnhBiaUrl())) {

            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getAnhBiaUrl());
            change.put("new", newStory.getAnhBiaUrl());

            changes.put("anhBiaUrl", change);
        }

        // So sánh tên truyện
        if (!truyenCu.getTenTruyen().equals(newStory.getTenTruyen())) {
            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getTenTruyen());
            change.put("new", newStory.getTenTruyen());
            changes.put("tenTruyen", change);
        }

        // So sánh tác giả
        if (!truyenCu.getTacGia().equals(newStory.getTacGia())) {
            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getTacGia());
            change.put("new", newStory.getTacGia());
            changes.put("tacGia", change);
        }

        // So sánh mô tả
        if (!truyenCu.getMoTa().equals(newStory.getMoTa())) {
            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getMoTa());
            change.put("new", newStory.getMoTa());
            changes.put("moTa", change);
        }

        // So sánh thể loại
        if (!truyenCu.getTheLoai().equals(newStory.getTheLoai())) {
            Map<String, Object> change = new HashMap<>();
            change.put("old", truyenCu.getTheLoai());
            change.put("new", newStory.getTheLoai());
            changes.put("theLoai", change);
        }

        // So sánh allowComment
        boolean newAllowComment = cbAllowComment.isChecked();
        if (oldAllowComment != newAllowComment) {

            Map<String, String> change = new HashMap<>();
            change.put("old", oldAllowComment ? "Bật" : "Tắt");
            change.put("new", newAllowComment ? "Bật" : "Tắt");

            changes.put("allowComment", change);
        }

        // Nếu không có thay đổi thì không lưu
        if (changes.isEmpty()) return;

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("editedAt", Timestamp.now());
        historyData.put("editedBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
        historyData.put("changes", changes);

        FirebaseFirestore.getInstance()
                .collection("StoryEditHistory")
                .document(maTruyen)
                .collection("logs")
                .add(historyData);
    }


}
