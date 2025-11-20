package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EditAdminPasswordActivity extends AppCompatActivity {

    EditText etCurrentPw, etNewPw, etConfirmPw;
    TextView tvAdminUser;
    Button btnUpdatePw;
    int adminId = -1;
    String username = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_admin_password);

        tvAdminUser = findViewById(R.id.tvAdminUser);
        etCurrentPw = findViewById(R.id.etCurrentPassword);
        etNewPw = findViewById(R.id.etNewPassword);
        etConfirmPw = findViewById(R.id.etConfirmNewPassword);
        btnUpdatePw = findViewById(R.id.btnUpdatePassword);

        adminId = getIntent().getIntExtra("admin_id", -1);
        username = getIntent().getStringExtra("username");
        if (adminId == -1 || username == null) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        tvAdminUser.setText("Admin: " + username);

        btnUpdatePw.setOnClickListener(v -> updatePassword());
    }

    private void updatePassword() {
        String currentPw = etCurrentPw.getText().toString();
        String newPw = etNewPw.getText().toString();
        String confirmPw = etConfirmPw.getText().toString();

        if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPw.equals(confirmPw)) {
            Toast.makeText(this, "New password and confirm password do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("adminUser", new String[]{"password"}, "id=?", new String[]{String.valueOf(adminId)}, null, null, null);
        if (c.moveToFirst()) {
            String dbPw = c.getString(c.getColumnIndexOrThrow("password"));
            if (!dbPw.equals(currentPw)) {
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                c.close();
                db.close();
                return;
            }
        } else {
            Toast.makeText(this, "Admin not found", Toast.LENGTH_SHORT).show();
            c.close();
            db.close();
            return;
        }
        c.close();
        db.close();

        // Update password
        SQLiteDatabase db2 = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("password", newPw);
        int rows = db2.update("adminUser", values, "id=?", new String[]{String.valueOf(adminId)});
        db2.close();
        if (rows > 0) {
            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
        }
    }
}
