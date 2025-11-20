package com.kirubas.alumniapp1;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class ViewAlumniActivity extends AppCompatActivity {

    EditText etSearch;
    Button btnSearch, btnNext, btnPrevious;
    TextView tvId, tvName, tvEmail, tvPhone, tvDepartment, tvGradYear;
    ImageView ivProfilePic;
    Button btnEdit, btnDelete;
    DatabaseHelper dbHelper;
    Cursor cursor;  // Holds search result cursor
    int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_alumni);

        etSearch = findViewById(R.id.etSearch);
        etSearch.setText("1");
        btnSearch = findViewById(R.id.btnSearch);

        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);

        tvId = findViewById(R.id.tvId);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvGradYear = findViewById(R.id.tvGradYear);
        ivProfilePic = findViewById(R.id.ivProfilePic);

        dbHelper = new DatabaseHelper(this);
        currentIndex = -1;

        btnSearch.setOnClickListener(v -> performSearch());
        btnNext.setOnClickListener(v -> moveNext());
        btnPrevious.setOnClickListener(v -> movePrevious());
        btnSearch.performClick();
        updateNavigationButtons();

        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v -> editCurrentRecord());
        btnDelete.setOnClickListener(v -> deleteCurrentRecord());


    }
    private void editCurrentRecord() {
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "No record to edit", Toast.LENGTH_SHORT).show();
            return;
        }
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        Intent intent = new Intent(this, AddAlumniActivity.class);
        intent.putExtra("edit_id", id);
        startActivity(intent);
    }

    private void deleteCurrentRecord() {
        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "No record to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));

        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete this profile?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    int count = db.delete("alumni", "id=?", new String[]{String.valueOf(id)});
                    db.close();
                    if (count > 0) {
                        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                        performSearch(); // Reload results
                    } else {
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void performSearch() {
        if (cursor != null) {
            cursor.close();
            cursor = null;
            currentIndex = -1;
        }

        String query = etSearch.getText().toString().trim();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (TextUtils.isEmpty(query)) {
            Toast.makeText(this, "Enter a value to search", Toast.LENGTH_SHORT).show();
            return;
        }

        // Search by id (number) or partial name or email or phone number
        String selection = "id = ? OR firstName LIKE ? OR lastName LIKE ? OR email LIKE ? OR phone LIKE ?";
        String likeQuery = "%" + query + "%";
        String[] selectionArgs = new String[]{query, likeQuery, likeQuery, likeQuery, likeQuery};

        cursor = db.query(
                "alumni",
                null,
                selection,
                selectionArgs,
                null, null,
                "id ASC"
        );

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "No record found", Toast.LENGTH_SHORT).show();
            clearDisplay();
            updateNavigationButtons();
            return;
        }

        currentIndex = 0;
        cursor.moveToPosition(currentIndex);
        displayCurrentRecord();
        updateNavigationButtons();
    }

    private void displayCurrentRecord() {
        if (cursor == null || cursor.getCount() == 0) {
            clearDisplay();
            return;
        }

        int id = cursor.isNull(cursor.getColumnIndexOrThrow("id")) ? 0 : cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        String firstName = cursor.isNull(cursor.getColumnIndexOrThrow("firstName")) ? "" : cursor.getString(cursor.getColumnIndexOrThrow("firstName"));
        String lastName = cursor.isNull(cursor.getColumnIndexOrThrow("lastName")) ? "" : cursor.getString(cursor.getColumnIndexOrThrow("lastName"));
        String email = cursor.isNull(cursor.getColumnIndexOrThrow("email")) ? "" : cursor.getString(cursor.getColumnIndexOrThrow("email"));
        String phone = cursor.isNull(cursor.getColumnIndexOrThrow("phone")) ? "" : cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        String department = cursor.isNull(cursor.getColumnIndexOrThrow("department")) ? "" : cursor.getString(cursor.getColumnIndexOrThrow("department"));
        String gradYearStr = cursor.isNull(cursor.getColumnIndexOrThrow("graduationYear")) ? "" : String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("graduationYear")));

        tvId.setText("ID: " + id);
        tvName.setText("Name: " + firstName + " " + lastName);
        tvEmail.setText("Email: " + email);
        tvPhone.setText("Phone: " + phone);
        tvDepartment.setText("Department: " + department);
        tvGradYear.setText("Graduation Year: " + gradYearStr);

        byte[] imgBytes = cursor.isNull(cursor.getColumnIndexOrThrow("profilePicture")) ?
                null : cursor.getBlob(cursor.getColumnIndexOrThrow("profilePicture"));

        if (imgBytes != null && imgBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
            ivProfilePic.setImageBitmap(bitmap);
        } else {
            ivProfilePic.setImageResource(R.drawable.ic_default_profile);
        }
    }


    private void moveNext() {
        if (cursor != null && currentIndex < cursor.getCount() - 1) {
            currentIndex++;
            cursor.moveToPosition(currentIndex);
            displayCurrentRecord();
            updateNavigationButtons();
        }
    }

    private void movePrevious() {
        if (cursor != null && currentIndex > 0) {
            currentIndex--;
            cursor.moveToPosition(currentIndex);
            displayCurrentRecord();
            updateNavigationButtons();
        }
    }

    private void updateNavigationButtons() {
        if (cursor == null || cursor.getCount() == 0) {
            btnNext.setEnabled(false);
            btnPrevious.setEnabled(false);
            return;
        }
        btnPrevious.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < cursor.getCount() - 1);
    }

    private void clearDisplay() {
        tvId.setText("");
        tvName.setText("");
        tvEmail.setText("");
        tvPhone.setText("");
        tvDepartment.setText("");
        tvGradYear.setText("");
        ivProfilePic.setImageResource(R.drawable.ic_default_profile);
    }

    @Override
    protected void onDestroy() {
        if (cursor != null) {
            cursor.close();
        }
        super.onDestroy();
    }
}
