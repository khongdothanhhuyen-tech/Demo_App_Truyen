package com.example.app_truyen.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.R;

import java.util.List;

public class AdapterRead extends RecyclerView.Adapter<AdapterRead.ReadViewHolder> {
    private Context context;
    private List<String> listAnhUrl;

    public AdapterRead(Context context, List<String> listAnhUrl) {
        this.context = context;
        this.listAnhUrl = listAnhUrl;
    }
    public static class ReadViewHolder extends RecyclerView.ViewHolder {
        ImageView imgChapter;
        public ReadViewHolder(@NonNull View itemView) {
            super(itemView);
            imgChapter = itemView.findViewById(R.id.imgChapter);
        }
    }

    @NonNull
    @Override
    public ReadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chapter, parent, false);
        return new ReadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReadViewHolder holder, int position) {
        String url = listAnhUrl.get(position);
        Glide.with(context).load(url).into(holder.imgChapter);
    }

    @Override
    public int getItemCount() {
        return listAnhUrl == null ? 0 : listAnhUrl.size();
    }


}