package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.example.app_truyen.Adapters.AdapterBannerStory;
import com.example.app_truyen.Adapters.AdapterStoryVerti;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private final Handler sliderHandler = new Handler();
    private ViewPager2 viewPagerBanner;
    private NestedScrollView nestedScrollView;
    private AdapterBannerStory adapterBanner;
    private LinearLayout containerGenres;

    @Override
    protected void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        nestedScrollView = findViewById(R.id.nestedScrollView);
        ImageView imgSearch = findViewById(R.id.imgSearch);
        ImageView imgCommunityChat = findViewById(R.id.imgCommunityChat);
        ImageView imgLeaderBoard = findViewById(R.id.imgLeaderBoard);
        ImageView imgChatBot = findViewById(R.id.imgChatBot);
        containerGenres = findViewById(R.id.containerGenres);

        imgSearch.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, SearchActivity.class));

        });

        imgCommunityChat.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CommunityChatActivity.class));
        });

        imgLeaderBoard.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, LeaderboardActivity.class));
        });

        imgChatBot.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, AIChatActivity.class));
        });

        setupBannerSlider();
        setupAllRecyclerViews();
        setupBottomNavigation();
    }

    private void setupAllRecyclerViews() {
        RecyclerView rvHori = findViewById(R.id.rv_truyen);
        if (rvHori != null) {
            ArrayList<Story> dsTruyenHori = new ArrayList<>();
            AdapterStoryVerti adapterVerti = new AdapterStoryVerti(this, dsTruyenHori);
            rvHori.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvHori.setAdapter(adapterVerti);
            fetchStories(dsTruyenHori, adapterVerti, null);
        }

        db.collection("TheLoai").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String genre = document.getId();
                    createDynamicGenreSection(genre);
                }
            }
        });
    }

    // Hàm sinh tự động UI cho từng Thể Loại
    private void createDynamicGenreSection(String genre) {
        View genreView = getLayoutInflater().inflate(R.layout.item_home_genre, containerGenres, false);

        TextView tvTitle = genreView.findViewById(R.id.tvGenreTitle);
        RecyclerView rvStories = genreView.findViewById(R.id.rvGenreStories);

        tvTitle.setText("Truyện " + genre);

        ArrayList<Story> listStories = new ArrayList<>();
        AdapterStoryVerti adapter = new AdapterStoryVerti(this, listStories);

        rvStories.setLayoutManager(new GridLayoutManager(this, 3));
        rvStories.setAdapter(adapter);

        fetchStories(listStories, adapter, genre);
        containerGenres.addView(genreView);
    }

    // Hàm gọi API lấy truyện (đã được tối ưu để tránh hiện View rỗng)
    private void fetchStories(ArrayList<Story> list, AdapterStoryVerti adapter, String genre) {
        Query query = db.collection("Truyen");
        if (genre != null) {
            query = query.whereArrayContains("theLoai", genre);
        }
        query.limit(6).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                list.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Story truyen = document.toObject(Story.class);
                    list.add(truyen);
                }
                adapter.notifyDataSetChanged();
            } else if (genre != null) {
            }
        });
    }

    // Hàm xử lý hiển thị ảnh Bìa chuyển động
    private void setupBannerSlider() {
        viewPagerBanner = findViewById(R.id.viewPagerBanner);
        List<Integer> dsBannerImg = Arrays.asList(
                R.drawable.demonslayer,
                R.drawable.jujutsukaisen,
                R.drawable.sakamotoday,
                R.drawable.chainsawman,
                R.drawable.iwanttoeatyourpancreas,
                R.drawable.tokyoghoul
        );
        adapterBanner = new AdapterBannerStory(this, dsBannerImg);
        viewPagerBanner.setAdapter(adapterBanner);

        viewPagerBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                    sliderHandler.postDelayed(sliderRunnable, 3000);
                } else if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    sliderHandler.removeCallbacks(sliderRunnable);
                }
            }
        });
    }
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            int currentItem = viewPagerBanner.getCurrentItem();
            int itemCount = adapterBanner.getItemCount();
            if (itemCount == 0) return;
            int nextItem = (currentItem + 1) % itemCount;
            viewPagerBanner.setCurrentItem(nextItem, true);
        }
    };
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.nav);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                if (nestedScrollView != null) nestedScrollView.smoothScrollTo(0, 0);
                return true;
            } else if (id == R.id.nav_comic) {
                startActivity(new Intent(HomeActivity.this, ComicActivity.class));
                overridePendingTransition(0, 0); // Tắt hiệu ứng chuyển trang
                return true;
            } else if (id == R.id.nav_library) {
                startActivity(new Intent(HomeActivity.this, LibraryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}

