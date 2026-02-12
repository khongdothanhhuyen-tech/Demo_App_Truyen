package com.example.app_truyen.Activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentActivity extends AppCompatActivity {
    private EditText edtComment;
    private AdapterComment adapter;
    private List<Comment> listComments;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String storyId;
    private String currentUserId;
    private String currentUserAvatarUrl = "";
    private String currentUserName = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        RecyclerView rvComment = findViewById(R.id.rv_Comment);
        edtComment = findViewById(R.id.edtComment);
        ImageView btnSend = findViewById(R.id.btnSend);

        storyId = getIntent().getStringExtra("MA_TRUYEN");
        if (storyId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy truyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
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
        adapter = new AdapterComment(this, listComments, currentUserId, storyId);
        rvComment.setAdapter(adapter);

        loadComments();
        btnSend.setOnClickListener(v -> postComment());
    }

    //Hàm lấy thông tin người dùng để hiển thị
    private void fetchCurrentUserInfo() {
        if (currentUserId == null) return;
        db.collection("TaiKhoan").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserAvatarUrl = documentSnapshot.getString("avatarUrl");
                        String email = auth.getCurrentUser().getEmail();
                        if (email != null && email.contains("@")) {
                            currentUserName = email.split("@")[0];
                        }
                    }
                });
    }
    // Hàm gửi bình luận
    private void postComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty()) return;

        String commentId = UUID.randomUUID().toString();
        Comment newCmt = new Comment(commentId, currentUserId, currentUserName, currentUserAvatarUrl, content, new ArrayList<>(),Timestamp.now());

        db.collection("Truyen").document(storyId)
                .collection("BinhLuan").document(commentId)
                .set(newCmt)
                .addOnSuccessListener(aVoid -> {
                    edtComment.setText("");
                    Toast.makeText(this, "Đã gửi!", Toast.LENGTH_SHORT).show();
                    loadComments();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gửi thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadComments() {
        db.collection("Truyen").document(storyId)
                .collection("BinhLuan")
                .orderBy("ngayDang", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listComments.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Comment cmt = doc.toObject(Comment.class);
                        listComments.add(cmt);
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}