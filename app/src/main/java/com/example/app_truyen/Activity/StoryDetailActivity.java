package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.app_truyen.Adapters.AdapterChapter;
import com.example.app_truyen.Models.Chapter;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StoryDetailActivity extends AppCompatActivity {
    private ImageView imgStoryPicture;
    private TextView tvTenTruyen;
    private TextView tvTheLoai;
    private TextView tvTacGia;
    private TextView tvMoTa;
    private AdapterChapter adapterChapter;
    private List<Chapter> dsChuong;
    private FirebaseFirestore db;
    private Story currentStory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_detail);
        ImageView imgBack = findViewById(R.id.imgBack);

        db = FirebaseFirestore.getInstance();

        imgStoryPicture = findViewById(R.id.img_storyPicture);
        tvTenTruyen = findViewById(R.id.tv_tenTruyen);
        tvTheLoai = findViewById(R.id.tv_theLoai);
        tvTacGia = findViewById(R.id.tv_tacGia);
        tvMoTa = findViewById(R.id.tv_moTa);
        TextView tvXemTatCa = findViewById(R.id.tv_xemTatCa);

        RecyclerView rvDsChuong = findViewById(R.id.rv_dsChuong);
        dsChuong = new ArrayList<>();
        adapterChapter = new AdapterChapter(this, dsChuong);

        rvDsChuong.setLayoutManager(new LinearLayoutManager(this));
        rvDsChuong.setAdapter(adapterChapter);

        db = FirebaseFirestore.getInstance();
        currentStory = (Story) getIntent().getSerializableExtra("TRUYEN_DATA");

        if (currentStory != null) {
            increaseViewCount(currentStory.getMaTruyen());
            loadStoryData();
        }


        imgBack.setOnClickListener(v -> finish());

        tvXemTatCa.setOnClickListener(v -> {
            if (currentStory != null) {
                Intent intentComment = new Intent(StoryDetailActivity.this, CommentActivity.class);
                intentComment.putExtra("MA_TRUYEN", currentStory.getMaTruyen());
                startActivity(intentComment);
            } else {
                Toast.makeText(StoryDetailActivity.this, "Dữ liệu truyện chưa tải xong", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStoryData() {
        Intent intent = getIntent();
        if (intent != null) {
            currentStory = (Story) intent.getSerializableExtra("TRUYEN_DATA");
        }
        if (currentStory != null) {
            tvTenTruyen.setText(currentStory.getTenTruyen());
            String urlAnh = currentStory.getAnhBiaUrl();
            if (urlAnh != null && !urlAnh.isEmpty()) {
                Glide.with(this).load(urlAnh).into(imgStoryPicture);
            }

            db.collection("Truyen").document(currentStory.getMaTruyen())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Story fullStory = documentSnapshot.toObject(Story.class);
                            if (fullStory != null) {
                                fullStory.setMaTruyen(documentSnapshot.getId());
                                currentStory = fullStory;

                                tvTenTruyen.setText(fullStory.getTenTruyen());

                                if (fullStory.getTheLoai() != null && !fullStory.getTheLoai().isEmpty()) {
                                    tvTheLoai.setText(String.join(", ", fullStory.getTheLoai()));
                                } else {
                                    tvTheLoai.setText("Đang cập nhật");
                                }

                                tvTacGia.setText(fullStory.getTacGia() != null ? fullStory.getTacGia() : "Đang cập nhật");
                                tvMoTa.setText(fullStory.getMoTa() != null ? fullStory.getMoTa() : "Chưa có mô tả");
                            }
                        }
                    });

            loadListChapters(currentStory.getMaTruyen());
            recordUniqueView(currentStory.getMaTruyen());
        }
    }

    private void loadListChapters(String maTruyen) {
        db.collection("Truyen").document(maTruyen).collection("chuong")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dsChuong.clear();
                        long now = System.currentTimeMillis();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Chapter chapter = document.toObject(Chapter.class);
                            chapter.setId(document.getId());
                            if (chapter.getPublishTime() <= now) {
                                dsChuong.add(chapter);
                            }
                        }
                        adapterChapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Lỗi tải chương: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---> THÊM HÀM XỬ LÝ LƯỢT XEM (TUẦN/THÁNG/TẤT CẢ) <---
    private void recordUniqueView(String maTruyen) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return; // Khách chưa đăng nhập không tính view

        String userId = user.getUid();

        db.collection("Truyen").document(maTruyen)
                .collection("NguoiDaXem").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Nếu ID người này chưa có trong danh sách đã xem
                    if (!documentSnapshot.exists()) {
                        // 1. Lưu ID người dùng để lần sau bấm vào không bị tính trùng
                        db.collection("Truyen").document(maTruyen)
                                .collection("NguoiDaXem").document(userId)
                                .set(new java.util.HashMap<>());

                        // 2. Lấy dữ liệu truyện hiện tại để cộng View
                        db.collection("Truyen").document(maTruyen).get().addOnSuccessListener(storyDoc -> {
                            if (storyDoc.exists()) {
                                Story story = storyDoc.toObject(Story.class);
                                if (story == null) return;

                                Calendar cal = Calendar.getInstance();
                                int year = cal.get(Calendar.YEAR);
                                int month = cal.get(Calendar.MONTH) + 1;
                                int week = cal.get(Calendar.WEEK_OF_YEAR);

                                String currentMonthKey = year + "_" + month;
                                String currentWeekKey = year + "_" + week;

                                int newViewAll = story.getViewCountAll() + 1;
                                int newViewMonth = (currentMonthKey.equals(story.getMonthKey())) ? story.getViewCountMonth() + 1 : 1;
                                int newViewWeek = (currentWeekKey.equals(story.getWeekKey())) ? story.getViewCountWeek() + 1 : 1;

                                // 3. Đẩy thông số cập nhật lên Firebase
                                db.collection("Truyen").document(maTruyen).update(
                                        "viewCountAll", newViewAll,
                                        "viewCountMonth", newViewMonth,
                                        "viewCountWeek", newViewWeek,
                                        "monthKey", currentMonthKey,
                                        "weekKey", currentWeekKey
                                );
                            }
                        });
                    }
                });
    }
    private void increaseViewCount(String maTruyen) {
        if (maTruyen == null || maTruyen.isEmpty()) return;

        db.collection("Truyen").document(maTruyen).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Story story = documentSnapshot.toObject(Story.class);
                        if (story == null) return;

                        // 1. LẤY MỐC THỜI GIAN HIỆN TẠI
                        Calendar cal = Calendar.getInstance();
                        String currentMonthKey = cal.get(Calendar.YEAR) + "_" + (cal.get(Calendar.MONTH) + 1);
                        String currentWeekKey = cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.WEEK_OF_YEAR);

                        // 2. XỬ LÝ AN TOÀN CHO TRUYỆN CŨ CHƯA CÓ KEY
                        String dbMonthKey = story.getMonthKey() != null ? story.getMonthKey() : "";
                        String dbWeekKey = story.getWeekKey() != null ? story.getWeekKey() : "";

                        // 3. TÍNH TOÁN LƯỢT XEM MỚI
                        int newViewAll = story.getViewCountAll() + 1;
                        int newViewMonth = currentMonthKey.equals(dbMonthKey) ? story.getViewCountMonth() + 1 : 1;
                        int newViewWeek = currentWeekKey.equals(dbWeekKey) ? story.getViewCountWeek() + 1 : 1;

                        // 4. CẬP NHẬT LÊN FIREBASE
                        db.collection("Truyen").document(maTruyen).update(
                                "viewCountAll", newViewAll,
                                "viewCountMonth", newViewMonth,
                                "viewCountWeek", newViewWeek,
                                "monthKey", currentMonthKey,
                                "weekKey", currentWeekKey
                        );
                    }
                });
    }
}