package com.kirubas.alumniapp1;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    RadioGroup rgUserType;
    RadioButton rbAdmin, rbIAAM;
    EditText etUsername, etPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        rgUserType = findViewById(R.id.rgUserType);
        rbAdmin = findViewById(R.id.rbAdmin);
        rbIAAM = findViewById(R.id.rbIAAM);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            int checkedId = rgUserType.getCheckedRadioButtonId();
            if (checkedId == -1) {
                Toast.makeText(this, "Select user type", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = null;
            Intent intent = new Intent(this, ControlPanelActivity.class);

            if (checkedId == R.id.rbAdmin) {
                // Check adminUser table
                c = db.query("adminUser", new String[]{"id"},
                        "username=? AND password=?",
                        new String[]{username, password}, null, null, null);
                if (c.moveToFirst()) {
                    int adminId = c.getInt(c.getColumnIndexOrThrow("id"));
                    c.close();
                    db.close();
                    intent.putExtra("user_type", "admin");
                    intent.putExtra("user_id", adminId);
                    startActivity(intent);
                    finish();
                    return;
                }
            } else if (checkedId == R.id.rbIAAM) {
                // Check institution table for IAAM account manager
                c = db.query("institution", new String[]{"id"},
                        "accountManagerUsername=? AND accountManagerPassword=?",
                        new String[]{username, password}, null, null, null);
                if (c.moveToFirst()) {
                    int institutionId = c.getInt(c.getColumnIndexOrThrow("id"));
                    c.close();
                    db.close();
                    intent.putExtra("user_type", "iaam");
                    intent.putExtra("institution_id", institutionId);
                    startActivity(intent);
                    finish();
                    return;
                }
            }

            if (c != null) c.close();
            db.close();
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        });
    }
}
