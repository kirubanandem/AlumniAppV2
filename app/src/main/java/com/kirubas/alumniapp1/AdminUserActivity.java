package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class AdminUserActivity extends AppCompatActivity {

    EditText  etPassword, etAdminUsername,etAdminName;
    ImageView ivProfile;
    Button btnSaveAdmin, btnSelectPic;
    ListView lvAdmins;
    ArrayList<String> adminInfos = new ArrayList<>();
    ArrayList<Integer> adminIds = new ArrayList<>();
    byte[] profilePicBytes = null;
    int editAdminId = -1;

    private ActivityResultLauncher<Intent> getImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user);
        etAdminUsername=findViewById(R.id.etAdminUsername);
        etAdminUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { // lost focus
                String enteredUsername = etAdminUsername.getText().toString().trim();
                if (!enteredUsername.isEmpty()) {
                    if (isUsernameTaken(enteredUsername)) {
                        etAdminUsername.setError("Username already exists. Please choose another.");
                        btnSaveAdmin.setEnabled(false);
                    } else {
                        etAdminUsername.setError(null);
                        btnSaveAdmin.setEnabled(true);
                    }
                }
            }
        });

        etAdminUsername = findViewById(R.id.etAdminUsername);
        etAdminName = findViewById(R.id.etAdminName);
        etPassword = findViewById(R.id.etAdminPassword);
        ivProfile = findViewById(R.id.ivAdminProfile);
        btnSelectPic = findViewById(R.id.btnSelectProfilePic);
        btnSaveAdmin = findViewById(R.id.btnSaveAdmin);
        lvAdmins = findViewById(R.id.lvAdmins);

        getImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            ivProfile.setImageBitmap(bitmap);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                            profilePicBytes = outputStream.toByteArray();
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        btnSelectPic.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            getImageLauncher.launch(intent);
        });

        editAdminId = getIntent().getIntExtra("edit_admin_id", -1);
        if (editAdminId != -1) {
            populateForEdit(editAdminId);
        }

        btnSaveAdmin.setOnClickListener(v -> saveAdmin());

        lvAdmins.setOnItemLongClickListener((parent, view, position, id) -> {
            int adminId = adminIds.get(position);
            String username = adminInfos.get(position);

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
        loadAdmins();
    }

    private void populateForEdit(int id) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("adminUser", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        if (c.moveToFirst()) {
            etAdminUsername.setText(c.getString(c.getColumnIndexOrThrow("username")));
            etAdminName.setText(c.getString(c.getColumnIndexOrThrow("name")));
            etPassword.setText(c.getString(c.getColumnIndexOrThrow("password")));
            byte[] imgBytes = c.getBlob(c.getColumnIndexOrThrow("profilePicture"));
            if (imgBytes != null && imgBytes.length > 0) {
                Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                ivProfile.setImageBitmap(bmp);
                profilePicBytes = imgBytes;
            } else {
                ivProfile.setImageResource(R.drawable.ic_default_profile);
                profilePicBytes = null;
            }
        }
        c.close();
        db.close();
    }

    private void saveAdmin() {
        String username = etAdminUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etAdminName.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password); // NOTE: Use strong hashing in production!
        cv.put("name", name);
        cv.put("profilePicture", profilePicBytes);

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
            etAdminUsername.setText("");
            etAdminName.setText("");
            etPassword.setText("");
            ivProfile.setImageResource(R.drawable.ic_default_profile);
            profilePicBytes = null;
            editAdminId = -1;
            loadAdmins();
        }
    }

    private void loadAdmins() {
        adminInfos.clear();
        adminIds.clear();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("adminUser", null, null, null, null, null, "username ASC");
        while (c.moveToNext()) {
            String info = c.getString(c.getColumnIndexOrThrow("username")) +
                    " (" + c.getString(c.getColumnIndexOrThrow("name")) + ")";
            adminInfos.add(info);
            adminIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, adminInfos);
        lvAdmins.setAdapter(adapter);
    }
    private boolean isUsernameTaken(String username) {
        // If we're editing, allow current record's own username
        if (editAdminId != -1) {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.query("adminUser", new String[]{"id"},
                    "username=? AND id!=?",
                    new String[]{username, String.valueOf(editAdminId)},
                    null, null, null);
            boolean exists = c.moveToFirst();
            c.close();
            db.close();
            return exists;
        } else {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.query("adminUser", new String[]{"id"},
                    "username=?",
                    new String[]{username},
                    null, null, null);
            boolean exists = c.moveToFirst();
            c.close();
            db.close();
            return exists;
        }
    }

}
