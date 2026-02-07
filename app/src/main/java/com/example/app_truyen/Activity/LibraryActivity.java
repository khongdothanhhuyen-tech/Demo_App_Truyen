package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterStoryHori;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class LibraryActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private AdapterStoryHori adapterHistory;
    private ArrayList<Story> dsLichSu;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        RecyclerView rvTruyenDangDoc = findViewById(R.id.rv_truyenDangDoc);
        dsLichSu = new ArrayList<>();
        adapterHistory = new AdapterStoryHori(this, dsLichSu , false );
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rvTruyenDangDoc.setLayoutManager(new LinearLayoutManager(this));
        rvTruyenDangDoc.setAdapter(adapterHistory);

        setupNavigation();
        fetchReadingHistory();
    }
    @Override
    protected void onResume() {
        super.onResume();
        fetchReadingHistory();

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_library);
        }
    }
    private void fetchReadingHistory() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        db.collection("TaiKhoan").document(userId)
                .collection("LichSuDoc")
                .orderBy("thoiGianDoc", Query.Direction.DESCENDING)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dsLichSu.clear();
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(LibraryActivity.this, "Bạn chưa đọc truyện nào", Toast.LENGTH_SHORT).show();
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Story story = document.toObject(Story.class);
                                dsLichSu.add(story);
                            }
                        }
                        adapterHistory.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Lỗi", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // Xử lý thanh điều hướng
    private void setupNavigation() {
        bottomNavigationView = findViewById(R.id.nav);
        bottomNavigationView.setSelectedItemId(R.id.nav_library);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intentHome = new Intent(LibraryActivity.this, HomeActivity.class);
                startActivity(intentHome);
            }
            else if (id == R.id.nav_comic) {
                Intent intentComic = new Intent(LibraryActivity.this, ComicActivity.class);
                startActivity(intentComic);
                return true;
            }
            else if (id == R.id.nav_library) {
                NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
                if (scrollView != null) {
                    scrollView.smoothScrollTo(0, 0);
                }
                return true;
            }
            else if (id == R.id.nav_profile) {
                Intent intentProfile = new Intent(LibraryActivity.this, ProfileActivity.class);
                startActivity(intentProfile);
                return true;
            }
            return false;
        });
    }
}
