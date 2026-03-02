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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;

public class AddEditStoryActivity extends AppCompatActivity {

    private String[] listTheLoai = new String[0];
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

        cbAllowComment = findViewById(R.id.cbAllowComment);

        tvHistory = findViewById(R.id.tvHistory);
        tvHistory.setOnClickListener(v -> {
            if (isEditMode && truyenCu != null) {
                Intent intent = new Intent(this, StoryHistoryActivity.class);
                intent.putExtra("MA_TRUYEN", truyenCu.getMaTruyen());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Phải lưu truyện trước khi xem lịch sử", Toast.LENGTH_SHORT).show();
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

        fetchGenresFromFirestore();
    }

    private void fetchGenresFromFirestore() {
        db.collection("TheLoai").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> tempGenres = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String genre = doc.getString("tenTheLoai");
                        if (genre == null || genre.isEmpty()) genre = doc.getString("ten");
                        if (genre == null || genre.isEmpty()) genre = doc.getString("theLoai");

                        if (genre == null || genre.isEmpty()) genre = doc.getId();

                        tempGenres.add(genre);
                    }

                    listTheLoai = tempGenres.toArray(new String[0]);
                    checkedTheLoai = new boolean[listTheLoai.length];
                    mapSelectedGenres();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải danh sách thể loại!", Toast.LENGTH_SHORT).show();
                });
    }

    private void mapSelectedGenres() {
        if (isEditMode && truyenCu != null && truyenCu.getTheLoai() != null) {
            userSelectedTheLoai.clear();
            userSelectedTheLoai.addAll(truyenCu.getTheLoai());

            for (int i = 0; i < listTheLoai.length; i++) {
                checkedTheLoai[i] = userSelectedTheLoai.contains(listTheLoai[i]);
            }
        }
    }

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

    private void setupUI() {
        if (isEditMode) {
            btnSave.setText("Cập Nhật Truyện");
            edtMaTruyen.setText(truyenCu.getMaTruyen());
            edtMaTruyen.setEnabled(false); // Khoá ID truyện

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
                            cbAllowComment.setChecked(true);
                        }
                    });

            edtTenTruyen.setText(truyenCu.getTenTruyen());

            if (truyenCu.getTheLoai() != null) {
                edtTheLoai.setText(String.join(", ", truyenCu.getTheLoai()));
            }

            edtMoTa.setText(truyenCu.getMoTa());
            edtTacGia.setText(truyenCu.getTacGia());
            btnChonAnh.setText("Chọn Ảnh Mới");
        } else {
            tvHistory.setVisibility(View.GONE);
            btnSave.setText("Lưu Truyện Mới");
            edtMaTruyen.setEnabled(true);
        }
    }

    private void showGenreDialog() {
        if (listTheLoai == null || listTheLoai.length == 0) {
            Toast.makeText(this, "Đang tải dữ liệu thể loại, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void save() {
        if (selectedImageUri != null) {
            uploadImageToCloudinary(selectedImageUri);
        } else {
            String imageUrl = (isEditMode && truyenCu != null) ? truyenCu.getAnhBiaUrl() : "";
            saveDataToFirestore(imageUrl);
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        setLoadingState(true);
        try {
            RequestBody requestBody = new RequestBody() {
                @Override
                public MediaType contentType() { return MediaType.parse("image/*"); }
                @Override
                public long contentLength() {
                    try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                        return is != null ? is.available() : -1;
                    } catch (IOException e) { return -1; }
                }
                @Override
                public void writeTo(@NonNull okio.BufferedSink sink) throws IOException {
                    try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                        if (is != null) sink.writeAll(okio.Okio.source(is));
                    }
                }
            };

            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "upload_" + System.currentTimeMillis() + ".jpg", requestBody);
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), "upload-story");

            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new Callback<CloudinaryResponse>() {
                @Override
                public void onResponse(@NonNull Call<CloudinaryResponse> call, @NonNull Response<CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        saveDataToFirestore(response.body().getSecure_url());
                    } else {
                        setLoadingState(false);
                        Toast.makeText(AddEditStoryActivity.this, "Lỗi Cloudinary!", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull Call<CloudinaryResponse> call, @NonNull Throwable t) {
                    setLoadingState(false);
                    Toast.makeText(AddEditStoryActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            setLoadingState(false);
            Toast.makeText(this, "Lỗi tạo file upload!", Toast.LENGTH_SHORT).show();
        }
    }

    // HÀM LƯU DỮ LIỆU
    private void saveDataToFirestore(String imgUrl) {
        String maTruyen = edtMaTruyen.getText().toString().trim();
        String tenTruyen = edtTenTruyen.getText().toString().trim();
        String moTa = edtMoTa.getText().toString().trim();
        String tacGia = edtTacGia.getText().toString().trim();

        if (maTruyen.isEmpty() || userSelectedTheLoai.isEmpty()) {
            setLoadingState(false);
            Toast.makeText(this, "Mã truyện và Thể loại không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("tenTruyen", tenTruyen);
        updates.put("tacGia", tacGia);
        updates.put("moTa", moTa);
        updates.put("theLoai", userSelectedTheLoai);

        if (imgUrl != null && !imgUrl.isEmpty()) {
            updates.put("anhBiaUrl", imgUrl);
        }

        // Lưu lịch sử chỉnh sửa
        saveEditHistory(maTruyen, tenTruyen, tacGia, moTa, imgUrl, userSelectedTheLoai);

        if (isEditMode) {
            db.collection("Truyen").document(maTruyen).update(updates)
                    .addOnSuccessListener(aVoid -> finishSaveSettings(maTruyen))
                    .addOnFailureListener(e -> handleError(e));
        } else {

            updates.put("maTruyen", maTruyen);
            updates.put("viewCountAll", 0);
            updates.put("viewCountMonth", 0);
            updates.put("viewCountWeek", 0);

            Calendar cal = Calendar.getInstance();
            updates.put("monthKey", cal.get(Calendar.YEAR) + "_" + (cal.get(Calendar.MONTH) + 1));
            updates.put("weekKey", cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.WEEK_OF_YEAR));

            db.collection("Truyen").document(maTruyen).set(updates)
                    .addOnSuccessListener(aVoid -> finishSaveSettings(maTruyen))
                    .addOnFailureListener(e -> handleError(e));
        }
    }

    private void finishSaveSettings(String maTruyen) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("allowComment", cbAllowComment.isChecked());

        FirebaseFirestore.getInstance().collection("StorySettings").document(maTruyen)
                .set(settings, SetOptions.merge());

        setLoadingState(false);
        Toast.makeText(AddEditStoryActivity.this, "Lưu thành công!", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void handleError(Exception e) {
        setLoadingState(false);
        Toast.makeText(AddEditStoryActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    // Cập nhật hàm lưu lịch sử
    private void saveEditHistory(String maTruyen, String newTen, String newTacGia, String newMoTa, String newImgUrl, List<String> newTheLoai) {
        if (!isEditMode || truyenCu == null) return;
        Map<String, Object> changes = new HashMap<>();

        if (newImgUrl != null && !newImgUrl.isEmpty() && !truyenCu.getAnhBiaUrl().equals(newImgUrl)) {
            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getAnhBiaUrl());
            change.put("new", newImgUrl);
            changes.put("anhBiaUrl", change);
        }
        if (!truyenCu.getTenTruyen().equals(newTen)) {
            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getTenTruyen());
            change.put("new", newTen);
            changes.put("tenTruyen", change);
        }
        if (!truyenCu.getTacGia().equals(newTacGia)) {
            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getTacGia());
            change.put("new", newTacGia);
            changes.put("tacGia", change);
        }
        if (!truyenCu.getMoTa().equals(newMoTa)) {
            Map<String, String> change = new HashMap<>();
            change.put("old", truyenCu.getMoTa());
            change.put("new", newMoTa);
            changes.put("moTa", change);
        }
        if (!truyenCu.getTheLoai().equals(newTheLoai)) {
            Map<String, Object> change = new HashMap<>();
            change.put("old", truyenCu.getTheLoai());
            change.put("new", newTheLoai);
            changes.put("theLoai", change);
        }

        boolean newAllowComment = cbAllowComment.isChecked();
        if (oldAllowComment != newAllowComment) {
            Map<String, String> change = new HashMap<>();
            change.put("old", oldAllowComment ? "Bật" : "Tắt");
            change.put("new", newAllowComment ? "Bật" : "Tắt");
            changes.put("allowComment", change);
        }

        if (changes.isEmpty()) return;

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("editedAt", Timestamp.now());
        historyData.put("editedBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
        historyData.put("changes", changes);

        FirebaseFirestore.getInstance().collection("StoryEditHistory").document(maTruyen).collection("logs").add(historyData);
    }
}