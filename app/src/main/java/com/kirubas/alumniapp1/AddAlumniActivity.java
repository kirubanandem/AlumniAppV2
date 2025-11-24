package com.kirubas.alumniapp1;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddAlumniActivity extends AppCompatActivity {

    EditText etUsername, etPassword, etFirstName, etLastName, etGender, etDOB, etEmail, etPhone;
    ImageView ivProfile;
    Button btnSave, btnSelectPhoto, btnTakePhoto;

    byte[] profilePicBytes = null;
    int editId = -1; // default to new profile

    private ActivityResultLauncher<Intent> getImageResultLauncher;
    private ActivityResultLauncher<Intent> getCameraResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alumni);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etGender = findViewById(R.id.etGender);
        etDOB = findViewById(R.id.etDateOfBirth);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);

        ivProfile = findViewById(R.id.ivProfile);
        btnSave = findViewById(R.id.btnSave);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            prefillAlumni(editId);
        }

        getImageResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            setProfileImage(bitmap);
                        } catch (IOException e) {
                            Log.e("AddAlumniActivity", "Failed loading image", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        getCameraResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap photo = result.getData().getExtras() != null ?
                                (Bitmap) result.getData().getExtras().get("data") : null;
                        if (photo != null) setProfileImage(photo);
                    }
                }
        );

        btnSelectPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            getImageResultLauncher.launch(intent);
        });

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            } else {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    getCameraResultLauncher.launch(cameraIntent);
                } else {
                    Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSave.setOnClickListener(v -> saveAlumni());
    }

    private void prefillAlumni(int alumniId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("alumni", null, "id=?", new String[]{String.valueOf(alumniId)}, null, null, null);
        if (c.moveToFirst()) {
            etUsername.setText(c.getString(c.getColumnIndexOrThrow("username")));
            etPassword.setText(c.getString(c.getColumnIndexOrThrow("password")));
            etFirstName.setText(c.getString(c.getColumnIndexOrThrow("firstName")));
            etLastName.setText(c.getString(c.getColumnIndexOrThrow("lastName")));
            etGender.setText(c.getString(c.getColumnIndexOrThrow("gender")));
            etDOB.setText(c.getString(c.getColumnIndexOrThrow("dateOfBirth")));
            etEmail.setText(c.getString(c.getColumnIndexOrThrow("email")));
            etPhone.setText(c.getString(c.getColumnIndexOrThrow("phone")));

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

    private boolean isUsernameOrEmailTaken(String username, String email) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("alumni", new String[]{"id"},
                "(username=? OR email=?) AND id!=?",
                new String[]{username, email, editId == -1 ? "-1" : String.valueOf(editId)},
                null, null, null);
        boolean exists = c.moveToFirst();
        c.close();
        db.close();
        return exists;
    }

    private void saveAlumni() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String gender = etGender.getText().toString().trim();
        String dob = etDOB.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isUsernameOrEmailTaken(username, email)) {
            Toast.makeText(this, "Username or Email already exists. Please choose another.", Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password); // IMPORTANT: hash in production!
        cv.put("firstName", firstName);
        cv.put("lastName", lastName);
        cv.put("gender", gender);
        cv.put("dateOfBirth", dob);
        cv.put("email", email);
        cv.put("phone", phone);
        cv.put("profilePicture", profilePicBytes);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long result;
        if (editId != -1) {
            result = db.update("alumni", cv, "id=?", new String[]{String.valueOf(editId)});
        } else {
            result = db.insert("alumni", null, cv);
        }
        db.close();

        if (result == -1) {
            Toast.makeText(this, "Failed to save profile (username/email may be duplicate)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(intent.resolveActivity(getPackageManager()) != null){
                getCameraResultLauncher.launch(intent);
            }
        } else{
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void setProfileImage(Bitmap bitmap){
        ivProfile.setImageBitmap(bitmap);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        profilePicBytes = outputStream.toByteArray();
    }
}
