package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app_truyen.Adapters.AdapterStoryHori;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private ArrayList<Story> dsTruyen;
    private AdapterStoryHori adapterStoryHori;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        /// /
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("TaiKhoan")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {

                    String role = snapshot.getString("role");

                    if (role == null || !role.equals("admin")) {
                        Toast.makeText(this,
                                "Bạn không có quyền truy cập",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
        /// /

        Button btnAddStory = findViewById(R.id.btnAddStory);
        Button btnAddCategory = findViewById(R.id.btnAddCategory);
        Button btnChat = findViewById(R.id.btnChat);
        Button btnForum = findViewById(R.id.btnForum);
        RecyclerView rvStory = findViewById(R.id.rvStory);

        /// /
        Button btnUserManagement = findViewById(R.id.btnUserManagement);

        btnUserManagement.setOnClickListener(v -> {
            startActivity(new Intent(this, UserManagementActivity.class));
        });

        progressBar = findViewById(R.id.progressBar);
        db = FirebaseFirestore.getInstance();
        dsTruyen = new ArrayList<>();
        adapterStoryHori = new AdapterStoryHori(this, dsTruyen , true);

        rvStory.setAdapter(adapterStoryHori);
        rvStory.setLayoutManager(new LinearLayoutManager(this));

        fetchDataFromFirestore();

        btnAddStory.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AddEditStoryActivity.class);
            startActivity(intent);
        });

        btnAddCategory.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, AddCategoryActivity.class);
            startActivity(intent);
        });

        btnChat.setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, CommunityChatActivity.class));
        });
        btnForum.setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, ForumActivity.class));
        });

    }
    // Hàm lấy dữ liệu từ Firestore
    private void fetchDataFromFirestore() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("Truyen")
                .get().addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
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
    protected void onResume() {
        super.onResume();
        fetchDataFromFirestore();
    }
}