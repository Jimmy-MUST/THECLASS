package com.example.theclass;

import android.content.Intent;
import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class Manage extends AppCompatActivity {

    private static HashMap<String, String> userDatabase = new HashMap<>();

    private static ArrayList<String> courseList = new ArrayList<>();

    static {
        userDatabase.put("admin", "00000");
        userDatabase.put("user1", "12345");
        userDatabase.put("user2", "54321");
        userDatabase.put("user3", "54321");
        userDatabase.put("user4", "54321");

        courseList.add("EIE444");
        courseList.add("EIE555");
        courseList.add("EIE666");
        courseList.add("EIE999");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage);

        updateUserList();

        Button btnAddUser = findViewById(R.id.btn_add_user);
        btnAddUser.setOnClickListener(v -> addNewUser());

        Button btnAddCourse = findViewById(R.id.btn_change_password);
        btnAddCourse.setText("Add Course");
        btnAddCourse.setOnClickListener(v -> addNewCourse());

        Button btnReturn = findViewById(R.id.btn_logout);
        btnReturn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void addNewUser() {
        EditText etNewUsername = findViewById(R.id.et_new_username);
        EditText etNewPassword = findViewById(R.id.et_new_password);

        String username = etNewUsername.getText().toString().trim();
        String password = etNewPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter name and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userDatabase.containsKey(username)) {
            Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
        } else {
            userDatabase.put(username, password);
            updateUserList();
            etNewUsername.setText("");
            etNewPassword.setText("");
            Toast.makeText(this, "User added successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private void addNewCourse() {
        EditText etNewCourse = findViewById(R.id.et_new_password2);
        String courseName = etNewCourse.getText().toString().trim();

        if (courseName.isEmpty()) {
            Toast.makeText(this, "Please enter course name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (courseList.contains(courseName)) {
            Toast.makeText(this, "Course already exists", Toast.LENGTH_SHORT).show();
        } else {
            courseList.add(courseName);
            etNewCourse.setText("");
            Toast.makeText(this, "Course added: " + courseName, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserList() {

        TextView tvUserList = findViewById(R.id.tv_user_list);
        StringBuilder listText = new StringBuilder("Users:\n");
        for (String user : userDatabase.keySet()) {
            listText.append("• ").append(user).append("\n");
        }
        tvUserList.setText(listText.toString());
    }

    public static boolean verifyLogin(String username, String password) {
        String storedPassword = userDatabase.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    public static ArrayList<String> getCourseList() {
        return courseList;
    }
}