package com.example.app_truyen.Adapters;

import android.content.Context;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.app_truyen.Models.Comment;
import com.example.app_truyen.R;
import com.google.firebase.Timestamp; // Import Timestamp của Firebase
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdapterComment extends RecyclerView.Adapter<AdapterComment.CommentViewHolder> {
    private final Context context;
    private final List<Comment> listComments;
    private final String currentUserId;
    private final int orangeColor;
    private final int grayColor;
    private final String targetId;
    private final String collectionPath;
    private boolean isAdmin = false;

    public AdapterComment(Context context, List<Comment> listComments, String currentUserId, String targetId, String collectionPath) {
        this.context = context;
        this.listComments = listComments;
        this.currentUserId = currentUserId;
        this.targetId = targetId;
        this.collectionPath = collectionPath;
        this.orangeColor = ContextCompat.getColor(context, R.color.orange);
        this.grayColor = ContextCompat.getColor(context, R.color.gray_text);
        checkAdminStatus();
    }
    private void checkAdminStatus() {
        if (currentUserId == null) return;
        FirebaseFirestore.getInstance().collection("TaiKhoan").document(currentUserId)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists() && "admin".equals(doc.getString("role"))) {
                        isAdmin = true;
                    }
                });
    }
    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserEmail, tvContent, tvTime, tvBtnLike, tvBtnReply, tvLikeCount;
        ImageView imgProfile, imgHeartIcon, imgCommentImage;
        LinearLayout layoutLikeCount;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBtnLike = itemView.findViewById(R.id.tvBtnLike);
            tvBtnReply = itemView.findViewById(R.id.tvBtnReply);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            imgCommentImage = itemView.findViewById(R.id.imgCommentImage);
            imgProfile = itemView.findViewById(R.id.imgProfile);
            imgHeartIcon = itemView.findViewById(R.id.imgHeartIcon);
            layoutLikeCount = itemView.findViewById(R.id.layoutLikeCount);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = listComments.get(position);
        if (comment == null) return;

        holder.tvUserEmail.setText(comment.getTenHienThi());
        holder.tvContent.setText(comment.getNoiDung());

        holder.tvTime.setText(convertTime(comment.getNgayDang()));

        String avtUrl = comment.getAvatarUrl();
        if (avtUrl != null && !avtUrl.isEmpty()) {
            Glide.with(context).load(avtUrl).circleCrop().placeholder(R.drawable.app_icon).into(holder.imgProfile);
        } else {
            holder.imgProfile.setImageResource(R.drawable.app_icon);
        }

        List<String> likes = comment.getDanhSachLikes();
        int likeCount = likes.size();
        boolean isLiked = (currentUserId != null) && likes.contains(currentUserId);

        updateLikeUI(holder, isLiked, likeCount);

        holder.tvBtnLike.setOnClickListener(v -> {
            if (likes.contains(currentUserId)) {
                likes.remove(currentUserId);
                removeLikeFromFirestore(comment.getId());
            } else {
                likes.add(currentUserId);
                addLikeToFirestore(comment.getId());
            }
            notifyItemChanged(position);
        });
        if (comment.getCommentImage() != null && !comment.getCommentImage().isEmpty()) {
            holder.imgCommentImage.setVisibility(View.VISIBLE);
            Glide.with(context).load(comment.getCommentImage()).into(holder.imgCommentImage);
        } else {
            holder.imgCommentImage.setVisibility(View.GONE);
        }
        // Xử lý Xóa
        holder.itemView.setOnLongClickListener(v -> {
            if (currentUserId != null && (currentUserId.equals(comment.getUid()) || isAdmin)) {
                showDeleteDialog(comment, position);
            } else {
                Toast.makeText(context, "Chỉ chủ nhân hoặc Admin mới được xóa!", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    // Hàm tính toán thời gian để hiển thị tgian cmt
    private String convertTime(Timestamp timestamp) {
        if (timestamp == null) return "Vừa xong";

        long timePosted = timestamp.toDate().getTime();
        long timeNow = System.currentTimeMillis();
        long diff = timeNow - timePosted;

        long oneMinute = 60 * 1000;
        long oneHour = 60 * oneMinute;
        long oneDay = 24 * oneHour;

        if (diff < oneMinute) {
            return "Vừa xong";
        } else if (diff < oneHour) {
            long minutes = diff / oneMinute;
            return minutes + " phút trước";
        } else if (diff < oneDay) {
            long hours = diff / oneHour;
            return hours + " giờ trước";
        } else {
            long days = diff / oneDay;
            return days + " ngày trước";
        }
    }
    // Hàm cập nhật trạng thái like
    private void updateLikeUI(CommentViewHolder holder, boolean isLiked, int count) {
        if (isLiked) {
            holder.tvBtnLike.setTextColor(orangeColor);
            holder.tvBtnLike.setText("Đã thích");
        } else {
            holder.tvBtnLike.setTextColor(grayColor);
            holder.tvBtnLike.setText("Thích");
        }

        if (count > 0) {
            holder.layoutLikeCount.setVisibility(View.VISIBLE);
            holder.tvLikeCount.setText(String.valueOf(count));
            if (isLiked) {
                holder.tvLikeCount.setTextColor(orangeColor);
                holder.imgHeartIcon.setColorFilter(orangeColor);
            } else {
                holder.tvLikeCount.setTextColor(grayColor);
                holder.imgHeartIcon.setColorFilter(grayColor);
            }
        } else {
            holder.layoutLikeCount.setVisibility(View.GONE);
        }
    }
    // Hàm xử lý người dùng bấm thích
    private void addLikeToFirestore(String commentId) {
        FirebaseFirestore.getInstance()
                .collection(collectionPath).document(targetId)
                .collection("BinhLuan").document(commentId)
                .update("danhSachLikes", FieldValue.arrayUnion(currentUserId));
    }

    private void removeLikeFromFirestore(String commentId) {
        FirebaseFirestore.getInstance()
                .collection(collectionPath).document(targetId)
                .collection("BinhLuan").document(commentId)
                .update("danhSachLikes", FieldValue.arrayRemove(currentUserId));
    }

    // Hiển thị Dialog xác nhận xoá
    private void showDeleteDialog(Comment comment, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa bình luận")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                            .collection(collectionPath).document(targetId)
                            .collection("BinhLuan").document(comment.getId())
                            .delete().addOnSuccessListener(aVoid -> {
                                listComments.remove(position);
                                notifyItemRemoved(position);
                            });
                }).show();
    }

    @Override
    public int getItemCount() {
        return listComments.size();
    }
}