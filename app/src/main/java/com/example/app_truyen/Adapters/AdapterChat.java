package com.example.app_truyen.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.Activity.StoryDetailActivity;
import com.example.app_truyen.Models.Comment;
import com.example.app_truyen.Models.Story;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdapterChat extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int MSG_TYPE_MINE = 1;
    private static final int MSG_TYPE_OTHER = 0;

    private Context context;
    private List<Comment> listMessages;
    private String currentUserId;

    // Biến kiểm tra quyền Admin
    private boolean isAdmin = false;

    public AdapterChat(Context context, List<Comment> listMessages, String currentUserId) {
        this.context = context;
        this.listMessages = listMessages;
        this.currentUserId = currentUserId;
    }

    // Hàm nhận quyền Admin từ CommunityChatActivity truyền sang
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (listMessages.get(position).getUid() != null && listMessages.get(position).getUid().equals(currentUserId)) {
            return MSG_TYPE_MINE;
        } else {
            return MSG_TYPE_OTHER;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_MINE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_mine, parent, false);
            return new MyMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_other, parent, false);
            return new OtherMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Comment message = listMessages.get(position);

        if (holder.getItemViewType() == MSG_TYPE_MINE) {
            MyMessageViewHolder myHolder = (MyMessageViewHolder) holder;

            // Xử lý Text
            if (message.getNoiDung() != null && !message.getNoiDung().trim().isEmpty()) {
                myHolder.tvMyMessage.setVisibility(View.VISIBLE);
                myHolder.tvMyMessage.setText(message.getNoiDung());
            } else {
                myHolder.tvMyMessage.setVisibility(View.GONE);
            }

            // Xử lý Truyện Đính Kèm
            handleAttachedStory(myHolder.layoutAttachedStory, myHolder.imgAttachedStory, myHolder.tvAttachedStoryName, message);

            // Bất kể là Admin hay User: Đều được phép xóa tin nhắn của CHÍNH MÌNH
            myHolder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Thu hồi tin nhắn")
                        .setMessage("Bạn có muốn thu hồi tin nhắn này không?")
                        .setPositiveButton("Thu hồi", (dialog, which) -> {
                            FirebaseFirestore.getInstance()
                                    .collection("PhongChat").document("CongDong")
                                    .collection("TinNhan").document(message.getId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã thu hồi", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            });

        } else {
            OtherMessageViewHolder otherHolder = (OtherMessageViewHolder) holder;

            // Xử lý Text
            if (message.getNoiDung() != null && !message.getNoiDung().trim().isEmpty()) {
                otherHolder.tvOtherMessage.setVisibility(View.VISIBLE);
                otherHolder.tvOtherMessage.setText(message.getNoiDung());
            } else {
                otherHolder.tvOtherMessage.setVisibility(View.GONE);
            }

            // Tên và Avatar
            otherHolder.tvOtherName.setText(message.getTenHienThi());
            if (message.getAvatarUrl() != null && !message.getAvatarUrl().isEmpty()) {
                Glide.with(context).load(message.getAvatarUrl()).circleCrop().into(otherHolder.imgOtherAvatar);
            } else {
                otherHolder.imgOtherAvatar.setImageResource(R.drawable.app_icon);
            }

            // Xử lý Truyện Đính Kèm
            handleAttachedStory(otherHolder.layoutAttachedStory, otherHolder.imgAttachedStory, otherHolder.tvAttachedStoryName, message);

            // PHÂN QUYỀN ADMIN CHO TIN NHẮN CỦA NGƯỜI KHÁC
            if (isAdmin) {
                // 1. Nhấn giữ Avatar để BAN (CẤM) người dùng
                otherHolder.imgOtherAvatar.setOnLongClickListener(v -> {
                    showBanDialog(message.getUid(), message.getTenHienThi());
                    return true;
                });

                // 2. Nhấn giữ Khung tin nhắn để XÓA tin nhắn của người dùng
                otherHolder.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Xóa tin nhắn (Quyền Admin)")
                            .setMessage("Bạn có chắc muốn xóa tin nhắn của " + message.getTenHienThi() + " không?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                FirebaseFirestore.getInstance()
                                        .collection("PhongChat").document("CongDong")
                                        .collection("TinNhan").document(message.getId())
                                        .delete()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã xóa tin nhắn", Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                    return true;
                });
            } else {
                // Nếu là User thường -> Tắt toàn bộ sự kiện nhấn giữ vào tin nhắn của người khác
                otherHolder.imgOtherAvatar.setOnLongClickListener(null);
                otherHolder.itemView.setOnLongClickListener(null);
            }
        }
    }

    // Hàm dùng chung để hiển thị truyện đính kèm
    private void handleAttachedStory(LinearLayout layoutAttached, ImageView imgCover, TextView tvName, Comment message) {
        if (message.getStoryIdDinhKem() != null && !message.getStoryIdDinhKem().isEmpty()) {
            layoutAttached.setVisibility(View.VISIBLE);
            tvName.setText(message.getTenTruyenDinhKem());

            if (message.getAnhTruyenDinhKem() != null && !message.getAnhTruyenDinhKem().isEmpty()) {
                Glide.with(context).load(message.getAnhTruyenDinhKem()).into(imgCover);
            } else {
                imgCover.setImageResource(R.drawable.app_icon);
            }

            layoutAttached.setOnClickListener(v -> {
                Story attachedStory = new Story();
                attachedStory.setMaTruyen(message.getStoryIdDinhKem());
                attachedStory.setTenTruyen(message.getTenTruyenDinhKem());
                attachedStory.setAnhBiaUrl(message.getAnhTruyenDinhKem());

                Intent intent = new Intent(context, StoryDetailActivity.class);
                intent.putExtra("TRUYEN_DATA", attachedStory);
                context.startActivity(intent);
            });
        } else {
            layoutAttached.setVisibility(View.GONE);
        }
    }

    // HÀM HIỂN THỊ MENU CẤM CHO ADMIN
    private void showBanDialog(String targetUserId, String targetUserName) {
        String[] options = {"Cấm 1 giờ", "Cấm 1 ngày", "Cấm 7 ngày", "Cấm 30 giây (Test)"};

        new AlertDialog.Builder(context)
                .setTitle("Phạt người dùng: " + targetUserName)
                .setItems(options, (dialog, which) -> {
                    long durationMillis = 0;
                    switch (which) {
                        case 0: durationMillis = 60L * 60 * 1000; break;
                        case 1: durationMillis = 24L * 60 * 60 * 1000; break;
                        case 2: durationMillis = 7L * 24 * 60 * 60 * 1000; break;
                        case 3: durationMillis = 30L * 1000; break;
                    }

                    long banUntilMillis = System.currentTimeMillis() + durationMillis;
                    java.util.Date date = new java.util.Date(banUntilMillis);
                    com.google.firebase.Timestamp banUntilTimestamp = new com.google.firebase.Timestamp(date);

                    java.util.Map<String, Object> banData = new java.util.HashMap<>();
                    banData.put("banUntil", banUntilTimestamp);
                    banData.put("reason", "Vi phạm quy định nhóm");

                    FirebaseFirestore.getInstance().collection("PhongChat").document("CongDong")
                            .collection("BannedUsers").document(targetUserId)
                            .set(banData)
                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã cấm " + targetUserName, Toast.LENGTH_SHORT).show());
                })
                .show();
    }

    @Override
    public int getItemCount() {
        return listMessages.size();
    }

    static class MyMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMyMessage, tvAttachedStoryName;
        LinearLayout layoutAttachedStory;
        ImageView imgAttachedStory;

        public MyMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMyMessage = itemView.findViewById(R.id.tvMyMessage);
            layoutAttachedStory = itemView.findViewById(R.id.layoutAttachedStory);
            imgAttachedStory = itemView.findViewById(R.id.imgAttachedStory);
            tvAttachedStoryName = itemView.findViewById(R.id.tvAttachedStoryName);
        }
    }

    static class OtherMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvOtherMessage, tvOtherName, tvAttachedStoryName;
        ImageView imgOtherAvatar, imgAttachedStory;
        LinearLayout layoutAttachedStory;

        public OtherMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOtherMessage = itemView.findViewById(R.id.tvOtherMessage);
            tvOtherName = itemView.findViewById(R.id.tvOtherName);
            imgOtherAvatar = itemView.findViewById(R.id.imgOtherAvatar);
            layoutAttachedStory = itemView.findViewById(R.id.layoutAttachedStory);
            imgAttachedStory = itemView.findViewById(R.id.imgAttachedStory);
            tvAttachedStoryName = itemView.findViewById(R.id.tvAttachedStoryName);
        }
    }
}