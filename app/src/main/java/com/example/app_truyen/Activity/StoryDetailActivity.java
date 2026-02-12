package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.Adapters.AdapterChapter;
import com.example.app_truyen.Models.Chapter;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StoryDetailActivity extends AppCompatActivity {
    private ImageView imgStoryPicture;
    private TextView tvTenTruyen, tvTheLoai, tvTacGia, tvMoTa,tvXemTatCa;
    private AdapterChapter adapterChapter;
    private List<Chapter> dsChuong;
    private FirebaseFirestore db;
    private LinearLayout btnOpenComment;
    private Story currentStory;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_detail);
        ImageView imgBack = findViewById(R.id.imgBack);


        db = FirebaseFirestore.getInstance();

        imgStoryPicture = findViewById(R.id.img_storyPicture);
        tvTenTruyen = findViewById(R.id.tv_tenTruyen);
        tvTheLoai = findViewById(R.id.tv_theLoai);
        tvTacGia = findViewById(R.id.tv_tacGia);
        tvMoTa = findViewById(R.id.tv_moTa);
        tvXemTatCa = findViewById(R.id.tv_xemTatCa);

        RecyclerView rvDsChuong = findViewById(R.id.rv_dsChuong);
        dsChuong = new ArrayList<>();
        adapterChapter = new AdapterChapter(this, dsChuong);

        rvDsChuong.setLayoutManager(new LinearLayoutManager(this));
        rvDsChuong.setAdapter(adapterChapter);

        loadData();

        imgBack.setOnClickListener(v -> finish());

        tvXemTatCa.setOnClickListener(v -> {
            if (currentStory != null) {
                Intent intentComment = new Intent(StoryDetailActivity.this, CommentActivity.class);
                intentComment.putExtra("MA_TRUYEN", currentStory.getMaTruyen());
                startActivity(intentComment);
            } else {
                Toast.makeText(StoryDetailActivity.this, "Dữ liệu truyện chưa tải xong", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadData() {
        Intent intent = getIntent();
        if (intent != null) {
            currentStory = (Story) intent.getSerializableExtra("TRUYEN_DATA");
        }

        if (currentStory != null) {
            tvTenTruyen.setText(currentStory.getTenTruyen());

            // Xử lý hiển thị thể loại
            if (currentStory.getTheLoai() != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    tvTheLoai.setText(String.join(", ", currentStory.getTheLoai()));
                } else {
                    tvTheLoai.setText(currentStory.getTheLoai().toString());
                }
            }

            tvTacGia.setText(currentStory.getTacGia());
            tvMoTa.setText(currentStory.getMoTa());

            // Load ảnh bìa
            String urlAnh = currentStory.getAnhBiaUrl();
            if (urlAnh != null && !urlAnh.isEmpty()) {
                Glide.with(this).load(urlAnh).into(imgStoryPicture);
            }

            // Load danh sách chương
            loadChapters(currentStory.getMaTruyen());
        }
    }

    private void loadChapters(String maTruyen) {
        db.collection("Truyen").document(maTruyen).collection("chuong")
                .orderBy("maChuong", Query.Direction.ASCENDING) // Sắp xếp tăng dần
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dsChuong.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Chapter chapter = document.toObject(Chapter.class);
                            chapter.setId(document.getId());
                            dsChuong.add(chapter);
                        }
                        adapterChapter.notifyDataSetChanged();
                    }
                });
    }
}
