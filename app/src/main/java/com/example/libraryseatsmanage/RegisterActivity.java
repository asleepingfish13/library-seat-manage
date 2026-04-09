package com.example.libraryseatsmanage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reigister);
        
        TextView backToLogin = findViewById(R.id.tv_back_to_login);
        Button registerBtn = findViewById(R.id.btn_register);
        TextInputLayout usernameLayout = findViewById(R.id.til_register_username);
        TextInputLayout emailLayout = findViewById(R.id.til_register_email);
        TextInputLayout passwordLayout = findViewById(R.id.til_register_password);
        TextInputLayout confirmPasswordLayout = findViewById(R.id.til_register_confirm_password);
        
        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = usernameLayout.getEditText().getText().toString().trim();
                String email = emailLayout.getEditText().getText().toString().trim();
                String password = passwordLayout.getEditText().getText().toString().trim();
                String confirmPassword = confirmPasswordLayout.getEditText().getText().toString().trim();
                
                if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "请填写所有信息", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(RegisterActivity.this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (saveUserData(username, email, password)) {
                    Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // 关闭注册页面，防止按返回键回到注册页面
                } else {
                    Toast.makeText(RegisterActivity.this, "注册失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private boolean saveUserData(String username, String email, String password) {
        try {
            File file = new File(getFilesDir(), "user_data.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(username + "," + email + "," + password);
            writer.newLine();
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}