package com.example.app_truyen.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app_truyen.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class AdapterHistory extends RecyclerView.Adapter<AdapterHistory.ViewHolder> {

    private final List<Map<String, Object>> historyList;
    private final OnRestoreClickListener listener;

    // ===== Interface callback =====
    public interface OnRestoreClickListener {
        void onRestoreClick(Map<String, Object> historyItem);
    }

    public AdapterHistory(List<Map<String, Object>> historyList,
                          OnRestoreClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_story_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Map<String, Object> item = historyList.get(position);

        // ===== Format thời gian =====
        Timestamp ts = (Timestamp) item.get("editedAt");
        if (ts != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            holder.tvTime.setText("Thời gian: " + sdf.format(ts.toDate()));
        }

        // ===== Format thay đổi =====
        Map<String, Object> changes =
                (Map<String, Object>) item.get("changes");

        StringBuilder builder = new StringBuilder();

        for (String key : changes.keySet()) {

            Map<String, Object> change =
                    (Map<String, Object>) changes.get(key);

            String fieldName = convertFieldName(key);

            if (key.equals("anhBiaUrl")) {
                builder.append("• Ảnh bìa đã được thay đổi\n\n");
            } else {
                builder.append("• ")
                        .append(fieldName)
                        .append("\n   ")
                        .append(change.get("old"))
                        .append(" → ")
                        .append(change.get("new"))
                        .append("\n\n");
            }
        }

        holder.tvChanges.setText(builder.toString());

        // ===== Click khôi phục =====
        holder.btnRestore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestoreClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTime, tvChanges;
        Button btnRestore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTime = itemView.findViewById(R.id.tvTime);
            tvChanges = itemView.findViewById(R.id.tvChanges);
            btnRestore = itemView.findViewById(R.id.btnRestore);
        }
    }

    // ===== Convert field name sang tiếng Việt =====
    private String convertFieldName(String key) {
        switch (key) {
            case "moTa": return "Mô tả";
            case "anhBiaUrl": return "Ảnh bìa";
            case "tacGia": return "Tác giả";
            case "tenTruyen": return "Tên truyện";
            case "theLoai": return "Thể loại";
            case "allowComment": return "Cho phép bình luận";
            default: return key;
        }
    }
}