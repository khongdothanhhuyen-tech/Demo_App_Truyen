package com.example.app_truyen.Activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.example.app_truyen.Adapters.AdapterUser;
import com.example.app_truyen.Models.User;
import com.example.app_truyen.R;
import com.google.firebase.firestore.*;
import java.util.*;

public class UserManagementActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private List<User> userList;
    private AdapterUser adapter;
    private Button btnBanMultiple;
    private Button btnChangeRoleMultiple;
    private Button btnDeleteMultiple;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        btnBanMultiple = findViewById(R.id.btnBanMultiple);
        btnChangeRoleMultiple = findViewById(R.id.btnChangeRoleMultiple);
        btnDeleteMultiple = findViewById(R.id.btnDeleteMultiple);

        rvUsers = findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new AdapterUser(this, userList);
        rvUsers.setAdapter(adapter);

        btnBanMultiple.setOnClickListener(v -> {

            for (User user : userList) {

                if (user.isSelected()) {

                    boolean newStatus = !(user.getIsBanned() != null && user.getIsBanned());

                    FirebaseFirestore.getInstance()
                            .collection("TaiKhoan")
                            .document(user.getUid())
                            .update("isBanned", newStatus);

                    user.setIsBanned(newStatus);
                    user.setSelected(false);
                }
            }

            adapter.notifyDataSetChanged();
        });

        btnChangeRoleMultiple.setOnClickListener(v -> {

            for (User user : userList) {

                if (user.isSelected()) {

                    String newRole = user.getRole().equals("admin") ? "user" : "admin";

                    FirebaseFirestore.getInstance()
                            .collection("TaiKhoan")
                            .document(user.getUid())
                            .update("role", newRole);

                    user.setRole(newRole);
                    user.setSelected(false);
                }
            }

            adapter.notifyDataSetChanged();
        });

        btnDeleteMultiple.setOnClickListener(v -> {

            List<User> selectedUsers = new ArrayList<>();

            for (User user : userList) {
                if (user.isSelected()) {
                    selectedUsers.add(user);
                }
            }

            if (selectedUsers.isEmpty()) {
                Toast.makeText(this, "Chưa chọn tài khoản nào", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc muốn xóa " + selectedUsers.size() + " tài khoản?")
                    .setPositiveButton("Xóa", (dialog, which) -> {

                        for (User user : selectedUsers) {

                            FirebaseFirestore.getInstance()
                                    .collection("TaiKhoan")
                                    .document(user.getUid())
                                    .delete();

                            userList.remove(user);
                        }

                        adapter.notifyDataSetChanged();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        loadUsers();
    }

    private void loadUsers() {

        FirebaseFirestore.getInstance()
                .collection("TaiKhoan")
                .get()
                .addOnSuccessListener(query -> {

                    userList.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {

                        User user = doc.toObject(User.class);

                        if (user != null) {
                            user.setUid(doc.getId());
                            userList.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}