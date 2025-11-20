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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddAlumniActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> getImageResultLauncher;
    private ActivityResultLauncher<Intent> getCameraResultLauncher;

    EditText firstName, lastName, email, phone, department, gradYear;
    ImageView profilePic;
    Button btnSave, btnSelectImg, btnTakePhoto;

    byte[] profilePicBytes = null;
    int editId = -1; // default not editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alumni);

        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        department = findViewById(R.id.department);
        gradYear = findViewById(R.id.gradYear);
        profilePic = findViewById(R.id.profilePic);
        btnSave = findViewById(R.id.btnSave);
        btnSelectImg = findViewById(R.id.btnSelectImg);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        // Editing? Pre-fill all fields, including image
        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
            Cursor cursor = db.query("alumni", null, "id=?", new String[]{String.valueOf(editId)}, null, null, null);
            if (cursor.moveToFirst()) {
                firstName.setText(cursor.isNull(cursor.getColumnIndexOrThrow("firstName")) ? "" :
                        cursor.getString(cursor.getColumnIndexOrThrow("firstName")));
                lastName.setText(cursor.isNull(cursor.getColumnIndexOrThrow("lastName")) ? "" :
                        cursor.getString(cursor.getColumnIndexOrThrow("lastName")));
                email.setText(cursor.isNull(cursor.getColumnIndexOrThrow("email")) ? "" :
                        cursor.getString(cursor.getColumnIndexOrThrow("email")));
                phone.setText(cursor.isNull(cursor.getColumnIndexOrThrow("phone")) ? "" :
                        cursor.getString(cursor.getColumnIndexOrThrow("phone")));
                department.setText(cursor.isNull(cursor.getColumnIndexOrThrow("department")) ? "" :
                        cursor.getString(cursor.getColumnIndexOrThrow("department")));
                gradYear.setText(cursor.isNull(cursor.getColumnIndexOrThrow("graduationYear")) ? "" :
                        String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("graduationYear"))));

                byte[] imgBytes = cursor.isNull(cursor.getColumnIndexOrThrow("profilePicture")) ?
                        null : cursor.getBlob(cursor.getColumnIndexOrThrow("profilePicture"));

                if (imgBytes != null && imgBytes.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                    profilePic.setImageBitmap(bitmap);
                    profilePicBytes = imgBytes;
                } else {
                    profilePic.setImageResource(R.drawable.ic_default_profile);
                    profilePicBytes = null;
                }
            }
            cursor.close();
            db.close();
        }

        // Gallery image selection launcher
        getImageResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            setProfileImage(bitmap);
                        } catch (IOException e) {
                            Log.e("AddAlumniActivity", "Error loading image", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        // Camera photo capture launcher
        getCameraResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap photo = (Bitmap) extras.get("data");
                            if (photo != null) {
                                setProfileImage(photo);
                            } else {
                                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        btnSelectImg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            getImageResultLauncher.launch(intent);
        });

        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            } else {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    getCameraResultLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnSave.setOnClickListener(v -> {
            String fName = firstName.getText().toString().trim();
            String lName = lastName.getText().toString().trim();
            String emailStr = email.getText().toString().trim();
            String phoneStr = phone.getText().toString().trim();
            String dept = department.getText().toString().trim();
            String gradYearStr = gradYear.getText().toString().trim();

            if (fName.isEmpty()) {
                Toast.makeText(this, "First Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lName.isEmpty()) {
                Toast.makeText(this, "Last Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (emailStr.isEmpty()) {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (gradYearStr.isEmpty()) {
                Toast.makeText(this, "Graduation Year is required", Toast.LENGTH_SHORT).show();
                return;
            }

            int gradYearVal;
            try {
                gradYearVal = Integer.parseInt(gradYearStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid Graduation Year", Toast.LENGTH_SHORT).show();
                return;
            }

            ContentValues cv = new ContentValues();
            cv.put("firstName", fName);
            cv.put("lastName", lName);
            cv.put("email", emailStr);
            cv.put("phone", phoneStr);
            cv.put("department", dept);
            cv.put("graduationYear", gradYearVal);
            cv.put("profilePicture", profilePicBytes);

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                long result;
                if (editId != -1) {
                    // Edit mode: UPDATE
                    result = db.update("alumni", cv, "id=?", new String[]{String.valueOf(editId)});
                } else {
                    // New: INSERT
                    result = db.insert("alumni", null, cv);
                }
                db.close();

                if (result == -1) {
                    Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Alumni saved successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("AddAlumniActivity", "DB error", e);
            }
        });
    }

    // for camera permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                getCameraResultLauncher.launch(intent);
            }
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void setProfileImage(Bitmap bitmap) {
        profilePic.setImageBitmap(bitmap);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        profilePicBytes = outputStream.toByteArray();
    }
}
