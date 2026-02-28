package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.Adapters.AdapterChat;
import com.example.app_truyen.Adapters.AdapterPickStory;
import com.example.app_truyen.Models.Comment;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommunityChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText edtMessage;
    private ImageView btnSend;

    private AdapterChat adapterChat;
    private List<Comment> listMessages;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String currentUserName = "User";
    private String currentUserAvatar = "";

    private Story attachedStory = null;
    private LinearLayout layoutPreviewStory;
    private ImageView imgPreviewStory, btnRemovePreview, btnAttachStory;
    private TextView tvPreviewStoryName;
    private boolean isAdmin = false;
    private TextView tvOnlineCount;
    private TextView tvPinnedMessage;
    private LinearLayout layoutPinnedMsg;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_chat);

        rvChat = findViewById(R.id.rvChat);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);

        layoutPreviewStory = findViewById(R.id.layoutPreviewStory);
        imgPreviewStory = findViewById(R.id.imgPreviewStory);
        tvPreviewStoryName = findViewById(R.id.tvPreviewStoryName);
        btnRemovePreview = findViewById(R.id.btnRemovePreview);
        btnAttachStory = findViewById(R.id.btnAttachStory);
        tvPinnedMessage = findViewById(R.id.tvPinnedMessage);
        layoutPinnedMsg = findViewById(R.id.layoutPinnedMsg);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        tvOnlineCount = findViewById(R.id.tvOnlineCount);

        if (user != null) {
            currentUserId = user.getUid();
            fetchCurrentUserInfo();
        }

        listMessages = new ArrayList<>();
        adapterChat = new AdapterChat(this, listMessages, currentUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(adapterChat);

        listenForMessages();
        listenForBanStatus();
        listenForPinnedMessage();
        listenForOnlineCount();
        // Sự kiện gửi tin nhắn
        btnSend.setOnClickListener(v -> sendMessage());

        // Sự kiện quay lại
        findViewById(R.id.imgBack).setOnClickListener(v -> finish());

        // Sự kiện huỷ bỏ truyện đang đính kèm
        btnRemovePreview.setOnClickListener(v -> {
            attachedStory = null;
            layoutPreviewStory.setVisibility(View.GONE);
        });

        // Sự kiện bấm vào icon Sách để chọn truyện
        btnAttachStory.setOnClickListener(v -> showBottomSheetSearchStory());
    }

    private void fetchCurrentUserInfo() {
        db.collection("TaiKhoan").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                currentUserAvatar = doc.getString("avatarUrl");
                String email = auth.getCurrentUser().getEmail();
                if (email != null && email.contains("@")) {
                    currentUserName = email.split("@")[0];
                }

                // XÉT QUYỀN ADMIN
                String role = doc.getString("role");
                boolean isAdmin = "admin".equals(role);
                adapterChat.setAdmin(isAdmin);

                // NẾU LÀ ADMIN -> BẤM VÀO KHUNG GHIM ĐỂ SỬA
                if (isAdmin) {
                    layoutPinnedMsg.setOnClickListener(v -> showEditPinnedMessageDialog());
                } else {
                    layoutPinnedMsg.setOnClickListener(null);
                }
            }
        });
    }

    private void listenForMessages() {
        db.collection("PhongChat").document("CongDong").collection("TinNhan")
                .orderBy("ngayDang", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi kết nối chat", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        listMessages.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Comment msg = doc.toObject(Comment.class);
                            listMessages.add(msg);
                        }
                        adapterChat.notifyDataSetChanged();

                        if (!listMessages.isEmpty()) {
                            rvChat.smoothScrollToPosition(listMessages.size() - 1);
                        }
                    }
                });
    }
    private void listenForBanStatus() {
        db.collection("PhongChat").document("CongDong")
                .collection("BannedUsers").document(currentUserId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;

                    if (doc != null && doc.exists()) {
                        Timestamp banUntil = doc.getTimestamp("banUntil");
                        if (banUntil != null) {
                            long timeNow = System.currentTimeMillis();
                            long banTime = banUntil.toDate().getTime();

                            // Nếu thời gian cấm lớn hơn thời gian hiện tại -> Đang bị cấm
                            if (banTime > timeNow) {
                                // Tính toán chính xác thời gian còn lại
                                long diffMillis = banTime - timeNow;
                                long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                                long diffHours = (diffMillis / (1000 * 60 * 60)) % 24;
                                long diffMinutes = (diffMillis / (1000 * 60)) % 60;
                                long diffSeconds = (diffMillis / 1000) % 60;

                                // Xây dựng chuỗi thông báo
                                StringBuilder sb = new StringBuilder();
                                if (diffDays > 0) sb.append(diffDays).append(" ngày ");
                                if (diffHours > 0) sb.append(diffHours).append(" giờ ");
                                if (diffMinutes > 0) sb.append(diffMinutes).append(" phút ");
                                if (diffSeconds > 0 || sb.length() == 0) sb.append(diffSeconds).append(" giây");

                                String thongBao = sb.toString().trim();

                                Toast.makeText(this, "Bạn đã bị cấm chat.\nThời gian còn lại: " + thongBao, Toast.LENGTH_LONG).show();

                                // LẬP TỨC ĐÁ NGƯỜI DÙNG VỀ TRANG CHỦ (HomeActivity)
                                Intent intent = new Intent(CommunityChatActivity.this, HomeActivity.class);
                                // Xóa toàn bộ ngăn xếp trước đó để không thể ấn nút Back quay lại phòng chat
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                doc.getReference().delete();
                            }
                        }
                    }
                });
    }
    private void listenForPinnedMessage() {
        db.collection("PhongChat").document("CongDong")
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;

                    if (doc != null && doc.exists()) {
                        String msg = doc.getString("pinnedMessage");
                        if (msg != null && !msg.isEmpty()) {
                            tvPinnedMessage.setText(msg);
                        } else {
                            tvPinnedMessage.setText("Chưa có thông báo ghim.");
                        }
                    } else {
                        tvPinnedMessage.setText("Các bạn có nhu cầu bắt kì truyện gì thì đề xuất để bọn mình làm nhé");
                    }
                });
    }

    // HÀM HIỂN THỊ HỘP THOẠI SỬA (CHỈ ADMIN MỚI GỌI ĐƯỢC)
    private void showEditPinnedMessageDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Sửa thông báo ghim");

        final EditText input = new EditText(this);
        input.setText(tvPinnedMessage.getText().toString());
        input.setPadding(40, 40, 40, 40);
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newMsg = input.getText().toString().trim();
            if (newMsg.isEmpty()) return;

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("pinnedMessage", newMsg);

            db.collection("PhongChat").document("CongDong")
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã cập nhật thông báo", Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // HÀM HIỂN THỊ BẢNG TÌM KIẾM TRUYỆN ĐÍNH KÈM
    private void showBottomSheetSearchStory() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_search_story, null);
        bottomSheetDialog.setContentView(view);

        EditText edtSearch = view.findViewById(R.id.edtSearchStoryChat);
        RecyclerView rvSearch = view.findViewById(R.id.rvSearchStoryChat);

        List<Story> searchResults = new ArrayList<>();

        // Sử dụng AdapterPickStory đã tạo ở bước trước
        AdapterPickStory pickAdapter = new AdapterPickStory(this, searchResults, pickedStory -> {
            // Khi người dùng bấm chọn 1 truyện
            attachedStory = pickedStory;

            // Hiển thị khung Preview lên trên ô chat
            layoutPreviewStory.setVisibility(View.VISIBLE);
            tvPreviewStoryName.setText(pickedStory.getTenTruyen());

            if(pickedStory.getAnhBiaUrl() != null && !pickedStory.getAnhBiaUrl().isEmpty()){
                Glide.with(this).load(pickedStory.getAnhBiaUrl()).into(imgPreviewStory);
            } else {
                imgPreviewStory.setImageResource(R.drawable.app_icon); // Hoặc ảnh mặc định của bạn
            }

            // Đóng bảng tìm kiếm
            bottomSheetDialog.dismiss();
        });

        rvSearch.setLayoutManager(new LinearLayoutManager(this));
        rvSearch.setAdapter(pickAdapter);

        // Bắt sự kiện gõ chữ để tìm kiếm trên Firestore realtime
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString().trim();
                if (!keyword.isEmpty()) {
                    db.collection("Truyen")
                            .whereGreaterThanOrEqualTo("tenTruyen", keyword)
                            .whereLessThanOrEqualTo("tenTruyen", keyword + "\uf8ff")
                            .get().addOnSuccessListener(queryDocumentSnapshots -> {
                                searchResults.clear();
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    searchResults.add(doc.toObject(Story.class));
                                }
                                pickAdapter.notifyDataSetChanged();
                            });
                } else {
                    searchResults.clear();
                    pickAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        bottomSheetDialog.show();
    }

    // HÀM GỬI TIN NHẮN (ĐÃ CẬP NHẬT ĐỂ MANG THEO TRUYỆN
    private void sendMessage() {
        String content = edtMessage.getText().toString().trim();

        // Cho phép gửi nếu có nội dung HOẶC có đính kèm truyện
        if (content.isEmpty() && attachedStory == null) return;

        String msgId = UUID.randomUUID().toString();

        Comment newMsg = new Comment(
                msgId,
                currentUserId,
                currentUserName,
                currentUserAvatar,
                content,
                new ArrayList<>(),
                Timestamp.now()
        );

        // NẾU CÓ TRUYỆN ĐÍNH KÈM THÌ GÁN VÀO OBJECT COMMENT
        if (attachedStory != null) {
            newMsg.setStoryIdDinhKem(attachedStory.getMaTruyen());
            newMsg.setTenTruyenDinhKem(attachedStory.getTenTruyen());
            newMsg.setAnhTruyenDinhKem(attachedStory.getAnhBiaUrl());
        }

        // Nút gửi mờ đi để chống spam
        btnSend.setEnabled(false);

        db.collection("PhongChat").document("CongDong").collection("TinNhan").document(msgId)
                .set(newMsg)
                .addOnSuccessListener(aVoid -> {
                    edtMessage.setText("");
                    btnSend.setEnabled(true);

                    // Gửi xong thì reset và ẩn khung Preview
                    attachedStory = null;
                    layoutPreviewStory.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gửi thất bại", Toast.LENGTH_SHORT).show();
                    btnSend.setEnabled(true);
                });
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateOnlineStatus(true);
    }

    // Khi người dùng thoát trang chat hoặc ẩn app
    @Override
    protected void onPause() {
        super.onPause();
        updateOnlineStatus(false);
    }

    // Hàm cập nhật trạng thái lên Firestore
    private void updateOnlineStatus(boolean isOnline) {
        if (currentUserId == null) return;

        if (isOnline) {
            // Lưu 1 document trống với ID là ID của người dùng
            db.collection("PhongChat").document("CongDong")
                    .collection("OnlineUsers").document(currentUserId)
                    .set(new java.util.HashMap<>());
        } else {
            // Xóa document đi khi thoát
            db.collection("PhongChat").document("CongDong")
                    .collection("OnlineUsers").document(currentUserId)
                    .delete();
        }
    }

    // Hàm lắng nghe và đếm số lượng người online
    private void listenForOnlineCount() {
        db.collection("PhongChat").document("CongDong")
                .collection("OnlineUsers")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        int count = value.size(); // Đếm tổng số document
                        tvOnlineCount.setText(count + " người online");
                    }
                });
    }
}