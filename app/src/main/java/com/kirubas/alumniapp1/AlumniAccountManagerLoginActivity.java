package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class AlumniAccountManagerLoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_account_manager_login);

        etUsername = findViewById(R.id.etManagerUsername);
        etPassword = findViewById(R.id.etManagerPassword);
        btnLogin = findViewById(R.id.btnManagerLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate institution account manager credentials
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.query("institution", new String[]{"id"},
                    "accountManagerUsername=? AND accountManagerPassword=?",
                    new String[]{username, password}, null, null, null);

            if (c.moveToFirst()) {
                int institutionId = c.getInt(c.getColumnIndexOrThrow("id"));
                c.close();
                db.close();
                // Proceed to admin assignment screen with institutionId
                Intent intent = new Intent(this, InstitutionAdminAssignmentActivity.class);
                intent.putExtra("institution_id", institutionId);
                startActivity(intent);
                finish();
            } else {
                c.close();
                db.close();
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
