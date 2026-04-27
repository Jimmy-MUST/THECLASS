package com.example.theclass;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;

import android.provider.MediaStore;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

import android.graphics.Bitmap;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

public class Function extends AppCompatActivity {
    private static final String DATABASE_URL = "https://the-class-6ced7-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private String pendingCourseForPhoto = null;

    private static final int REQUEST_BLUETOOTH_PERMISSION = 101;
    private static final int REQUEST_ENABLE_BT = 102;

    private static final String TARGET_BEACON_UUID = "0112233445566778899AABBCCDDEEFF0";
    private static final int TARGET_BEACON_MAJOR = 0x0708;
    private static final int TARGET_BEACON_MINOR = 0x0506;

    private boolean isBeaconConnected = false;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    private Handler scanHandler = new Handler();
    private Runnable scanRunnable;
    private ScanSettings scanSettings;

    private HashMap<String, Boolean> attendanceMap = new HashMap<>();
    private HashMap<String, Button> buttonMap = new HashMap<>();

    private String currentUsername;

    private TeachableMachine classifier;

    private ValueEventListener attendanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_function);

        Face.initOpenCV(this);

        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null) {
            currentUsername = "user1";
        }

        classifier = TeachableMachine.getInstance(this);


        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            }
        }

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Open the Bluetooth", Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         //   startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        checkBluetoothPermissions();

        LinearLayout courseContainer = findViewById(R.id.course_container);
        ArrayList<String> courseList = Manage.getCourseList();

        if (courseList.isEmpty()) {
            TextView noCourseText = new TextView(this);
            noCourseText.setText("No courses available");
            noCourseText.setTextSize(16);
            noCourseText.setPadding(0, 20, 0, 20);
            courseContainer.addView(noCourseText);
        } else {
            for (String course : courseList) {
                if (!attendanceMap.containsKey(course)) {
                    attendanceMap.put(course, false);
                }

                LinearLayout courseItem = new LinearLayout(this);
                courseItem.setOrientation(LinearLayout.HORIZONTAL);
                courseItem.setPadding(0, 16, 0, 16);

                TextView courseName = new TextView(this);
                courseName.setText(course);
                courseName.setTextSize(18);
                courseName.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

                Button attendanceBtn = new Button(this);
                attendanceBtn.setTag(course);
                buttonMap.put(course, attendanceBtn);

                if (attendanceMap.get(course)) {
                    attendanceBtn.setText("Signed");
                    attendanceBtn.setBackgroundColor(0xFF4CAF50);
                    attendanceBtn.setEnabled(false);
                } else {
                    attendanceBtn.setText("Sign In");
                    attendanceBtn.setBackgroundColor(0xFF9E9E9E);
                    attendanceBtn.setEnabled(false);
                }

                attendanceBtn.setOnClickListener(v -> {
                    String courseName1 = (String) v.getTag();
                    if (isBeaconConnected) {
                        pendingCourseForPhoto = courseName1;
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        } else {
                            Toast.makeText(Function.this, "No camera app found", Toast.LENGTH_SHORT).show();
                            pendingCourseForPhoto = null;
                        }
                    } else {
                        Toast.makeText(Function.this, "Please connect to Bluetooth beacon first", Toast.LENGTH_SHORT).show();
                    }
                });

                courseItem.addView(courseName);
                courseItem.addView(attendanceBtn);
                courseContainer.addView(courseItem);
            }
        }

        startFirebaseListener();

        Button btnReturn = findViewById(R.id.btn_logout);
        btnReturn.setOnClickListener(v -> {
            if (classifier != null) {
                classifier.close();
            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void startFirebaseListener() {
        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);
        DatabaseReference attendanceRef = database.getReference("attendance")
                .child(currentUsername);

        if (attendanceListener != null) {
            attendanceRef.removeEventListener(attendanceListener);
        }

        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (String course : Manage.getCourseList()) {
                    Boolean status = snapshot.child(course).getValue(Boolean.class);
                    if (status != null) {
                        attendanceMap.put(course, status);
                        updateButtonUI(course, status);
                    } else {
                        attendanceMap.put(course, false);
                        updateButtonUI(course, false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };
        attendanceRef.addValueEventListener(attendanceListener);
    }

    private void updateButtonUI(String course, boolean isSigned) {
        Button btn = buttonMap.get(course);
        if (btn != null) {
            if (isSigned) {
                btn.setText("Signed");
                btn.setBackgroundColor(0xFF4CAF50);
                btn.setEnabled(false);
            } else {
                btn.setText("Sign In");
                if (isBeaconConnected) {
                    btn.setBackgroundColor(0xFF2196F3);
                    btn.setEnabled(true);
                } else {
                    btn.setBackgroundColor(0xFF9E9E9E);
                    btn.setEnabled(false);
                }
            }
        }
    }

    private void saveAttendanceToFirebase(String course) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        HashMap<String, Object> record = new HashMap<>();
        record.put("username", currentUsername);
        record.put("course", course);
        record.put("time", currentTime);
        record.put("status", "success");

        FirebaseDatabase database = FirebaseDatabase.getInstance(DATABASE_URL);

        DatabaseReference recordsRef = database.getReference("signin_records");
        recordsRef.push().setValue(record);

        DatabaseReference attendanceRef = database.getReference("attendance")
                .child(currentUsername)
                .child(course);
        attendanceRef.setValue(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap photo = (Bitmap) extras.get("data");

                if (photo != null && pendingCourseForPhoto != null) {
                    if (classifier == null) {
                        Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
                        pendingCourseForPhoto = null;
                        photo.recycle();
                        return;
                    }

                    TeachableMachine.RecognitionResult result = classifier.recognize(photo);

                    if (result.isSuccess() && result.username != null) {
                        String recognizedUser = result.username.trim();
                        String loggedInUser = currentUsername.trim();

                        if (recognizedUser.equals(loggedInUser)) {
                            // 签到成功
                            saveAttendanceToFirebase(pendingCourseForPhoto);
                            attendanceMap.put(pendingCourseForPhoto, true);
                            updateAttendanceButton(pendingCourseForPhoto);
                            Toast.makeText(this, "Success", Toast.LENGTH_LONG).show();
                        } else {
                            // 人脸不匹配
                            Toast.makeText(this, "Fail", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        // 识别失败
                        Toast.makeText(this, "Fail", Toast.LENGTH_LONG).show();
                    }

                    pendingCourseForPhoto = null;
                    photo.recycle();
                }
            }
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            checkBluetoothPermissions();
        }
    }

    private void updateAttendanceButton(String courseName) {
        LinearLayout container = findViewById(R.id.course_container);
        if (container == null) return;

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout courseItem = (LinearLayout) child;
                for (int j = 0; j < courseItem.getChildCount(); j++) {
                    View innerChild = courseItem.getChildAt(j);
                    if (innerChild instanceof Button) {
                        Button btn = (Button) innerChild;
                        if (courseName.equals(btn.getTag())) {
                            btn.setText("Signed");
                            btn.setBackgroundColor(0xFF4CAF50);
                            btn.setEnabled(false);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, REQUEST_BLUETOOTH_PERMISSION);
                return;
            }
        }
        startScanning();
    }

    private void startScanning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return;
        }

        if (bluetoothLeScanner == null) {
            return;
        }

        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

            bluetoothLeScanner.startScan(null, scanSettings, scanCallback);

        scanRunnable = () -> {
                if (bluetoothLeScanner != null && scanCallback != null) {
                    bluetoothLeScanner.stopScan(scanCallback);
                    bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
                }
        };
        scanHandler.postDelayed(scanRunnable, 5000);
    }

    private boolean parseAndMatchBeacon(byte[] scanRecord) {
        if (scanRecord.length > 28 &&
                scanRecord[5] == 0x4C && scanRecord[6] == 0x00 &&
                scanRecord[7] == 0x02 && scanRecord[8] == 0x15) {

            StringBuilder uuidBuilder = new StringBuilder();
            for (int i = 9; i <= 24; i++) {
                uuidBuilder.append(String.format("%02x", scanRecord[i]));
            }
            String uuid = uuidBuilder.toString();

            int major = ((scanRecord[25] & 0xFF) << 8) | (scanRecord[26] & 0xFF);
            int minor = ((scanRecord[27] & 0xFF) << 8) | (scanRecord[28] & 0xFF);

            if (uuid.equalsIgnoreCase(TARGET_BEACON_UUID) &&
                    major == TARGET_BEACON_MAJOR &&
                    minor == TARGET_BEACON_MINOR) {
                return true;
            }
        }
        return false;
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            byte[] scanRecord = result.getScanRecord().getBytes();
            if (parseAndMatchBeacon(scanRecord)) {
                if (!isBeaconConnected) {
                    isBeaconConnected = true;
                    runOnUiThread(() -> {
                        updateButtonsForBeaconConnection();
                        Toast.makeText(Function.this, "Bluetooth is connected！", Toast.LENGTH_LONG).show();
                    });
                }
            }
        }

        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                byte[] scanRecord = result.getScanRecord().getBytes();
                if (parseAndMatchBeacon(scanRecord)) {
                    if (!isBeaconConnected) {
                        isBeaconConnected = true;
                        runOnUiThread(() -> updateButtonsForBeaconConnection());
                        break;
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private void updateButtonsForBeaconConnection() {
        LinearLayout container = findViewById(R.id.course_container);
        if (container == null) return;

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout courseItem = (LinearLayout) child;
                for (int j = 0; j < courseItem.getChildCount(); j++) {
                    View innerChild = courseItem.getChildAt(j);
                    if (innerChild instanceof Button) {
                        Button btn = (Button) innerChild;
                        String course = (String) btn.getTag();
                        if (course != null && (!attendanceMap.containsKey(course) || !attendanceMap.get(course))) {
                            btn.setText("Sign In");
                            btn.setBackgroundColor(0xFF2196F3);
                            btn.setEnabled(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (attendanceListener != null && currentUsername != null) {
            DatabaseReference attendanceRef = FirebaseDatabase.getInstance(DATABASE_URL)
                    .getReference("attendance")
                    .child(currentUsername);
            attendanceRef.removeEventListener(attendanceListener);
        }

            if (bluetoothLeScanner != null && scanCallback != null) {
           //     bluetoothLeScanner.stopScan(scanCallback);
            }
            if (scanHandler != null && scanRunnable != null) {
                scanHandler.removeCallbacks(scanRunnable);
            }

    }
}