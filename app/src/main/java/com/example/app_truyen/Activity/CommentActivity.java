package com.example.app_truyen.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterComment;
import com.example.app_truyen.Models.Comment;
import com.example.app_truyen.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class CommentActivity extends AppCompatActivity {
    private EditText edtComment;
    private AdapterComment adapter;
    private List<Comment> listComments;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String currentUserAvatarUrl = "";
    private String currentUserName = "User";
    private Uri selectedImageUri;
    private String targetId;
    private String collectionPath;
    private com.example.app_truyen.API.CloudinaryService cloudinaryService;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Toast.makeText(this, "Đã chọn ảnh!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        cloudinaryService = com.example.app_truyen.API.RetrofitClient.getClient().create(com.example.app_truyen.API.CloudinaryService.class);
        RecyclerView rvComment = findViewById(R.id.rv_Comment);
        edtComment = findViewById(R.id.edtComment);
        ImageView btnSend = findViewById(R.id.btnSend);
        ImageView btnBack = findViewById(R.id.imgBack);

        targetId = getIntent().getStringExtra("TARGET_ID");
        if (targetId == null) targetId = getIntent().getStringExtra("MA_TRUYEN"); // Tương thích với phần Truyện

        collectionPath = getIntent().getStringExtra("PATH");
        if (collectionPath == null) collectionPath = getIntent().getStringExtra("TYPE");
        if (collectionPath == null) collectionPath = "Truyen";

        if (targetId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy dữ liệu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if ("Truyen".equals(collectionPath)) {
            FirebaseFirestore.getInstance().collection("StorySettings").document(targetId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean allow = documentSnapshot.getBoolean("allowComment");
                            if (allow != null && !allow) {
                                edtComment.setVisibility(View.GONE);
                                btnSend.setVisibility(View.GONE);
                                Toast.makeText(this, "Bình luận đã bị tắt bởi admin", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            fetchCurrentUserInfo();
        }

        listComments = new ArrayList<>();
        rvComment.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterComment(this, listComments, currentUserId, targetId, collectionPath);
        rvComment.setAdapter(adapter);

        loadComments();
        btnSend.setOnClickListener(v -> postComment());
        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchCurrentUserInfo() {
        if (currentUserId == null) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            currentUserName = user.getEmail().split("@")[0];
        } else {
            currentUserName = "Đạo hữu";
        }

        db.collection("TaiKhoan").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserAvatarUrl = documentSnapshot.getString("avatarUrl");
                    }
                });
    }

    private void postComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty() && selectedImageUri == null) return;

        if (selectedImageUri != null) {
            uploadCommentImageToCloudinary(content, selectedImageUri);
        } else {
            saveCommentToFirestore(content, "");
        }
    }

    private void uploadCommentImageToCloudinary(String content, Uri uri) {
        try {
            RequestBody requestBody = new RequestBody() {
                @Override
                public MediaType contentType() { return MediaType.parse("image/*"); }
                @Override
                public void writeTo(@NonNull okio.BufferedSink sink) throws IOException {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) sink.writeAll(okio.Okio.source(inputStream));
                }
            };
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "cmt_" + System.currentTimeMillis() + ".jpg", requestBody);
            RequestBody uploadPreset = RequestBody.create(MediaType.parse("text/plain"), "upload-story");

            cloudinaryService.uploadImage(uploadPreset, body).enqueue(new retrofit2.Callback<com.example.app_truyen.API.CloudinaryResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.app_truyen.API.CloudinaryResponse> call, retrofit2.Response<com.example.app_truyen.API.CloudinaryResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        saveCommentToFirestore(content, response.body().getSecure_url());
                        selectedImageUri = null;
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<com.example.app_truyen.API.CloudinaryResponse> call, Throwable t) {
                    Toast.makeText(CommentActivity.this, "Lỗi upload ảnh bình luận", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveCommentToFirestore(String content, String imageUrl) {
        String commentId = UUID.randomUUID().toString();

        // Chặn lỗi ẩn danh khi bình luận nhanh
        if (currentUserName == null || currentUserName.equals("User")) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                currentUserName = user.getEmail().split("@")[0];
            }
        }

        Comment newCmt = new Comment(commentId, currentUserId, currentUserName, currentUserAvatarUrl, content, new ArrayList<>(), Timestamp.now());
        newCmt.setCommentImage(imageUrl);

        db.collection(collectionPath).document(targetId)
                .collection("BinhLuan").document(commentId)
                .set(newCmt)
                .addOnSuccessListener(aVoid -> {
                    addExpForAction("lastCommented");
                    edtComment.setText("");
                    loadComments();
                });
    }

    private void loadComments() {
        db.collection(collectionPath).document(targetId)
                .collection("BinhLuan")
                .orderBy("ngayDang", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listComments.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment cmt = doc.toObject(Comment.class);
                        listComments.add(cmt);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
    private void addExpForAction(String actionField) {
        if (currentUserId == null) return;
        String today = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        FirebaseFirestore.getInstance().collection("TaiKhoan").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String lastAction = doc.getString(actionField);
                if (lastAction == null || !lastAction.equals(today)) {
                    int exp = doc.getLong("exp") != null ? doc.getLong("exp").intValue() : 0;
                    FirebaseFirestore.getInstance().collection("TaiKhoan").document(currentUserId)
                            .update("exp", exp + 10, actionField, today);
                }
            }
        });
    }
}