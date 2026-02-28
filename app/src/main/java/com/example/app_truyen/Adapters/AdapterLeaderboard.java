package com.example.app_truyen.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.app_truyen.Activity.StoryDetailActivity;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import java.util.List;

public class AdapterLeaderboard extends RecyclerView.Adapter<AdapterLeaderboard.RankViewHolder> {
    private Context context;
    private List<Story> listStories;
    private String mode; // THÊM BIẾN NÀY ĐỂ NHẬN BIẾT ĐANG Ở TAB NÀO

    // Đã thêm tham số mode vào Constructor
    public AdapterLeaderboard(Context context, List<Story> listStories, String mode) {
        this.context = context;
        this.listStories = listStories;
        this.mode = mode;
    }

    // Hàm nhận chế độ mới khi người dùng bấm chuyển tab
    public void setMode(String mode) {
        this.mode = mode;
    }

    @NonNull
    @Override
    public RankViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new RankViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankViewHolder holder, int position) {
        Story story = listStories.get(position);
        holder.tvTen.setText(story.getTenTruyen());
        holder.tvMoTa.setText(story.getMoTa());

        // KIỂM TRA MODE ĐỂ HIỂN THỊ ĐÚNG SỐ LƯỢT XEM CỦA TAB ĐÓ
        int currentViews = 0;
        if (mode.equals("ALL")) {
            currentViews = story.getViewCountAll();
        } else if (mode.equals("MONTH")) {
            currentViews = story.getViewCountMonth();
        } else if (mode.equals("WEEK")) {
            currentViews = story.getViewCountWeek();
        }

        // Format số lượt xem (ví dụ: 1200 -> 1.2K)
        holder.tvLuotXem.setText(formatViewCount(currentViews) + " Lượt xem");

        Glide.with(context).load(story.getAnhBiaUrl()).placeholder(R.drawable.app_icon).into(holder.imgBia);

        // Xử lý Rank (Thứ hạng)
        int rank = position + 1;
        holder.tvRank.setText(String.valueOf(rank));

        GradientDrawable bgShape = (GradientDrawable) holder.tvRank.getBackground();
        if (rank == 1) {
            bgShape.setColor(Color.parseColor("#FFD700")); // Vàng
            holder.tvRank.setTextColor(Color.BLACK);
        } else if (rank == 2) {
            bgShape.setColor(Color.parseColor("#C0C0C0")); // Bạc
            holder.tvRank.setTextColor(Color.BLACK);
        } else if (rank == 3) {
            bgShape.setColor(Color.parseColor("#CD7F32")); // Đồng
            holder.tvRank.setTextColor(Color.WHITE);
        } else {
            bgShape.setColor(Color.parseColor("#333333")); // Xám đen cho các thứ hạng khác
            holder.tvRank.setTextColor(Color.WHITE);
        }

        // Click để xem truyện
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StoryDetailActivity.class);
            intent.putExtra("TRUYEN_DATA", story);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return listStories.size(); }

    private String formatViewCount(int count) {
        if (count >= 1000000) return String.format("%.1fM", count / 1000000.0);
        if (count >= 1000) return String.format("%.1fK", count / 1000.0);
        return String.valueOf(count);
    }

    static class RankViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBia;
        TextView tvTen, tvMoTa, tvLuotXem, tvRank;
        public RankViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBia = itemView.findViewById(R.id.imgBiaBXH);
            tvTen = itemView.findViewById(R.id.tvTenBXH);
            tvMoTa = itemView.findViewById(R.id.tvMoTaBXH);
            tvLuotXem = itemView.findViewById(R.id.tvLuotXemBXH);
            tvRank = itemView.findViewById(R.id.tvRank);
        }
    }
}