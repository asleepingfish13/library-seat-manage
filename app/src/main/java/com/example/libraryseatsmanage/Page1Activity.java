package com.example.libraryseatsmanage;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Page1Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page1);
        
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_seats:
                        // 显示座位信息页面
                        break;
                    case R.id.nav_reservation:
                        // 显示预约信息页面
                        break;
                    case R.id.nav_profile:
                        // 显示个人信息页面
                        break;
                }
                return true;
            }
        });
        
        // 默认选中座位信息页面
        bottomNavigationView.setSelectedItemId(R.id.nav_seats);
    }
}
