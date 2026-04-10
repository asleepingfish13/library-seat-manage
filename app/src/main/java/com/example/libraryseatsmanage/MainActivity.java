package com.example.libraryseatsmanage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public static JSONObject currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView register0;
        TextInputLayout zhanghao,mima;
        Button denglu;
        register0=findViewById(R.id.tv_register);
        zhanghao=findViewById(R.id.til_username);
        mima=findViewById(R.id.til_password);
        denglu= findViewById(R.id.btn_login);
        denglu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = zhanghao.getEditText().getText().toString().trim();
                String password = mima.getEditText().getText().toString().trim();
                
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入用户名和密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (validateLogin(username, password)) {
                    Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, Page1Activity.class);
                    startActivity(intent);
                    finish(); // 关闭登录页面，防止按返回键回到登录页面
                } else {
                    Toast.makeText(MainActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
        register0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validateLogin(String username, String password) {
        try {
            File file = new File(getFilesDir(), "user_data.json");
            if (!file.exists()) {
                return false;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            
            JSONArray usersArray = new JSONArray(builder.toString());
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObject = usersArray.getJSONObject(i);
                String storedUsername = userObject.getString("username");
                String storedPassword = userObject.getString("password");
                if (username.equals(storedUsername) && password.equals(storedPassword)) {
                    currentUser = userObject;
                    return true;
                }
            }
            return false;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();


    }
}
