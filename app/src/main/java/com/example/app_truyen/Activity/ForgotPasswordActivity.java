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

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText edtEmail ;
    private Button btnSent ;
    private FirebaseAuth auth ;
    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        TextView tvBack = findViewById(R.id.tvBack);
        edtEmail = findViewById(R.id.edtEmail);
        btnSent = findViewById(R.id.btnSent);
        auth = FirebaseAuth.getInstance();

        tvBack.setOnClickListener(v -> {
            Intent intentBack = new Intent(ForgotPasswordActivity.this, LoginMainActivity.class);
            startActivity(intentBack);
            finish();
        });

        // Xử lý nút GỬI
        btnSent.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isValidEmail(email)) {
                Toast.makeText(ForgotPasswordActivity.this, "Địa chỉ email không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSent.setEnabled(false);
            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                btnSent.setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Vui lòng kiểm tra hộp thư email của bạn.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Lỗi: Không thể gửi mail", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
