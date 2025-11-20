package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class AdminUserActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnSaveAdmin;
    ListView lvAdmins;
    ArrayList<String> usernames = new ArrayList<>();
    ArrayList<Integer> adminIds = new ArrayList<>();
    int editAdminId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user);

        etUsername = findViewById(R.id.etAdminUsername);
        etPassword = findViewById(R.id.etAdminPassword);
        btnSaveAdmin = findViewById(R.id.btnSaveAdmin);
        lvAdmins = findViewById(R.id.lvAdmins);

        // If editing an admin user
        editAdminId = getIntent().getIntExtra("edit_admin_id", -1);
        if (editAdminId != -1) {
            populateForEdit(editAdminId);
        }

        btnSaveAdmin.setOnClickListener(v -> saveAdmin());

        lvAdmins.setOnItemLongClickListener((parent, view, position, id) -> {
            int adminId = adminIds.get(position);
            String username = usernames.get(position);

            new AlertDialog.Builder(AdminUserActivity.this)
                    .setTitle("Edit Password")
                    .setMessage("Edit password for admin: " + username + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        Intent intent = new Intent(AdminUserActivity.this, EditAdminPasswordActivity.class);
                        intent.putExtra("admin_id", adminId);
                        intent.putExtra("username", username);
                        startActivity(intent);
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAdmins(); // Always refresh admin list
    }

    private void populateForEdit(int id) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("adminUser", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToFirst()) {
            etUsername.setText(c.getString(c.getColumnIndexOrThrow("username")));
            etPassword.setText(c.getString(c.getColumnIndexOrThrow("password")));
        }
        c.close();
        db.close();
    }

    private void saveAdmin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password); // NOTE: Use strong hashing in production!

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long result;
        if (editAdminId != -1) {
            result = db.update("adminUser", cv, "id=?", new String[]{String.valueOf(editAdminId)});
        } else {
            result = db.insert("adminUser", null, cv);
        }
        db.close();

        if (result == -1) {
            Toast.makeText(this, "Failed to save admin user", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Admin user saved", Toast.LENGTH_SHORT).show();
            etUsername.setText("");
            etPassword.setText("");
            editAdminId = -1;
            loadAdmins();
        }
    }

    private void loadAdmins() {
        usernames.clear();
        adminIds.clear();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("adminUser", null, null, null, null, null, "username ASC");
        while (c.moveToNext()) {
            usernames.add(c.getString(c.getColumnIndexOrThrow("username")));
            adminIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, usernames);
        lvAdmins.setAdapter(adapter);
    }
}
