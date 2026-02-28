package com.example.app_truyen.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.R;

import java.util.List;

public class AdapterCategoryAdmin extends RecyclerView.Adapter<AdapterCategoryAdmin.ViewHolder> {

    private Context context;
    private List<String> categoryList;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String category);
    }

    public AdapterCategoryAdmin(Context context, List<String> categoryList, OnDeleteClickListener deleteListener) {
        this.context = context;
        this.categoryList = categoryList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categoryList.get(position);
        holder.tvName.setText(category);
        holder.tvName.setTextColor(context.getResources().getColor(R.color.white));

        holder.itemView.setOnLongClickListener(v -> {
            deleteListener.onDeleteClick(category);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
        }
    }
}
