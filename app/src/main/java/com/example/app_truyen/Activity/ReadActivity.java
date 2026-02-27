package com.example.app_truyen.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Adapters.AdapterRead;
import com.example.app_truyen.Adapters.PdfAdapter;
import com.example.app_truyen.Models.Chapter;
import com.example.app_truyen.R;

import java.util.ArrayList;

/// /
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class ReadActivity extends AppCompatActivity {
    private final ArrayList<String> dsAnh = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        RecyclerView rvPages = findViewById(R.id.rv_read_pages);
        RecyclerView rvPdf = findViewById(R.id.rvPdfPages);
        ImageView btnBack = findViewById(R.id.imgBack);
        btnBack.setOnClickListener(v -> finish());

        Chapter currentChapter = (Chapter) getIntent().getSerializableExtra("CHAPTER_DATA");

        // ===== Nếu là chương PDF =====
        if (currentChapter != null &&
                currentChapter.getPdfUrl() != null &&
                !currentChapter.getPdfUrl().isEmpty()) {

            rvPages.setVisibility(View.GONE);   // ẨN ảnh
            rvPdf.setVisibility(View.VISIBLE);  // HIỆN PDF

            downloadPdf(currentChapter.getPdfUrl());
            return;
        }

        if (currentChapter != null && currentChapter.getAnhChuong() != null) {
            dsAnh.addAll(currentChapter.getAnhChuong());
        }

        AdapterRead adapterRead = new AdapterRead(this, dsAnh);
        rvPages.setLayoutManager(new LinearLayoutManager(this));
        rvPages.setAdapter(adapterRead);

    }
    private void downloadPdf(String url) {
        new Thread(() -> {
            try {
                File file = new File(getCacheDir(), "temp.pdf");

                InputStream input = new java.net.URL(url).openStream();
                FileOutputStream output = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }

                input.close();
                output.close();

                runOnUiThread(() -> renderPdf(file));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void renderPdf(File file) {
        try {
            ParcelFileDescriptor descriptor =
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

            PdfRenderer renderer = new PdfRenderer(descriptor);

            List<Bitmap> pages = new ArrayList<>();

            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth(),
                        page.getHeight(),
                        Bitmap.Config.ARGB_8888
                );

                page.render(bitmap, null, null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                pages.add(bitmap);
                page.close();
            }

            renderer.close();
            descriptor.close();

            RecyclerView rv = findViewById(R.id.rvPdfPages);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new PdfAdapter(pages));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}