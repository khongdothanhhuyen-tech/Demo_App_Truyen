package com.example.app_truyen.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.app_truyen.Activity.StoryDetailActivity;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;

import java.util.ArrayList;
import androidx.recyclerview.widget.RecyclerView;
public class AdapterStoryVerti extends RecyclerView.Adapter<AdapterStoryVerti.TruyenViewHolder> {

    private final Context context;
    private final ArrayList<Story> dsTruyen;

    public AdapterStoryVerti(Context context, ArrayList<Story> dsTruyen) {
        this.context = context;
        this.dsTruyen = dsTruyen;
    }
    public static class TruyenViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAnhBia;
        TextView tvTenTruyen;

        public TruyenViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAnhBia = itemView.findViewById(R.id.img_anhBia);
            tvTenTruyen = itemView.findViewById(R.id.tv_tenTruyen);
        }
    }

    @NonNull
    @Override
    public TruyenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_vertical, parent, false);
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int screenWidth = display.getWidth();

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = screenWidth / 3;
        view.setLayoutParams(layoutParams);
        return new TruyenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TruyenViewHolder holder, int position) {
        Story truyen = dsTruyen.get(position);
        if (truyen == null) return;
        holder.tvTenTruyen.setText(truyen.getTenTruyen());
        String urlAnh = truyen.getAnhBiaUrl();

        if (urlAnh != null && !urlAnh.isEmpty() && !urlAnh.equals("chưa có") && !urlAnh.equals("null")) {
            Glide.with(context)
                    .load(urlAnh)
                    .placeholder(R.drawable.opm)
                    .error(R.drawable.opm)
                    .into(holder.imgAnhBia);
        } else {
            holder.imgAnhBia.setImageResource(R.drawable.opm);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, StoryDetailActivity.class);
            intent.putExtra("TRUYEN_DATA", truyen);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return dsTruyen != null ? dsTruyen.size() : 0;
    }

}