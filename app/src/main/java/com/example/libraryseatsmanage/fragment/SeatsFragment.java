package com.example.libraryseatsmanage.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.libraryseatsmanage.MainActivity;
import com.example.libraryseatsmanage.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SeatsFragment extends Fragment {

    private GridLayout gridLayout;
    private ZoomControls zoomControls;
    private float scale = 1.0f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seats, container, false);
        gridLayout = view.findViewById(R.id.grid_layout);
        zoomControls = view.findViewById(R.id.zoom_controls);
        loadSeatsData();
        setupZoomControls();
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadSeatsData(); // 重新加载座位数据，确保显示最新的预约状态
    }
    
    private void setupZoomControls() {
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scale < 2.0f) {
                    scale += 0.1f;
                    gridLayout.setScaleX(scale);
                    gridLayout.setScaleY(scale);
                }
            }
        });
        
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scale > 0.5f) {
                    scale -= 0.1f;
                    gridLayout.setScaleX(scale);
                    gridLayout.setScaleY(scale);
                }
            }
        });
    }

    private void loadSeatsData() {
        try {
            // 清空之前的视图
            gridLayout.removeAllViews();
            
            InputStream inputStream = getResources().openRawResource(R.raw.library_seats);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonString = builder.toString();

            JSONObject jsonObject = new JSONObject(jsonString);
            int floor = jsonObject.getInt("floor");
            JSONArray seatsArray = jsonObject.getJSONArray("seats");

            // 找出最大的行和列数
            int maxRow = 0;
            int maxCol = 0;
            for (int i = 0; i < seatsArray.length(); i++) {
                JSONObject seatObject = seatsArray.getJSONObject(i);
                int x = seatObject.getInt("x");
                int y = seatObject.getInt("y");
                maxRow = Math.max(maxRow, x);
                maxCol = Math.max(maxCol, y);
            }

            // 设置网格布局的行列数
            gridLayout.setRowCount(maxRow);
            gridLayout.setColumnCount(maxCol);

            // 创建一个二维数组来存储座位
            ImageButton[][] seats = new ImageButton[maxRow][maxCol];

            // 初始化所有座位为空白
            for (int i = 0; i < maxRow; i++) {
                for (int j = 0; j < maxCol; j++) {
                    ImageButton seat = new ImageButton(getContext());
                    seat.setBackgroundResource(android.R.color.transparent);
                    seat.setEnabled(false);
                    seat.setScaleType(ImageView.ScaleType.CENTER);
                    seat.setMaxWidth(60);
                    seat.setMaxHeight(60);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                    params.rowSpec = GridLayout.spec(i);
                    params.columnSpec = GridLayout.spec(j);
                    params.setMargins(4, 4, 4, 4);
                    seat.setLayoutParams(params);
                    gridLayout.addView(seat);
                    seats[i][j] = seat;
                }
            }

            // 根据JSON数据设置座位
            for (int i = 0; i < seatsArray.length(); i++) {
                JSONObject seatObject = seatsArray.getJSONObject(i);
                int x = seatObject.getInt("x") - 1; // 转换为0-based索引
                int y = seatObject.getInt("y") - 1;
                int status = seatObject.getInt("status");

                if (status == 1) {
                    ImageButton seat = seats[x][y];
                    seat.setImageResource(R.drawable.ic_seat);
                    seat.setEnabled(true);
                    seat.setOnClickListener(new SeatClickListener(seatObject));
                }
            }
            
            // 更新座位预约状态
            updateSeatReservationStatus();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.load_seats_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private class SeatClickListener implements View.OnClickListener {
        private JSONObject seatObject;

        public SeatClickListener(JSONObject seatObject) {
            this.seatObject = seatObject;
        }

        @Override
        public void onClick(View view) {
            try {
                int x = seatObject.getInt("x");
                int y = seatObject.getInt("y");
                JSONArray reservationsArray = getSeatReservations(x, y);

                // 创建预约详情对话框
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
                builder.setTitle(getString(R.string.seat) + " " + x + "-" + y);

                StringBuilder message = new StringBuilder();
                if (reservationsArray.length() == 0) {
                    message.append(getString(R.string.no_reservation));
                } else {
                    message.append(getString(R.string.reserved_time) + "\n");
                    for (int i = 0; i < reservationsArray.length(); i++) {
                        JSONObject reservationObject = reservationsArray.getJSONObject(i);
                        String date = reservationObject.getString("date");
                        String startTime = reservationObject.getString("startTime");
                        String endTime = reservationObject.getString("endTime");
                        message.append(date + " " + startTime + "-" + endTime + "\n");
                    }
                }

                builder.setMessage(message.toString());
                builder.setPositiveButton(R.string.cancel, null);
                builder.setNegativeButton(R.string.reservation, (dialog, which) -> {
                    showCalendarDialog(x, y);
                });

                builder.show();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        private JSONArray getSeatReservations(int x, int y) {
            JSONArray reservationsArray = new JSONArray();
            try {
                // 读取所有用户的预约信息
                File userFile = new File(getContext().getFilesDir(), "user_data.json");
                if (!userFile.exists()) {
                    return reservationsArray;
                }
                
                BufferedReader reader = new BufferedReader(new FileReader(userFile));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();
                
                JSONArray usersArray = new JSONArray(builder.toString());
                
                // 遍历所有用户的预约信息
                for (int i = 0; i < usersArray.length(); i++) {
                    JSONObject userObj = usersArray.getJSONObject(i);
                    JSONArray userReservationsArray = userObj.getJSONArray("reservations");
                    
                    for (int j = 0; j < userReservationsArray.length(); j++) {
                        JSONObject reservationObj = userReservationsArray.getJSONObject(j);
                        String seatId = reservationObj.getString("seatId");
                        // 解析座位ID，格式为"x-y"
                        String[] parts = seatId.split("-");
                        if (parts.length == 2) {
                            int reservedX = Integer.parseInt(parts[0]);
                            int reservedY = Integer.parseInt(parts[1]);
                            
                            if (reservedX == x && reservedY == y) {
                                reservationsArray.put(reservationObj);
                            }
                        }
                    }
                }
                
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return reservationsArray;
        }

        private void showCalendarDialog(final int x, final int y) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_calendar, null);
            builder.setView(dialogView);

            final EditText etYear = dialogView.findViewById(R.id.et_year);
            final EditText etMonth = dialogView.findViewById(R.id.et_month);
            final EditText etDay = dialogView.findViewById(R.id.et_day);
            final EditText etStartHour = dialogView.findViewById(R.id.et_start_hour);
            final EditText etStartMinute = dialogView.findViewById(R.id.et_start_minute);
            final EditText etEndHour = dialogView.findViewById(R.id.et_end_hour);
            final EditText etEndMinute = dialogView.findViewById(R.id.et_end_minute);
            Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
            Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

            final android.app.AlertDialog dialog = builder.create();

            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });

            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        String year = etYear.getText().toString().trim();
                        String month = etMonth.getText().toString().trim();
                        String day = etDay.getText().toString().trim();
                        String startHour = etStartHour.getText().toString().trim();
                        String startMinute = etStartMinute.getText().toString().trim();
                        String endHour = etEndHour.getText().toString().trim();
                        String endMinute = etEndMinute.getText().toString().trim();

                        if (year.isEmpty() || month.isEmpty() || day.isEmpty() ||
                                startHour.isEmpty() || startMinute.isEmpty() ||
                                endHour.isEmpty() || endMinute.isEmpty()) {
                            Toast.makeText(getContext(), R.string.please_fill_all, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String date = year + "." + month + "." + day;
                        String startTime = startHour + ":" + startMinute;
                        String endTime = endHour + ":" + endMinute;

                        if (saveReservation(x, y, date, startTime, endTime)) {
                            Toast.makeText(getContext(), R.string.reservation_success, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadSeatsData(); // 重新加载座位数据
                        } else {
                            Toast.makeText(getContext(), R.string.reservation_failed, Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), R.string.reservation_failed, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.show();
        }

        private boolean saveReservation(int x, int y, String date, String startTime, String endTime) {
            try {
                // 更新用户预约信息
                if (MainActivity.currentUser != null) {
                    File userFile = new File(getContext().getFilesDir(), "user_data.json");
                    BufferedReader reader = new BufferedReader(new FileReader(userFile));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    reader.close();

                    JSONArray usersArray = new JSONArray(builder.toString());
                    String username = MainActivity.currentUser.getString("username");

                    boolean userUpdated = false;
                    for (int i = 0; i < usersArray.length(); i++) {
                        JSONObject userObj = usersArray.getJSONObject(i);
                        if (userObj.getString("username").equals(username)) {
                            JSONArray userReservationsArray = userObj.getJSONArray("reservations");
                            JSONObject newReservation = new JSONObject();
                            newReservation.put("seatId", x + "-" + y);
                            newReservation.put("date", date);
                            newReservation.put("startTime", startTime);
                            newReservation.put("endTime", endTime);
                            userReservationsArray.put(newReservation);

                            BufferedWriter writer = new BufferedWriter(new FileWriter(userFile));
                            writer.write(usersArray.toString());
                            writer.close();
                            userUpdated = true;
                            // 更新currentUser，使其包含最新的预约信息
                            MainActivity.currentUser = userObj;
                            break;
                        }
                    }
                    return userUpdated;
                }

                return false;

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    
    private void updateSeatReservationStatus() {
        try {
            // 读取所有用户的预约信息
            File userFile = new File(getContext().getFilesDir(), "user_data.json");
            if (!userFile.exists()) {
                return;
            }
            
            BufferedReader reader = new BufferedReader(new FileReader(userFile));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            
            JSONArray usersArray = new JSONArray(builder.toString());
            
            // 遍历所有用户的预约信息
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject userObj = usersArray.getJSONObject(i);
                JSONArray reservationsArray = userObj.getJSONArray("reservations");
                
                for (int j = 0; j < reservationsArray.length(); j++) {
                    JSONObject reservationObj = reservationsArray.getJSONObject(j);
                    String seatId = reservationObj.getString("seatId");
                    // 解析座位ID，格式为"x-y"
                    String[] parts = seatId.split("-");
                    if (parts.length == 2) {
                        int x = Integer.parseInt(parts[0]) - 1; // 转换为0-based索引
                        int y = Integer.parseInt(parts[1]) - 1;
                        
                        // 更新座位图标，标记为已预约
                        if (x >= 0 && x < gridLayout.getRowCount() && y >= 0 && y < gridLayout.getColumnCount()) {
                            View seat = gridLayout.getChildAt(x * gridLayout.getColumnCount() + y);
                            if (seat instanceof ImageButton) {
                                ImageButton seatButton = (ImageButton) seat;
                                // 可以设置不同的图标或颜色来表示预约状态
                                seatButton.setColorFilter(0xFFFF0000); // 红色表示已预约
                            }
                        }
                    }
                }
            }
            
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
