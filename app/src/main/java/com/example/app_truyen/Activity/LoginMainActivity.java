package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.app_truyen.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginMainActivity extends AppCompatActivity {
    private EditText edtEmail ;
    private EditText edtPassword ;
    private FirebaseAuth auth ;
    private FirebaseFirestore db ;

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    //Hàm phân quyền Admin và User
    private void checkRole(String uid) {
        db.collection("TaiKhoan").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("admin".equals(role)) {
                            Toast.makeText(LoginMainActivity.this, "Admin đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            Intent intentAdmin = new Intent(LoginMainActivity.this, AdminActivity.class);
                            startActivity(intentAdmin);
                            finish();
                        } else {
                            Toast.makeText(LoginMainActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            Intent intentUser = new Intent(LoginMainActivity.this, HomeActivity.class);
                            startActivity(intentUser);
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginMainActivity.this, "Lỗi: Không tìm thấy hồ sơ người dùng.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(LoginMainActivity.this, "Vui lòng kiểm tra kết nối mạng", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_main);

        TextView tvBack = findViewById(R.id.tvBack);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUpNow = findViewById(R.id.tvSignUpNow);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvBack.setOnClickListener(v -> finish());
        tvSignUpNow.setOnClickListener(v -> {
            Intent intentRegister = new Intent(LoginMainActivity.this, SignUpActivity.class);
            startActivity(intentRegister);
            finish();
        });


        //Kiểm tra nếu vừa đăng kí sẽ hiển thị luôn thông tin đăng kí
        Intent intent = getIntent();
        if (intent!=null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                edtEmail.setText(bundle.getString("email"));
                edtPassword.setText(bundle.getString("password"));
            }
        }

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString();
            String password = edtPassword.getText().toString();
            if (email.isEmpty()||password.isEmpty()){
                Toast.makeText(LoginMainActivity.this, "Vui lòng nhập đầy đủ tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(LoginMainActivity.this, "Địa chỉ email không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(LoginMainActivity.this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                checkRole(user.getUid());
                            } else {
                                Toast.makeText(LoginMainActivity.this, "Lỗi, không tìm thấy user", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginMainActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
