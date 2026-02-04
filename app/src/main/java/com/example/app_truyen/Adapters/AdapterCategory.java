package com.example.app_truyen.Adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.R;
import com.google.android.material.chip.Chip;

import java.util.List;

public class AdapterCategory extends RecyclerView.Adapter<AdapterCategory.CategoryViewHolder> {

    private final Context context;
    private final List<String> listCategories;
    private final OnCategoryClickListener listener;

    private int selectedPosition = 0;

    public AdapterCategory(Context context, List<String> listCategories, OnCategoryClickListener listener) {
        this.context = context;
        this.listCategories = listCategories;
        this.listener = listener;
    }
    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        Chip chip;
        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            chip = itemView.findViewById(R.id.chipItem);
        }
    }
    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chip, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = listCategories.get(position);
        holder.chip.setText(category);

        boolean isSelected = (selectedPosition == position);
        holder.chip.setChecked(isSelected);

        if (isSelected) {
            holder.chip.setCheckedIconVisible(true);
            holder.chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.orange)));
            holder.chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.orange)));
            holder.chip.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.chip.setCheckedIconVisible(false);
            holder.chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black)));
            holder.chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white_transparent)));
            holder.chip.setTextColor(ContextCompat.getColor(context, R.color.white));
        }

        holder.chip.setOnClickListener(v -> {
            int newPosition = holder.getBindingAdapterPosition();
            int previousPosition = selectedPosition;
            selectedPosition = newPosition;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onCategoryClick(listCategories.get(selectedPosition));
            }
        });

    }

    @Override
    public int getItemCount() {
        return listCategories.size();
    }


}
