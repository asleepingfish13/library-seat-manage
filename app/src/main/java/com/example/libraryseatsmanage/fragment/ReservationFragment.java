package com.example.libraryseatsmanage.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.libraryseatsmanage.MainActivity;
import com.example.libraryseatsmanage.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReservationFragment extends Fragment {

    private ListView reservationList;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reservation, container, false);
        reservationList = view.findViewById(R.id.reservation_list);
        emptyText = view.findViewById(R.id.empty_text);
        loadReservationData();
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadReservationData(); // 重新加载预约信息，确保显示最新数据
    }

    private void loadReservationData() {
        try {
            if (MainActivity.currentUser == null) {
                emptyText.setVisibility(View.VISIBLE);
                reservationList.setVisibility(View.GONE);
                return;
            }

            JSONArray reservationsArray = MainActivity.currentUser.getJSONArray("reservations");
            ArrayList<String> reservationListItems = new ArrayList<>();

            if (reservationsArray.length() == 0) {
                emptyText.setVisibility(View.VISIBLE);
                reservationList.setVisibility(View.GONE);
                return;
            }

            for (int i = 0; i < reservationsArray.length(); i++) {
                JSONObject reservationObject = reservationsArray.getJSONObject(i);
                String seatId = reservationObject.getString("seatId");
                String date = reservationObject.getString("date");
                String startTime = reservationObject.getString("startTime");
                String endTime = reservationObject.getString("endTime");

                reservationListItems.add(getString(R.string.seat) + "：" + seatId + "\n" + getString(R.string.date) + date + "\n" + getString(R.string.time) + startTime + "-" + endTime);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, reservationListItems);
            reservationList.setAdapter(adapter);
            emptyText.setVisibility(View.GONE);
            reservationList.setVisibility(View.VISIBLE);

        } catch (JSONException e) {
            e.printStackTrace();
            emptyText.setVisibility(View.VISIBLE);
            reservationList.setVisibility(View.GONE);
        }
    }
}
