package com.example.app_truyen.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterRead;
import com.example.app_truyen.Models.Chapter;
import com.example.app_truyen.R;

import java.util.ArrayList;

public class ReadActivity extends AppCompatActivity {

    RecyclerView rvPages;
    AdapterRead adapterRead;
    ArrayList<String> dsAnh = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        rvPages = findViewById(R.id.rv_read_pages);
        Chapter currentChapter = (Chapter) getIntent().getSerializableExtra("CHAPTER_DATA");

        if (currentChapter != null && currentChapter.getAnhChuong() != null) {
            dsAnh.addAll(currentChapter.getAnhChuong());
        }

        adapterRead = new AdapterRead(this, dsAnh);
        rvPages.setLayoutManager(new LinearLayoutManager(this));
        rvPages.setAdapter(adapterRead);
    }
}