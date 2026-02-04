package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StoryDetailActivity extends AppCompatActivity {
    private ImageView imgStoryPicture;
    private TextView tvTenTruyen, tvTheLoai, tvTacGia, tvMoTa;
    private AdapterChapter adapterChapter;
    private List<Chapter> dsChuong;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_detail);

        db = FirebaseFirestore.getInstance();

        imgStoryPicture = findViewById(R.id.img_storyPicture);
        tvTenTruyen = findViewById(R.id.tv_tenTruyen);
        tvTheLoai = findViewById(R.id.tv_theLoai);
        tvTacGia = findViewById(R.id.tv_tacGia);
        tvMoTa = findViewById(R.id.tv_moTa);

        RecyclerView rvDsChuong = findViewById(R.id.rv_dsChuong);
        dsChuong = new ArrayList<>();
        adapterChapter = new AdapterChapter(this, dsChuong);

        rvDsChuong.setLayoutManager(new LinearLayoutManager(this));
        rvDsChuong.setAdapter(adapterChapter);

        loadStoryData();
    }
    private void loadStoryData() {
        Intent intent = getIntent();
        if (intent != null) {
            Story truyen = (Story) intent.getSerializableExtra("TRUYEN_DATA");
            if (truyen != null) {
                tvTenTruyen.setText(truyen.getTenTruyen());
                String theLoaiStr = String.join(", ", truyen.getTheLoai());
                tvTheLoai.setText(theLoaiStr);
                tvTacGia.setText(truyen.getTacGia());
                tvMoTa.setText(truyen.getMoTa());
                String urlAnh = truyen.getAnhBiaUrl();

                if (urlAnh != null && !urlAnh.isEmpty() && !urlAnh.equals("chưa có")) {
                    Glide.with(this).load(urlAnh).into(imgStoryPicture);
                } else {
                    imgStoryPicture.setImageResource(R.drawable.demonslayer);
                }
                loadListChapters(truyen.getMaTruyen());
            }
        }
    }

    private void loadListChapters(String maTruyen) {
        db.collection("Truyen").document(maTruyen).collection("chuong")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        dsChuong.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Chapter chapter = document.toObject(Chapter.class);
                            chapter.setId(document.getId());
                            dsChuong.add(chapter);
                        }
                        adapterChapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Lỗi tải chương: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}