package com.example.app_truyen.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterLeaderboard;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private AdapterLeaderboard adapter;
    private List<Story> listRankedStories;
    private FirebaseFirestore db;
    private TextView btnTatCa, btnThang, btnTuan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        ImageView imgBack = findViewById(R.id.imgBack);
        btnTatCa = findViewById(R.id.btnTatCa);
        btnThang = findViewById(R.id.btnThang);
        btnTuan = findViewById(R.id.btnTuan);
        RecyclerView rvLeaderboard = findViewById(R.id.rvLeaderboard);

        listRankedStories = new ArrayList<>();
        // Truyền thêm cờ "ALL" mặc định để Adapter biết đang hiển thị số view của cột nào
        adapter = new AdapterLeaderboard(this, listRankedStories, "ALL");
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);

        imgBack.setOnClickListener(v -> finish());

        // Gắn sự kiện chuyển Tab
        btnTatCa.setOnClickListener(v -> {
            updateTabUI(btnTatCa);
            loadLeaderboard("viewCountAll", "ALL");
        });
        btnThang.setOnClickListener(v -> {
            updateTabUI(btnThang);
            loadLeaderboard("viewCountMonth", "MONTH");
        });
        btnTuan.setOnClickListener(v -> {
            updateTabUI(btnTuan);
            loadLeaderboard("viewCountWeek", "WEEK");
        });

        // Load mặc định
        loadLeaderboard("viewCountAll", "ALL");
    }

    // Hàm đổi màu nút khi chọn Tab
    private void updateTabUI(TextView selectedBtn) {
        btnTatCa.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
        btnThang.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
        btnTuan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));

        selectedBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.orange)));
    }

    private void loadLeaderboard(String orderByField, String mode) {
        adapter.setMode(mode);

        db.collection("Truyen")
                .orderBy(orderByField, Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listRankedStories.clear();

                    Calendar cal = Calendar.getInstance();
                    String currentMonthKey = cal.get(Calendar.YEAR) + "_" + (cal.get(Calendar.MONTH) + 1);
                    String currentWeekKey = cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.WEEK_OF_YEAR);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Story story = doc.toObject(Story.class);

                        // Xử lý chống hiển thị data cũ rác
                        if (mode.equals("MONTH") && !currentMonthKey.equals(story.getMonthKey())) {
                            story.setViewCountMonth(0);
                        }
                        if (mode.equals("WEEK") && !currentWeekKey.equals(story.getWeekKey())) {
                            story.setViewCountWeek(0);
                        }

                        // Chỉ đưa vào BXH nếu có lượt xem > 0
                        boolean hasViews = (mode.equals("ALL") && story.getViewCountAll() > 0) ||
                                (mode.equals("MONTH") && story.getViewCountMonth() > 0) ||
                                (mode.equals("WEEK") && story.getViewCountWeek() > 0);

                        if (hasViews) {
                            listRankedStories.add(story);
                        }
                    }
                    adapter.notifyDataSetChanged();

                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(LeaderboardActivity.this, "Lỗi tải BXH: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                });

    }
}