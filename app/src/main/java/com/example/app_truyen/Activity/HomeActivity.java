package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
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


        setupBannerSlider();
        setupAllRecyclerViews();
    }
    // Hàm cài đặt RecycleView
    private void setupAllRecyclerViews() {
        RecyclerView rvHori = findViewById(R.id.rv_truyen);
        ArrayList<Story> dsTruyenHori = new ArrayList<>();
        AdapterStoryVerti adapterVerti = new AdapterStoryVerti(this, dsTruyenHori);
        rvHori.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvHori.setAdapter(adapterVerti);

        fetchStories(dsTruyenHori, adapterVerti, null);

        ArrayList<Story> dsHanhDong = new ArrayList<>();
        AdapterStoryVerti adtHanhDong = new AdapterStoryVerti(this, dsHanhDong);
        setupGenreStory(R.id.rv_truyenHanhDong, dsHanhDong, adtHanhDong, "Hành động");

        ArrayList<Story> dsTinhCam = new ArrayList<>();
        AdapterStoryVerti adtTinhCam = new AdapterStoryVerti(this, dsTinhCam);
        setupGenreStory(R.id.rv_truyenTinhCam, dsTinhCam, adtTinhCam, "Tình cảm");

        ArrayList<Story> dsPhieuLuu = new ArrayList<>();
        AdapterStoryVerti adtPhieuLuu = new AdapterStoryVerti(this, dsPhieuLuu);
        setupGenreStory(R.id.rv_truyenPhieuLuu, dsPhieuLuu, adtPhieuLuu, "Phiêu lưu");

        ArrayList<Story> dsSatThu = new ArrayList<>();
        AdapterStoryVerti adtSatThu = new AdapterStoryVerti(this, dsSatThu);
        setupGenreStory(R.id.rv_truyenSatThu, dsSatThu, adtSatThu, "Sát thủ");

        ArrayList<Story> dsKinhDi = new ArrayList<>();
        AdapterStoryVerti adtKinhDi = new AdapterStoryVerti(this, dsKinhDi);
        setupGenreStory(R.id.rv_truyenKinhDi, dsKinhDi, adtKinhDi, "Kinh dị");
    }

    // Hàm xử lý hiển thị thể loại truyện
    private void setupGenreStory(int recyclerViewId, ArrayList<Story> list, AdapterStoryVerti adapter, String genre) {
        RecyclerView rv = findViewById(recyclerViewId);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        rv.setAdapter(adapter);
        fetchStories(list, adapter, genre);
    }

    // Hàm xử lý tìm thể loại truyện
    private void fetchStories(ArrayList<Story> list, AdapterStoryVerti adapter , String genre) {
        Query query = db.collection("Truyen");
        if (genre != null) {
            query = query.whereArrayContains("theLoai", genre);
        }
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                list.clear();
                if (!task.getResult().isEmpty()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Story truyen = document.toObject(Story.class);
                        list.add(truyen);
                    }
                } else {
                    Toast.makeText(this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
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
//    AVCCC
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
}

