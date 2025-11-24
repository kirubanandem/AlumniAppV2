package com.kirubas.alumniapp1;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class InstitutionEventsActivity extends AppCompatActivity {

    ListView lvEvents;
    ArrayList<String> eventTitles = new ArrayList<>();
    ArrayList<String> eventDescriptions = new ArrayList<>();
    int institutionId;
    int alumniId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institution_events);

        lvEvents = findViewById(R.id.lvEvents);

        institutionId = getIntent().getIntExtra("institution_id", -1);
        alumniId = getIntent().getIntExtra("alumni_id", -1);

        if (institutionId == -1) {
            Toast.makeText(this, "Invalid Institution", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadEvents();
    }

    private void loadEvents() {
        eventTitles.clear();
        eventDescriptions.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Assuming you have an 'events' table with columns: id, institutionId, title, description, eventDate
        // Modify query as per your actual schema
        Cursor cursor = db.query("events", null,
                "institutionId=?", new String[]{String.valueOf(institutionId)},
                null, null, "eventDate DESC");

        while (cursor.moveToNext()) {
            eventTitles.add(cursor.getString(cursor.getColumnIndexOrThrow("title")));
            String desc = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            eventDescriptions.add(desc != null ? desc : "");
        }
        cursor.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, eventTitles);
        lvEvents.setAdapter(adapter);

        // Optional: show event descriptions in Toast on click
        lvEvents.setOnItemClickListener((parent, view, position, id) -> {
            String desc = eventDescriptions.get(position);
            if (!desc.isEmpty()) {
                Toast.makeText(this, desc, Toast.LENGTH_LONG).show();
            }
        });
    }
}
