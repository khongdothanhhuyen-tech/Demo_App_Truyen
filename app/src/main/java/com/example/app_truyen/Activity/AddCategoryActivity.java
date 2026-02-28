package com.example.app_truyen.Activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.app_truyen.Adapters.AdapterCategoryAdmin;
import com.example.app_truyen.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddCategoryActivity extends AppCompatActivity {

    private TextInputEditText edtTheLoai;
    private Button btnAdd;
    private TextView tvBack;
    private RecyclerView rvCategories;

    private FirebaseFirestore db;
    private ArrayList<String> listTheLoai;
    private AdapterCategoryAdmin adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        edtTheLoai = findViewById(R.id.edtTheLoai);
        btnAdd = findViewById(R.id.btnAdd);
        tvBack = findViewById(R.id.tvBack);
        rvCategories = findViewById(R.id.rvCategories);

        db = FirebaseFirestore.getInstance();
        listTheLoai = new ArrayList<>();

        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdapterCategoryAdmin(this, listTheLoai, category -> showDeleteConfirmDialog(category));
        rvCategories.setAdapter(adapter);

        loadCategories();

        tvBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            String newCategory = edtTheLoai.getText().toString().trim();

            if (newCategory.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên thể loại", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isExist = false;
            for (String category : listTheLoai) {
                if (category.equalsIgnoreCase(newCategory)) {
                    isExist = true;
                    break;
                }
            }

            if (isExist) {
                Toast.makeText(this, "Thể loại này đã tồn tại!", Toast.LENGTH_SHORT).show();
                return;
            }
            showAddConfirmDialog(newCategory);
        });
    }

    // Hàm hiển thị Dialog xác nhận THÊM
    private void showAddConfirmDialog(String newCategory) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thêm")
                .setMessage("Bạn có chắc chắn muốn thêm thể loại '" + newCategory + "' không?")
                .setPositiveButton("Thêm", (dialog, which) -> {
                    // Nếu đồng ý, gọi hàm lưu lên Firestore
                    addCategoryToFirestore(newCategory);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Hàm hiển thị Dialog xác nhận XÓA
    private void showDeleteConfirmDialog(String category) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa thể loại '" + category + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteCategoryFromFirestore(category);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Tải danh sách thể loại từ Firestore
    private void loadCategories() {
        db.collection("TheLoai").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                listTheLoai.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    listTheLoai.add(document.getId());
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Lỗi tải danh sách thể loại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Thêm thể loại mới lên Firestore
    private void addCategoryToFirestore(String category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", category);

        db.collection("TheLoai").document(category)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                    edtTheLoai.setText("");
                    edtTheLoai.clearFocus();
                    loadCategories();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Xóa thể loại khỏi Firestore
    private void deleteCategoryFromFirestore(String category) {
        db.collection("TheLoai").document(category)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa thể loại", Toast.LENGTH_SHORT).show();
                    loadCategories();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}