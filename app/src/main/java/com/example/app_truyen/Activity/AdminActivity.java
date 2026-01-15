package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app_truyen.Adapters.AdapterStoryHori;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private ArrayList<Story> dsTruyen;
    private AdapterStoryHori adapterStoryHori;

    private static final int MANAGE_STORY_REQUEST_CODE = 100;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Button btnAddStory = findViewById(R.id.btnAddStory);
        RecyclerView rvStory = findViewById(R.id.rvStory);

        db = FirebaseFirestore.getInstance();
        dsTruyen = new ArrayList<>();
        adapterStoryHori = new AdapterStoryHori(this, dsTruyen , true);

        rvStory.setAdapter(adapterStoryHori);
        rvStory.setLayoutManager(new LinearLayoutManager(this));

        fetchDataFromFirestore();

        btnAddStory.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AddEditStoryActivity.class);
            startActivityForResult(intent, MANAGE_STORY_REQUEST_CODE);
        });

    }
    // Hàm lấy dữ liệu từ Firestore
    private void fetchDataFromFirestore() {
        db.collection("Truyen")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dsTruyen.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Story truyen = document.toObject(Story.class);
                            dsTruyen.add(truyen);
                        }
                        adapterStoryHori.notifyDataSetChanged();
                    } else {
                        Toast.makeText(AdminActivity.this, "Lỗi khi tải dữ liệu.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MANAGE_STORY_REQUEST_CODE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Đang cập nhật danh sách...", Toast.LENGTH_SHORT).show();
            fetchDataFromFirestore();
        }
    }

}