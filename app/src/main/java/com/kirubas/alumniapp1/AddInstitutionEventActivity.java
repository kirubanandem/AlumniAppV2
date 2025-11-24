package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddInstitutionEventActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etEventDate, etVenue;
    Button btnSaveEvent;

    int institutionId = -1; // will be passed via Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_institution_event);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etEventDate = findViewById(R.id.etEventDate);
        etVenue = findViewById(R.id.etVenue);
        btnSaveEvent = findViewById(R.id.btnSaveEvent);

        institutionId = getIntent().getIntExtra("institution_id", -1);
        if (institutionId == -1) {
            Toast.makeText(this, "Missing institution information", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Optional: Pre-fill event date with today's date
        etEventDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        btnSaveEvent.setOnClickListener(v -> saveEvent());
    }

    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String eventDate = etEventDate.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();

        if (title.isEmpty() || eventDate.isEmpty()) {
            Toast.makeText(this, "Title and event date are required", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("institutionId", institutionId);
        cv.put("title", title);
        cv.put("description", description);
        cv.put("eventDate", eventDate);
        cv.put("venue", venue);
        cv.put("createdAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long res = db.insert("institutionEvents", null, cv);
        db.close();

        if (res == -1) {
            Toast.makeText(this, "Failed to save event", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Event saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
