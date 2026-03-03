package com.example.app_truyen.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.Models.User;
import com.example.app_truyen.R;

import java.text.SimpleDateFormat;
import java.util.List;

public class AdapterUser extends RecyclerView.Adapter<AdapterUser.UserViewHolder> {

    private Context context;
    private List<User> userList;

    public AdapterUser(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {

        User user = userList.get(position);

        // Avatar
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(context)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_launcher_background);
        }

        // Text info
        holder.tvEmail.setText(user.getEmail());
        holder.tvRole.setText("Role: " + user.getRole());

        boolean banned = user.getIsBanned() != null && user.getIsBanned();
        holder.tvStatus.setText(banned ? "Status: BANNED" : "Status: ACTIVE");

        // IMPORTANT: tránh lỗi checkbox nhảy lung tung
        holder.checkboxSelect.setOnCheckedChangeListener(null);
        holder.checkboxSelect.setChecked(user.isSelected());

        if ("admin@gmail.com".equalsIgnoreCase(user.getEmail())) {
            holder.checkboxSelect.setVisibility(View.GONE); // Ẩn checkbox
        } else {
            holder.checkboxSelect.setVisibility(View.VISIBLE);

            holder.checkboxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                user.setSelected(isChecked);
            });
        }

        holder.checkboxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            user.setSelected(isChecked);
        });

        // Click xem chi tiết
        holder.layoutRoot.setOnClickListener(v -> {

            String message =
                    "Email: " + safe(user.getEmail()) + "\n\n" +

                            "Ngày tạo: " + formatTimestamp(user.getNgayTao()) + "\n\n" +

                            "Lần cuối Check-in: " + safe(user.getLastCheckIn()) + "\n" +
                            "Lần cuối Comment: " + safe(user.getLastCommented()) + "\n" +
                            "Lần cuối Like: " + safe(user.getLastLiked()) + "\n" +
                            "Lần cuối Đăng bài: " + safe(user.getLastPosted()) + "\n\n" +

                            "EXP: " + user.getExp() + "\n" +
                            "Streak: " + user.getStreak();

            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Thông tin tài khoản")
                    .setMessage(message)
                    .setPositiveButton("Đóng", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ===== VIEW HOLDER =====
    static class UserViewHolder extends RecyclerView.ViewHolder {

        ImageView imgAvatar;
        TextView tvEmail, tvRole, tvStatus;
        LinearLayout layoutRoot;
        CheckBox checkboxSelect;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);

            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            layoutRoot = itemView.findViewById(R.id.layoutRoot);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
        }
    }

    // ===== HELPER =====
    private String safe(String value) {
        return (value == null || value.isEmpty()) ? "Chưa có" : value;
    }

    private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "Chưa có";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(timestamp.toDate());
    }
}