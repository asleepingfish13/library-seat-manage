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
import android.view.MotionEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import java.util.Calendar;

public class SeatsFragment extends Fragment {

    private GridLayout gridLayout;
    private ZoomControls zoomControls;
    private Spinner floorSpinner;
    private int currentFloor = 1;
    private float scale = 1.0f;
    private float lastX, lastY;
    private boolean isDragging = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seats, container, false);
        gridLayout = view.findViewById(R.id.grid_layout);
        zoomControls = view.findViewById(R.id.zoom_controls);
        floorSpinner = view.findViewById(R.id.floor_spinner);
        setupFloorSpinner();
        setupZoomControls();
        setupTouchListener();
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        loadSeatsData(); // 重新加载座位数据，确保显示最新的预约状态
    }
    
    private void setupFloorSpinner() {
        // 创建楼层数组
        String[] floors = {"1层", "2层", "3层", "4层"};
        
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, floors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // 设置适配器
        floorSpinner.setAdapter(adapter);
        
        // 设置默认选中第一层
        floorSpinner.setSelection(0);
        
        // 设置选择监听器
        floorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFloor = position + 1; // 楼层从1开始
                loadSeatsData(); // 加载对应楼层的座位数据
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做处理
            }
        });
    }
    
    private void setupTouchListener() {
        gridLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = event.getRawX();
                        lastY = event.getRawY();
                        isDragging = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true); // 防止父容器拦截触摸事件
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (isDragging) {
                            float deltaX = event.getRawX() - lastX;
                            float deltaY = event.getRawY() - lastY;
                            
                            // 更新网格布局的位置
                            float currentX = gridLayout.getX();
                            float currentY = gridLayout.getY();
                            gridLayout.setX(currentX + deltaX);
                            gridLayout.setY(currentY + deltaY);
                            
                            lastX = event.getRawX();
                            lastY = event.getRawY();
                            return true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isDragging = false;
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false;
            }
        });
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
            
            String jsonString;
            // 优先读取我们写入的座位数据文件
            File seatsFile = new File(getContext().getFilesDir(), "library_seats_floor" + currentFloor + ".json");
            if (seatsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(seatsFile));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();
                jsonString = builder.toString();
            } else {
                // 如果文件不存在，从raw资源中读取（暂时使用同一个文件，后续可以为每层创建不同的文件）
                InputStream inputStream = getResources().openRawResource(R.raw.library_seats);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();
                jsonString = builder.toString();
                
                // 修改JSON中的楼层信息
                JSONObject jsonObject = new JSONObject(jsonString);
                jsonObject.put("floor", currentFloor);
                jsonString = jsonObject.toString();
            }

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
                builder.setTitle(getString(R.string.seat) + " " + currentFloor + "-" + x + "-" + y);

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
                        // 过滤掉已签退的预约
                        boolean isCheckedOut = reservationObj.optBoolean("checkedOut", false);
                        if (!isCheckedOut) {
                            String seatId = reservationObj.getString("seatId");
                            // 解析座位ID，格式为"floor-x-y"
                            String[] parts = seatId.split("-");
                            if (parts.length == 3) {
                                int reservedFloor = Integer.parseInt(parts[0]);
                                int reservedX = Integer.parseInt(parts[1]);
                                int reservedY = Integer.parseInt(parts[2]);
                                
                                if (reservedFloor == currentFloor && reservedX == x && reservedY == y) {
                                    reservationsArray.put(reservationObj);
                                }
                            } else if (parts.length == 2) {
                                // 兼容旧格式
                                int reservedX = Integer.parseInt(parts[0]);
                                int reservedY = Integer.parseInt(parts[1]);
                                
                                if (reservedX == x && reservedY == y) {
                                    reservationsArray.put(reservationObj);
                                }
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

            // 自动填充当前日期和最近的整10分钟时间
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1; // 月份从0开始，需要+1
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            
            // 计算最近的整10分钟时间
            int roundedMinute = ((minute + 5) / 10) * 10;
            if (roundedMinute == 60) {
                roundedMinute = 0;
                hour += 1;
                if (hour == 24) {
                    hour = 0;
                    // 日期加一天
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    year = calendar.get(Calendar.YEAR);
                    month = calendar.get(Calendar.MONTH) + 1;
                    day = calendar.get(Calendar.DAY_OF_MONTH);
                }
            }
            
            // 填充日期
            etYear.setText(String.valueOf(year));
            etMonth.setText(String.valueOf(month));
            etDay.setText(String.valueOf(day));
            
            // 填充开始时间（最近的整10分钟）
            etStartHour.setText(String.valueOf(hour));
            etStartMinute.setText(roundedMinute < 10 ? "0" + roundedMinute : String.valueOf(roundedMinute));
            
            // 填充结束时间（开始时间+2小时）
            int endHour = hour + 2;
            int endMinute = roundedMinute;
            if (endHour >= 24) {
                endHour -= 24;
            }
            etEndHour.setText(String.valueOf(endHour));
            etEndMinute.setText(endMinute < 10 ? "0" + endMinute : String.valueOf(endMinute));

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
                            newReservation.put("seatId", currentFloor + "-" + x + "-" + y);
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
                    // 检查预约是否已经被签退
                    boolean isCheckedOut = reservationObj.optBoolean("checkedOut", false);
                    if (!isCheckedOut) {
                        String seatId = reservationObj.getString("seatId");
                        // 解析座位ID，格式为"floor-x-y"
                        String[] parts = seatId.split("-");
                        if (parts.length == 3) {
                            int floor = Integer.parseInt(parts[0]);
                            int x = Integer.parseInt(parts[1]) - 1; // 转换为0-based索引
                            int y = Integer.parseInt(parts[2]) - 1;
                            
                            // 只更新当前楼层的座位
                            if (floor == currentFloor && x >= 0 && x < gridLayout.getRowCount() && y >= 0 && y < gridLayout.getColumnCount()) {
                                View seat = gridLayout.getChildAt(x * gridLayout.getColumnCount() + y);
                                if (seat instanceof ImageButton) {
                                    ImageButton seatButton = (ImageButton) seat;
                                    // 可以设置不同的图标或颜色来表示预约状态
                                    seatButton.setColorFilter(0xFFFF0000); // 红色表示已预约
                                }
                            }
                        } else if (parts.length == 2) {
                            // 兼容旧格式
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
            }
            
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
