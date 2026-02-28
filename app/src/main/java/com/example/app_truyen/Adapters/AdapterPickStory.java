package com.example.app_truyen.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import java.util.List;

public class AdapterPickStory extends RecyclerView.Adapter<AdapterPickStory.ViewHolder> {
    private Context context;
    private List<Story> listStories;
    private OnStoryPickListener listener;

    public interface OnStoryPickListener {
        void onPick(Story story);
    }

    public AdapterPickStory(Context context, List<Story> listStories, OnStoryPickListener listener) {
        this.context = context;
        this.listStories = listStories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Story story = listStories.get(position);
        holder.tvTen.setText(story.getTenTruyen());
        holder.tvMoTa.setText(story.getMoTa());
        Glide.with(context).load(story.getAnhBiaUrl()).placeholder(R.drawable.opm).into(holder.imgBia);

        holder.itemView.setOnClickListener(v -> listener.onPick(story));
    }

    @Override
    public int getItemCount() { return listStories.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBia; TextView tvTen, tvMoTa;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBia = itemView.findViewById(R.id.img_anhBia);
            tvTen = itemView.findViewById(R.id.tv_tenTruyen);
            tvMoTa = itemView.findViewById(R.id.tv_moTa);
        }
    }
}
