package com.example.app_truyen.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.Activity.ReadActivity;
import com.example.app_truyen.Models.Chapter;
import com.example.app_truyen.R;

import java.util.List;

public class AdapterChapter extends RecyclerView.Adapter<AdapterChapter.ChapterViewHolder> {
    private final List<Chapter> dsChuong;
    private final Context context;

    public AdapterChapter(Context context, List<Chapter> dsChuong) {
        this.context = context;
        this.dsChuong = dsChuong;
    }
    public static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenChuong;
        public ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTenChuong = itemView.findViewById(R.id.tv_tenChuong);
        }
    }
    @NonNull
    @Override
    public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chaptername, parent, false);
        return new ChapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
        Chapter chapter = dsChuong.get(position);
        if (chapter == null) return;

        holder.tvTenChuong.setText(chapter.getTenChuong());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReadActivity.class);
            intent.putExtra("CHAPTER_DATA", chapter);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        if (dsChuong != null) {
            return dsChuong.size();
        }
        return 0;
    }

}