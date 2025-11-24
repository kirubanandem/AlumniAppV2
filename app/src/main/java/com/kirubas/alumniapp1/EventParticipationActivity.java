package com.kirubas.alumniapp1;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class EventParticipationActivity extends AppCompatActivity {

    ListView lvAlumni;
    ArrayList<Integer> participationIds = new ArrayList<>();
    ArrayList<Integer> alumniIds = new ArrayList<>();
    ArrayList<String> alumniNames = new ArrayList<>();
    ArrayAdapter<String> adapter;

    int eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_participation);

        lvAlumni = findViewById(R.id.lvAlumni);

        eventId = getIntent().getIntExtra("event_id", -1);
        if (eventId == -1) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, alumniNames);
        lvAlumni.setAdapter(adapter);

        lvAlumni.setOnItemClickListener((parent, view, position, id) -> showStatusDialog(position));

        loadParticipants();
    }

    private void loadParticipants() {
        alumniNames.clear();
        participationIds.clear();
        alumniIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT ep.id as participationId, a.id as alumniId, a.firstName, a.lastName, ep.status " +
                "FROM eventParticipation ep " +
                "JOIN alumni a ON ep.alumniId = a.id " +
                "WHERE ep.eventId = ? " +
                "ORDER BY a.firstName ASC";
        Cursor c = db.rawQuery(query, new String[]{String.valueOf(eventId)});

        while (c.moveToNext()) {
            int participationId = c.getInt(c.getColumnIndexOrThrow("participationId"));
            int alumniId = c.getInt(c.getColumnIndexOrThrow("alumniId"));
            String firstName = c.getString(c.getColumnIndexOrThrow("firstName"));
            String lastName = c.getString(c.getColumnIndexOrThrow("lastName"));
            String status = c.getString(c.getColumnIndexOrThrow("status"));

            participationIds.add(participationId);
            alumniIds.add(alumniId);
            alumniNames.add(firstName + " " + lastName + " (" + status + ")");
        }
        c.close();
        db.close();
        adapter.notifyDataSetChanged();
    }

    private void showStatusDialog(int position) {
        String[] statuses = {"may_be", "attending", "not_attending", "cancelled", "attended"};
        final int[] currentStatusIndex = {0};

        // Extract current status to pre-select
        String item = alumniNames.get(position);
        for (int i = 0; i < statuses.length; i++) {
            if (item.toLowerCase().contains(statuses[i])) {
                currentStatusIndex[0] = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Participation Status")
                .setSingleChoiceItems(statuses, currentStatusIndex[0], (dialog, which) -> currentStatusIndex[0] = which)
                .setPositiveButton("Save", (dialog, which) -> saveStatus(position, statuses[currentStatusIndex[0]]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveStatus(int position, String status) {
        int participationId = participationIds.get(position);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("status", status);

        int updated = db.update("eventParticipation", cv, "id=?", new String[]{String.valueOf(participationId)});
        db.close();

        if (updated > 0) {
            Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
            loadParticipants();
        } else {
            Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
        }
    }
}
