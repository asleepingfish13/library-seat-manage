package com.example.libraryseatsmanage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.libraryseatsmanage.fragment.ReservationFragment;
import com.example.libraryseatsmanage.fragment.SeatsFragment;
import com.example.libraryseatsmanage.fragment.ProfileFragment;
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
                        replaceFragment(new SeatsFragment());
                        break;
                    case R.id.nav_reservation:
                        replaceFragment(new ReservationFragment());
                        break;
                    case R.id.nav_profile:
                        replaceFragment(new ProfileFragment());
                        break;
                }
                return true;
            }
        });
        
        // 默认选中座位信息页面
        bottomNavigationView.setSelectedItemId(R.id.nav_seats);
    }
    
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}
