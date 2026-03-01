package com.example.app_truyen.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.app_truyen.Activity.CommentActivity;
import com.example.app_truyen.Models.Post;
import com.example.app_truyen.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class AdapterForum extends RecyclerView.Adapter<AdapterForum.PostViewHolder> {
    private Context context;
    private List<Post> listPosts;
    private String currentUserId;
    private boolean isAdmin = false;

    public AdapterForum(Context context, List<Post> listPosts, String currentUserId) {
        this.context = context;
        this.listPosts = listPosts;
        this.currentUserId = currentUserId;
        checkAdminStatus();
    }

    // ============================================
    // HÀM MỚI: Cập nhật danh sách khi tìm kiếm
    // ============================================
    public void setFilter(List<Post> filteredList) {
        this.listPosts = filteredList;
        notifyDataSetChanged();
    }

    private void checkAdminStatus() {
        if (currentUserId == null) return;
        FirebaseFirestore.getInstance().collection("TaiKhoan").document(currentUserId)
                .get().addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        isAdmin = "admin".equals(role);
                    }
                });
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = listPosts.get(position);
        if (post == null) return;

        // Xử lý hiển thị tên (Dò tìm lại từ Email)
        String displayName = post.getUserName();
        if (displayName == null || displayName.trim().isEmpty() || displayName.equals("Đạo hữu ẩn danh") || displayName.equals("User")) {
            holder.tvUserName.setText("Đang tải...");
            FirebaseFirestore.getInstance().collection("TaiKhoan").document(post.getUserId())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String emailDb = doc.getString("email");
                            if (emailDb != null && emailDb.contains("@")) {
                                holder.tvUserName.setText(emailDb.split("@")[0]);
                            } else {
                                holder.tvUserName.setText("Đạo hữu");
                            }
                        } else {
                            holder.tvUserName.setText("Đạo hữu");
                        }
                    });
        } else {
            holder.tvUserName.setText(displayName);
        }

        holder.tvContent.setText(post.getContent());
        holder.tvPostTime.setText(getRelativeTime(post.getTimestamp()));

        Glide.with(context).load(post.getUserAvatar()).placeholder(R.drawable.app_icon).circleCrop().into(holder.imgUserAvt);

        if (post.getPostImage() != null && !post.getPostImage().isEmpty()) {
            holder.imgPostContent.setVisibility(View.VISIBLE);
            Glide.with(context).load(post.getPostImage()).into(holder.imgPostContent);
        } else {
            holder.imgPostContent.setVisibility(View.GONE);
        }

        List<String> likes = post.getLikes();
        int likeCount = (likes != null) ? likes.size() : 0;
        holder.tvLikeStats.setText(likeCount + " Lượt thích");

        if (likes != null && likes.contains(currentUserId)) {
            holder.imgLikeIcon.setImageResource(R.drawable.heart);
            holder.imgLikeIcon.setColorFilter(context.getResources().getColor(R.color.orange));
        } else {
            holder.imgLikeIcon.setImageResource(R.drawable.ic_heart_outline);
            holder.imgLikeIcon.setColorFilter(null);
        }

        holder.btnLikePost.setOnClickListener(v -> {
            FirebaseFirestore.getInstance().collection("DienDan").document(post.getPostId())
                    .update("likes", likes != null && likes.contains(currentUserId) ?
                            FieldValue.arrayRemove(currentUserId) : FieldValue.arrayUnion(currentUserId));
        });

        // BẤM VÀO SỐ LƯỢT THÍCH ĐỂ XEM AI ĐÃ TIM
        holder.tvLikeStats.setOnClickListener(v -> {
            if (likes != null && !likes.isEmpty()) {
                showLikersDialog(likes);
            } else {
                Toast.makeText(context, "Chưa có lượt thích nào", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnCommentPost.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("TARGET_ID", post.getPostId());
            intent.putExtra("PATH", "DienDan");
            context.startActivity(intent);
        });

        FirebaseFirestore.getInstance().collection("DienDan").document(post.getPostId())
                .collection("BinhLuan")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        holder.tvCommentStats.setText(value.size() + " Bình luận");
                    }
                });

        // Nhấn giữ để xóa bài viết
        holder.itemView.setOnLongClickListener(v -> {
            if (post.getUserId().equals(currentUserId) || isAdmin) {
                new AlertDialog.Builder(context)
                        .setTitle("Xóa bài viết")
                        .setMessage("Đạo hữu có chắc chắn muốn xóa bài viết này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            FirebaseFirestore.getInstance().collection("DienDan").document(post.getPostId()).delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã xóa bài viết!", Toast.LENGTH_SHORT).show());
                        }).setNegativeButton("Hủy", null).show();
            } else {
                Toast.makeText(context, "Đạo hữu không có quyền xóa bài viết này!", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    // Hàm tạo giao diện danh sách người thích
    private void showLikersDialog(List<String> uids) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);

        // 1. Tạo Layout
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        // Bo góc
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setColor(android.graphics.Color.parseColor("#1E1E1E"));
        shape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        mainLayout.setBackground(shape);

        // 2. Tạo thanh gạt
        View handle = new View(context);
        int handleWidth = (int) (40 * context.getResources().getDisplayMetrics().density);
        int handleHeight = (int) (5 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(handleWidth, handleHeight);
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = (int) (12 * context.getResources().getDisplayMetrics().density);
        handle.setLayoutParams(handleParams);
        android.graphics.drawable.GradientDrawable handleShape = new android.graphics.drawable.GradientDrawable();
        handleShape.setCornerRadius(50);
        handleShape.setColor(android.graphics.Color.parseColor("#555555"));
        handle.setBackground(handleShape);
        mainLayout.addView(handle);

        // 3. Tiêu đề
        TextView tvHeader = new TextView(context);
        tvHeader.setText("Những người đã thích (" + uids.size() + ")");
        tvHeader.setTextColor(android.graphics.Color.WHITE);
        tvHeader.setTextSize(18);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setGravity(Gravity.CENTER);
        tvHeader.setPadding(0, 40, 0, 30);
        mainLayout.addView(tvHeader);

        // Đường kẻ ngang phân cách
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(android.graphics.Color.parseColor("#333333"));
        mainLayout.addView(divider);

        // 4. Tạo ScrollView chứa danh sách
        ScrollView scrollView = new ScrollView(context);
        int maxHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.6);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight));

        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        listContainer.setPadding(30, 20, 30, 40);
        scrollView.addView(listContainer);
        mainLayout.addView(scrollView);

        bottomSheetDialog.setContentView(mainLayout);
        bottomSheetDialog.show();

        // 5. Load dữ liệu người dùng
        for (String uid : uids) {
            FirebaseFirestore.getInstance().collection("TaiKhoan").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String email = doc.getString("email");
                    String avatarUrl = doc.getString("avatarUrl");
                    String displayName = doc.getString("tenHienThi");

                    // Lấy tên ưu tiên: tenHienThi -> cắt email
                    String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName
                            : ((email != null && email.contains("@")) ? email.split("@")[0] : "Đạo hữu");

                    // Layout từng dòng
                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(20, 25, 20, 25);

                    // Hiệu ứng click (Ripple effect)
                    android.util.TypedValue outValue = new android.util.TypedValue();
                    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                    row.setBackgroundResource(outValue.resourceId);

                    // Ảnh Avatar
                    ImageView imgAvatar = new ImageView(context);
                    int size = (int) (45 * context.getResources().getDisplayMetrics().density); // Tăng size lên 45dp
                    imgAvatar.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                    Glide.with(context).load(avatarUrl).placeholder(R.drawable.app_icon).circleCrop().into(imgAvatar);

                    // Tên người dùng
                    TextView tvName = new TextView(context);
                    tvName.setText(name);
                    tvName.setTextColor(android.graphics.Color.parseColor("#EEEEEE"));
                    tvName.setTextSize(16);
                    tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvName.setPadding(40, 0, 0, 0);

                    row.addView(imgAvatar);
                    row.addView(tvName);
                    listContainer.addView(row);
                }
            });
        }
    }

    private String getRelativeTime(Timestamp timestamp) {
        if (timestamp == null) return "Vừa xong";
        long diff = System.currentTimeMillis() - timestamp.toDate().getTime();
        if (diff < 60000) return "Vừa xong";
        if (diff < 3600000) return (diff / 60000) + " phút trước";
        if (diff < 86400000) return (diff / 3600000) + " giờ trước";
        return (diff / 86400000) + " ngày trước";
    }

    @Override
    public int getItemCount() { return listPosts != null ? listPosts.size() : 0; }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvContent, tvLikeStats, tvCommentStats, tvPostTime;
        ImageView imgUserAvt, imgPostContent, imgLikeIcon;
        View btnLikePost, btnCommentPost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvPostUser);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvPostContent);
            tvLikeStats = itemView.findViewById(R.id.tvLikeStats);
            tvCommentStats = itemView.findViewById(R.id.tvCommentStats);
            imgUserAvt = itemView.findViewById(R.id.imgUserAvt);
            imgPostContent = itemView.findViewById(R.id.imgPostContent);
            imgLikeIcon = itemView.findViewById(R.id.imgLikeIcon);
            btnLikePost = itemView.findViewById(R.id.btnLikePost);
            btnCommentPost = itemView.findViewById(R.id.btnCommentPost);
        }
    }
}