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

    private RecyclerView rvComment;
    private EditText edtComment;
    private ImageView btnSend;
    private AdapterComment adapter;
    private List<Comment> listComments;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String storyId;

    private boolean isAdmin = false;
    private String currentUserId;
    private String currentUserAvatarUrl = "";
    private String currentUserName = "User";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        rvComment = findViewById(R.id.rv_Comment);
        edtComment = findViewById(R.id.edtComment);
        btnSend = findViewById(R.id.btnSend);

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
        }

        listComments = new ArrayList<>();
        rvComment.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterComment(this, listComments, isAdmin, currentUserId, storyId);
        rvComment.setAdapter(adapter);

        if (user != null) {
            fetchCurrentUserInfo();
            checkUserRole();
        }

        loadComments();

        btnSend.setOnClickListener(v -> postComment());
    }

    //Xử lý tên người dùng
    private void fetchCurrentUserInfo() {
        if (currentUserId == null) return;
        db.collection("TaiKhoan").document(currentUserId).get()
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

    private void checkUserRole() {
        if (currentUserId == null) return;

        db.collection("TaiKhoan").document(currentUserId)
                .get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        isAdmin = "admin".equals(role);

                        // Nếu là Admin, cập nhật lại Adapter để hiện chức năng xóa
                        if (isAdmin && adapter != null) {
                            adapter = new AdapterComment(this, listComments, true, currentUserId, storyId);
                            rvComment.setAdapter(adapter);
                        }
                    }
                });
    }

    private void postComment() {
        String content = edtComment.getText().toString().trim();
        if (content.isEmpty()) return;

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Bạn cần đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo ID ngẫu nhiên
        String commentId = UUID.randomUUID().toString();

        // Tạo đối tượng Comment (Sử dụng thông tin đã cache ở fetchCurrentUserInfo)
        Comment newComment = new Comment(
                commentId,
                currentUserId,
                currentUserName,        // Dùng biến đã lưu
                currentUserAvatarUrl,   // Dùng biến đã lưu
                content,
                new ArrayList<>(),      // QUAN TRỌNG: Khởi tạo list like rỗng
                Timestamp.now()
        );

        // Gửi lên Firestore
        db.collection("Truyen").document(storyId)
                .collection("BinhLuan").document(commentId)
                .set(newComment)
                .addOnSuccessListener(aVoid -> {
                    edtComment.setText("");
                    Toast.makeText(this, "Đã gửi!", Toast.LENGTH_SHORT).show();
                    loadComments(); // Load lại danh sách
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
                        // Firebase sẽ tự map dữ liệu vào class Comment
                        // Nhớ đảm bảo class Comment có Constructor rỗng và Getter/Setter đầy đủ
                        Comment cmt = doc.toObject(Comment.class);
                        listComments.add(cmt);
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}