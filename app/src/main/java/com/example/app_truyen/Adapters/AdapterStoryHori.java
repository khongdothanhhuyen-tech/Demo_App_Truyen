package com.example.app_truyen.Adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.Activity.AddEditStoryActivity;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth; // <-- Import này
import com.google.firebase.auth.FirebaseUser; // <-- Import này
import com.google.firebase.firestore.FieldValue; // <-- Import này
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap; // <-- Import này
import java.util.Map;     // <-- Import này

public class AdapterStoryHori extends RecyclerView.Adapter<AdapterStoryHori.StoryViewHolder> {
    private final Context context;
    private final ArrayList<Story> dsTruyen;
    private final FirebaseFirestore db;
    private static final int MANAGE_STORY_REQUEST_CODE = 100;

    private boolean isAdmin;
    public AdapterStoryHori(Context context, ArrayList<Story> dsTruyen ,boolean isAdmin) {
        this.context = context;
        this.dsTruyen = dsTruyen;
        this.db = FirebaseFirestore.getInstance();
        this.isAdmin = isAdmin;
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAnhBia;
        TextView tvTenTruyen;
        TextView tvMoTa;
        public StoryViewHolder(@NonNull View view) {
            super(view);
            imgAnhBia = view.findViewById(R.id.img_anhBia);
            tvTenTruyen = view.findViewById(R.id.tv_tenTruyen);
            tvMoTa = view.findViewById(R.id.tv_moTa);
        }
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story_horizontal, viewGroup, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = dsTruyen.get(position);
        if (story == null) return;

        holder.tvTenTruyen.setText(story.getTenTruyen());
        holder.tvMoTa.setText(story.getMoTa());
        String urlAnh = story.getAnhBiaUrl();

        if (urlAnh != null && !urlAnh.isEmpty() && !urlAnh.equals("chưa có")) {
            Glide.with(context).load(urlAnh).into(holder.imgAnhBia);
        } else {
            holder.imgAnhBia.setImageResource(R.drawable.neverland);
        }

        // Sự kiện Long Click (Hiện menu Admin)
        if (isAdmin) {
            holder.itemView.setOnLongClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                Story selectedStory = dsTruyen.get(currentPos);
                showOptions(selectedStory, currentPos);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    // Hàm thêm lịch sử đọc
    private void addToHistory(Story story) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String userId = user.getUid();

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("maTruyen", story.getMaTruyen());
        historyData.put("tenTruyen", story.getTenTruyen());
        historyData.put("anhBiaUrl", story.getAnhBiaUrl());
        historyData.put("tacGia", story.getTacGia());
        historyData.put("moTa", story.getMoTa());
        historyData.put("theLoai", story.getTheLoai());

        //Lưu thời gian
        historyData.put("thoiGianDoc", FieldValue.serverTimestamp());

        db.collection("TaiKhoan").document(userId)
                .collection("LichSuDoc").document(story.getMaTruyen())
                .set(historyData) // Dùng set để ghi đè (cập nhật thời gian mới nhất)
                .addOnSuccessListener(aVoid -> {
                });
    }

    private void showOptions(Story story, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.item_edit_delete, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvEdit = sheetView.findViewById(R.id.tvEdit);
        TextView tvDelete = sheetView.findViewById(R.id.tvDelete);

        // Sửa
        tvEdit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(context, AddEditStoryActivity.class);
            intent.putExtra("TRUYEN_DATA", story);
            ((Activity) context).startActivityForResult(intent, MANAGE_STORY_REQUEST_CODE);
        });

        // Xóa
        tvDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Xóa truyện '" + story.getTenTruyen() + "'?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteStoryFromFirestore(story, position))
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }
    // Hàm xóa dữ liêu ở Firestore
    private void deleteStoryFromFirestore(Story story, int position) {
        if (story.getMaTruyen() == null || story.getMaTruyen().isEmpty()) {
            Toast.makeText(context, "Lỗi: Không có mã truyện", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("Truyen").document(story.getMaTruyen())
                .delete().addOnSuccessListener(aVoid -> {
                    dsTruyen.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, dsTruyen.size());
                    Toast.makeText(context, "Đã xóa truyện", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        if (dsTruyen != null) {
            return dsTruyen.size();
        }
        return 0;
    }
}