package com.example.app_truyen.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.app_truyen.R;

public class LoginAltActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_alt);

        Button btnLoginAlt = findViewById(R.id.btnLoginAlt);
        btnLoginAlt.setOnClickListener(v -> {
            Intent intent = new Intent(LoginAltActivity.this, LoginMainActivity.class);
            startActivity(intent);
        });
    }
}
