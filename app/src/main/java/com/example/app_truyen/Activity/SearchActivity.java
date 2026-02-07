package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterStoryHori;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity {
    private Button btnBack, btnChinhSua;
    private SearchView searchView;
    private FlexboxLayout historyContainer;
    private final ArrayList<String> listKeywords = new ArrayList<>();
    private boolean isEditMode = false;
    private AdapterStoryHori adapterHienThi;
    private ArrayList<Story> dsKetQuaSearch;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        dsKetQuaSearch = new ArrayList<>();
        adapterHienThi = new AdapterStoryHori(this, dsKetQuaSearch , false);
        btnBack = findViewById(R.id.btnBack);
        searchView = findViewById(R.id.searchView);
        historyContainer = findViewById(R.id.history_container);
        btnChinhSua = findViewById(R.id.btnChinhSua);
        RecyclerView rvHienThi = findViewById(R.id.rvHienThi);
        rvHienThi.setLayoutManager(new LinearLayoutManager(this));
        rvHienThi.setAdapter(adapterHienThi);

        btnBack.setOnClickListener(v -> {
            Intent intentBack = new Intent(SearchActivity.this, HomeActivity.class);
            startActivity(intentBack);
            finish();
        });
        btnChinhSua.setOnClickListener(v -> {
            isEditMode = !isEditMode;
            refreshHistoryLayout();
        });

        setupSearchView();
    }

    private void setupSearchView() {
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.clearFocus();

        EditText searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchText.setTextColor(ContextCompat.getColor(this, R.color.white));
        searchText.setHintTextColor(ContextCompat.getColor(this, R.color.hint_text));
        ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        searchIcon.setColorFilter(ContextCompat.getColor(this, R.color.hint_text));
        ImageView closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setColorFilter(ContextCompat.getColor(this, R.color.hint_text));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.isEmpty()) return false;
                String keyword = query.trim();

                if (isKeywordExist(keyword)) {
                    for (int i = 0; i < listKeywords.size(); i++) {
                        if (listKeywords.get(i).equalsIgnoreCase(keyword)) {
                            listKeywords.remove(i);
                            break;
                        }
                    }
                }
                listKeywords.add(0, keyword);
                refreshHistoryLayout();

                searchStories(keyword);
                searchView.clearFocus();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    dsKetQuaSearch.clear();
                    adapterHienThi.notifyDataSetChanged();
                }
                return false;
            }
        });
    }


    //Hàm kiểm tra từ đã tồn tại trong danh sách hay chưa
    private boolean isKeywordExist(String keyword) {
        for (String s : listKeywords) {
            if (s.equalsIgnoreCase(keyword)) return true;
        }
        return false;
    }

    // Hàm quản lý lịch sử tìm kiếm
    private void refreshHistoryLayout() {
        historyContainer.removeAllViews();
        for (String keyword : listKeywords) {
            View historyView = LayoutInflater.from(this).inflate(R.layout.history_btn, historyContainer, false);
            TextView textView = historyView.findViewById(R.id.history_text);
            ImageView deleteIcon = historyView.findViewById(R.id.delete_icon);
            textView.setText(keyword);

            if (isEditMode) {
                deleteIcon.setVisibility(View.VISIBLE);
                btnChinhSua.setText("Xong");
                View.OnClickListener deleteAction = v -> {
                    listKeywords.remove(keyword);
                    refreshHistoryLayout();
                };
                historyView.setOnClickListener(deleteAction);
                deleteIcon.setOnClickListener(deleteAction);

            } else {
                deleteIcon.setVisibility(View.GONE);
                btnChinhSua.setText("Chỉnh sửa");
                historyView.setOnClickListener(v -> {
                    searchView.setQuery(keyword, true);
                });
            }
            historyContainer.addView(historyView);    
        }
    }

    //Hàm tìm kiếm truyện
    private void searchStories(String query) {
        Toast.makeText(this, "Đang tìm kiếm: " + query, Toast.LENGTH_SHORT).show();
        String searchQuery = query;
        db.collection("Truyen")
                .whereGreaterThanOrEqualTo("tenTruyen", searchQuery)
                .whereLessThanOrEqualTo("tenTruyen", searchQuery + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dsKetQuaSearch.clear();
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(SearchActivity.this, "Không tìm thấy kết quả", Toast.LENGTH_SHORT).show();
                        } else {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Story truyen = document.toObject(Story.class);
                                dsKetQuaSearch.add(truyen);
                            }
                        }
                        adapterHienThi.notifyDataSetChanged();
                    } else {
                        Toast.makeText(SearchActivity.this, "Lỗi", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}