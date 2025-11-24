package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AlumniInstitutionRequestActivity extends AppCompatActivity {

    ListView lvInstitutions;
    Button btnRequestJoin;
    EditText etDepartment, etFromYear, etToYear, etGradYear;

    ArrayList<String> institutionNames = new ArrayList<>();
    ArrayList<Integer> institutionIds = new ArrayList<>();
    int selectedInstitutionId = -1;
    int alumniId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_institution_request);

        lvInstitutions = findViewById(R.id.lvInstitutions);
        btnRequestJoin = findViewById(R.id.btnRequestJoin);

        etDepartment = findViewById(R.id.etDepartment);
        etFromYear = findViewById(R.id.etFromYear);
        etToYear = findViewById(R.id.etToYear);
        etGradYear = findViewById(R.id.etGradYear);

        alumniId = getIntent().getIntExtra("alumni_id", -1);
        if (alumniId == -1) {
            Toast.makeText(this, "Invalid alumni ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadInstitutions();

        lvInstitutions.setOnItemClickListener((parent, view, position, id) -> {
            selectedInstitutionId = institutionIds.get(position);
            lvInstitutions.setItemChecked(position, true);
        });

        btnRequestJoin.setOnClickListener(v -> sendJoinRequest());
    }

    private void loadInstitutions() {
        institutionNames.clear();
        institutionIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("institution", null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            institutionNames.add(c.getString(c.getColumnIndexOrThrow("name")));
            institutionIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, institutionNames);
        lvInstitutions.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lvInstitutions.setAdapter(adapter);
    }

    private void sendJoinRequest() {
        if (selectedInstitutionId == -1) {
            Toast.makeText(this, "Please select an institution", Toast.LENGTH_SHORT).show();
            return;
        }

        String dept = etDepartment.getText().toString().trim();
        String fromYearStr = etFromYear.getText().toString().trim();
        String toYearStr = etToYear.getText().toString().trim();
        String gradYearStr = etGradYear.getText().toString().trim();

        if (dept.isEmpty() || fromYearStr.isEmpty() || toYearStr.isEmpty() || gradYearStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all institution-specific details", Toast.LENGTH_SHORT).show();
            return;
        }

        int fromYear, toYear, gradYear;
        try {
            fromYear = Integer.parseInt(fromYearStr);
            toYear = Integer.parseInt(toYearStr);
            gradYear = Integer.parseInt(gradYearStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid year input", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if request or relation already exists
        Cursor c = db.query("alumniInstitution", null,
                "alumniId=? AND institutionId=?",
                new String[]{String.valueOf(alumniId), String.valueOf(selectedInstitutionId)},
                null, null, null);
        if (c.moveToFirst()) {
            Toast.makeText(this, "Join request already exists or you are already a member", Toast.LENGTH_SHORT).show();
            c.close();
            db.close();
            return;
        }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("alumniId", alumniId);
        cv.put("institutionId", selectedInstitutionId);
        cv.put("department", dept);
        cv.put("fromYear", fromYear);
        cv.put("toYear", toYear);
        cv.put("graduationYear", gradYear);
        cv.put("approved", 0); // pending approval
        cv.put("joinDate", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        long res = db.insert("alumniInstitution", null, cv);
        db.close();

        if (res == -1) {
            Toast.makeText(this, "Failed to send join request", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Join request sent successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
