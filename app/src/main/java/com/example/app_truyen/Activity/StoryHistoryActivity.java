package com.example.app_truyen.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterHistory;
import com.example.app_truyen.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoryHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private List<Map<String, Object>> historyList;
    private AdapterHistory adapter;
    private FirebaseFirestore db;
    private String storyId;

    private TextView tvEmpty;

    private boolean oldAllowComment = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_history);

        rvHistory = findViewById(R.id.rvHistory);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (rvHistory == null) {
            Toast.makeText(this, "Lỗi layout rvHistory null", Toast.LENGTH_LONG).show();
            return;
        }

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        historyList = new ArrayList<>();
        adapter = new AdapterHistory(historyList, historyItem -> {

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Xác nhận khôi phục")
                    .setMessage("Bạn có chắc muốn khôi phục về phiên bản này?")
                    .setPositiveButton("Khôi phục", (dialog, which) -> {
                        restoreVersion(historyItem);
                    })
                    .setNegativeButton("Huỷ", null)
                    .show();
        });
        rvHistory.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        storyId = getIntent().getStringExtra("MA_TRUYEN");

        if (storyId == null || storyId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã truyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadHistory();
    }

    private void loadHistory() {

        db.collection("StoryEditHistory")
                .document(storyId)
                .collection("logs")
                .orderBy("editedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    historyList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            historyList.add(data);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (historyList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvHistory.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Lỗi tải lịch sử: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void restoreVersion(Map<String, Object> historyItem) {

        Map<String, Object> changes =
                (Map<String, Object>) historyItem.get("changes");

        if (changes == null) return;

        Map<String, Object> restoreData = new java.util.HashMap<>();

        for (String key : changes.keySet()) {

            Map<String, Object> change =
                    (Map<String, Object>) changes.get(key);

            restoreData.put(key, change.get("old"));
        }

        db.collection("Truyen")
                .document(storyId)
                .update(restoreData)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(this,
                            "Khôi phục thành công!",
                            Toast.LENGTH_SHORT).show();

                    // ====== BƯỚC 3: GHI LOG RESTORE ======

                    Map<String, Object> restoreLog = new java.util.HashMap<>();
                    restoreLog.put("editedAt",
                            com.google.firebase.Timestamp.now());

                    restoreLog.put("changes", changes);

                    restoreLog.put("isRestore", true);

                    db.collection("StoryEditHistory")
                            .document(storyId)
                            .collection("logs")
                            .add(restoreLog);

                    // Reload lại lịch sử
                    loadHistory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Lỗi khôi phục: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}