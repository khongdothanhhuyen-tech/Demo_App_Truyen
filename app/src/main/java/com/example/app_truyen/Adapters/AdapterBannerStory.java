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

public class AdapterBannerStory extends RecyclerView.Adapter<AdapterBannerStory.SliderViewHolder> {
    private final List<Integer> sliderItems;
    private final Context context;

    public AdapterBannerStory(Context context, List<Integer> sliderItems) {
        this.context = context;
        this.sliderItems = sliderItems;
    }
    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSlide;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSlide = itemView.findViewById(R.id.imgBanner);
        }
    }
    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_banner, viewGroup, false);
        return new SliderViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        int imageResourceId = sliderItems.get(position);
        Glide.with(context)
                .load(imageResourceId)
                .placeholder(R.drawable.demonslayer)
                .into(holder.imgSlide);
    }
    @Override
    public int getItemCount() {
        if (sliderItems != null) {
            return sliderItems.size();
        }
        return 0;
    }

}