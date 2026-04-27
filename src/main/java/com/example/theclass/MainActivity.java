package com.example.theclass;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText etName, etPassword;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.login_input1);
        etPassword = findViewById(R.id.login_input2);
        Button btnLogin = findViewById(R.id.login_button);

        btnLogin.setOnClickListener(v -> {
            String username = etName.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter name and password", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isValid = Manage.verifyLogin(username, password);

            if (!isValid) {
                Toast.makeText(this, "Wrong name or password", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent;


            if (username.equals("admin")) {
                intent = new Intent(this, Manage.class);
            } else {
                intent = new Intent(this, Function.class);
            }

            intent.putExtra("username", username);
            startActivity(intent);

            etName.setText("");
            etPassword.setText("");
        });
    }
}