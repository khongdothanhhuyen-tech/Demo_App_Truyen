package com.example.app_truyen.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.app_truyen.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;


public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth auth ;
    private FirebaseFirestore db ;
    private EditText  edtEmail , edtPassword , edtConfirmPassword ;
    private CheckBox cbConfirm ;

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView tvBack = findViewById(R.id.tvBack);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvLoginNow = findViewById(R.id.tvLoginNow);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        cbConfirm = findViewById(R.id.cbConfirm);

        tvBack.setOnClickListener(v -> finish());
        tvLoginNow.setOnClickListener(view -> {
            Intent intentLoginNow = new Intent(SignUpActivity.this, LoginMainActivity.class);
            startActivity(intentLoginNow);
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if(email.isEmpty()||password.isEmpty()|| confirmPassword.isEmpty()){
                Toast.makeText(SignUpActivity.this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)){
                Toast.makeText(SignUpActivity.this,    "Mật khẩu không khớp nhau!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(SignUpActivity.this, "Địa chỉ email không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6 || !Character.isUpperCase(password.charAt(0))) {
                Toast.makeText(SignUpActivity.this, "Mật khẩu phải có ít nhất 6 kí tự và viết hoa chữ cái đầu tiên!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!cbConfirm.isChecked()) {
                Toast.makeText(SignUpActivity.this, "Vui lòng đồng ý với điều khoản của chúng tôi!", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignUpActivity.this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if(user != null) {
                                Map<String, Object> chiTietNguoiDung = new HashMap<>();
                                chiTietNguoiDung.put("email", user.getEmail());
                                chiTietNguoiDung.put("ngayTao", FieldValue.serverTimestamp());
                                chiTietNguoiDung.put("role", "user");

                                String userId = user.getUid();
                                db.collection("TaiKhoan").document(userId)
                                        .set(chiTietNguoiDung)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(SignUpActivity.this, "Đăng Kí Thành Công!", Toast.LENGTH_SHORT).show();

                                            Intent intentLogin = new Intent(SignUpActivity.this, LoginMainActivity.class);
                                            intentLogin.putExtra("email", email);
                                            intentLogin.putExtra("password", password);
                                            startActivity(intentLogin);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(SignUpActivity.this, "Đăng ký thành công, nhưng lỗi tạo hồ sơ.", Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(SignUpActivity.this, "Lỗi: Không lấy được thông tin người dùng.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "Đăng ký thất bại " , Toast.LENGTH_LONG).show();
                        }
                    });
        });


    }
}
