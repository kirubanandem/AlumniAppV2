package com.kirubas.alumniapp1;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class EventManagementActivity extends AppCompatActivity {

    ListView lvEvents;
    Button btnAddEvent;
    ArrayList<Integer> eventIds = new ArrayList<>();
    ArrayList<String> eventTitles = new ArrayList<>();
    ArrayAdapter<String> adapter;

    String userType;
    int userId = -1;
    int institutionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_management);

        lvEvents = findViewById(R.id.lvEvents);
        btnAddEvent = findViewById(R.id.btnAddEvent);

        userType = getIntent().getStringExtra("user_type");
        if ("admin".equals(userType)) {
            userId = getIntent().getIntExtra("user_id", -1);
        } else if ("iaam".equals(userType)) {
            institutionId = getIntent().getIntExtra("institution_id", -1);
        }

        loadEvents();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, eventTitles);
        lvEvents.setAdapter(adapter);

        lvEvents.setOnItemClickListener((parent, view, position, id) -> {
            int eventId = eventIds.get(position);
            showEventOptionsDialog(eventId);
        });

        btnAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddInstitutionEventActivity.class);
            if ("admin".equals(userType)) {
                // Pass user or institution info if needed
            } else if ("iaam".equals(userType)) {
                intent.putExtra("institution_id", institutionId);
            }
            startActivity(intent);
        });
    }

    private void loadEvents() {
        eventIds.clear();
        eventTitles.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;

        if ("iaam".equals(userType)) {
            selection = "institutionId=?";
            selectionArgs = new String[]{String.valueOf(institutionId)};
        }

        Cursor c = db.query("institutionEvents", new String[]{"id", "title", "eventDate"},
                selection, selectionArgs, null, null, "eventDate DESC");
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow("id"));
            String title = c.getString(c.getColumnIndexOrThrow("title"));
            String eventDate = c.getString(c.getColumnIndexOrThrow("eventDate"));
            eventIds.add(id);
            eventTitles.add(title + " (" + eventDate + ")");
        }
        c.close();
        db.close();
    }

    private void showEventOptionsDialog(int eventId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an action");

        String[] options = {"View Participants", "Edit Event", "Delete Event"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    Intent intent = new Intent(this, EventParticipationActivity.class);
                    intent.putExtra("event_id", eventId);
                    startActivity(intent);
                    break;
                case 1:
                    Toast.makeText(this, "Edit Event feature to be implemented", Toast.LENGTH_SHORT).show();
                    // Redirect to edit event activity if implemented
                    break;
                case 2:
                    confirmDeleteEvent(eventId);
                    break;
            }
        });
        builder.show();
    }

    private void confirmDeleteEvent(int eventId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Yes", (dialog, which) -> deleteEvent(eventId))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteEvent(int eventId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows = db.delete("institutionEvents", "id=?", new String[]{String.valueOf(eventId)});
        db.close();

        if (rows > 0) {
            Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
            loadEvents();
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
