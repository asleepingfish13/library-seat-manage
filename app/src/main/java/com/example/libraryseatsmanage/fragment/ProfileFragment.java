package com.example.libraryseatsmanage.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.libraryseatsmanage.MainActivity;
import com.example.libraryseatsmanage.R;

import org.json.JSONException;

public class ProfileFragment extends Fragment {

    private TextView tvUsername;
    private TextView tvEmail;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        btnLogout = view.findViewById(R.id.btn_logout);

        loadUserData();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        return view;
    }

    private void loadUserData() {
        try {
            if (MainActivity.currentUser != null) {
                String username = MainActivity.currentUser.getString("username");
                String email = MainActivity.currentUser.getString("email");
                tvUsername.setText(username);
                tvEmail.setText(email);
            } else {
                tvUsername.setText(R.string.not_logged_in);
                tvEmail.setText(R.string.not_logged_in);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            tvUsername.setText(R.string.not_logged_in);
            tvEmail.setText(R.string.not_logged_in);
        }
    }

    private void logout() {
        MainActivity.currentUser = null;
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        getActivity().finish();
    }
}
