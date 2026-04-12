package com.example.libraryseatsmanage.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class ReservationFragment extends Fragment {

    private ListView reservationList;
    private TextView emptyText;
    private ArrayList<JSONObject> reservationsList;

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
            JSONArray newReservationsArray = new JSONArray();
            reservationsList = new ArrayList<>();

            if (reservationsArray.length() == 0) {
                emptyText.setVisibility(View.VISIBLE);
                reservationList.setVisibility(View.GONE);
                return;
            }

            boolean hasChanges = false;
            for (int i = 0; i < reservationsArray.length(); i++) {
                JSONObject reservationObject = reservationsArray.getJSONObject(i);
                boolean isCheckedIn = reservationObject.optBoolean("checkedIn", false);
                
                // 检查是否已经超时20分钟，如果是且未签到则自动取消
                if (!isCheckedIn && isOverdue(reservationObject.getString("date"), reservationObject.getString("startTime"))) {
                    hasChanges = true;
                    continue; // 跳过这个预约，不添加到新数组中
                }
                
                newReservationsArray.put(reservationObject);
                reservationsList.add(reservationObject);
            }

            // 如果有预约被自动取消，更新用户数据
            if (hasChanges) {
                MainActivity.currentUser.put("reservations", newReservationsArray);
                saveUserData();
            }

            if (reservationsList.size() == 0) {
                emptyText.setVisibility(View.VISIBLE);
                reservationList.setVisibility(View.GONE);
                return;
            }

            // 排序：已签到的记录置顶，未签到的次顶，已经签退的最下
            sortReservations();

            ReservationAdapter adapter = new ReservationAdapter();
            reservationList.setAdapter(adapter);
            emptyText.setVisibility(View.GONE);
            reservationList.setVisibility(View.VISIBLE);

        } catch (JSONException e) {
            e.printStackTrace();
            emptyText.setVisibility(View.VISIBLE);
            reservationList.setVisibility(View.GONE);
        }
    }

    private void sortReservations() {
        // 排序：已签到但未签退 > 未签到 > 已签退
        java.util.Collections.sort(reservationsList, (o1, o2) -> {
            try {
                boolean o1CheckedIn = o1.optBoolean("checkedIn", false);
                boolean o1CheckedOut = o1.optBoolean("checkedOut", false);
                boolean o2CheckedIn = o2.optBoolean("checkedIn", false);
                boolean o2CheckedOut = o2.optBoolean("checkedOut", false);

                // 已签退的排在最后
                if (o1CheckedOut && !o2CheckedOut) return 1;
                if (!o1CheckedOut && o2CheckedOut) return -1;

                // 已签到但未签退的排在前面
                if (o1CheckedIn && !o2CheckedIn) return -1;
                if (!o1CheckedIn && o2CheckedIn) return 1;

                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    private void showDeleteConfirmationDialog(final int position) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("确认删除")
                .setMessage("确定要删除这条预约记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    deleteReservation(position);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteReservation(int position) {
        try {
            JSONArray reservationsArray = MainActivity.currentUser.getJSONArray("reservations");
            JSONArray newReservationsArray = new JSONArray();

            for (int i = 0; i < reservationsArray.length(); i++) {
                if (i != position) {
                    newReservationsArray.put(reservationsArray.getJSONObject(i));
                } else {
                    // 释放座位的预约占用
                    releaseSeatReservation(reservationsArray.getJSONObject(i));
                }
            }

            MainActivity.currentUser.put("reservations", newReservationsArray);
            saveUserData();
            Toast.makeText(getContext(), "删除成功", Toast.LENGTH_SHORT).show();
            loadReservationData();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isOverdue(String date, String startTime) {
        try {
            // 处理日期格式：将"2026.4.11"转换为"2026-04-11"
            String[] dateParts = date.split("\\.");
            String formattedDate = dateParts[0] + "-" + 
                    (dateParts[1].length() == 1 ? "0" + dateParts[1] : dateParts[1]) + "-" + 
                    (dateParts[2].length() == 1 ? "0" + dateParts[2] : dateParts[2]);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date reservationTime = sdf.parse(formattedDate + " " + startTime);
            Date currentTime = new Date();

            // 计算时间差（分钟）
            long diff = (currentTime.getTime() - reservationTime.getTime()) / (1000 * 60);

            // 超时20分钟
            return diff > 20;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private class ReservationAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return reservationsList.size();
        }

        @Override
        public Object getItem(int position) {
            return reservationsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ViewHolder holder;

            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.reservation_item, parent, false);
                holder = new ViewHolder();
                holder.seatInfo = view.findViewById(R.id.seat_info);
                holder.checkInButton = view.findViewById(R.id.check_in_button);
                holder.cancelButton = view.findViewById(R.id.cancel_button);
                holder.deleteButton = view.findViewById(R.id.delete_button);
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ViewHolder) view.getTag();
            }

            // 删除按钮点击事件
            holder.deleteButton.setOnClickListener(v -> {
                showDeleteConfirmationDialog(position);
            });

            try {
                JSONObject reservation = reservationsList.get(position);
                String seatId = reservation.getString("seatId");
                String date = reservation.getString("date");
                String startTime = reservation.getString("startTime");
                String endTime = reservation.getString("endTime");

                holder.seatInfo.setText(getString(R.string.seat) + "：" + seatId + "\n" + 
                        getString(R.string.date) + date + "\n" + 
                        getString(R.string.time) + startTime + "-" + endTime);

                // 检查是否已经签到
                boolean isCheckedIn = reservation.optBoolean("checkedIn", false);
                boolean isCheckedOut = reservation.optBoolean("checkedOut", false);
                
                if (isCheckedOut) {
                    holder.checkInButton.setText("已签退");
                    holder.checkInButton.setEnabled(false);
                    holder.cancelButton.setText("已完成");
                    holder.cancelButton.setEnabled(false);
                } else if (isCheckedIn) {
                    holder.checkInButton.setText("已签到");
                    holder.checkInButton.setEnabled(false);
                    holder.cancelButton.setText("签退");
                    holder.cancelButton.setEnabled(true);
                    holder.cancelButton.setOnClickListener(v -> {
                        checkOutReservation(position);
                    });
                } else {
                    // 检查是否可以签到
                    if (canCheckIn(date, startTime)) {
                        holder.checkInButton.setEnabled(true);
                        holder.checkInButton.setText("签到");
                    } else {
                        holder.checkInButton.setEnabled(false);
                        holder.checkInButton.setText("无法签到");
                    }
                    holder.cancelButton.setText("取消预约");
                    holder.cancelButton.setEnabled(true);
                    holder.cancelButton.setOnClickListener(v -> {
                        cancelReservation(position);
                    });
                }

                // 签到按钮
                holder.checkInButton.setOnClickListener(v -> {
                    checkInReservation(position);
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return view;
        }

        private class ViewHolder {
            TextView seatInfo;
            Button checkInButton;
            Button cancelButton;
            ImageView deleteButton;
        }
    }

    private boolean canCheckIn(String date, String startTime) {
        try {
            // 处理日期格式：将"2026.4.11"转换为"2026-04-11"
            String[] dateParts = date.split("\\.");
            String formattedDate = dateParts[0] + "-" + 
                    (dateParts[1].length() == 1 ? "0" + dateParts[1] : dateParts[1]) + "-" + 
                    (dateParts[2].length() == 1 ? "0" + dateParts[2] : dateParts[2]);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Date reservationTime = sdf.parse(formattedDate + " " + startTime);
            Date currentTime = new Date();

            // 计算时间差（分钟）
            long diff = (reservationTime.getTime() - currentTime.getTime()) / (1000 * 60);

            // 打印调试信息
            System.out.println("Reservation time: " + sdf.format(reservationTime));
            System.out.println("Current time: " + sdf.format(currentTime));
            System.out.println("Time difference (minutes): " + diff);

            // 允许提前10分钟签到，且未超时20分钟
            return diff <= 10 && diff >= -20;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void checkInReservation(int position) {
        try {
            JSONObject reservation = reservationsList.get(position);
            reservation.put("checkedIn", true);
            saveUserData();
            Toast.makeText(getContext(), "签到成功", Toast.LENGTH_SHORT).show();
            loadReservationData();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "签到失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkOutReservation(int position) {
        try {
            JSONObject reservation = reservationsList.get(position);
            reservation.put("checkedOut", true);
            reservation.put("checkOutTime", new Date().toString());
            
            // 释放座位的预约占用
            releaseSeatReservation(reservation);
            
            // 从用户预约列表中删除该预约
            JSONArray reservationsArray = MainActivity.currentUser.getJSONArray("reservations");
            JSONArray newReservationsArray = new JSONArray();
            
            for (int i = 0; i < reservationsArray.length(); i++) {
                if (i != position) {
                    newReservationsArray.put(reservationsArray.getJSONObject(i));
                }
            }
            
            MainActivity.currentUser.put("reservations", newReservationsArray);
            
            // 将签退的预约添加到历史记录中
            if (!MainActivity.currentUser.has("historyReservations")) {
                MainActivity.currentUser.put("historyReservations", new JSONArray());
            }
            JSONArray historyArray = MainActivity.currentUser.getJSONArray("historyReservations");
            historyArray.put(reservation);
            
            saveUserData();
            Toast.makeText(getContext(), "签退成功", Toast.LENGTH_SHORT).show();
            loadReservationData();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "签退失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseSeatReservation(JSONObject reservation) {
        try {
            // 读取座位数据
            String seatId = reservation.getString("seatId");
            String date = reservation.getString("date");
            String startTime = reservation.getString("startTime");
            String endTime = reservation.getString("endTime");
            
            // 读取座位文件
            java.io.InputStream inputStream = getContext().getResources().openRawResource(R.raw.library_seats);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            
            JSONObject seatsData = new JSONObject(builder.toString());
            JSONArray seatsArray = seatsData.getJSONArray("seats");
            
            // 查找并更新座位信息
            for (int i = 0; i < seatsArray.length(); i++) {
                JSONObject seat = seatsArray.getJSONObject(i);
                String currentSeatId = seat.getInt("x") + "-" + seat.getInt("y");
                if (currentSeatId.equals(seatId)) {
                    JSONArray reservationsArray = seat.getJSONArray("reservations");
                    JSONArray newReservationsArray = new JSONArray();
                    
                    // 移除该预约
                    for (int j = 0; j < reservationsArray.length(); j++) {
                        JSONObject seatReservation = reservationsArray.getJSONObject(j);
                        if (!seatReservation.getString("date").equals(date) ||
                            !seatReservation.getString("startTime").equals(startTime) ||
                            !seatReservation.getString("endTime").equals(endTime)) {
                            newReservationsArray.put(seatReservation);
                        }
                    }
                    
                    seat.put("reservations", newReservationsArray);
                    // 如果没有预约了，将座位状态设置为可用
                    if (newReservationsArray.length() == 0) {
                        seat.put("status", 1);
                    }
                    break;
                }
            }
            
            // 写回座位数据到文件
            // 注意：由于raw资源是只读的，我们需要将数据写入到其他位置
            File seatsFile = new File(getContext().getFilesDir(), "library_seats.json");
            FileWriter writer = new FileWriter(seatsFile);
            writer.write(seatsData.toString());
            writer.close();
            
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelReservation(int position) {
        try {
            JSONArray reservationsArray = MainActivity.currentUser.getJSONArray("reservations");
            JSONArray newReservationsArray = new JSONArray();

            for (int i = 0; i < reservationsArray.length(); i++) {
                if (i != position) {
                    newReservationsArray.put(reservationsArray.getJSONObject(i));
                } else {
                    // 释放座位的预约占用
                    releaseSeatReservation(reservationsArray.getJSONObject(i));
                }
            }

            MainActivity.currentUser.put("reservations", newReservationsArray);
            saveUserData();
            Toast.makeText(getContext(), "取消预约成功", Toast.LENGTH_SHORT).show();
            loadReservationData();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "取消预约失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData() {
        try {
            File file = new File(getContext().getFilesDir(), "user_data.json");
            JSONArray usersArray = new JSONArray();

            // 读取现有用户数据
            if (file.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                reader.close();
                usersArray = new JSONArray(builder.toString());
            }

            // 更新当前用户数据
            for (int i = 0; i < usersArray.length(); i++) {
                JSONObject user = usersArray.getJSONObject(i);
                if (user.getString("username").equals(MainActivity.currentUser.getString("username"))) {
                    usersArray.put(i, MainActivity.currentUser);
                    break;
                }
            }

            // 写入文件
            FileWriter writer = new FileWriter(file);
            writer.write(usersArray.toString());
            writer.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}
