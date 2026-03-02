package com.example.app_truyen.Adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
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

        // Xử lý tên
        String displayName = post.getUserName();
        if (displayName == null || displayName.trim().isEmpty() || displayName.equals("Đạo hữu ẩn danh") || displayName.equals("User")) {
            holder.tvUserName.setText("Đang tải...");
            FirebaseFirestore.getInstance().collection("TaiKhoan").document(post.getUserId())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String emailDb = doc.getString("email");
                            if (emailDb != null && emailDb.contains("@")) holder.tvUserName.setText(emailDb.split("@")[0]);
                            else holder.tvUserName.setText("Đạo hữu");
                        } else holder.tvUserName.setText("Đạo hữu");
                    });
        } else {
            holder.tvUserName.setText(displayName);
        }

        holder.tvContent.setText(post.getContent());
        holder.tvPostTime.setText(getRelativeTime(post.getTimestamp()));
        Glide.with(context).load(post.getUserAvatar()).placeholder(R.drawable.app_icon).circleCrop().into(holder.imgUserAvt);

        //  XỬ LÝ LƯỚI ĐA ẢNH VÀ CLICK XEM ẢNH
        List<String> images = post.getPostImages();
        if (images != null && !images.isEmpty()) {
            holder.layoutImageGrid.setVisibility(View.VISIBLE);
            int size = images.size();

            holder.row1.setVisibility(View.VISIBLE);
            holder.row2.setVisibility(View.GONE);
            holder.imgPost1.setVisibility(View.GONE);
            holder.imgPost2.setVisibility(View.GONE);
            holder.imgPost3.setVisibility(View.GONE);
            holder.framePost4.setVisibility(View.GONE);
            holder.tvMoreImages.setVisibility(View.GONE);

            // Bắt sự kiện click để mở Gallery xem ảnh
            holder.imgPost1.setOnClickListener(v -> showImageGalleryDialog(images, 0));
            holder.imgPost2.setOnClickListener(v -> showImageGalleryDialog(images, 1));
            holder.imgPost3.setOnClickListener(v -> showImageGalleryDialog(images, 2));
            holder.framePost4.setOnClickListener(v -> showImageGalleryDialog(images, 3));

            if (size == 1) {
                holder.imgPost1.setVisibility(View.VISIBLE);
                Glide.with(context).load(images.get(0)).into(holder.imgPost1);
            } else if (size == 2) {
                holder.imgPost1.setVisibility(View.VISIBLE);
                holder.imgPost2.setVisibility(View.VISIBLE);
                Glide.with(context).load(images.get(0)).into(holder.imgPost1);
                Glide.with(context).load(images.get(1)).into(holder.imgPost2);
            } else if (size == 3) {
                holder.row2.setVisibility(View.VISIBLE);
                holder.imgPost1.setVisibility(View.VISIBLE);
                holder.imgPost3.setVisibility(View.VISIBLE);
                holder.framePost4.setVisibility(View.VISIBLE);
                Glide.with(context).load(images.get(0)).into(holder.imgPost1);
                Glide.with(context).load(images.get(1)).into(holder.imgPost3);
                Glide.with(context).load(images.get(2)).into(holder.imgPost4);
            } else {
                holder.row2.setVisibility(View.VISIBLE);
                holder.imgPost1.setVisibility(View.VISIBLE);
                holder.imgPost2.setVisibility(View.VISIBLE);
                holder.imgPost3.setVisibility(View.VISIBLE);
                holder.framePost4.setVisibility(View.VISIBLE);
                Glide.with(context).load(images.get(0)).into(holder.imgPost1);
                Glide.with(context).load(images.get(1)).into(holder.imgPost2);
                Glide.with(context).load(images.get(2)).into(holder.imgPost3);
                Glide.with(context).load(images.get(3)).into(holder.imgPost4);

                if (size > 4) {
                    holder.tvMoreImages.setVisibility(View.VISIBLE);
                    holder.tvMoreImages.setText("+" + (size - 4));
                }
            }
        } else {
            holder.layoutImageGrid.setVisibility(View.GONE);
        }

        // Like & Comment
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
            boolean isLiking = (likes == null || !likes.contains(currentUserId));

            FirebaseFirestore.getInstance().collection("DienDan").document(post.getPostId())
                    .update("likes", isLiking ? FieldValue.arrayUnion(currentUserId) : FieldValue.arrayRemove(currentUserId))
                    .addOnSuccessListener(aVoid -> {
                        // CHỈ CỘNG ĐIỂM KHI NGƯỜI DÙNG BẤM "THÍCH" (Lưu 1 lần/ngày)
                        if (isLiking) {
                            String today = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
                            FirebaseFirestore.getInstance().collection("TaiKhoan").document(currentUserId).get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            String lastLiked = doc.getString("lastLiked");
                                            if (lastLiked == null || !lastLiked.equals(today)) {
                                                int exp = doc.getLong("exp") != null ? doc.getLong("exp").intValue() : 0;
                                                FirebaseFirestore.getInstance().collection("TaiKhoan").document(currentUserId)
                                                        .update("exp", exp + 10, "lastLiked", today);
                                            }
                                        }
                                    });
                        }
                    });
        });

        holder.tvLikeStats.setOnClickListener(v -> {
            if (likes != null && !likes.isEmpty()) showLikersDialog(likes);
            else Toast.makeText(context, "Chưa có lượt thích nào", Toast.LENGTH_SHORT).show();
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
                    if (value != null) holder.tvCommentStats.setText(value.size() + " Bình luận");
                });

        holder.itemView.setOnLongClickListener(v -> {
            if (post.getUserId().equals(currentUserId) || isAdmin) {
                new AlertDialog.Builder(context).setTitle("Xóa bài viết")
                        .setMessage("Đạo hữu có chắc chắn muốn xóa bài viết này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            FirebaseFirestore.getInstance().collection("DienDan").document(post.getPostId()).delete()
                                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã xóa bài viết!", Toast.LENGTH_SHORT).show());
                        }).setNegativeButton("Hủy", null).show();
            } else Toast.makeText(context, "Không có quyền xóa bài!", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    // ===== HÀM TẠO TRÌNH XEM ẢNH TOÀN MÀN HÌNH (GALLERY) =====
    private void showImageGalleryDialog(List<String> images, int startIndex) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        RelativeLayout rootLayout = new RelativeLayout(context);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootLayout.setBackgroundColor(Color.BLACK);

        // RecyclerView cấu hình lướt ngang như ViewPager
        RecyclerView rv = new RecyclerView(context);
        rv.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rv.setLayoutManager(layoutManager);

        // PagerSnapHelper giúp vuốt ảnh dính từng khung hình một (như Facebook)
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rv);

        // Số thứ tự ảnh (Ví dụ: 1 / 5)
        TextView tvIndicator = new TextView(context);
        RelativeLayout.LayoutParams tvParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        tvParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        tvParams.topMargin = 50;
        tvIndicator.setLayoutParams(tvParams);
        tvIndicator.setTextColor(Color.WHITE);
        tvIndicator.setTextSize(16);
        tvIndicator.setText((startIndex + 1) + " / " + images.size());

        // Adapter tải ảnh vào RecyclerView
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ImageView iv = new ImageView(context);
                iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER); // Căn ảnh vừa màn hình, không bị cắt
                return new RecyclerView.ViewHolder(iv) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                ImageView iv = (ImageView) holder.itemView;
                Glide.with(context).load(images.get(position)).into(iv);
            }

            @Override
            public int getItemCount() {
                return images.size();
            }
        });

        // Lắng nghe sự kiện lướt để cập nhật số thứ tự
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int pos = layoutManager.findFirstVisibleItemPosition();
                    tvIndicator.setText((pos + 1) + " / " + images.size());
                }
            }
        });

        // Cuộn đến ảnh người dùng vừa click
        rv.scrollToPosition(startIndex);

        // Nút X (Đóng)
        ImageView btnClose = new ImageView(context);
        RelativeLayout.LayoutParams closeParams = new RelativeLayout.LayoutParams(120, 120);
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        closeParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        closeParams.topMargin = 40;
        closeParams.leftMargin = 40;
        btnClose.setLayoutParams(closeParams);
        btnClose.setImageResource(R.drawable.ic_back1);
        btnClose.setColorFilter(Color.WHITE);
        btnClose.setPadding(30, 30, 30, 30);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        rootLayout.addView(rv);
        rootLayout.addView(tvIndicator);
        rootLayout.addView(btnClose);

        dialog.setContentView(rootLayout);
        dialog.show();
    }

    private void showLikersDialog(List<String> uids) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(context);
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        shape.setColor(Color.parseColor("#1E1E1E"));
        shape.setCornerRadii(new float[]{60, 60, 60, 60, 0, 0, 0, 0});
        mainLayout.setBackground(shape);

        View handle = new View(context);
        int handleWidth = (int) (40 * context.getResources().getDisplayMetrics().density);
        int handleHeight = (int) (5 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(handleWidth, handleHeight);
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = (int) (12 * context.getResources().getDisplayMetrics().density);
        handle.setLayoutParams(handleParams);
        android.graphics.drawable.GradientDrawable handleShape = new android.graphics.drawable.GradientDrawable();
        handleShape.setCornerRadius(50);
        handleShape.setColor(Color.parseColor("#555555"));
        handle.setBackground(handleShape);
        mainLayout.addView(handle);

        TextView tvHeader = new TextView(context);
        tvHeader.setText("Những người đã thích (" + uids.size() + ")");
        tvHeader.setTextColor(Color.WHITE);
        tvHeader.setTextSize(18);
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvHeader.setGravity(Gravity.CENTER);
        tvHeader.setPadding(0, 40, 0, 30);
        mainLayout.addView(tvHeader);

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

        for (String uid : uids) {
            FirebaseFirestore.getInstance().collection("TaiKhoan").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String email = doc.getString("email");
                    String avatarUrl = doc.getString("avatarUrl");
                    String displayName = doc.getString("tenHienThi");
                    String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName
                            : ((email != null && email.contains("@")) ? email.split("@")[0] : "Đạo hữu");

                    LinearLayout row = new LinearLayout(context);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(20, 25, 20, 25);
                    ImageView imgAvatar = new ImageView(context);
                    int size = (int) (45 * context.getResources().getDisplayMetrics().density);
                    imgAvatar.setLayoutParams(new LinearLayout.LayoutParams(size, size));
                    Glide.with(context).load(avatarUrl).placeholder(R.drawable.app_icon).circleCrop().into(imgAvatar);

                    TextView tvName = new TextView(context);
                    tvName.setText(name);
                    tvName.setTextColor(Color.parseColor("#EEEEEE"));
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
        ImageView imgUserAvt, imgLikeIcon;
        View btnLikePost, btnCommentPost;

        LinearLayout layoutImageGrid, row1, row2;
        ImageView imgPost1, imgPost2, imgPost3, imgPost4;
        FrameLayout framePost4;
        TextView tvMoreImages;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvPostUser);
            tvPostTime = itemView.findViewById(R.id.tvPostTime);
            tvContent = itemView.findViewById(R.id.tvPostContent);
            tvLikeStats = itemView.findViewById(R.id.tvLikeStats);
            tvCommentStats = itemView.findViewById(R.id.tvCommentStats);
            imgUserAvt = itemView.findViewById(R.id.imgUserAvt);
            imgLikeIcon = itemView.findViewById(R.id.imgLikeIcon);
            btnLikePost = itemView.findViewById(R.id.btnLikePost);
            btnCommentPost = itemView.findViewById(R.id.btnCommentPost);

            layoutImageGrid = itemView.findViewById(R.id.layoutImageGrid);
            row1 = itemView.findViewById(R.id.row1);
            row2 = itemView.findViewById(R.id.row2);
            imgPost1 = itemView.findViewById(R.id.imgPost1);
            imgPost2 = itemView.findViewById(R.id.imgPost2);
            imgPost3 = itemView.findViewById(R.id.imgPost3);
            imgPost4 = itemView.findViewById(R.id.imgPost4);
            framePost4 = itemView.findViewById(R.id.framePost4);
            tvMoreImages = itemView.findViewById(R.id.tvMoreImages);
        }
    }
}