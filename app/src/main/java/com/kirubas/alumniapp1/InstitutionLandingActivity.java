package com.kirubas.alumniapp1;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class InstitutionLandingActivity extends AppCompatActivity {

    int institutionId;
    int alumniId;

    ListView lvEvents, lvPolls;
    ArrayList<String> activeEventTitles = new ArrayList<>();
    ArrayList<Integer> activeEventIds = new ArrayList<>();
    ArrayList<String> activePollQuestions = new ArrayList<>();
    ArrayList<Integer> activePollIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institution_landing);

        lvEvents = findViewById(R.id.lvEvents);
        lvPolls = findViewById(R.id.lvPolls);

        institutionId = getIntent().getIntExtra("institution_id", -1);
        alumniId = getIntent().getIntExtra("alumni_id", -1);

        if (institutionId == -1 || alumniId == -1) {
            Toast.makeText(this, "Invalid institution or alumni ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadActiveEvents();
        loadActivePolls();

        lvEvents.setOnItemClickListener((parent, view, position, id) -> {
            int eventId = activeEventIds.get(position);
            Intent intent = new Intent(this, EventParticipationActivity.class);
            intent.putExtra("event_id", eventId);
            intent.putExtra("alumni_id", alumniId);
            startActivity(intent);
        });

        lvPolls.setOnItemClickListener((parent, view, position, id) -> {
            int pollId = activePollIds.get(position);
            Intent intent = new Intent(this, AlumniPollsActivity.class);
            intent.putExtra("poll_id", pollId);
            intent.putExtra("alumni_id", alumniId);
            intent.putExtra("institution_id", institutionId);
            startActivity(intent);
        });

        // Placeholder: Add more feature bindings here later
    }

    private void loadActiveEvents() {
        activeEventTitles.clear();
        activeEventIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Assuming status 'active' indicates ongoing/upcoming events
        Cursor c = db.query("institutionEvents",
                new String[]{"id", "title"},
                "institutionId=? AND status=?",
                new String[]{String.valueOf(institutionId), "active"},
                null, null, "eventDate ASC");

        while (c.moveToNext()) {
            activeEventIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
            activeEventTitles.add(c.getString(c.getColumnIndexOrThrow("title")));
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, activeEventTitles);
        lvEvents.setAdapter(adapter);
    }

    private void loadActivePolls() {
        activePollQuestions.clear();
        activePollIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Polls with status 'new' or 'active' to include ongoing polls
        Cursor c = db.query("institutionPolls",
                new String[]{"id", "question"},
                "institutionId=? AND (status=? OR status=?)",
                new String[]{String.valueOf(institutionId), "new", "active"},
                null, null, "createdAt DESC");

        while (c.moveToNext()) {
            activePollIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
            activePollQuestions.add(c.getString(c.getColumnIndexOrThrow("question")));
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, activePollQuestions);
        lvPolls.setAdapter(adapter);
    }
}
