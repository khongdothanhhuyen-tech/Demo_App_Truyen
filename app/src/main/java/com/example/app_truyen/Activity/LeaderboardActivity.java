package com.example.app_truyen.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterLeaderboard;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private AdapterLeaderboard adapter;
    private List<Story> listRankedStories;
    private FirebaseFirestore db;
    private TextView btnTatCa, btnThang, btnTuan;
    private String currentMode = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        db = FirebaseFirestore.getInstance();
        listRankedStories = new ArrayList<>();

        ImageView imgBack = findViewById(R.id.imgBack);
        btnTatCa = findViewById(R.id.btnTatCa);
        btnThang = findViewById(R.id.btnThang);
        btnTuan = findViewById(R.id.btnTuan);

        RecyclerView rvLeaderboard = findViewById(R.id.rvLeaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterLeaderboard(this, listRankedStories, currentMode);
        rvLeaderboard.setAdapter(adapter);

        imgBack.setOnClickListener(v -> finish());

        btnTatCa.setOnClickListener(v -> changeMode("ALL"));
        btnThang.setOnClickListener(v -> changeMode("MONTH"));
        btnTuan.setOnClickListener(v -> changeMode("WEEK"));

        changeMode("ALL");
    }

    private void changeMode(String mode) {
        currentMode = mode;
        adapter.setMode(mode);

        // Gọi hàm làm đẹp giao diện theo mode tương ứng
        if (mode.equals("ALL")) {
            updateTabUI(btnTatCa);
        } else if (mode.equals("MONTH")) {
            updateTabUI(btnThang);
        } else if (mode.equals("WEEK")) {
            updateTabUI(btnTuan);
        }

        loadLeaderboard(mode);
    }

    // HÀM XỬ LÝ GIAO DIỆN
    private void updateTabUI(TextView selectedBtn) {
        btnTatCa.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
        btnThang.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
        btnTuan.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));

        btnTatCa.setTextColor(Color.parseColor("#AAAAAA"));
        btnThang.setTextColor(Color.parseColor("#AAAAAA"));
        btnTuan.setTextColor(Color.parseColor("#AAAAAA"));

        selectedBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.orange)));
        selectedBtn.setTextColor(Color.WHITE);
    }

    private void loadLeaderboard(String mode) {
        db.collection("Truyen").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listRankedStories.clear();
                    List<Story> tempList = new ArrayList<>();

                    Calendar cal = Calendar.getInstance();
                    String currentMonthKey = cal.get(Calendar.YEAR) + "_" + (cal.get(Calendar.MONTH) + 1);
                    String currentWeekKey = cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.WEEK_OF_YEAR);

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Story story = doc.toObject(Story.class);
                        story.setMaTruyen(doc.getId());

                        // BẢO VỆ DỮ LIỆU: Ngăn chặn lỗi Null đối với truyện cũ
                        String dbMonthKey = story.getMonthKey() != null ? story.getMonthKey() : "";
                        String dbWeekKey = story.getWeekKey() != null ? story.getWeekKey() : "";

                        // DỌN DẸP TRÊN RAM: Cứ khác Key là ép về 0 (Reset tuần/tháng)
                        if (!currentMonthKey.equals(dbMonthKey)) {
                            story.setViewCountMonth(0);
                        }
                        if (!currentWeekKey.equals(dbWeekKey)) {
                            story.setViewCountWeek(0);
                        }

                        tempList.add(story);
                    }

                    // SẮP XẾP BXH
                    Collections.sort(tempList, (s1, s2) -> {
                        if (mode.equals("WEEK")) return Integer.compare(s2.getViewCountWeek(), s1.getViewCountWeek());
                        if (mode.equals("MONTH")) return Integer.compare(s2.getViewCountMonth(), s1.getViewCountMonth());
                        return Integer.compare(s2.getViewCountAll(), s1.getViewCountAll());
                    });

                    // LỌC DANH SÁCH (Chỉ lấy những truyện có lượt xem > 0)
                    for (Story s : tempList) {
                        boolean hasViews = (mode.equals("ALL") && s.getViewCountAll() > 0) ||
                                (mode.equals("MONTH") && s.getViewCountMonth() > 0) ||
                                (mode.equals("WEEK") && s.getViewCountWeek() > 0);

                        if (hasViews) {
                            listRankedStories.add(s);
                        }
                        if (listRankedStories.size() >= 20) break; // Giới hạn top 20
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(LeaderboardActivity.this, "Lỗi tải BXH: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}