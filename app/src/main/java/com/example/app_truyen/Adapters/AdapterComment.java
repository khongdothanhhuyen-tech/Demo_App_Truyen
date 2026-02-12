package com.example.app_truyen.Adapters;

import android.app.AlertDialog;
import android.content.Context;
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
import com.example.app_truyen.Models.Comment;
import com.example.app_truyen.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
public class AdapterComment extends RecyclerView.Adapter<AdapterComment.CommentViewHolder> {
    private Context context;
    private List<Comment> listComments;
    private boolean isAdmin;
    private String currentUserId;
    private String storyId;
    private int orangeColor;
    private int grayColor;

    public AdapterComment(Context context, List<Comment> listComments, boolean isAdmin, String currentUserId, String storyId) {
        this.context = context;
        this.listComments = listComments;
        this.isAdmin = isAdmin;
        this.currentUserId = currentUserId;
        this.storyId = storyId;

        this.orangeColor = ContextCompat.getColor(context, R.color.orange);
        this.grayColor = ContextCompat.getColor(context, R.color.gray_text);
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

        List<String> likes = comment.getDanhSachLikes();
        int likeCount = likes.size();
        boolean isLiked = likes.contains(currentUserId);

        updateLikeUI(holder, isLiked, likeCount);

        holder.tvBtnLike.setOnClickListener(v -> {
            boolean currentStatus = likes.contains(currentUserId);

            if (currentStatus) {
                likes.remove(currentUserId);
                removeLikeFromFirestore(comment.getId());
            } else {
                likes.add(currentUserId);
                addLikeToFirestore(comment.getId());
            }
            notifyItemChanged(position);
        });
        holder.itemView.setOnLongClickListener(v -> {

            boolean isOwner = (currentUserId != null) && currentUserId.equals(comment.getUid());


            if (isAdmin || isOwner) {
                showDeleteDialog(comment, position);
                return true;
            }
            return false;
        });
    }


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

    // Gọi API thêm Like
    private void addLikeToFirestore(String commentId) {
        FirebaseFirestore.getInstance()
                .collection("Truyen").document(storyId)
                .collection("BinhLuan").document(commentId)
                .update("danhSachLikes", FieldValue.arrayUnion(currentUserId));
    }

    // Gọi API xóa Like
    private void removeLikeFromFirestore(String commentId) {
        FirebaseFirestore.getInstance()
                .collection("Truyen").document(storyId)
                .collection("BinhLuan").document(commentId)
                .update("danhSachLikes", FieldValue.arrayRemove(currentUserId));
    }

    @Override
    public int getItemCount() {
        return listComments.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserEmail, tvContent, tvBtnLike, tvLikeCount;
        ImageView imgHeartIcon;
        LinearLayout layoutLikeCount; // Layout bao quanh số like

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvBtnLike = itemView.findViewById(R.id.tvBtnLike);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            imgHeartIcon = itemView.findViewById(R.id.imgHeartIcon);
            layoutLikeCount = itemView.findViewById(R.id.layoutLikeCount);
        }
    }
    private void showDeleteDialog(Comment comment, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa bình luận")
                .setMessage("Bạn muốn xóa bình luận này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                            .collection("Truyen").document(storyId)
                            .collection("BinhLuan").document(comment.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                listComments.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "Đã xóa!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}