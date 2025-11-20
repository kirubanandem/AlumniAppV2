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
import java.util.ArrayList;

public class AddAlumniActivity extends AppCompatActivity {
    Spinner spnInstitute;
    LinearLayout layoutUdfFields;
    ArrayList<Integer> instituteIds = new ArrayList<>();
    ArrayList<Integer> udfFieldIds = new ArrayList<>();
    ArrayList<EditText> udfEditTexts = new ArrayList<>();
    int selectedInstituteId = -1;

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

        // Views
        spnInstitute = findViewById(R.id.spnInstitute);
        layoutUdfFields = findViewById(R.id.layoutUdfFields);
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

        loadInstitutes();

        spnInstitute.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedInstituteId = instituteIds.get(pos);
                loadUdfFields(selectedInstituteId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Pre-fill if edit
        editId = getIntent().getIntExtra("edit_id", -1);
        if (editId != -1) {
            preFillEdit(editId);
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
                            Log.e("AddAlumniActivity", "Error loading image", e);
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        getCameraResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap photo = (Bitmap) extras.get("data");
                            if (photo != null) {
                                setProfileImage(photo);
                            }
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

        btnSave.setOnClickListener(v -> saveAlumni());
    }

    private void preFillEdit(int alumniId) {
        SQLiteDatabase db = new DatabaseHelper(this).getReadableDatabase();
        Cursor cursor = db.query("alumni", null, "id=?", new String[]{String.valueOf(alumniId)}, null, null, null);
        if (cursor.moveToFirst()) {
            firstName.setText(cursor.getString(cursor.getColumnIndexOrThrow("firstName")));
            lastName.setText(cursor.getString(cursor.getColumnIndexOrThrow("lastName")));
            email.setText(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            phone.setText(cursor.getString(cursor.getColumnIndexOrThrow("phone")));
            department.setText(cursor.getString(cursor.getColumnIndexOrThrow("department")));
            gradYear.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("graduationYear"))));
            selectedInstituteId = cursor.getInt(cursor.getColumnIndexOrThrow("institutionId"));

            // Set spinner
            if (!instituteIds.isEmpty()) {
                int idx = instituteIds.indexOf(selectedInstituteId);
                if (idx >= 0) spnInstitute.setSelection(idx);
            }

            byte[] imgBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("profilePicture"));
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
        loadUdfFields(selectedInstituteId);
        preFillUdfValues(alumniId);
    }

    private void preFillUdfValues(int alumniId) {
        // Load previously entered udf values if editing
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        for (int i = 0; i < udfFieldIds.size(); i++) {
            int fieldId = udfFieldIds.get(i);
            EditText et = udfEditTexts.get(i);
            Cursor c = db.query("userDefinedValues", new String[]{"value"},
                    "alumniId=? AND fieldId=?", new String[]{String.valueOf(alumniId), String.valueOf(fieldId)}, null, null, null);
            if (c.moveToFirst()) {
                et.setText(c.getString(c.getColumnIndexOrThrow("value")));
            }
            c.close();
        }
        db.close();
    }

    private void saveAlumni() {
        String fName = firstName.getText().toString().trim();
        String lName = lastName.getText().toString().trim();
        String emailStr = email.getText().toString().trim();
        String phoneStr = phone.getText().toString().trim();
        String dept = department.getText().toString().trim();
        String gradYearStr = gradYear.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty() || emailStr.isEmpty() || gradYearStr.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int gradYearVal;
        try { gradYearVal = Integer.parseInt(gradYearStr); }
        catch (NumberFormatException e) {
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
        cv.put("institutionId", selectedInstituteId);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long result;
        if (editId != -1) {
            result = db.update("alumni", cv, "id=?", new String[]{String.valueOf(editId)});
            if (result > 0) {
                // UDF values: update or insert as needed
                for (int i = 0; i < udfFieldIds.size(); i++) {
                    String udfVal = udfEditTexts.get(i).getText().toString().trim();
                    int fieldId = udfFieldIds.get(i);

                    ContentValues udfCv = new ContentValues();
                    udfCv.put("value", udfVal);

                    int updated = db.update("userDefinedValues", udfCv,
                            "alumniId=? AND fieldId=?",
                            new String[]{String.valueOf(editId), String.valueOf(fieldId)});
                    if (updated == 0) {
                        udfCv.put("alumniId", editId);
                        udfCv.put("fieldId", fieldId);
                        udfCv.put("institutionId", selectedInstituteId);
                        db.insert("userDefinedValues", null, udfCv);
                    }
                }
            }
        } else {
            result = db.insert("alumni", null, cv);
            if (result != -1) {
                long alumniId = result;
                for (int i = 0; i < udfFieldIds.size(); i++) {
                    String udfVal = udfEditTexts.get(i).getText().toString().trim();
                    ContentValues udfCv = new ContentValues();
                    udfCv.put("alumniId", alumniId);
                    udfCv.put("fieldId", udfFieldIds.get(i));
                    udfCv.put("institutionId", selectedInstituteId);
                    udfCv.put("value", udfVal);

                    db.insert("userDefinedValues", null, udfCv);
                }
            }
        }
        db.close();

        if (result == -1) {
            Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Alumni saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadInstitutes() {
        instituteIds.clear();
        ArrayList<String> names = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("institution", null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            names.add(c.getString(c.getColumnIndexOrThrow("name")));
            instituteIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnInstitute.setAdapter(adapter);
        if (!instituteIds.isEmpty()) selectedInstituteId = instituteIds.get(0);
    }

    private void loadUdfFields(int instituteId) {
        layoutUdfFields.removeAllViews();
        udfEditTexts.clear();
        udfFieldIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("userDefinedFields", null, "institutionId=?",
                new String[]{String.valueOf(instituteId)}, null, null, "fieldName ASC");
        while (c.moveToNext()) {
            int fieldId = c.getInt(c.getColumnIndexOrThrow("id"));
            String fieldName = c.getString(c.getColumnIndexOrThrow("fieldName"));

            TextView tv = new TextView(this);
            tv.setText(fieldName);
            EditText et = new EditText(this);
            et.setHint("Enter " + fieldName);

            layoutUdfFields.addView(tv);
            layoutUdfFields.addView(et);

            udfFieldIds.add(fieldId);
            udfEditTexts.add(et);
        }
        c.close();
        db.close();
    }

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
