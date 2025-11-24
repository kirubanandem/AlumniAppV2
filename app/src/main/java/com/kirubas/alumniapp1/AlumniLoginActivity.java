package com.kirubas.alumniapp1;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class AlumniLoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor c = db.query("alumni", new String[] { "id" },
                    "username=? AND password=?",
                    new String[] { username, password }, null, null, null);

            if (c.moveToFirst()) {
                int alumniId = c.getInt(c.getColumnIndexOrThrow("id"));
                c.close();
                db.close();

                // Check if alumni is joined to any institution
                db = dbHelper.getReadableDatabase();
                Cursor ci = db.query("alumniInstitution", new String[]{"institutionId"},
                        "alumniId=? AND approved=1", new String[]{String.valueOf(alumniId)},
                        null, null, null);

                if (ci.getCount() == 0) {
                    // Not joined, go to join request activity
                    Intent intent = new Intent(this, AlumniInstitutionRequestActivity.class);
                    intent.putExtra("alumni_id", alumniId);
                    startActivity(intent);
                } else {
                    // Already joined institutions, show list
                    Intent intent = new Intent(this, AlumniInstitutionsActivity.class);
                    intent.putExtra("alumni_id", alumniId);
                    startActivity(intent);
                }
                ci.close();
                db.close();
                finish();
            } else {
                c.close();
                db.close();
                Toast.makeText(this, "Invalid username/password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
