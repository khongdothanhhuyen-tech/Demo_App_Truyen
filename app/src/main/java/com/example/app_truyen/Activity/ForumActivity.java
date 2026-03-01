package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.app_truyen.Adapters.AdapterForum;
import com.example.app_truyen.Models.Post;
import com.example.app_truyen.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class ForumActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId = FirebaseAuth.getInstance().getUid();
    private List<Post> listPosts = new ArrayList<>();
    private AdapterForum adapter;

    private ImageView imgUserAvtStatus;
    private TextView tvStatusHint;
    private EditText edtSearchForum;
    private ListenerRegistration defaultPostsListener; // Để quản lý listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum);

        imgUserAvtStatus = findViewById(R.id.imgUserAvtStatus);
        tvStatusHint = findViewById(R.id.tvStatusHint);
        edtSearchForum = findViewById(R.id.edtSearchForum);

        fetchCurrentUserProfile();

        findViewById(R.id.cardStatusStart).setOnClickListener(v -> {
            startActivity(new Intent(ForumActivity.this, CreatePostActivity.class));
        });

        findViewById(R.id.imgBack).setOnClickListener(v -> finish());

        RecyclerView rvForum = findViewById(R.id.rvForum);
        adapter = new AdapterForum(this, listPosts, currentUserId);
        rvForum.setLayoutManager(new LinearLayoutManager(this));
        rvForum.setAdapter(adapter);

        loadDefaultPosts(); // Tải 10 bài mặc định

        // LẮNG NGHE SỰ KIỆN TÌM KIẾM
        edtSearchForum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                if (keyword.isEmpty()) {
                    loadDefaultPosts(); // Nếu ô tìm kiếm trống, quay về 10 bài gần nhất
                } else {
                    searchPosts(keyword); // Nếu có chữ, bắt đầu tìm kiếm
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchCurrentUserProfile() {
        if (currentUserId == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String displayName = user.getEmail().split("@")[0];
            if (tvStatusHint != null) tvStatusHint.setText(displayName + " ơi, bạn đang nghĩ gì thế?");
        }
        db.collection("TaiKhoan").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String avatarUrl = doc.getString("avatarUrl");
                if (avatarUrl != null && !avatarUrl.isEmpty() && imgUserAvtStatus != null) {
                    Glide.with(this).load(avatarUrl).circleCrop().into(imgUserAvtStatus);
                }
            }
        });
    }

    // TÍNH NĂNG: CHỈ HIỂN THỊ 10 BÀI GẦN NHẤT
    private void loadDefaultPosts() {
        if (defaultPostsListener != null) defaultPostsListener.remove(); // Xóa listener cũ tránh trùng lặp

        defaultPostsListener = db.collection("DienDan")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10) // Giới hạn 10 bài
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        listPosts.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            listPosts.add(doc.toObject(Post.class));
                        }
                        adapter.setFilter(listPosts); // Cập nhật adapter
                    }
                });
    }

    // TÍNH NĂNG: TÌM KIẾM CHUỖI KHÔNG PHÂN BIỆT HOA/THƯỜNG
    private void searchPosts(String keyword) {
        if (defaultPostsListener != null) defaultPostsListener.remove(); // Tạm dừng realtime 10 bài

        String lowerKeyword = keyword.toLowerCase();
        db.collection("DienDan")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> searchResults = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        if (post != null && post.getContent() != null) {
                            // Kiểm tra chuỗi chứa từ khóa
                            if (post.getContent().toLowerCase().contains(lowerKeyword)) {
                                searchResults.add(post);
                            }
                        }
                    }
                    adapter.setFilter(searchResults);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (defaultPostsListener != null) defaultPostsListener.remove();
    }
}