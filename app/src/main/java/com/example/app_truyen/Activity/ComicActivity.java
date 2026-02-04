package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterCategory;
import com.example.app_truyen.Adapters.AdapterStoryHori;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComicActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private AdapterStoryHori adapterComic;
    private ArrayList<Story> dsTruyen;
    private ArrayList<Story> dsTruyenGoc;
    private final String [] listTheLoai = {"Hành động", "Tình cảm", "Học đường", "Phiêu lưu", "Sát thủ", "Kinh dị", "Hài hước", "Khoa học viễn tưởng", "Siêu anh hùng"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic);

        RecyclerView rvComic = findViewById(R.id.rv_comic);
        RecyclerView rvCategories = findViewById(R.id.rvCategories);
        MaterialCardView cardFilter = findViewById(R.id.cardFilter);

        bottomNavigationView = findViewById(R.id.nav);
        db = FirebaseFirestore.getInstance();
        dsTruyen = new ArrayList<>();
        dsTruyenGoc = new ArrayList<>();

        adapterComic = new AdapterStoryHori(this, dsTruyen,false );
        rvComic.setLayoutManager(new LinearLayoutManager(this));
        rvComic.setAdapter(adapterComic);

        List<String> listCategoryDisplay = new ArrayList<>();
        listCategoryDisplay.add("Tất cả");
        listCategoryDisplay.addAll(Arrays.asList(listTheLoai));

        AdapterCategory adapterCategory = new AdapterCategory(this, listCategoryDisplay, category -> {
            if (category.equals("Tất cả")) {
                fetchStories(null);
            } else {
                fetchStories(category);
            }
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(adapterCategory);

        // Xử lý nhấn vào lọc truyện theo nhiều thể loại
        cardFilter.setOnClickListener(v -> {
            BottomSheetDialog dialog = new BottomSheetDialog(ComicActivity.this);
            View view = getLayoutInflater().inflate(R.layout.filter_layout , null );
            dialog.setContentView(view);

            ChipGroup chipGroupFilter = view.findViewById(R.id.cg_filter);
            Button btnApply = view.findViewById(R.id.btn_apply);

            for (String genre : listTheLoai) {
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip, chipGroupFilter, false);
                chip.setText(genre);
                chipGroupFilter.addView(chip);
            }
            btnApply.setOnClickListener(v1 -> {
                List<String> selectedGenres = new ArrayList<>();
                for (int i = 0; i < chipGroupFilter.getChildCount(); i++) {
                    Chip chip = (Chip) chipGroupFilter.getChildAt(i);
                    if (chip.isChecked()) {
                        selectedGenres.add(chip.getText().toString());
                    }
                }
                filterMultiGenre(selectedGenres);
                dialog.dismiss();
            });

            dialog.show();
        });
        fetchAllStories();
        setupNavigation();
    }
    // Lấy tất cả truyện
    private void fetchAllStories() {
        db.collection("Truyen").get().addOnSuccessListener(queryDocumentSnapshots -> {
            dsTruyenGoc.clear();
            dsTruyen.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Story story = doc.toObject(Story.class);
                dsTruyenGoc.add(story);
                dsTruyen.add(story);
            }
            adapterComic.notifyDataSetChanged();
        });
    }

    // Lọc truyện theo nhiều thể loại người dùng chọn
    private void filterMultiGenre(List<String> selectedGenres) {
        if (selectedGenres.isEmpty()) {
            dsTruyen.clear();
            dsTruyen.addAll(dsTruyenGoc);
            adapterComic.notifyDataSetChanged();
            return;
        }
        ArrayList<Story> filteredList = new ArrayList<>();

        for (Story story : dsTruyenGoc) {
            List<String> storyGenres = story.getTheLoai();
            if (storyGenres != null && !storyGenres.isEmpty()) {
                Set<String> storyGenresSet = new HashSet<>(storyGenres);
                if (storyGenresSet.containsAll(selectedGenres)) {
                    filteredList.add(story);
                }
            }
        }
        dsTruyen.clear();
        dsTruyen.addAll(filteredList);
        adapterComic.notifyDataSetChanged();
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "Không có truyện nào thỏa mãn!", Toast.LENGTH_SHORT).show();
        }
    }

    //Lọc truyện theo thể loại
    private void fetchStories(String genre) {
        Query query = db.collection("Truyen");
        if (genre != null) {
            query = query.whereArrayContains("theLoai", genre);
        }
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dsTruyen.clear();
                if (task.getResult().isEmpty()) {
                    Toast.makeText(this, "Chưa có truyện thuộc thể loại này", Toast.LENGTH_SHORT).show();
                } else {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Story story = document.toObject(Story.class);
                        dsTruyen.add(story);
                    }
                }
                adapterComic.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Lỗi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Xử lý thanh điều hướng
    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_comic);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ComicActivity.this, HomeActivity.class));
                return true;
            }
            else if (id == R.id.nav_comic) {
                NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
                if (scrollView != null) scrollView.smoothScrollTo(0, 0);
                return true;
            }
            else if (id == R.id.nav_library) {
                startActivity(new Intent(ComicActivity.this, LibraryActivity.class));
                return true;
            }else if (id == R.id.nav_profile) {
//                startActivity(new Intent(ComicActivity.this, ProfileActivity.class));
//                return true;
            }
            return false;
        });
    }
}