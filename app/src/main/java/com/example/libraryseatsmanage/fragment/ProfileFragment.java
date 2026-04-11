package com.example.libraryseatsmanage.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.libraryseatsmanage.MainActivity;
import com.example.libraryseatsmanage.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProfileFragment extends Fragment {

    private TextView tvUsername;
    private TextView tvEmail;
    private Button btnLogout;
    private Button btnCheckinCheckout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        tvUsername = view.findViewById(R.id.tv_username);
        tvEmail = view.findViewById(R.id.tv_email);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnCheckinCheckout = view.findViewById(R.id.btn_checkin_checkout);

        loadUserData();
        updateCheckinCheckoutButton();

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });

        btnCheckinCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleCheckinCheckout();
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
                checkExpiredReservations();
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

    private void checkExpiredReservations() throws JSONException {
        JSONArray reservationsArray = MainActivity.currentUser.getJSONArray("reservations");
        boolean autoCheckedOut = false;

        for (int i = 0; i < reservationsArray.length(); i++) {
            JSONObject reservation = reservationsArray.getJSONObject(i);
            if (reservation.has("checkedIn") && reservation.getBoolean("checkedIn")) {
                if (!isReservationValid(reservation)) {
                    checkout(reservation);
                    autoCheckedOut = true;
                }
            }
        }

        if (autoCheckedOut) {
            Toast.makeText(getContext(), "部分预约已自动签退", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCheckinCheckoutButton() {
        if (MainActivity.currentUser == null) {
            btnCheckinCheckout.setEnabled(false);
            return;
        }

        try {
            JSONArray reservationsArray = MainActivity.currentUser.getJSONArray("reservations");
            boolean hasCheckedInReservation = false;

            for (int i = 0; i < reservationsArray.length(); i++) {
                JSONObject reservation = reservationsArray.getJSONObject(i);
                if (reservation.has("checkedIn") && reservation.getBoolean("checkedIn")) {
                    hasCheckedInReservation = true;
                    break;
                }
            }

            if (hasCheckedInReservation) {
                btnCheckinCheckout.setText(R.string.checkout);
            } else {
                btnCheckinCheckout.setText(R.string.checkin);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            btnCheckinCheckout.setText(R.string.checkin);
        }
    }

    private void handleCheckinCheckout() {
        if (MainActivity.currentUser == null) {
            Toast.makeText(getContext(), R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray reservationsArray = MainActivity.currentUser.getJSONArray("reservations");
            if (reservationsArray.length() == 0) {
                Toast.makeText(getContext(), R.string.no_reservation_to_checkin, Toast.LENGTH_SHORT).show();
                return;
            }

            boolean hasCheckedInReservation = false;
            for (int i = 0; i < reservationsArray.length(); i++) {
                JSONObject reservation = reservationsArray.getJSONObject(i);
                if (reservation.has("checkedIn") && reservation.getBoolean("checkedIn")) {
                    hasCheckedInReservation = true;
                    checkout(reservation);
                    break;
                }
            }

            if (!hasCheckedInReservation) {
                boolean checkedIn = false;
                for (int i = 0; i < reservationsArray.length(); i++) {
                    JSONObject reservation = reservationsArray.getJSONObject(i);
                    if (isReservationValid(reservation)) {
                        checkin(reservation);
                        checkedIn = true;
                        break;
                    }
                }
                if (!checkedIn) {
                    Toast.makeText(getContext(), R.string.no_reservation_to_checkin, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "操作失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkin(JSONObject reservation) throws JSONException {
        reservation.put("checkedIn", true);
        reservation.put("checkinTime", getCurrentTime());
        Toast.makeText(getContext(), R.string.checkin_success, Toast.LENGTH_SHORT).show();
        updateCheckinCheckoutButton();
    }

    private void checkout(JSONObject reservation) throws JSONException {
        reservation.put("checkedIn", false);
        reservation.put("checkoutTime", getCurrentTime());
        Toast.makeText(getContext(), R.string.checkout_success, Toast.LENGTH_SHORT).show();
        updateCheckinCheckoutButton();
    }

    private boolean isReservationValid(JSONObject reservation) throws JSONException {
        String date = reservation.getString("date");
        String startTime = reservation.getString("startTime");
        String endTime = reservation.getString("endTime");
        
        // 获取当前手机时间
        Date currentDate = new Date();
        
        // 解析预约时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            String reservationStartDateTime = date + " " + startTime;
            String reservationEndDateTime = date + " " + endTime;
            Date reservationStart = sdf.parse(reservationStartDateTime);
            Date reservationEnd = sdf.parse(reservationEndDateTime);
            
            // 检查当前时间是否在预约时间范围内
            return currentDate.after(reservationStart) && currentDate.before(reservationEnd);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getCurrentDate() {
        // 使用手机当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    private String getCurrentTime() {
        // 使用手机当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        return sdf.format(new Date());
    }

    private void logout() {
        MainActivity.currentUser = null;
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        getActivity().finish();
    }
}
